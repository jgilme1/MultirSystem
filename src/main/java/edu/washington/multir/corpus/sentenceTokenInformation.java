package edu.washington.multir.corpus;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;

public class sentenceTokenInformation extends SentenceInformationI<CoreAnnotations.TokensAnnotation> {

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
	public String write(List<CoreLabel> annotations) {
		return "Nothing yet";
	}


}
