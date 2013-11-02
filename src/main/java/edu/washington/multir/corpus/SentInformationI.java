package edu.washington.multir.corpus;

import edu.stanford.nlp.ling.CoreAnnotation;

public interface SentInformationI<AT,DT>{
	AT read(DT t);
	DT write(AT t);
	Class<? extends CoreAnnotation<AT>> getAnnotationKey();
	String name();
}
