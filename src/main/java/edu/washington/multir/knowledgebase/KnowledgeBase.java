package edu.washington.multir.knowledgebase;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import edu.stanford.nlp.util.Pair;
import edu.washington.multir.util.BufferedIOUtils;
import edu.washington.multir.util.TypeConstraintUtils;

/**
 * The KnowledgeBase consists of a flat KB representation
 * that has
 * 1. a triple relationship format
 *    entity1Id entity2Id relation....
 *    ...
 * 2. a map from entityIds to entity names
 * 	  entityId	entityName
 * 	  .....
 * 3. A list of target relations to filter the KB
 *    relation1
 *    relation2
 *    ...
 * @author jgilme1
 *
 */
public class KnowledgeBase {
	
	//map from a name to a list of Ids
	private Map<String,List<String>> entityMap;
	//map from a string representation of an entity pair to a list of their relations
	private Map<String,List<Pair<String,String>>> entityPairRelationMap;
	
	public Map<String,List<String>> getEntityMap() {return entityMap;}
	public Map<String,List<Pair<String,String>>> getEntityPairRelationMap() {return entityPairRelationMap;}

	/**
	 * The constructor reads over the whole relationKBFilePath
	 * selecting only the relations that are in the target relation set.
	 * Then it reads over the whole entityKBFilePath file and adds
	 * to the id to name map ids that could have participated in
	 * the target relations.
	 * @param relationKBFilePath - path to the relation kb file
	 * @param entityKBFilePath - path to the id name file
	 * @param targetRelationPath - path to the target relations file
	 * @throws IOException
	 */
	public KnowledgeBase(String relationKBFilePath, String entityKBFilePath, String targetRelationPath) throws IOException{
		
		//get set of target relations
		Set<String> targetRelations = new HashSet<String>();
		File targetRelationFile = new File(targetRelationPath);
		String targetRelationsString = FileUtils.readFileToString(targetRelationFile);
		String[] targetRelationLines = targetRelationsString.split("\n");
		for(String line : targetRelationLines){
			targetRelations.add(line);
		}
		
		
		Map<String,List<Pair<String,String>>> entityPairRelationMap = new HashMap<String,List<Pair<String,String>>>();
		Set<String> relevantEntities = new HashSet<String>();
		
		//create a map of the filtered KB and silmultaneously
		//keep track of the relevant entities
		long start = System.currentTimeMillis();
		BufferedReader relationReader = BufferedIOUtils.getBufferedReader(new File(relationKBFilePath));
		int index =0;
		String relationLine;
		while((relationLine = relationReader.readLine())!=null){
			String[] lineValues = relationLine.split("\t");
			String e1 = lineValues[0];
			String e2 = lineValues[1];
			String rel = lineValues[2];
			//map key is the 2ids concatenated
			//String entityPairKey = e1+e2;
			Pair<String,String> relPair = new Pair<>(rel,e2);
			if(targetRelations.contains(rel)){
				relevantEntities.add(e1);
				relevantEntities.add(e2);
				if(entityPairRelationMap.containsKey(e1)){
					entityPairRelationMap.get(e1).add(relPair);
				}
				else{
					List<Pair<String,String>> relationPairs = new ArrayList<Pair<String,String>>();
					relationPairs.add(relPair);
					entityPairRelationMap.put(e1,relationPairs);
				}
			}
			if(index % 1000000 == 0){
				System.out.println(index + " lines processed");
			}
			index ++;
		}
		relationReader.close();
		long end = System.currentTimeMillis();
		
		
		//load the name to ids entity map
		File inputFile = new File(entityKBFilePath);
		BufferedReader entityReader = BufferedIOUtils.getBufferedReader(inputFile);	
		Map<String,List<String>> entityMap= new HashMap<String,List<String>>();
		
		int lineNumber =0;
		String entityLine;
		while((entityLine = entityReader.readLine())!=null){
			String[] vals = entityLine.split("\t");
			String entityId = vals[0];
			String entityName = vals[1];
			
			if(relevantEntities.contains(entityId)){
				
				if(entityMap.containsKey(entityName)){
					entityMap.get(entityName).add(entityId);
				}
				else{
					List<String> entityIds = new ArrayList<String>();
					entityIds.add(entityId);
					
					entityMap.put(entityName, entityIds);
				}
			}
			
			
			if(lineNumber % 1000000 == 0){
				System.out.println("Read " + lineNumber+ " lines");
			}
			lineNumber++;
		}
		entityReader.close();
		
		
		System.out.println("Time took = " + (end- start) + " milliseconds");

		long heapSize = Runtime.getRuntime().totalMemory();
		long heapMaxSize = Runtime.getRuntime().maxMemory();
		long freeSize = Runtime.getRuntime().freeMemory();
		System.out.println("Total memory = " + heapSize);
		System.out.println("Max memory = " + heapMaxSize);
		System.out.println("Free memory = " + freeSize);
		
		this.entityMap = entityMap;
		this.entityPairRelationMap = entityPairRelationMap;
	}
	public List<String> getRelationsBetweenArgumentIds(String arg1Id,
			String arg2Id) {
		
		List<String> relations = new ArrayList<String>();
		if(hasRelationWith(arg1Id,arg2Id)){
			List<Pair<String,String>> relationPairs = entityPairRelationMap.get(arg1Id);
			for(Pair<String,String> p : relationPairs){
				if(p.second.equals(arg2Id)){
					relations.add(p.first);
				}
			}
		}
		
		return relations;
	}
	
	public boolean participatesInRelationAsArg1(String arg1Id, String relation){
		
		if(entityPairRelationMap.containsKey(arg1Id)){
			List<Pair<String,String>> relationPairs = entityPairRelationMap.get(arg1Id);
			for(Pair<String,String> p : relationPairs){
				if(p.first.equals(relation)){
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean targetRelationHolds(String arg1Id, String arg2Id, String relation){
		
		if(entityPairRelationMap.containsKey(arg1Id)){
			List<Pair<String,String>> relationPairs = entityPairRelationMap.get(arg1Id);
			for(Pair<String,String> p : relationPairs){
				if(p.first.equals(relation)){
					if(p.second.equals(arg2Id)){
					  return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public boolean hasRelationWith(String arg1Id, String arg2Id) {
		
		if(entityPairRelationMap.containsKey(arg1Id)){
			List<Pair<String,String>> relationPairs = entityPairRelationMap.get(arg1Id);
			for(Pair<String,String> p : relationPairs){
				if(p.second.equals(arg2Id)){
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean noRelationsHold(List<String> arg1Ids, List<String> arg2Ids) {
		
		for(String arg1ID : arg1Ids){
			for(String arg2ID: arg2Ids){
				if(hasRelationWith(arg1ID, arg2ID)){
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Gets a list of all the relations that hold between all arg1s and some
	 * argument and none of those arguments are contained in the parameter
	 * arg2Ids
	 * @param arg1Ids
	 * @param arg2Ids
	 * @param kB
	 * @return
	 */
	public List<String> getTrueNegativeRelations(List<String> arg1Ids,
			List<String> arg2Ids, String arg2Type, KnowledgeBase kB, List<String> targetRelations) {
		
		Set<String> trueNegativeRelations = new HashSet<String>();

		//For a relation to be a true negative, every arg1Id must participate in this
		//relation, and for all pairs of arg1Id and arg2Id this relation cannot hold
		
		
		//1. All arg1s must participate	
		Set<String> candidateRelations = new HashSet<String>();

		for(String rel: targetRelations){
			boolean allArgsParticipate = true;
			for(String arg1Id: arg1Ids){
				if(!participatesInRelationAsArg1(arg1Id,rel)){
					allArgsParticipate = false;
				}
			}
			if(allArgsParticipate){
				candidateRelations.add(rel);
			}
		}
		
		//2. For each candidate relation there cannot be a pair of arg1 and arg2 that holds for this relation
		for(String rel: candidateRelations){
			boolean isNegativeRelation = true;
			
			for(String arg1Id: arg1Ids){
				for(String arg2Id: arg2Ids){
					List<String> relations = getRelationsBetweenArgumentIds(arg1Id,arg2Id);
					for(String heldRelation: relations){
						if(heldRelation.equals(rel)){
							isNegativeRelation = false;
						}
					}
				}
			}
			
			if(isNegativeRelation){
				trueNegativeRelations.add(rel);
			}
		}
		
		return new ArrayList<>(trueNegativeRelations);
	}
	
//	/**
//	 * Returns all relations that arg1 participates in where the second argument
//	 * meets the general type constraints.
//	 * @param arg1Id
//	 * @param arg2Type
//	 * @return
//	 */
//	private List<Pair<String, String>> getTypeFilteredRelations(String arg1Id,
//			String arg2Type) {
//		
//		List<Pair<String,String>> filteredRelations = new ArrayList<>();
//		if(entityPairRelationMap.containsKey(arg1Id)){
//			List<Pair<String,String>> relationPairs = entityPairRelationMap.get(arg1Id);
//			for(Pair<String,String> p : relationPairs){
//				String rel = p.first;
//				String arg2Id = p.second;
//				if(TypeConstraintUtils.meetsArgTypeConstraint(arg2Id, arg2Type)){
//					filteredRelations.add(p);
//				}
//			}
//		}
//		
//		
//		return filteredRelations;
//		
//	}
}
