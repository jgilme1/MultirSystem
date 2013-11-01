package edu.washington.multir.argumentidentification;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.data.Argument;

public class DefaultArgumentIdentification implements ArgumentIdentification {

	@Override
	public List<Argument> identifyArguments(Annotation s, Annotation d) {
		List<Argument> arguments = new ArrayList<Argument>();
		String text = s.get(CoreAnnotations.TextAnnotation.class);
		Integer globalId = s.get(Corpus.SentGlobalID.class);
		List<CoreLabel> tokens =s.get(CoreAnnotations.TokensAnnotation.class);
		for(int i =1; i < tokens.size(); i ++){
			if ( i % 2 != 0){
				if(tokens.get(i).value().matches("[a-z]+")){
					arguments.add(new Argument(globalId,))
				}
			}
		}	
		return arguments;
	}
}
