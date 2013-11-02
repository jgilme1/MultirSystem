package edu.washington.multir.corpus;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.LabelFactory;


public abstract class CorpusInformationSpecification {
	
	protected final List<SentInformationI> sentenceInformation;
	//private final List<DocumentInformationI> documentInformation;
	protected final List<TokenInformationI>  tokenInformation;
	private static final LabelFactory coreLabelFactory = CoreLabel.factory();


	public CorpusInformationSpecification(){
		sentenceInformation = new ArrayList<SentInformationI>();
		tokenInformation = new ArrayList<TokenInformationI>();
		sentenceInformation.add(sentGlobalIDInformatIoninstance);
		sentenceInformation.add(sentDocNameInformationInstance);
		sentenceInformation.add(sentTextInformationInstance);
		sentenceInformation.add(sentTokensInformationInstance);
	}
	
	private static final SentGlobalIDInformation sentGlobalIDInformatIoninstance = new SentGlobalIDInformation();
	private static final class SentGlobalIDInformation implements SentInformationI<Integer,Integer>{

		@Override
		public Integer read(Integer t) {
			return t;
		}

		@Override
		public Integer write(Integer t) {
			return t;
		}

		@Override
		public Class<? extends CoreAnnotation<Integer>> getAnnotationKey() {
			return SentGlobalID.class;
		}
		
	    private static class SentGlobalID implements CoreAnnotation<Integer>{
			@Override
			public Class<Integer> getType() {
				return Integer.class;
			}
	    }

		@Override
		public String name() {
			return "SENTID";
		}
	}
	
	private static final SentDocNameInformation sentDocNameInformationInstance  = new SentDocNameInformation();
    private static final class SentDocNameInformation implements SentInformationI<String,String>{
		@Override
		public String read(String t) {
			return t;
		}

		@Override
		public String write(String t) {
			return t;
		}

		@Override
		public Class<? extends CoreAnnotation<String>> getAnnotationKey() {
				return SentDocName.class;
		}
		
	    private static class SentDocName implements CoreAnnotation<String>{
			@Override
			public Class<String> getType() {
				return String.class;
			}
	    }

		@Override
		public String name() {
			return "DOCNAME";
		}
    }
    
	private static final SentTextInformation sentTextInformationInstance = new SentTextInformation();
	private static final class SentTextInformation implements SentInformationI<String,String>{
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

		@Override
		public String name() {
			return this.getClass().getSimpleName();
		}
	}
	
	private static final SentTokensInformation sentTokensInformationInstance = new SentTokensInformation();
	private static final class SentTokensInformation implements SentInformationI<List<CoreLabel>,String>{

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

		@Override
		public String name() {
			return this.getClass().getSimpleName();
		}
	}
}
