package edu.washington.multir.corpus;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.LabelFactory;
import edu.stanford.nlp.pipeline.Annotation;
import java.util.List;

public abstract class SentenceInformationI<A extends CoreAnnotation<List<CoreLabel>>> {
	 A a;
	 LabelFactory coreLabelFactory = CoreLabel.factory();
	 public abstract List<CoreLabel> read(String s);
	 
	 public Class<?> getCoreAnnotationKey(){
		 return a.getClass();
	 }
	
	/**
	 * This method should return the same String representation 
	 * that is used to deserialize in the read method
	 * @param a
	 * @param annotationType
	 * @return
	 */
	 public abstract String write(List<CoreLabel> annotations);
}
