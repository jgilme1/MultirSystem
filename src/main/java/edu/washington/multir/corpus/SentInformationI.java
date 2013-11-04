package edu.washington.multir.corpus;

import edu.stanford.nlp.ling.CoreAnnotation;

public interface SentInformationI<AT,DT>{
	AT readFromDb(DT t);
	DT writeToDb(AT t);
	DT readFromString(String t);
	Class<? extends CoreAnnotation<AT>> getAnnotationKey();
	String name();
}
