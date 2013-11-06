package edu.washington.multir.corpus;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.util.CoreMap;

public interface SentInformationI{
	void read(String s, CoreMap c);
	String write(CoreMap c);
	String name();
}
