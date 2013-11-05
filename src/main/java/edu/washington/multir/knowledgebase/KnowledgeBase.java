package edu.washington.multir.knowledgebase;

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

public class KnowledgeBase {
	
	private Map<String,List<String>> entityMap;
	private Map<String,List<String>> entityPairRelationMap;
	
	public Map<String,List<String>> getEntityMap() {return entityMap;}
	public Map<String,List<String>> getEntityPairRelationMap() {return entityPairRelationMap;}

	public KnowledgeBase(String relationKBFilePath, String entityKBFilePath, String targetRelationPath) throws IOException{
		Set<String> targetRelations = new HashSet<String>();
		File targetRelationFile = new File(targetRelationPath);
		String targetRelationsString = FileUtils.readFileToString(targetRelationFile);
		String[] targetRelationLines = targetRelationsString.split("\n");
		for(String line : targetRelationLines){
			targetRelations.add(line);
		}
		
		Map<String,List<String>> entityPairRelationMap = new HashMap<String,List<String>>();
		Set<String> relevantEntities = new HashSet<String>();
		
		long start = System.currentTimeMillis();
		LineIterator li = FileUtils.lineIterator(new File(relationKBFilePath));
		int index =0;
		while(li.hasNext()){
			String line = li.nextLine();
			String[] lineValues = line.split("\t");
			String e1 = lineValues[0];
			String e2 = lineValues[1];
			String rel = lineValues[2];
			String entityPairKey = e1+e2;
			if(targetRelations.contains(rel)){
				relevantEntities.add(e1);
				relevantEntities.add(e2);
				if(entityPairRelationMap.containsKey(entityPairKey)){
					entityPairRelationMap.get(entityPairKey).add(rel);
				}
				else{
					List<String> relations = new ArrayList<String>();
					relations.add(rel);
					entityPairRelationMap.put(entityPairKey,relations);
				}
			}
			if(index % 1000000 == 0){
				System.out.println(index + " lines processed");
			}
			index ++;
		}
		li.close();
		long end = System.currentTimeMillis();
		
		File inputFile = new File(entityKBFilePath);
		
		LineIterator entityli = FileUtils.lineIterator(inputFile);
		
		Map<String,List<String>> entityMap= new HashMap<String,List<String>>();
		
		int lineNumber =0;
		while(entityli.hasNext()){
			String line = entityli.nextLine();
			String[] vals = line.split("\t");
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
		entityli.close();
		
		
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
}
