package edu.washington.multir.sententialextraction;

import java.io.File;
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
import edu.stanford.nlp.util.Triple;
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
import edu.washington.multir.argumentidentification.NELArgumentIdentification;
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
				Triple<String,Double,Double> relationScoreTriple = getPrediction(features,arg1,arg2,senText);
				if(relationScoreTriple !=null){
					String extractionString = arg1.getArgName() + " " + relationScoreTriple.first + " " + arg2.getArgName();
					extractions.add(new Pair<String,Double>(extractionString,relationScoreTriple.third));
				}
			}
		}
		
		for(Pair<String,Double> extr: extractions){
			String extrString = extr.first;
			Double score = extr.second;
			
			System.out.println(extrString + "\t" + score);
		}
	}
	
	public Triple<String,Double,Double> extractFromSententialInstance(Argument arg1, Argument arg2, CoreMap sentence, Annotation doc){
		String senText = sentence.get(CoreAnnotations.TextAnnotation.class);
		List<String> features = 
				fg.generateFeatures(arg1.getStartOffset(), arg1.getEndOffset(), arg2.getStartOffset(), arg2.getEndOffset(), sentence, doc);
		Triple<String,Double,Double> relationConfidenceTriple = getPrediction(features,arg1,arg2,senText);
		return relationConfidenceTriple;
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
	private Triple<String,Double,Double> getPrediction(List<String> features, Argument arg1,
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
		//System.out.println("Features:");
		for (String f : features) {
			//System.out.println(f);
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
		
		//System.out.println("Features...");
		int k = 0;
		for (int f : ftrset) {
			//System.out.print(f + " ");
			sv.ids[k++] = f;
		}
		//System.out.println();
		
		String relation = "";
		Double conf = 0.0;
		Parse parse = FullInference.infer(doc, scorer, params);
		

		//System.out.println(senText);
		//System.out.println(arg1.getArgName() + "\t" + arg2.getArgName());
		//System.out.println("Score = " +parse.score);
		int[] Yp = parse.Y;
		if (parse.Z[0] > 0) {
			relation = relID2rel.get(parse.Z[0]);
			Arrays.sort(parse.allScores[0]);
//			double secondHighestScore = parse.allScores[0][parse.allScores[0].length-2];
//			double combinedScore = parse.score + secondHighestScore;
			double combinedScore = parse.score;
			
			for(int i =0; i < parse.allScores[0].length-1; i++){
				double s = parse.allScores[0][i];
				if( s > 0.0){
					combinedScore +=s;
				}
			}
			double confidence = (combinedScore <= 0.0 || parse.score <= 0.0) ? .1 : (parse.score/combinedScore);
			if(combinedScore == parse.score && parse.score > 0.0){
				confidence = .001;
			}
			conf = confidence;
		} else {
			return null;
		}

		return new Triple<String,Double,Double>(relation,conf,parse.score);
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
		
		String testDir = args[1];
		File f = new File(args[1]);
		int count = 0;
		for(File doc : f.listFiles()){
			de.extractFromDocument(doc.getAbsolutePath());
			System.out.println("Processed file " + count);
			count ++;
		}		
	}

}
