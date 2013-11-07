package edu.washington.multir.argumentidentification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.util.Triple;
import edu.washington.multir.data.Argument;

public class DefaultRelationMatching implements RelationMatching {

	@Override
	public List<Triple<Argument,Argument,String>> matchRelations(List<Argument> arguments,
			Map<String, List<String>> relationMap) {
		
		List<Triple<Argument,Argument,String>> distantSupervisionAnnotations = new ArrayList<>();		
		for(int i =0; i < arguments.size(); i++){
			for(int j =0; j < arguments.size(); j++){
				if( (j!=i)){
					Argument arg1 = arguments.get(i);
					Argument arg2 = arguments.get(j);
					if(arg1.getStartOffset() != arg2.getStartOffset()){
						String key = arg1.getArgID()+arg2.getArgID();
						if(relationMap.containsKey(key)){
							List<String> relations = relationMap.get(key);
							for(String relation: relations){
								distantSupervisionAnnotations.add(
										new Triple<>(arg1,arg2,relation));
							}
						}
					}
				}
			}
		}
		return distantSupervisionAnnotations;
	}
}
