package edu.washington.multir.argumentidentification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.data.Argument;
import edu.washington.multir.data.KBArgument;
import edu.washington.multir.knowledgebase.KnowledgeBase;

public class NELRelationMatching implements RelationMatching {

	
	private static NELRelationMatching instance = null;
	
	public static NELRelationMatching getInstance(){
		if(instance == null){
			instance = new NELRelationMatching();
		}
		return instance;
	}
	
	private NELRelationMatching(){}
	
	@Override
	public List<Triple<KBArgument, KBArgument, String>> matchRelations(
			List<Pair<Argument, Argument>> sententialInstances, KnowledgeBase KB) {

		List<Triple<KBArgument,KBArgument,String>> dsRelations = new ArrayList<>();
		
		Map<String,List<String>> entityRelationMap =KB.getEntityPairRelationMap();
		
		for(Pair<Argument,Argument> sententialInstance : sententialInstances){
			Argument arg1 = sententialInstance.first;
			Argument arg2 = sententialInstance.second;
			//if both arguments have ids in the KB
			if((arg1 instanceof KBArgument) && (arg2 instanceof KBArgument)){
				//check if they have a relation
				KBArgument kbArg1 = (KBArgument)arg1;
				KBArgument kbArg2 = (KBArgument)arg2;
				String key = kbArg1.getKbId()+kbArg2.getKbId();
				//if the two KB entities participate in a relatoin
				if(entityRelationMap.containsKey(key)){
					List<String> relations = entityRelationMap.get(key);
					for(String rel: relations){
						Triple<KBArgument,KBArgument,String> dsRelation = new Triple<>(kbArg1,kbArg2,rel);
						dsRelations.add(dsRelation);
					}
				}
			}
		}
		return dsRelations;
	}

}
