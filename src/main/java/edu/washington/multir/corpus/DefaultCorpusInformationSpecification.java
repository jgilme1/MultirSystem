package edu.washington.multir.corpus;

import java.util.Arrays;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;

public class DefaultCorpusInformationSpecification extends
		CorpusInformationSpecification {
	
	public DefaultCorpusInformationSpecification(){
		super();
		tokenInformation.add(tokenNERInformationInstance);
	}
	
	private static final TokenNERInformation tokenNERInformationInstance = new TokenNERInformation();
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
		@Override
		public String name() {
			return this.getClass().getSimpleName();
		}
	}
}
