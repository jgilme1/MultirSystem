package edu.washington.multir.preprocess;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;
import edu.stanford.nlp.util.CoreMap;

public class CorpusPreprocessing {
	private static String options = "invertible=true,ptb3Escaping=true";
	private static Pattern ldcPattern = Pattern.compile("<DOCID>\\s+.+LDC");
	private static Pattern xmlParagraphPattern = Pattern.compile("<P>((?:[\\s\\S](?!<P>))+)</P>");
	private static LexedTokenFactory<CoreLabel> ltf = new CoreLabelTokenFactory(true);
	private static WordToSentenceProcessor<CoreLabel> sen = new WordToSentenceProcessor<CoreLabel>();

	public static void main(String[] args) throws IOException{

		String docPath = args[0];
		String documentString = FileUtils.readFileToString(new File(docPath));

		List<String> paragraphs = cleanDocument(documentString);
		List<List<CoreLabel>> sentences = new ArrayList<List<CoreLabel>>();
		List<CoreMap> coreMapSentences = new ArrayList<CoreMap>();
		for(String par: paragraphs){
			PTBTokenizer<CoreLabel> tok = new PTBTokenizer<CoreLabel>(
					new StringReader(par), ltf, options);
			List<CoreLabel> l = tok.tokenize();
			List<List<CoreLabel>> snts = sen.process(l);
			for(List<CoreLabel> snt: snts){
				sentences.add(snt);
			}
		}
		
		for(List<CoreLabel> sentence : sentences){
			String rawString = 
		}
		
//		int count =0;
//		while(count < sentences.size()){
//			List<CoreLabel> sent = sentences.get(count);
//			System.out.println("Sentence " + (count+1));
//			for(CoreLabel token : sent){
//				System.out.println(token.value());
//			}
//			count++;
//		}
		
		Annotation doc = new Annotation(coreMapSentences);
	}

	private static List<String> cleanDocument(String documentString) {
		Matcher m = ldcPattern.matcher(documentString);
		if(m.find()){
			return getXMLParagraphs(documentString);
		}else{
			return getParagraphs(documentString);
		}
	}

	private static List<String> getParagraphs(String documentString) {
		List<String> paragraphs = new ArrayList<String>();
		String[] ps = documentString.split("\\n{2,}");
		for(String p : ps){
			paragraphs.add(p);
		}
		return paragraphs;
	}

	private static List<String> getXMLParagraphs(String documentString) {
		Matcher m = xmlParagraphPattern.matcher(documentString);
		List<String> paragraphs = new ArrayList<String>();
		while(m.find()){
			String paragraph = m.group(1);
			paragraphs.add(paragraph);
		}
		return paragraphs;
	}
}
