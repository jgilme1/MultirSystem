package edu.washington.multir.preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.washington.multir.corpus.CorpusInformationSpecification;

public class PreParseProcessor {
	//private static PreParseProcessor instance = new PreParseProcessor();
	//public static PreParseProcessor getInstance() {return instance;}
	
	
	private final Properties props = new Properties();
	private final StanfordCoreNLP pipeline;
	
	public PreParseProcessor(){
		props.put("annotators", "pos,lemma,ner");
		props.put("thread", "8");
		props.put("sutime.binders","0");
		pipeline = new StanfordCoreNLP(props,false);
	}
	
	public Annotation preParseProcessDocument(DocumentData dd){
		
		List<String> sentenceTokens = dd.sentenceTokens;
		List<String> sentText = dd.sentText;
		List<Pair<Integer,Integer>> sentenceOffsets = dd.sentenceOffsets;
		int startSentId = dd.startSentId;
		List<List<Pair<Integer,Integer>>> tokenOffsets = dd.tokenOffsets;
	
		List<CoreMap> sentences = new ArrayList<>();
		for(int j =0; j < sentenceTokens.size(); j++){
			String tokenString = sentenceTokens.get(j);
			Annotation senAnno = new Annotation(sentText.get(j));
			
			Pair<Integer,Integer> sentOffsets = sentenceOffsets.get(j);
			Integer sentStartOffset = sentOffsets.first;
			Integer sentEndOffset = sentOffsets.second;
			
			senAnno.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, sentStartOffset);
			senAnno.set(CoreAnnotations.CharacterOffsetEndAnnotation.class,sentEndOffset);
			senAnno.set(CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID.class,startSentId+j);
			
			List<Pair<Integer,Integer>> sentenceTokenOffsets = tokenOffsets.get(j);
			
			System.err.println(startSentId+j);
			System.err.println(sentText.get(j));
			System.err.println(tokenString);
			
			List<CoreLabel> coreLabelTokens = new ArrayList<>();
			String[] tokens = tokenString.split("\\s+");
			for(int i =0; i < tokens.length; i++){
				String token = tokens[i];
				Pair<Integer,Integer> offsets = sentenceTokenOffsets.get(i);
				Integer startOffset = offsets.first;
				Integer endOffset = offsets.second;
				CoreLabel coreLabelToken = new CoreLabel();
				coreLabelToken.setWord(token);
				coreLabelToken.setBeginPosition(startOffset+sentStartOffset);
				coreLabelToken.setEndPosition(endOffset+sentStartOffset);
				coreLabelTokens.add(coreLabelToken);
			}
			
			if(coreLabelTokens.size() != tokens.length){
				throw new IllegalStateException("CoreLabel Token length should be equivalent to raw string token length");
			}
			
			senAnno.set(CoreAnnotations.TokensAnnotation.class, coreLabelTokens);
			sentences.add(senAnno);
		}

		Annotation doc = new Annotation(sentences);
		pipeline.annotate(doc);
		//process with appropriate annotators
		return doc;
	}
	
	
	private String convertDocument(Annotation doc){
		
		return "";
	}
	
	//intput should include:
	// 1 tokens file
	// 2 token offsets file
	// 3 sentence offsets file
	// 4 sentence text file
	// 5 target output NER file
	// 6 target output POS file
	// 7 number of processors
	public static void main(String[] args) throws IOException{
		
		
		Integer numProcessor = Integer.parseInt(args[6]);
		
		File tokenFile = new File(args[0]);
		File tokenOffsetsFile = new File(args[1]);
		File sentenceOffsetsFile = new File(args[2]);
		File sentenceTextFile = new File(args[3]);
		File outputNERFile = new File(args[4]);
		File outputPOSFile = new File(args[5]);
		
		
		BufferedReader tokenFileReader = new BufferedReader(new FileReader(tokenFile));
		BufferedReader tokenOffsetsFileReader = new BufferedReader(new FileReader(tokenOffsetsFile));
		BufferedReader sentenceOffsetsFileReader = new BufferedReader(new FileReader(sentenceOffsetsFile));
		BufferedReader sentenceTextFileReader = new BufferedReader(new FileReader(sentenceTextFile));

		BufferedWriter outputNERFileWriter = new BufferedWriter(new FileWriter(outputNERFile));
		BufferedWriter outputPOSFileWriter = new BufferedWriter(new FileWriter(outputPOSFile));

		
		String tokenLine;
		String tokenOffsetLine;
		String sentenceOffsetLine;
		String sentenceTextLine;
		List<String> listOfSentenceTokens = new ArrayList<>();
		List<Pair<Integer,Integer>> listOfSentenceOffsets = new ArrayList<>();
		List<List<Pair<Integer,Integer>>> listOfSentenceTokenOffsets = new ArrayList<>();
		List<String> listOfSentenceTexts = new ArrayList<>();
		
		List<Annotation> preParseProcessedDocuments = new ArrayList<>();
		List<DocumentData> documentData = new ArrayList<>();
		
		boolean timeToConstructAnnotationDocument = false;
		String lastDocument = "";
		Integer startSentID = 0;
		int lineCount = 0;
		long start = System.currentTimeMillis();
		while((tokenLine = tokenFileReader.readLine())!= null){
			tokenOffsetLine = tokenOffsetsFileReader.readLine();
			sentenceOffsetLine = sentenceOffsetsFileReader.readLine();
			sentenceTextLine = sentenceTextFileReader.readLine();
			
			//print each line
			printLine(tokenLine,tokenOffsetLine,sentenceOffsetLine,sentenceTextLine);
			
			//get information from token file
			String [] tokenFileValues = tokenLine.split("\t");
			Integer sentId = Integer.parseInt(tokenFileValues[0]);
			String docName = tokenFileValues[1];
			String tokens = tokenFileValues[2];
			
			
			//get information from offsets file
			String [] tokenOffsetFileValues = tokenOffsetLine.split("\t");
			String offsetsString = tokenOffsetFileValues[1];
			
			//get information from sentence offset line
			String [] sentenceOffsetFileValues = sentenceOffsetLine.split("\t");
			String sentenceOffsetsString = sentenceOffsetFileValues[1];
			
			//get information from sentence text line
			String [] sentenceTextFileValues = sentenceTextLine.split("\t");
			String sentenceTextString = sentenceTextFileValues[1];
			
			if(lineCount == 0){
				lastDocument = docName;
				startSentID = sentId;
			}
			if((!lastDocument.equals("")) && (!lastDocument.equals(docName))){
				
				timeToConstructAnnotationDocument = true;
			}
			
			if(timeToConstructAnnotationDocument){
				
				System.out.println("STORING PREVIOUS DOCUMENT as Document starting at sentid " + startSentID );
				//add Document Data
				documentData.add(new DocumentData(listOfSentenceTokens, listOfSentenceTokenOffsets,
						startSentID, listOfSentenceOffsets, listOfSentenceTexts));
				
				if(documentData.size() == 1000){
					//split up jobs by number of processors
					runInParallel(preParseProcessedDocuments,documentData,numProcessor);
					documentData = new ArrayList<DocumentData>();
					writeNERAndPOS(preParseProcessedDocuments,outputNERFileWriter,outputPOSFileWriter);
					preParseProcessedDocuments = new ArrayList<Annotation>();
				}
				
//				Annotation doc = pp.preParseProcessDocument(listOfSentenceTokens, listOfSentenceTokenOffsets,
//						startSentID, listOfSentenceOffsets, listOfSentenceTexts);
//				preParseProcessedDocuments.add(doc);
				
				
				timeToConstructAnnotationDocument = false;
				lastDocument = docName;
				startSentID = sentId;
				
				listOfSentenceTokens = new ArrayList<>();
				listOfSentenceTokenOffsets = new ArrayList<>();
				listOfSentenceTexts = new ArrayList<>();
				listOfSentenceOffsets = new ArrayList<>();
				
			}
			
			//parse and add values to lists
			
			// add sentence text
			listOfSentenceTexts.add(sentenceTextString);
			
			//add sentence offsets
			String[] sentenceOffsetValues = sentenceOffsetsString.split("\\s+");
			Integer startSentOffset = Integer.parseInt(sentenceOffsetValues[0]);
			Integer endSentOffset = Integer.parseInt(sentenceOffsetValues[1]);
			Pair<Integer,Integer> sentenceOffsets = new Pair<>(startSentOffset,endSentOffset);
			listOfSentenceOffsets.add(sentenceOffsets);
			
			//add token offsets
			String[] tokenOffsetStringValues = offsetsString.split("\\s+");
			List<Pair<Integer,Integer>> sentenceTokenOffsets = new ArrayList<>();
			for(String offsetPair : tokenOffsetStringValues){
				String[] offsetPairValues = offsetPair.split(":");
				Integer startOffset = Integer.parseInt(offsetPairValues[0]);
				Integer endOffset = Integer.parseInt(offsetPairValues[1]);
				Pair<Integer,Integer> tokenOffsets = new Pair<>(startOffset,endOffset);
				sentenceTokenOffsets.add(tokenOffsets);
			}
			listOfSentenceTokenOffsets.add(sentenceTokenOffsets);
			
			//add tokens
			listOfSentenceTokens.add(tokens);
			
			lineCount++;
			if(lineCount % 1000 == 0){
				//long now = System.currentTimeMillis();
				//System.out.println(lineCount + " lines processed ");
				//System.out.println("1000 lines processed in " + (now-start) + " milliseconds");
				//start = now;
			}
			
		}
		
		//save the last document too
		if(!listOfSentenceTokens.isEmpty()){
			
			//get Annotation Document
			documentData.add(new DocumentData(listOfSentenceTokens, listOfSentenceTokenOffsets,
					startSentID, listOfSentenceOffsets, listOfSentenceTexts));

			
			listOfSentenceTokens = new ArrayList<>();
			listOfSentenceTokenOffsets = new ArrayList<>();;
			listOfSentenceTexts = new ArrayList<>();
			listOfSentenceOffsets = new ArrayList<>();
		}
		
		runInParallel(preParseProcessedDocuments,documentData,numProcessor);
		documentData = new ArrayList<>();
		writeNERAndPOS(preParseProcessedDocuments,outputNERFileWriter,outputPOSFileWriter);
		preParseProcessedDocuments = new ArrayList<>();

		
		outputNERFileWriter.close();
		outputPOSFileWriter.close();
		tokenFileReader.close();
		tokenOffsetsFileReader.close();
		sentenceOffsetsFileReader.close();
		sentenceTextFileReader.close();
	}

	private static void printLine(String tokenLine, String tokenOffsetLine,
			String sentenceOffsetLine, String sentenceTextLine) {
		System.out.println(tokenLine);
		System.out.println(tokenOffsetLine);
		System.out.println(sentenceOffsetLine);
		System.out.println(sentenceTextLine);
	}

	private static void runInParallel(
			List<Annotation> preParseProcessedDocuments,
			List<DocumentData> documentData, Integer numProcessors) {
		
		Integer numDocuments = documentData.size();
		ExecutorService tp = java.util.concurrent.Executors.newFixedThreadPool(numProcessors);
		CompletionService<List<Annotation>> pool = new ExecutorCompletionService<>(tp);
		
		
		int lastIndex = 0;
		for(int j =1; j <= numProcessors; j++){
	      int newIndex = lastIndex + (numDocuments/numProcessors);
	      List<DocumentData> part;
	      if(j == numProcessors){
			  part = new ArrayList<DocumentData>(documentData.subList(lastIndex, numDocuments));
	      }else{
			  part = new ArrayList<DocumentData>(documentData.subList(lastIndex,newIndex));			    	  
	      }
		  Callable<List<Annotation>> c = new ThreadProcessDocument(part,new PreParseProcessor());
	      pool.submit(c);
		  lastIndex = newIndex;
		}
		
		long start = System.currentTimeMillis();
		
		try{
			for(int j =1; j <= numProcessors; j++){
				Future<List<Annotation>> fut = pool.poll(20, TimeUnit.MINUTES);
				List<Annotation> r = fut.get();
				preParseProcessedDocuments.addAll(r);
			}
			long now = System.currentTimeMillis();
			long millis = now-start;
			//System.out.println( "1000  docs processed in " + millis);
		}
		catch (Exception e){
			e.printStackTrace();
			System.err.println("CAUGHT EXCEPTION");
		}
		finally{
			tp.shutdownNow();
		}				
	}

	private static void writeNERAndPOS(
			List<Annotation> preParseProcessedDocuments,
			BufferedWriter outputNERFileWriter,
			BufferedWriter outputPOSFileWriter) throws IOException {
		//System.out.println("PRINTING");
		
		for(Annotation doc: preParseProcessedDocuments){
			//System.out.println("NEXT DOC");
			List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
			for(CoreMap sentence: sentences){
				Integer sentId = sentence.get(CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID.class);
				//System.out.println("SENTID = " + sentId);
				StringBuilder nerBuilder = new StringBuilder();
				StringBuilder posBuilder = new StringBuilder();
				
				nerBuilder.append(sentId);
				nerBuilder.append("\t");
				
				posBuilder.append(sentId);
				posBuilder.append("\t");
				
				for(CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)){
					nerBuilder.append(token.ner());
					nerBuilder.append(" ");
					posBuilder.append(token.get(CoreAnnotations.PartOfSpeechAnnotation.class));
					posBuilder.append(" ");
				}
				
				nerBuilder.setLength(nerBuilder.length()-1);
				posBuilder.setLength(posBuilder.length()-1);
				nerBuilder.append("\n");
				posBuilder.append("\n");
				
				outputNERFileWriter.write(nerBuilder.toString());
				outputPOSFileWriter.write(posBuilder.toString());
			}
		}
	}
	
	
	private static class DocumentData{
		public List<String> sentenceTokens;
		public List<List<Pair<Integer,Integer>>> tokenOffsets;
		public int startSentId;
		public List<Pair<Integer,Integer>> sentenceOffsets;
		public List<String> sentText;
		
		public DocumentData(
				List<String> sentenceTokens, List<List<Pair<Integer,Integer>>> tokenOffsets, int startSentId, 
				List<Pair<Integer,Integer>> sentenceOffsets, List<String> sentText ){
			//check for validity of all arguments.
			int sentenceTokensLength = sentenceTokens.size();
			int tokenOffsetsLength = tokenOffsets.size();
			int sentenceOffsetsLength = sentenceOffsets.size();
			int sentTextLength = sentText.size();
			
			if(!((sentenceTokensLength == tokenOffsetsLength)  &&
					(sentenceTokensLength == sentenceOffsetsLength) &&
					(sentenceTokensLength == sentTextLength))){
				throw new IllegalArgumentException("Inputs don't match up");
			}
			
			
			
			
			this.sentenceTokens = sentenceTokens;
			this.tokenOffsets = tokenOffsets;
			this.startSentId = startSentId;
			this.sentenceOffsets = sentenceOffsets;
			this.sentText = sentText;
		}
	}
	
	private static class ThreadProcessDocument implements Callable<List<Annotation>>{
		
		private List<DocumentData> docData;
		private PreParseProcessor pp;
		public ThreadProcessDocument(List<DocumentData> docData, PreParseProcessor pp){
			this.docData = docData;
			this.pp = pp;
		}

		@Override
		public List<Annotation> call() throws Exception {
			
			List<Annotation> preParseProcessedDocs = new ArrayList<>();
			for(DocumentData dd : docData){
				preParseProcessedDocs.add(pp.preParseProcessDocument(dd));
			}
			return preParseProcessedDocs;
		}

		
	}

}
