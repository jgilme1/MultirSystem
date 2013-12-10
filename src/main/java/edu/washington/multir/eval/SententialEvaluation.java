package edu.washington.multir.eval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.washington.multir.argumentidentification.NERArgumentIdentification;
import edu.washington.multir.argumentidentification.NERSententialInstanceGeneration;
import edu.washington.multir.data.Argument;
import edu.washington.multir.featuregeneration.DefaultFeatureGenerator;
import edu.washington.multir.preprocess.CorpusPreprocessing;
import edu.washington.multir.sententialextraction.DocumentExtractor;

public class SententialEvaluation {
	
	private static Pattern argument1Pattern = Pattern.compile("\\[([^\\[\\]]+)\\]1");
	private static Pattern argument2Pattern = Pattern.compile("\\[([^\\[\\]]+)\\]2");
	
	
	private static Pattern punctPattern = Pattern.compile("\\S(\\s)[\\.!,?:;]");
	private static Pattern encloseQuotesPattern = Pattern.compile("(?:''|\\\")(\\s)[^(?:'')(?:\\\")\\s]+(?:\\s[^(?:'')(?:\\\")\\s]+)*(\\s)(?:''|\\\")");
	private static Pattern moveQuotesLeftPattern = Pattern.compile(".(\\s)(?:''|\\\")(?!\\S)");
	private static Pattern moveApostropheLeftPattern = Pattern.compile(".(\\s)'(?!')");

	private static Set<String> validRelations;
	
	private static DocumentExtractor de;
	
	private static List<String> cjParses;
	
	
	public static void main(String[] args) throws IOException, InterruptedException{
		
		//initialize cjParses
		cjParses = new ArrayList<>();
		loadCjParses(args[3]);
		
		//read in relations from mapping file
		validRelations = new HashSet<String>();
		loadRelations(args[0]);
		
		
		//load in annotations
		List<Label> annotations;
		String annotationFilePath = args[1];
		annotations = loadAnnotations(annotationFilePath);
		
		//get extractions
		de = new DocumentExtractor(args[2],
				new DefaultFeatureGenerator(), NERArgumentIdentification.getInstance(), NERSententialInstanceGeneration.getInstance());
		List<Extraction> extractions;
		extractions = extract(annotations);
		
		
		
		
		score(annotations,extractions);
		
		System.out.println("Number of annotations is " + annotations.size());
		
		
//		List<Label> annotations;
//		String annotationFilePath = args[1];
//		annotations = loadAnnotations(annotationFilePath);
//		
//		
//		writeCJParses(annotations,"sentential-by-relation.cjparses.input.txt");
		
	}
	
	


	private static void loadCjParses(String cjParseFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(cjParseFile)));
		String  nextLine;
		
		while((nextLine = br.readLine())!=null){
			cjParses.add(nextLine);
		}
		
		
		br.close();
	}




	private static void writeCJParses(List<Label> annotations,
			String outFileName) throws IOException {
		
		List<Pair<Integer,Annotation>> annotatedSentences = new ArrayList<>();
		for(Label l : annotations){
			Integer ID = l.ID;
			Annotation sen = CorpusPreprocessing.preParseProcess(l.sentence);
			Pair<Integer,Annotation> p = new Pair<>(ID,sen);
			annotatedSentences.add(p);
		}
		
		//serialize cj parses
		
		StringBuilder cjInput = new StringBuilder();
		Integer lastIndex = annotations.get(annotations.size()-1).ID;
		int annotatedSentencesIndex = 0;
		for(int i =0; i <= lastIndex; i++){
			
			Pair<Integer,Annotation> p = annotatedSentences.get(annotatedSentencesIndex);
			if(i == p.first){
				StringBuilder tokenStringBuilder = new StringBuilder();
				CoreMap sentence = p.second.get(CoreAnnotations.SentencesAnnotation.class).get(0);
				List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
				for(CoreLabel tok : tokens){
					tokenStringBuilder.append(tok.word());
					tokenStringBuilder.append(" ");
				}
				cjInput.append("<s> ");
				cjInput.append(tokenStringBuilder.toString().trim());
				cjInput.append(" </s>");
				cjInput.append("\n\n");
				annotatedSentencesIndex++;
			}
			
			//void sentence..
			else{
				cjInput.append("<s> ");
				cjInput.append("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX </s>");
				cjInput.append("\n\n");
			}
		}
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outFileName)));
		bw.write(cjInput.toString().trim());
		bw.close();
		
	}




	private static List<Extraction> extract(List<Label> annotations) throws IOException, InterruptedException {
		List<Extraction> extractions = new ArrayList<>();
		
		
		for(Label a : annotations){
			Extraction e= getExtraction(a);
			if( e != null){
				extractions.add(e);
			}
		}
		return extractions;
	}




	private static Extraction getExtraction(Label a) throws IOException, InterruptedException {
		
		
		
		//Annotation doc = CorpusPreprocessing.getTestDocumentFromRawString(a.sentence, "doc"+String.valueOf(a.ID));
		Annotation doc = CorpusPreprocessing.preParseProcess(a.sentence);
		String cjParse = cjParses.get(a.ID);
		CoreMap sentence = doc.get(CoreAnnotations.SentencesAnnotation.class).get(0);
		CorpusPreprocessing.postParseProcessSentence(sentence,cjParse);
		List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
		Argument arg1 = new Argument(a.r.arg1.getArgName(),a.r.arg1.getStartOffset(),a.r.arg1.getEndOffset());
		Argument arg2 = new Argument(a.r.arg2.getArgName(),a.r.arg2.getStartOffset(),a.r.arg2.getEndOffset());
		Pair<String,Double> result = de.extractFromSententialInstance(arg1, arg2, sentences.get(0), doc);
		
		if(result == null){
			return null;
		}
		
		//convert to extraction
		else{
			Extraction e = new Extraction();
			e.sentence = a.sentence;
			e.ID = a.ID;
			e.conf = result.second;
			
			Relation r = new Relation();
			r.rel = result.first;
			r.arg1 = arg1;
			r.arg2 = arg2;
			
			e.r = r;
			return e;
		}
	}




	private static void score(List<Label> annotations,
			List<Extraction> extractions) {
		
		
		int numberOfTotalCorrectLabels =0;
		int numberOfTotalCorrectExtractions = 0;
		int totalExtractions =0;
		
		for(Label l : annotations){
			if(l.b){
				numberOfTotalCorrectLabels++;
			}
		}
		
		for(Extraction e: extractions){
			//find Label
			Label matchingLabel = null;
			for(Label l : annotations){
				if(e.ID.equals(l.ID)){
					matchingLabel = l;
					break;
				}
			}
			
			//if there should be an extractoin
			if(matchingLabel.b){
				//if its the right extraction
				if(e.r.rel.equals(matchingLabel.r.rel)){
					numberOfTotalCorrectExtractions++;
				}
			}
			
			totalExtractions++;
		}
		
		
		double recall = (numberOfTotalCorrectLabels == 0)? 0.0 : (((double)numberOfTotalCorrectExtractions) / ((double)numberOfTotalCorrectLabels));
		double precision = (totalExtractions == 0) ? 0.0 : (((double)numberOfTotalCorrectExtractions) / ((double)totalExtractions));
		
		System.out.println("Recall = " + recall);
		System.out.println("Precision = " + precision);
	}




	private static void loadRelations(String mappingFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader( new File(mappingFile)));
		String firstLine;
		firstLine = br.readLine();
		Integer numRelations = Integer.parseInt(firstLine);
		
		int i =0;
		String nextLine;
		while(i < numRelations){
			nextLine = br.readLine();
			String[] lineValues = nextLine.split("\t");
			String rel = lineValues[1];
			validRelations.add(rel);
			i++;
		}
		br.close();
	}




	private static class Label{
		Relation r;
		boolean b;
		String sentence;
		Integer ID;
	}
	
	private static class Relation{
		String rel;
		Argument arg1;
		Argument arg2;
	}
	
	private static class Extraction{
		Relation r;
		String sentence;
		Integer ID;
		double conf;
	}

	
	private static List<Label> loadAnnotations(String annotationFilePath) throws IOException {
		List<Label> annotations = new ArrayList<>();
		
		BufferedReader br = new BufferedReader(new FileReader( new File(annotationFilePath)));
		String nextLine;
		
		Integer id = 0;
		while((nextLine = br.readLine()) !=null){
			String[] lineValues = nextLine.split("\t");
			String relString = lineValues[3];
			String label = lineValues[4];
			String annoSentence = lineValues[5];
			String argument1String = lineValues[6];
			String argument2String = lineValues[7];
			String tokenizedSentence = lineValues[8];
			
			
			//step 1: identify what occurrence of the string the arguments are.
			Integer argument1OccurrenceNumber = getArgumentOccurrence(annoSentence,argument1Pattern);
			Integer argument2OccurrenceNumber = getArgumentOccurrence(annoSentence,argument2Pattern);
//			System.out.println(argument1OccurrenceNumber);
//			System.out.println(argument2OccurrenceNumber);
			
			
			//step 2: convert tokenized sentence to raw sentence
			String rawSentence = convertTokenizedSentence(tokenizedSentence);
			
			
			//step 3: identify offsets of each argument
			Pair<Integer,Integer> argument1Offsets = getOffsetsOfArgument(argument1String,argument1OccurrenceNumber,rawSentence);
			Pair<Integer,Integer> argument2Offsets = getOffsetsOfArgument(argument2String,argument2OccurrenceNumber,rawSentence);
			
			
			if(argument1Offsets!=null && argument2Offsets!=null){
				
				//create Annotation object and store it
				Label a = new Label();
				a.ID = id;
				a.b = (label.equals("y") || (label.equals("indirect")))? true : false;
				a.sentence = rawSentence;
				Relation r = new Relation();
				r.rel = relString;
				r.arg1 = new Argument(argument1String,argument1Offsets.first,argument1Offsets.second);
				r.arg2 = new Argument(argument2String,argument2Offsets.first,argument2Offsets.second);
				a.r =r;
				
				
				//if OccurrenceNumbers are negative don't had to annotations set.. or if relatin wasn't trained on..
				if(isValidAnnotation(a)){
					annotations.add(a);
				}
			}
			
			id++;
		}
		
		br.close();
		
		return annotations;
	}


	private static String convertTokenizedSentence(String tokenizedSentence) {
	//	System.out.println("CONVERTING TOKENIZED STRING");
		try{
			String rawSentence = tokenizedSentence;
			if(tokenizedSentence.startsWith("\"") && tokenizedSentence.endsWith("\"")){
				rawSentence = tokenizedSentence.substring(1, tokenizedSentence.length()-1);				
			}
			rawSentence = applyReplacementPattern(rawSentence,punctPattern);
			rawSentence = applyReplacementPattern(rawSentence,encloseQuotesPattern);
			rawSentence = applyReplacementPattern(rawSentence,moveQuotesLeftPattern);
			rawSentence = applyReplacementPattern(rawSentence,moveApostropheLeftPattern);
			rawSentence = rawSentence.replaceAll("-LRB-\\s", "(");
			rawSentence = rawSentence.replaceAll("\\s-RRB-", ")");
			
			
//			System.out.println(tokenizedSentence);
//			System.out.println(rawSentence);
			return rawSentence;
		}
		catch(Exception e){
			System.err.println(e);
			return null;
		}
	}




	private static String applyReplacementPattern(String rawSentence,Pattern pattern) {

		Matcher m = pattern.matcher(rawSentence);
		if(m.find()){
			do{
				//change string and change matcher to point to new string
				int cumulativeLengthDifference =0;
				for(int i =1; i <= m.groupCount(); i++){
					int removalStart = m.start(i);
					int removalEnd = m.end(i);
					rawSentence = rawSentence.substring(0, removalStart-cumulativeLengthDifference) + rawSentence.substring(removalEnd-cumulativeLengthDifference);
					cumulativeLengthDifference += (removalEnd-removalStart);
				}
				m = pattern.matcher(rawSentence);
			}
			while(m.find());
		}
		return rawSentence;
	}




	private static boolean isValidAnnotation(Label a) {
		
		if(!validRelations.contains(a.r.rel)){
			return false;
		}
		if(a.sentence == null){
			return false;
		}
		return true;
	}


	private static Integer getArgumentOccurrence(String annoSentence, Pattern argPattern) {
//		System.out.println(annoSentence);
		Matcher m = argPattern.matcher(annoSentence);
		if(m.find()){
			String word = m.group(1);
			Integer startOffset = m.start(1);
			
//			System.out.println(word);
//			System.out.println(startOffset);
			//iterate over occurrence of word in sentence to find ith occurrence that matches the pattern
	        Pattern wordPattern = Pattern.compile(word);
	        Matcher wordPatternMatcher = wordPattern.matcher(annoSentence);
	        
	        int i =1;
	        while(wordPatternMatcher.find()){
	        	Integer nextWordStartOffset = wordPatternMatcher.start();
//	        	System.out.println(nextWordStartOffset);
	        	if(nextWordStartOffset.equals(startOffset)){
	        		break;
	        	}
	        	i++;
	        }
	        return i;
		}
		else{
			return -1;
		}
			
	}
	
	private static Pair<Integer,Integer> getOffsetsOfArgument(String argString, Integer occurenceNum, String rawSentence){
		Pattern p = Pattern.compile(argString);
		Matcher m = p.matcher(rawSentence);
		int i =0;
		while(m.find()){
			i++;
			if(i == occurenceNum){
				return new Pair<Integer,Integer>(m.start(),m.end());
			}
		}
		return null;
	}
}
