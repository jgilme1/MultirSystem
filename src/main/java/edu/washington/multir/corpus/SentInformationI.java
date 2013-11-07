package edu.washington.multir.corpus;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.util.CoreMap;

public interface SentInformationI{
	public void read(String s, CoreMap c);
	public String write(CoreMap c);
	public String name();
}
