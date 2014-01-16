package edu.washington.multir.eval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.argumentidentification.ArgumentIdentification;
import edu.washington.multir.argumentidentification.SententialInstanceGeneration;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.CorpusInformationSpecification.SentDocNameInformation.SentDocName;
import edu.washington.multir.data.Argument;
import edu.washington.multir.data.Extraction;
import edu.washington.multir.data.ExtractionAnnotation;
import edu.washington.multir.featuregeneration.FeatureGenerator;
import edu.washington.multir.sententialextraction.DocumentExtractor;


/**
 * This class is designed for a more accurate evaluation of 
 * extractors. The input is a set of previously annotated extractions
 * and extractor configuration. If the extractor extracts extractions
 * that are not in the input annotations then an exception is thrown
 * and the diff must be annotated and combined with the previous annotation
 * input. This ensures that we will return true precision of the extractor.
 * Rather than recall we will produce yield as the number of correct
 * extractions produced by the extractor.
 * @author jgilme1
 *
 */
public class ManualEvaluation {
	
	private static Set<String> targetRelations = null;
	
	public static void main (String[] args) throws ParseException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, SQLException, IOException{
		
		Options options = new Options();
		options.addOption("cis",true,"corpusInformationSpecification algorithm class");
		options.addOption("ai",true,"argumentIdentification algorithm class");
		options.addOption("sig",true,"sententialInstanceGeneration algorithm class");
		options.addOption("fg",true,"featureGeneration algorithm class");
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		List<String> remainingArgs = cmd.getArgList();
		
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		CorpusInformationSpecification cis = null;
		ArgumentIdentification ai = null;
		SententialInstanceGeneration sig = null;
		FeatureGenerator fg = null;
		
		
		String corpusInformationSpecificationName = cmd.getOptionValue("cis");
		String argumentIdentificationName = cmd.getOptionValue("ai");
		String sententialInstanceGenerationName = cmd.getOptionValue("sig");
		String featureGenerationName = cmd.getOptionValue("fg");
		
		if(corpusInformationSpecificationName != null){
			String corpusInformationSpecificationClassPrefix = "edu.washington.multir.corpus.";
			Class<?> c = cl.loadClass(corpusInformationSpecificationClassPrefix+corpusInformationSpecificationName);
			cis = (CorpusInformationSpecification) c.newInstance();
		}
		else{
			throw new IllegalArgumentException("corpusInformationSpecification Class Argument is invalid");
		}
		
		if(argumentIdentificationName != null){
			String argumentIdentificationClassPrefix = "edu.washington.multir.argumentidentification.";
			Class<?> c = cl.loadClass(argumentIdentificationClassPrefix+argumentIdentificationName);
			Method m = c.getMethod("getInstance");
			ai = (ArgumentIdentification) m.invoke(null);
		}
		else{
			throw new IllegalArgumentException("argumentIdentification Class Argument is invalid");
		}
		
		if(sententialInstanceGenerationName != null){
			String sententialInstanceClassPrefix = "edu.washington.multir.argumentidentification.";
			Class<?> c = cl.loadClass(sententialInstanceClassPrefix+sententialInstanceGenerationName);
			Method m = c.getMethod("getInstance");
			sig = (SententialInstanceGeneration) m.invoke(null);
		}
		else{
			throw new IllegalArgumentException("sententialInstanceGeneration Class Argument is invalid");
		}
		
		if(featureGenerationName != null){
			String featureGenerationClassPrefix = "edu.washington.multir.featuregeneration.";
			Class<?> c = cl.loadClass(featureGenerationClassPrefix+featureGenerationName);
			fg = (FeatureGenerator) c.newInstance();
		}
		else{
			throw new IllegalArgumentException("argumentIdentification Class Argument is invalid");
		}
		
		
		//remaining args are
		// 0 - TestCorpusDerbyDatabase
		// 1 - annotations input file
		// 2 - evaluation relations file
		
		String testCorpusDatabasePath = remainingArgs.get(0);
		String multirModelPath = remainingArgs.get(1);
		String annotationsInputFilePath = remainingArgs.get(2);
		String evaluationRelationsFilePath = remainingArgs.get(3);
		
		loadTargetRelations(evaluationRelationsFilePath);
		
		//load test corpus
		Corpus c = new Corpus(testCorpusDatabasePath,cis,true);
		DocumentExtractor de = new DocumentExtractor(multirModelPath,fg,ai,sig);
		List<Extraction> extractions = getExtractions(c,ai,sig,de);
		
		List<ExtractionAnnotation> annotations = loadAnnotations(annotationsInputFilePath);
		
		List<Extraction> diffExtractions = getDiff(extractions,annotations);
		
		//if there is a diff then don't evaluate algorithm yet
		if(diffExtractions.size() > 0){
			//output diff
			String diffOutputName = annotationsInputFilePath + ".diff";
			writeExtractions(diffExtractions,diffOutputName);
			throw new IllegalStateException("inputAnnotations do not include all of the extractions, tag the diff at "
					+ diffOutputName + " and merge with annotations");
		}
		else{
			eval(extractions,annotations);
		}
	}

	private static void loadTargetRelations(String evaluationRelationsFilePath) throws IOException {
		BufferedReader br= new BufferedReader( new FileReader(new File(evaluationRelationsFilePath)));
		String nextLine;
		targetRelations = new HashSet<String>();
		while((nextLine = br.readLine())!=null){
			targetRelations.add(nextLine.trim());
		}
		
		br.close();
	}

	private static void eval(List<Extraction> extractions,
			List<ExtractionAnnotation> annotations) {
		System.out.println("evaluating...");
		
		//sort extractions
		Collections.sort(extractions, new Comparator<Extraction>(){
			@Override
			public int compare(Extraction e1, Extraction e2) {
				return e1.getScore().compareTo(e2.getScore());
			}
			
		});
		Collections.reverse(extractions);
		
		for(Extraction extr: extractions){
			System.out.println(extr + "\t" + extr.getScore());
		}
		
		List<Pair<Double,Integer>> precisionYieldValues = new ArrayList<>();
		for(int i =1; i < extractions.size(); i++){
			Pair<Double,Integer> pr = getPrecisionYield(extractions.subList(0, i),annotations,targetRelations);
			precisionYieldValues.add(pr);
		}
		
		System.out.println("Precision and Yield");
		for(Pair<Double,Integer> p : precisionYieldValues){
			System.out.println(p.first + "\t" + p.second);
		}
	}

	private static Pair<Double, Integer> getPrecisionYield(List<Extraction> subList,
			List<ExtractionAnnotation> annotations, Set<String> relationSet) {
		
		int totalExtractions = 0;
		int correctExtractions = 0;
		
		for(Extraction e : subList){
			if(relationSet.contains(e.getRelation())){
				totalExtractions++;
				List<ExtractionAnnotation> matchingAnnotations = new ArrayList<>();
				for(ExtractionAnnotation ea : annotations){
					if(ea.getExtraction().equals(e)){
						matchingAnnotations.add(ea);
					}
				}
				if(matchingAnnotations.size() == 1){
					ExtractionAnnotation matchedAnnotation = matchingAnnotations.get(0);
					if(matchedAnnotation.getLabel()){
						correctExtractions++;
					}
				}
				else{
					StringBuilder errorStringBuilder = new StringBuilder();
					errorStringBuilder.append("There should be exactly 1 matching extraction in the annotation set\n");
					errorStringBuilder.append("There are " + matchingAnnotations.size() +" and they are listed below: ");
					for(ExtractionAnnotation ea : matchingAnnotations){
						errorStringBuilder.append(ea.getExtraction().toString()+"\n");
					}
					throw new IllegalArgumentException(errorStringBuilder.toString());
				}
			}
		}
		
		double precision = (totalExtractions == 0) ? 0.0 : ((double)correctExtractions /(double)totalExtractions);
		Pair<Double,Integer> p = new Pair<Double,Integer>(precision,correctExtractions);
		return p;
	}

	private static void writeExtractions(List<Extraction> diffExtractions,
			String diffOutputName) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(diffOutputName)));
		for(Extraction e : diffExtractions){
			bw.write(e.toString()+"\n");
		}
		bw.close();
	}

	private static List<Extraction> getDiff(List<Extraction> extractions,
			List<ExtractionAnnotation> annotations) {
		
		List<Extraction> extrsNotInAnnotations = new ArrayList<Extraction>();
		
		for(Extraction e : extractions){
			boolean inAnnotation = false;
			for(ExtractionAnnotation ea : annotations){
				Extraction annoExtraction = ea.getExtraction();
				if(annoExtraction.equals(e)){
					inAnnotation = true;
				}
			}
			if(!inAnnotation){
				extrsNotInAnnotations.add(e);
			}
		}
		
		
		return extrsNotInAnnotations;
	}

	private static List<ExtractionAnnotation> loadAnnotations(
			String annotationsInputFilePath) throws IOException {
		List<ExtractionAnnotation> extrAnnotations = new ArrayList<ExtractionAnnotation>();
		List<Extraction> extrs = new ArrayList<Extraction>();
		BufferedReader br = new BufferedReader(new FileReader(new File(annotationsInputFilePath)));
		String nextLine;
		boolean duplicates = false;
		while((nextLine = br.readLine())!=null){
			ExtractionAnnotation extrAnnotation = ExtractionAnnotation.deserialize(nextLine);
			if(extrs.contains(extrAnnotation.getExtraction())){
				System.err.println("DUPLICATE ANNOTATION: " + nextLine);
				duplicates = true;
			}
			else{
			  extrs.add(extrAnnotation.getExtraction());
			  extrAnnotations.add(extrAnnotation);
			}
		}
		
		if(duplicates){
			br.close();
			throw new IllegalArgumentException("Annotations file contains multiple instances of the same extraction");
		}
		
		br.close();
		return extrAnnotations;
	}

	private static List<Extraction> getExtractions(Corpus c,
			ArgumentIdentification ai, SententialInstanceGeneration sig,
			DocumentExtractor de) throws SQLException {
		List<Extraction> extrs = new ArrayList<Extraction>();
		Iterator<Annotation> docs = c.getDocumentIterator();
		while(docs.hasNext()){
			Annotation doc = docs.next();
			List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
			int sentenceCount =1;
			for(CoreMap sentence : sentences){				
				//argument identification
				List<Argument> arguments =  ai.identifyArguments(doc,sentence);
				//sentential instance generation
				List<Pair<Argument,Argument>> sententialInstances = sig.generateSententialInstances(arguments, sentence);
				for(Pair<Argument,Argument> p : sententialInstances){
					Triple<String,Double,Double> extrTriple = 
					de.extractFromSententialInstance(p.first, p.second, sentence, doc);
					if(extrTriple != null){
						String rel = extrTriple.first;
						if(targetRelations.contains(rel)){
							String docName = sentence.get(SentDocName.class);
							String senText = sentence.get(CoreAnnotations.TextAnnotation.class);
							Extraction e = new Extraction(p.first,p.second,docName,rel,sentenceCount,extrTriple.third,senText);
							extrs.add(e);
						}
					}
				}
				sentenceCount++;
			}
		}
		return getUniqueList(extrs);
	}

	private static List<Extraction> getUniqueList(List<Extraction> extrs) {
		List<Extraction> uniqueList = new ArrayList<Extraction>();
		
		for(Extraction extr: extrs){
			boolean unique = true;
			for(Extraction extr1: uniqueList){
				if(extr.equals(extr1)){
					unique =false;
				}
			}
			if(unique){
			 uniqueList.add(extr);
			}
		}
		return uniqueList;
	}
}