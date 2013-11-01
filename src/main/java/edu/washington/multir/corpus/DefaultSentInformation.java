package edu.washington.multir.corpus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.LabelFactory;
import edu.stanford.nlp.util.ErasureUtils;

public class DefaultSentInformation {
	
	private static final LabelFactory coreLabelFactory = CoreLabel.factory();
	
	
	public static final SentTextInformation getSentTextInformation() {return sentTextInformationInstance;}
	private static final SentTextInformation sentTextInformationInstance = new SentTextInformation();
	private static final class SentTextInformation implements SentInformationI<String>{
		@Override
		public String read(String s) {
			return s;
		}

		@Override
		public String write(String t) {
			return t;
		}

		@Override
		public Class<? extends CoreAnnotation<String>> getAnnotationKey() {
			return CoreAnnotations.TextAnnotation.class;
		}
	}
	
	public static final SentTokensInformation getSentTokensInformation() {return sentTokensInformationInstance;}
	private static final SentTokensInformation sentTokensInformationInstance = new SentTokensInformation();
	private static final class SentTokensInformation implements SentInformationI<List<CoreLabel>>{

		@Override
		public List<CoreLabel> read(String s) {
			String[] tokens = s.split("\\s+");
			List<CoreLabel> tokenAnnotations = new ArrayList<CoreLabel>();
			for(String token: tokens){
				tokenAnnotations.add((CoreLabel) coreLabelFactory.newLabel(token));
			}
			return tokenAnnotations;
		}

		@Override
		public String write(List<CoreLabel> t) {
			StringBuilder tokens = new StringBuilder();
			for(CoreLabel l : t){
				tokens.append(l.originalText()+" ");
			}
			return tokens.substring(0, tokens.length()-1).toString();
		}
		@Override
		public Class<? extends CoreAnnotation<List<CoreLabel>>> getAnnotationKey() {
			return CoreAnnotations.TokensAnnotation.class;
		}
	}
	
	
	private static final class TokenNERInformation implements TokenInformationI{
		@Override
		public String read(String s) {
			if(!s.equals("0")){
				return s;
			}
			else{
				return null;
			}
		}
		@Override
		public String write(String t) {
			if(t == null){
				return "0";
			}
			else{
				return t;
			}
		}
		@Override
		public Class<? extends CoreAnnotation<String>> getAnnotationKey() {
			return CoreAnnotations.NamedEntityTagAnnotation.class;
		}
		
		@Override
		public List<String> getTokenSeparatedValues(String s) {
			return Arrays.asList(s.split("\\s+"));
		}
	}
}
