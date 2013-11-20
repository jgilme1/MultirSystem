package edu.washington.multir.multiralgorithm;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.util.Pair;

/**
 * This main method takes the featuresTest
 * and featuresTrain file and creates all the necessary
 * Multir files like mapping, model, train, test.
 * @author jgilme1
 *
 */
public class Preprocess {

	/**
	 * args[0] is path to featuresTrain
	 * args[1] is path to featuresTest
	 * args[2] is path directory for new multir files like
	 * 			mapping, model, train, test..
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) 
	throws IOException {
    	long start = System.currentTimeMillis();

		String trainFile = args[0];
		String testFile = args[1];
		String outDir = args[2];
		String mappingFile = outDir + File.separatorChar + "mapping";
		String modelFile = outDir + File.separatorChar + "model";
		{
			String output1 = outDir + File.separatorChar + "train";
			convertFeatureFileToMILDocument(trainFile, output1, mappingFile, true, true);
		}
		
		{
			String output2 = outDir + File.separatorChar + "test";
			convertFeatureFileToMILDocument(testFile, output2, mappingFile, false, false);
		}
		
		{
			Model m = new Model();
			Mappings mappings = new Mappings();
			mappings.read(mappingFile);
			m.numRelations = mappings.numRelations();
			m.numFeaturesPerRelation = new int[m.numRelations];
			for (int i=0; i < m.numRelations; i++)
				m.numFeaturesPerRelation[i] = mappings.numFeatures();
			m.write(modelFile);
		}
		
    	long end = System.currentTimeMillis();
    	System.out.println("Preprocessing took " + (end-start) + " millisseconds");
	}
	
	/**
	 * Converts featuresTrain or featuresTest to 
	 * train or test by aggregating the entity pairs
	 * into relations and their mentions.
	 * @param input - the test/train file in non-multir
	 * 				  format
	 * @param output - the test/train file in multir,
	 * 					MILDoc format
	 * @param mappingFile - the mapping file that holds
	 * 			int representation of features and 
	 * 			relations
	 * @param writeFeatureMapping - boolean flag
	 * 		    for writing new features, should
	 * 			be on for training and off for test
	 * @param writeRelationMapping - boolean flag
	 * 			for writing new relations, should be
	 * 			on for training and off for test
	 * @throws IOException
	 */
	private static void convertFeatureFileToMILDocument(String input, String output, String mappingFile, 
			boolean writeFeatureMapping, boolean writeRelationMapping) throws IOException {
		
		
		Mappings m = new Mappings();
		
		//if test time, load the mapping
		if (!writeFeatureMapping || !writeRelationMapping)
			m.read(mappingFile);
		//else start writing a new mapping
		else
			// ensure that relation NA gets ID 0
			m.getRelationID("NA", true);
		
		//open input and output streams
		DataOutputStream os = new DataOutputStream
			(new BufferedOutputStream(new FileOutputStream(output)));
	
		BufferedReader br = new BufferedReader(new FileReader(new File(input)));
		System.out.println("Set up buffered reader");
	    
	    //create MILDocument data map
	    //load feature generation data into map from argument pair keys to
	    //a Pair of List<String> relations and a List<List<String>> for features
	    //for each instance
	    Map<String,Pair<List<String>,List<List<String>>>> relationMentionMap = new HashMap<>();
	    
	    String line;
	    while((line = br.readLine()) != null){
	    	String[] values = line.split("\t");
	    	String arg1Id = values[1];
	    	String arg2Id = values[2];
	    	String rel = values[3];
	    	// entity pair key separated by a delimiter
	    	String key = arg1Id+"%"+arg2Id;
	    	List<String> features = new ArrayList<>();
	    	//add all features
	    	for(int i = 4; i < values.length; i++){
	    		features.add(values[i]);
	    	}
	    	
	    	//update map entry
	    	if(relationMentionMap.containsKey(key)){
	    		Pair<List<String>, List<List<String>>> p = relationMentionMap.get(key);
	    		List<String> oldRelations = p.first;
	    		List<List<String>> oldFeatures = p.second;
	    		if(!oldRelations.contains(rel)){
	    			oldRelations.add(rel);
	    		}
	    		oldFeatures.add(features);
	    	}
	    	
	    	//new map entry
	    	else{
	    		List<String> relations = new ArrayList<>();
	    		relations.add(rel);
	    		List<List<String>> newFeatureList = new ArrayList<>();
	    		newFeatureList.add(features);
	    		Pair<List<String>, List<List<String>>> p = new Pair<List<String>, List<List<String>>>(relations, newFeatureList);
	    		relationMentionMap.put(key,p);
	    	}
	    	if(relationMentionMap.size() % 1000 == 0){
	    		System.out.println("Number of entity pairs read in =" + relationMentionMap.size());
	    	}
	    }
	    
	    br.close();
	    
	    
	    MILDocument doc = new MILDocument();	    
    	
	    //iterate over keys in the map and create MILDocuments
	    int count =0;
	    for(String key : relationMentionMap.keySet()){
	    	doc.clear();

	    	String[] keySplit = key.split("%");
	    	String arg1 = keySplit[0];
	    	String arg2 = keySplit[1];
	    	Pair<List<String>,List<List<String>>> p = relationMentionMap.get(key);
	    	List<String> relations = p.first;
	    	List<List<String>> features = p.second;
	    	
	    	doc.arg1 = arg1;
	    	doc.arg2 = arg2;
	    	
	    	// set relations
	    	{
		    	String[] rels = new String[relations.size()];
		    	for(int i = 0; i < rels.length; i++){
		    		rels[i] = relations.get(i);
		    	}
		    	int[] irels = new int[rels.length];
		    	for (int i=0; i < rels.length; i++)
		    		irels[i] = m.getRelationID(rels[i], writeRelationMapping);
		    	Arrays.sort(irels);
		    	// ignore NA and non-mapped relations
		    	int countUnique = 0;
		    	for (int i=0; i < irels.length; i++)
		    		if (irels[i] > 0 && (i == 0 || irels[i-1] != irels[i]))
		    			countUnique++;
		    	doc.Y = new int[countUnique];
		    	int pos = 0;
		    	for (int i=0; i < irels.length; i++)
		    		if (irels[i] > 0 && (i == 0 || irels[i-1] != irels[i]))
		    			doc.Y[pos++] = irels[i];
	    	}
	    	
	    	// set mentions
	    	doc.setCapacity(features.size());
	    	doc.numMentions = features.size();
	    	
	    	for (int j=0; j < features.size(); j++) {
		    	doc.Z[j] = -1;
	    		doc.mentionIDs[j] = j;
	    		SparseBinaryVector sv = doc.features[j] = new SparseBinaryVector();
	    		
	    		List<String> instanceFeatures = features.get(j);
	    		int[] fts = new int[instanceFeatures.size()];
	    		
	    		for (int i=0; i < instanceFeatures.size(); i++)
	    			fts[i] = m.getFeatureID(instanceFeatures.get(i), writeFeatureMapping);
	    		Arrays.sort(fts);
		    	int countUnique = 0;
		    	for (int i=0; i < fts.length; i++)
		    		if (fts[i] != -1 && (i == 0 || fts[i-1] != fts[i]))
		    			countUnique++;
		    	sv.num = countUnique;
		    	sv.ids = new int[countUnique];
		    	int pos = 0;
		    	for (int i=0; i < fts.length; i++)
		    		if (fts[i] != -1 && (i == 0 || fts[i-1] != fts[i]))
		    			sv.ids[pos++] = fts[i];
	    	}
	    	doc.write(os);
	    	count ++;
	    	
	    	if(count % 1000 == 0){
	    		System.out.println(count + " entity pairs processed");
	    	}
	    }
		br.close();
		os.close();
		if (writeFeatureMapping || writeRelationMapping)
			m.write(mappingFile);
	}
}