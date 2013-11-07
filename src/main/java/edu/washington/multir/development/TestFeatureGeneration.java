package edu.washington.multir.development;


import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;

public class TestFeatureGeneration {
	
	public static void main(String[] args){
		
		//artificially create the annotations of a sentence document that was in the original paper
		//and make sure the features are the same.
		
		Annotation multirAnnotation;
		Annotation originalSentence = new Annotation("Hansen, a former NASA historian who interviewed Neil Armstrong for 50 hours in preparing ''First Man,'' recounts Armstrong's boyhood in Ohio, his combat missions over Korea as a naval aviator," +
				" the 1966 Gemini 8 mission, and the Apollo 11 triumph three years later.");
		
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		props.put("ner.useSUTime", "false");
		props.put("sutime.binders","0");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		pipeline.annotate(originalSentence);
		
		
		System.out.println(originalSentence.get(CoreAnnotations.TextAnnotation.class));
		
		//text is correct
		System.out.println("KEYS");
		for( Class<?>c :originalSentence.keySet()){
			System.out.println(c);
		}
		List<CoreMap> sentences =originalSentence.get(CoreAnnotations.SentencesAnnotation.class);
		System.out.println("# of sentences is " + sentences.size());
		System.out.println("Sentence level keys:");
		for(Class<?> c :sentences.get(0).keySet()){
			System.out.println(c);
		}
		
		//get tokens annotations
		CoreMap original = sentences.get(0);
		
		multirAnnotation = new Annotation(original.get(CoreAnnotations.TextAnnotation.class));
		multirAnnotation.set(CoreAnnotations.TokensAnnotation.class,original.get(CoreAnnotations.TokensAnnotation.class));
		
		SemanticGraph g = original.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);

		System.out.println(g.toString("plain"));
		
		
		
	}

}
