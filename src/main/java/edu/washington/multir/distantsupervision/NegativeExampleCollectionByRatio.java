package edu.washington.multir.distantsupervision;

import java.util.Collections;
import java.util.List;

import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.data.KBArgument;
import edu.washington.multir.knowledgebase.KnowledgeBase;

public class NegativeExampleCollectionByRatio extends NegativeExampleCollection{
	

	@Override
	public List<Pair<Triple<KBArgument, KBArgument, String>, Integer>> filter(
			List<Pair<Triple<KBArgument, KBArgument, String>, Integer>> negativeExamples,
			List<Pair<Triple<KBArgument, KBArgument, String>, Integer>> positiveExamples,
			KnowledgeBase kb, List<CoreMap> sentences) {
		
		Collections.shuffle(negativeExamples);
		return negativeExamples.subList(0,Math.min(negativeExamples.size(),(int)Math.floor(positiveExamples.size()*negativeToPositiveRatio)));
	}

	public static NegativeExampleCollection getInstance(double ratio) {
		NegativeExampleCollection nec = new NegativeExampleCollectionByRatio();
		nec.negativeToPositiveRatio = ratio;
		return nec;
	}
}
