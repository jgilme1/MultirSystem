package edu.washington.multir.sententialextraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.washington.multir.multiralgorithm.FullInference;
import edu.washington.multir.multiralgorithm.MILDocument;
import edu.washington.multir.multiralgorithm.Parse;
import edu.washington.multir.multiralgorithm.SparseBinaryVector;
import edu.washington.multir.multiralgorithm.Mappings;
import edu.washington.multir.multiralgorithm.Model;
import edu.washington.multir.multiralgorithm.Parameters;
import edu.washington.multir.multiralgorithm.Scorer;
import edu.washington.multir.preprocess.CorpusPreprocessing;

import edu.washington.multir.argumentidentification.ArgumentIdentification;
import edu.washington.multir.argumentidentification.NERArgumentIdentification;
import edu.washington.multir.argumentidentification.NERSententialInstanceGeneration;
import edu.washington.multir.argumentidentification.SententialInstanceGeneration;
import edu.washington.multir.data.Argument;
import edu.washington.multir.featuregeneration.DefaultFeatureGenerator;
import edu.washington.multir.featuregeneration.FeatureGenerator;
/**
 * An extractor that provides extractions
 * from a document based on a trained
 * Multir model. Based on the DEFT framework
 * @author jgilme1
 *
 */
public class DocumentExtractor {
	
	private FeatureGenerator fg;
	private ArgumentIdentification ai;
	private SententialInstanceGeneration sig;
	
	private String dir;
	private Mappings mapping;
	private Model model;
	private Parameters params;
	private Scorer scorer;
	
	private Map<Integer, String> relID2rel = new HashMap<Integer, String>();


	public DocumentExtractor(String pathToMultirFiles, FeatureGenerator fg,
			ArgumentIdentification ai, SententialInstanceGeneration sig){
		this.fg = fg;
		this.ai = ai;
		this.sig = sig;
		dir = pathToMultirFiles;
		try {
			mapping = new Mappings();
			mapping.read(dir + "/mapping");

			model = new Model();
			model.read(dir + "/model");

			params = new Parameters();
			params.model = model;
			params.deserialize(dir + "/params");

			scorer = new Scorer();
			
			for(String key :mapping.getRel2RelID().keySet()){
				Integer id = mapping.getRel2RelID().get(key);
				relID2rel.put(id, key);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void extractFromDocument(String pathToDocument) throws IOException, InterruptedException{
		
		Annotation doc = CorpusPreprocessing.getTestDocument(pathToDocument);
		List<Pair<String,Double>> extractions = new ArrayList<>();
		
		List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap s : sentences){
			String senText = s.get(CoreAnnotations.TextAnnotation.class);
			List<Argument> args = ai.identifyArguments(doc, s);
			List<Pair<Argument,Argument>> sigs = sig.generateSententialInstances(args, s);
			for(Pair<Argument,Argument> p : sigs){
				Argument arg1 = p.first;
				Argument arg2 = p.second;
				List<String> features = 
						fg.generateFeatures(arg1.getStartOffset(), arg1.getEndOffset(), arg2.getStartOffset(), arg2.getEndOffset(), s, doc);
				Pair<String,Double> relationConfidencePair = getPrediction(features,arg1,arg2,senText);
				if(relationConfidencePair !=null){
					String extractionString = arg1.getArgName() + " " + relationConfidencePair.first + " " + arg2.getArgName();
					extractions.add(new Pair<String,Double>(extractionString,relationConfidencePair.second));
				}
			}
		}
		
		for(Pair<String,Double> extr: extractions){
			String extrString = extr.first;
			Double confidence = extr.second;
			
			System.out.println(extrString + "\t" + confidence);
		}
	}

	/**
	 * Conver features and args to MILDoc
	 * and run Multir sentential extraction
	 * algorithm, return null if no extraction
	 * was predicted.
	 * @param features
	 * @param arg1
	 * @param arg2
	 * @return
	 */
	private Pair<String,Double> getPrediction(List<String> features, Argument arg1,
			Argument arg2, String senText) {
		
		MILDocument doc = new MILDocument();
		
		doc.arg1 = arg1.getArgName();
		doc.arg2 = arg2.getArgName();
		doc.Y = new int[1];
		doc.numMentions = 1;// sentence level prediction
		doc.setCapacity(1);
		SparseBinaryVector sv = doc.features[0] = new SparseBinaryVector();
		
		
		SortedSet<Integer> ftrset = new TreeSet<Integer>();
		int totalfeatures = 0;
		int featuresInMap = 0;
		for (String f : features) {
			totalfeatures ++;
			int ftrid = mapping.getFeatureID(f, false);
			if (ftrid >= 0) {
				featuresInMap++;
				ftrset.add(ftrid);
			}
		}
		//if( arg1.getArgName().equals("China") && (arg2.getArgName().equals("Beijing"))){
//			System.out.println("Total Features = " + totalfeatures);
//			for(String f : features){
//				System.out.println(f);
//			}
//			System.out.println("Num features in training = " + featuresInMap);
		//}
		
		sv.num = ftrset.size();
		sv.ids = new int[sv.num];
		
		int k = 0;
		for (int f : ftrset) {
			System.out.println(f);
			sv.ids[k++] = f;
		}
		
		String relation = "";
		Double conf = 0.0;
		Parse parse = FullInference.infer(doc, scorer, params);

		System.out.println(senText);
		System.out.println(arg1.getArgName() + "\t" + arg2.getArgName());
		System.out.println("Score = " +parse.score);
		int[] Yp = parse.Y;
		if (parse.Z[0] > 0) {
			relation = relID2rel.get(parse.Z[0]);
//			Arrays.sort(parse.allScores[0]);
//			double secondHighestScore = parse.allScores[0][parse.allScores[0].length-2];
//			double combinedScore = parse.score + secondHighestScore;
//			double confidence = (combinedScore <= 0.0 || parse.score <= 0.0) ? .1 : (parse.score/combinedScore);
//			conf = confidence;
		} else {
			return null;
		}

		return new Pair<String,Double>(relation,conf);
	}
	
	
	/**
	 * args[0] is path to Multir Files directory
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException{
		
		DocumentExtractor de = new DocumentExtractor(args[0],
				new DefaultFeatureGenerator(), NERArgumentIdentification.getInstance(), NERSententialInstanceGeneration.getInstance());
		
		de.extractFromDocument("/homes/gws/jgilme1/XIN_ENG_20021028.0184.LDC2007T07.sgm");
		
	}

}
