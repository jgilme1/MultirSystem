package edu.washington.multir.argumentidentification;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecificationWithNEL.SentNamedEntityLinkingInformation.NamedEntityLinkingAnnotation;
import edu.washington.multir.data.Argument;
import edu.washington.multir.data.KBArgument;

/**
 * NELArgumentIdentification returns every token span with
 * a link as an argument and all NER-recognized arguments as well.
 * @author jgilme1
 *
 */
public class NELArgumentIdentification implements ArgumentIdentification{

	
	private static NELArgumentIdentification instance = null;
	
	private NELArgumentIdentification(){}
	public static NELArgumentIdentification getInstance(){
		if(instance == null) instance = new NELArgumentIdentification();
		return instance;
		}
	
	@Override
	public List<Argument> identifyArguments(Annotation d, CoreMap s) {
		//first grab all the NER arguments and store are nil links
		List<Argument> arguments = new ArrayList<>();
		List<Argument> nerArguments = NERArgumentIdentification.getInstance().identifyArguments(d, s);
		List<CoreLabel> tokens = s.get(CoreAnnotations.TokensAnnotation.class);
		
		arguments.addAll(nerArguments);
		
		
		//then grab all the NERL arguments
		List<Triple<Pair<Integer,Integer>,String,Float>> nelAnnotation = s.get(NamedEntityLinkingAnnotation.class);
		for(Triple<Pair<Integer,Integer>,String,Float> trip : nelAnnotation){
			String id = trip.second;
			//if token span has a link create a new argument
			if(!id.equals("null")){
				//get character offsets
				Integer startTokenOffset = trip.first.first;
				Integer endTokenOffset = trip.first.second;
				if(startTokenOffset >= 0 && startTokenOffset < tokens.size() && endTokenOffset >= 0 && endTokenOffset < tokens.size()){
					Integer startCharacterOffset = tokens.get(startTokenOffset).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
					Integer endCharacterOffset = tokens.get(endTokenOffset-1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
	
					
					//get argument string
					String sentText = s.get(CoreAnnotations.TextAnnotation.class);
					if(sentText != null && startCharacterOffset !=null && endCharacterOffset!=null){
						String argumentString = sentText.substring(startCharacterOffset, endCharacterOffset);
						
						//add argument to list
						KBArgument nelArgument = new KBArgument(new Argument(argumentString,startCharacterOffset,endCharacterOffset),id);
						arguments.add(nelArgument);
					}
				}
			}
		}
		return arguments;
	}
}
