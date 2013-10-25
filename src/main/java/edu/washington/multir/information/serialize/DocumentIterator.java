package edu.washington.multir.information.serialize;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.LabelFactory;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

public class DocumentIterator implements Iterator<Annotation>{

	File parentDir;
	LineIterator sentenceTextLineIterator;
	LineIterator sentenceTokenLineIterator;
	BufferedReader metaFileReader;
	LabelFactory coreLabelFactory;
	
	private static int markLimit = 10000;
	
	public DocumentIterator(String pathString) throws IOException{
		parentDir = new File(pathString);
		sentenceTextLineIterator = FileUtils.lineIterator(new File(parentDir.getPath()+"/sentences.text"));
		sentenceTokenLineIterator = FileUtils.lineIterator(new File(parentDir.getPath()+"/sentences.tokens"));
		metaFileReader = new BufferedReader(new FileReader(parentDir.getPath()+"/sentences.meta"));
		coreLabelFactory = CoreLabel.factory();
	}
	
	@Override
	public boolean hasNext() {
		return sentenceTextLineIterator.hasNext();
	}

	@Override
	public Annotation next() {
		List<CoreMap> sentences = new ArrayList<CoreMap>();

		try {
			String nextLine = metaFileReader.readLine();
			String docName = nextLine.split("\t")[2];
			while((nextLine != null) && nextLine.contains(docName)){
				String[] values = nextLine.split("\t");
				int globalSentId = Integer.parseInt(values[0]);
				Annotation sentence = getSentenceText(sentenceTextLineIterator.nextLine(),globalSentId);
				annotateSentenceWithTokens(sentenceTokenLineIterator.nextLine(),globalSentId,sentence);
				sentences.add(sentence);
				//mark start of new Document
				metaFileReader.mark(markLimit);
				nextLine = metaFileReader.readLine();
			}
			
			
			//reset metaFileReader to beginning of following document
			metaFileReader.reset();
		}
		catch(IOException e){
			System.exit(0);
		}

		Annotation doc = new Annotation(sentences);
		return doc;
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}
	
	private Annotation getSentenceText(String sentenceTextLine,int globalSentId){
		String[] values = sentenceTextLine.split("\t");
		int lineGlobalSentId = Integer.parseInt(values[0]);
		if(globalSentId != lineGlobalSentId){
			throw new IllegalArgumentException("sent ids do not match");
		}
		else{
			return new Annotation(values[1]);
		}
	}
	
	private void annotateSentenceWithTokens(String sentenceTokensLine, int globalSentId, Annotation sentence){
		String [] values = sentenceTokensLine.split("\t");
		int lineGlobalSentId = Integer.parseInt(values[0]);
		if(globalSentId != lineGlobalSentId){
			throw new IllegalArgumentException("sent ids do not match");
		}
		else{
			List<CoreLabel> sentTokens = new ArrayList<CoreLabel>();
			for(String s : values[1].split(" ")){
				sentTokens.add((CoreLabel) coreLabelFactory.newLabel(s));
			}
			sentence.set(CoreAnnotations.TokensAnnotation.class, sentTokens);
		}
	}
	
	
	public static void main(String[] args) throws IOException{
		DocumentIterator di = new DocumentIterator(args[0]);
		int dIndex = 0;
		while(di.hasNext()){
			Annotation d = di.next();
			for(Class<?> key: d.keySet()){
				System.out.println(key.getCanonicalName());
			}
			System.out.println("Document " + dIndex + " has ");
			System.out.println(d.get(CoreAnnotations.SentencesAnnotation.class).size());
			System.out.println("sentences");
			dIndex ++;
		}
	}
}
