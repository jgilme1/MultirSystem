package edu.washington.multir.argumentidentification;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multir.data.Argument;
import edu.washington.multir.knowledgebase.KnowledgeBase;

/**
 * Has reference to the KB in order to find IDs.
 * @author jgilme1
 *
 */
public class DefaultArgumentIdentification implements ArgumentIdentification {

	private static String[] relevantNERTypes = {"ORGANIZATION", "PERSON", "LOCATION"};
	
	private static DefaultArgumentIdentification instance = null;
	
	private KnowledgeBase kb = null;
	
	private DefaultArgumentIdentification(){}
	public static DefaultArgumentIdentification getInstance(){
		if(instance == null) instance = new DefaultArgumentIdentification();
		return instance;
		}
	
	
	@Override
	public List<Argument> identifyArguments(Annotation d, CoreMap s) {
		int globalSentId = s.get(SentGlobalID.class);
		List<Argument> arguments = new ArrayList<Argument>();
		List<CoreLabel> tokens = s.get(CoreAnnotations.TokensAnnotation.class);
		List<List<CoreLabel>> argumentTokenSpans = new ArrayList<List<CoreLabel>>();
		
		for(int i =0; i < tokens.size();){
			if (isRelevant(tokens.get(i))){
				List<CoreLabel> tokenSequence = getRelevantTokenSequence(tokens,i);
				argumentTokenSpans.add(tokenSequence);
				i += tokenSequence.size();
			}
			else{
				i++;
			}
		}
		for(List<CoreLabel> argumentTokenSpan : argumentTokenSpans){
			StringBuilder argumentSB = new StringBuilder();
			for(CoreLabel token: argumentTokenSpan){
				argumentSB.append(token.value());
				argumentSB.append(" ");
			}
			String argumentString = argumentSB.toString().trim();
			//check for argumentString in KB

			if(kb.getEntityMap().containsKey(argumentString)){
				List<String> matchingKBIDs = kb.getEntityMap().get(argumentString);
				for(String kbID : matchingKBIDs){
					Argument arg = new Argument(globalSentId,argumentString,kbID);
					arguments.add(arg);
				}
			}
		}
		return arguments;
	}
	
	private List<CoreLabel> getRelevantTokenSequence(List<CoreLabel> tokens,
			int i) {
		List<CoreLabel> tokenSequence = new ArrayList<CoreLabel>();
		tokenSequence.add(tokens.get(i));
		String ner = tokens.get(i).get(CoreAnnotations.NamedEntityTagAnnotation.class);
		i++;
		while(i < tokens.size()){
			String nextNer = tokens.get(i).get(CoreAnnotations.NamedEntityTagAnnotation.class);
			if(ner.equals(nextNer)){
				tokenSequence.add(tokens.get(i));
			}
			i++;
		}
		return tokenSequence;
	}
	
	private boolean isRelevant(CoreLabel token) {
		String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
		for(String relevantNER : relevantNERTypes){
			if(relevantNER.equals(ner)){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void setKB(KnowledgeBase kb) {
		this.kb = kb;
	}
}
