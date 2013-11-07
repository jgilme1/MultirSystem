package edu.washington.multir.featuregeneration;

import java.util.List;

import edu.stanford.nlp.pipeline.Annotation;

public interface FeatureGenerator {

	public List<String> generateFeatures(Integer arg1StartOffset, Integer arg1EndOffset, 
			Integer arg2StartOffset, Integer arg2EndOffset, Annotation sentence, Annotation document );
}
