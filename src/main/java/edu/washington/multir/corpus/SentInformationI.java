package edu.washington.multir.corpus;

import edu.stanford.nlp.ling.CoreAnnotation;

public interface SentInformationI<T>{
	T read(String s);
	String write(T t);
	Class<? extends CoreAnnotation<T>> getAnnotationKey();
}
