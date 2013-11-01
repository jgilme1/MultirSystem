package edu.washington.multir.corpus;

import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotation;

public interface TokenInformationI {
	String read(String s);
	String write(String s);
	Class<? extends CoreAnnotation<String>> getAnnotationKey();
	List<String> getTokenSeparatedValues(String s);
}
