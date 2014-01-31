package edu.washington.multir.featuregeneration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.Pair;
import edu.knowitall.tool.wordnet.JwiTools;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;

public class FeatureGeneratorMethods {
	
	public static final String PAD = "#PAD#";
	
	
	public static Set<String> acceptablePPChunkTokens = new HashSet<String>();
	private static JwiTools jt;
	private static Dictionary d;
	private static WordnetStemmer stemmer;
	
	
	static{
	 acceptablePPChunkTokens.add("I-NP");
	 acceptablePPChunkTokens.add("B-NP");
	 acceptablePPChunkTokens.add("I-PP");
		d = new Dictionary(new File("/scratch2/resources/WordNet-3.0/WN/dict"));
		try {
			d.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stemmer = new WordnetStemmer(d);
		jt = new JwiTools(d);
	 
	 
	}
	
	public static List<CoreLabel> getMiddleTokens(Integer arg1EndOffset, Integer arg2StartOffset, List<CoreLabel> tokens){
		List<CoreLabel> middleTokens = new ArrayList<CoreLabel>();
		for(CoreLabel tok: tokens){
			Integer tokBeginOffset = tok.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
			Integer tokEndOffset =  tok.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
			if(tokBeginOffset >= arg1EndOffset && tokEndOffset < arg2StartOffset){
				middleTokens.add(tok);
			}
		}
		return middleTokens;
	}
	
	public static List<CoreLabel> getLeftWindowTokens(Integer arg1StartOffset, List<CoreLabel> tokens){
		List<CoreLabel> leftWindowTokens = new ArrayList<CoreLabel>();
		
		for(CoreLabel tok : tokens){
			int startOffset = tok.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
			if(startOffset < arg1StartOffset){
				leftWindowTokens.add(tok);
			}
			else{
				break;
			}
		}
		return leftWindowTokens;
	}
	
	public static List<CoreLabel> getRightWindowTokens(Integer arg2EndOffset, List<CoreLabel> tokens){
		List<CoreLabel> rightWindowTokens = new ArrayList<CoreLabel>();
		
		for(CoreLabel tok : tokens){
			int endOffset = tok.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
			if(endOffset > arg2EndOffset){
				rightWindowTokens.add(tok);
			}
		}
		return rightWindowTokens;
	}
	
	public static List<CoreLabel> getRightWindowTokens(Integer arg1EndOffset, List<CoreLabel> tokens, int windowLength){
		List<CoreLabel> rightWindowTokens = getRightWindowTokens(arg1EndOffset,tokens);		
		return rightWindowTokens.subList(0, Math.min(2,rightWindowTokens.size()));
	}
	
	public static List<CoreLabel> getLeftWindowTokens(Integer arg1StartOffset, List<CoreLabel> tokens, int windowLength){
		List<CoreLabel> leftWindowTokens = getLeftWindowTokens(arg1StartOffset,tokens);		
		return leftWindowTokens.subList(Math.max(0,leftWindowTokens.size()-2), leftWindowTokens.size());
	}

	/**
	 * Tokens must have Chunk Annotation on them generated in the Feature Generator
	 * @param middleTokens
	 * @return
	 */
	public static List<String> getGeneralSequence(List<CoreLabel> middleTokens) {
		List<String> generalSequence = new ArrayList<String>();
		
		generalSequence.add(PAD);
		List<List<CoreLabel>> ppSequences = getPPSequences(middleTokens);
		List<String> ppFeatures = generatePPFeatures(ppSequences);
		for(String ppf : ppFeatures){
			generalSequence.add(ppf);
		}
		
		generalSequence.add(PAD);
		return generalSequence;
	}

	private static List<String> generatePPFeatures(
			List<List<CoreLabel>> ppSequences) {
		List<String> ppFeatures = new ArrayList<String>();
		
		
		for(List<CoreLabel> ppSequence : ppSequences){
			String ppFeature = generatePPFeature(ppSequence);
			if(ppFeature != null) ppFeatures.add(ppFeature);
		}
		
		return ppFeatures;
		
	}

	private static String generatePPFeature(List<CoreLabel> ppSequence) {
		StringBuilder sb = new StringBuilder();
		String headNoun = findHeadString(ppSequence);
		if(headNoun != null){
			
			sb.append("PP(");
			sb.append(headNoun);
			sb.append(")");
			return sb.toString();
			
			
		}
		return null;

	}

	private static String findHeadString(List<CoreLabel> sequence) {
		String entityType  = findHeadEntityType(sequence);
	    if(entityType != null){
	    	return entityType;
	    }
	    return findHeadNounByWordnet(sequence);
	}
	
	private static String findHeadNounByPOS(List<CoreLabel> sequence){
		List<String> nounWords = new ArrayList<String>();
		
		for(int j = sequence.size()-1; j > -1; j--){
			CoreLabel t = sequence.get(j);
			String pos = t.get(CoreAnnotations.PartOfSpeechAnnotation.class);
			if(pos.startsWith("N")){
				nounWords.add(t.get(CoreAnnotations.TextAnnotation.class));
				
			}
			else{
				break;
			}
		}
		
		if(nounWords.size() == 0){
			return null;
		}
		
		Collections.reverse(nounWords);
		StringBuilder sb = new StringBuilder();
		for(String nw: nounWords){
			sb.append(nw);
			sb.append(" ");
		}
		return sb.toString().trim();
	}
	
	private static String findHeadNounByWordnet(List<CoreLabel> sequence){
		StringBuilder sb = new StringBuilder();
		
		for(int j = sequence.size()-1; j > -1; j--){
			CoreLabel t = sequence.get(j);
			String word = t.get(CoreAnnotations.TextAnnotation.class);
			StringBuilder newWord = new StringBuilder();
			newWord.append(word);
			newWord.append(" ");
			newWord.append(sb);
			String wnLemma = getWordNetNounLemma(newWord.toString().trim());
			if(wnLemma != null){
				sb = newWord;
			}
			else{
				break;
			}
		}
		
		if(sb.length() >0) {
			return sb.toString().trim();
		}
		else{
			return null;
		}
		
		
	}
	
	private static String getWordNetNounLemma(String p){
		List<String> stems = stemmer.findStems(p, POS.NOUN);
		if(stems.size()> 0){
			return stems.get(0);
		}
		return null;
	}
	
	private static String getWordNetVerbLemma(String p){
		List<String> stems = stemmer.findStems(p, POS.VERB);
		if(stems.size()> 0){
			return stems.get(0);
		}
		return null;
	}

	private static String findHeadEntityType(List<CoreLabel> sequence) {
		
		CoreLabel lastToken = sequence.get(sequence.size()-1);
		String type = lastToken.get(CoreAnnotations.NamedEntityTagAnnotation.class);
		if(type != null){
			if(!type.equals("O")){
				return type;
			}
		}
		return null;
		
	}

	private static List<List<CoreLabel>> getPPSequences(
			List<CoreLabel> middleTokens) {
		List<List<CoreLabel>> ppSequences = new ArrayList<List<CoreLabel>>();

		for(int i =0; i < middleTokens.size(); i++){
			List<CoreLabel> ppSequence = getPPSequence(i,middleTokens.size(),middleTokens);
			if(ppSequence.size() > 0){
				ppSequences.add(ppSequence);
				i += ppSequence.size()-1;
			}
		}
		
		return ppSequences;
	}

	private static List<CoreLabel> getPPSequence(int start, int end,
			List<CoreLabel> tokens) {
		List<CoreLabel> ppSequence = new ArrayList<CoreLabel>();
		CoreLabel startToken = tokens.get(start);
		String startTokenChunk = startToken.get(CoreAnnotations.ChunkAnnotation.class);
		if(startTokenChunk.equals("B-PP")){
			ppSequence.add(startToken);
			
			for(int i = start+1; i< end; i++){
				CoreLabel t = tokens.get(i);
				String chunkString = t.get(CoreAnnotations.ChunkAnnotation.class);
				if(acceptablePPChunkTokens.contains(chunkString)){
					ppSequence.add(t);
				}
				else{
					break;
				}
			}
		}
		return ppSequence;
	}

	public static Pair<Integer, Integer> getLeftArgOffsets(
			Integer arg1StartOffset, Integer arg1EndOffset,
			Integer arg2StartOffset, Integer arg2EndOffset) {
		
		if(arg1StartOffset < arg2StartOffset){
			return new Pair<Integer,Integer>(arg1StartOffset,arg1EndOffset);
		}
		else{
			return new Pair<Integer,Integer>(arg2StartOffset,arg2EndOffset);
		}
	}

	public static Pair<Integer, Integer> getRightArgOffsets(
			Integer arg1StartOffset, Integer arg1EndOffset,
			Integer arg2StartOffset, Integer arg2EndOffset) {
		if(arg1StartOffset > arg2StartOffset){
			return new Pair<Integer,Integer>(arg1StartOffset,arg1EndOffset);
		}
		else{
			return new Pair<Integer,Integer>(arg2StartOffset,arg2EndOffset);
		}
	}

}
