package edu.washington.multir.argumentidentification;

import java.util.List;

import edu.stanford.nlp.pipeline.Annotation;
import edu.washington.multir.data.Argument;

/**
 * The burden to make sure that the Annotation scheme
 * defined in the corpus is compatible with the algorithm
 * implemented in ArgumentIdentification.
 * @author jgilme1
 *
 */
public interface ArgumentIdentification {
	
	List<Argument> identifyArguments(Annotation s, Annotation d);
}
