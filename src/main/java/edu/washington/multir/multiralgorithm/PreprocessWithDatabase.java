package edu.washington.multir.multiralgorithm;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.util.Pair;
import edu.washington.multir.database.CustomDerbyDB;

/**
 * This main method takes the featuresTest and featuresTrain file and creates
 * all the necessary Multir files like mapping, model, train, test.
 * 
 * @author jgilme1
 * 
 */
public class PreprocessWithDatabase {
	private static Map<String, Integer> keyToIntegerMap = new HashMap<String, Integer>();
	private static Map<Integer, String> intToKeyMap = new HashMap<Integer, String>();
	private static final int FEATURE_THRESHOLD = 2;

	private static final double GIGABYTE_DIVISOR = 1073741824;

	/**
	 * args[0] is path to featuresTrain args[1] is path directory for new multir
	 * files like mapping, model, train, test..
	 * 
	 * @param args
	 * @throws IOException
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws IOException, SQLException {
		long start = System.currentTimeMillis();

		printMemoryStatistics();

		String trainFile = args[0];
		String outDir = args[1];
		String mappingFile = outDir + File.separatorChar + "mapping";
		String modelFile = outDir + File.separatorChar + "model";

		System.out.println("GETTING Mapping form training data");
		Mappings mapping = getMappingFromTrainingData(trainFile, mappingFile);
//
//		System.out.println("PREPROCESSING TRAIN FEATURES");
//		{
//			String output1 = outDir + File.separatorChar + "train";
//			convertFeatureFileToMILDocument(trainFile, output1, mapping);
//		}
//
//		System.out.println("FINISHED PREPROCESSING TRAIN FEATURES");
//		printMemoryStatistics();
//		keyToIntegerMap.clear();
//
//		intToKeyMap.clear();
//
//		System.out.println("Writing model and mapping file");
//		printMemoryStatistics();
//
//		{
//			Model m = new Model();
//			m.numRelations = mapping.numRelations();
//			m.numFeaturesPerRelation = new int[m.numRelations];
//			for (int i = 0; i < m.numRelations; i++)
//				m.numFeaturesPerRelation[i] = mapping.numFeatures();
//			m.write(modelFile);
//			mapping.write(mappingFile);
//		}
//
//		long end = System.currentTimeMillis();
//		System.out.println("Preprocessing took " + (end - start)
//				+ " millisseconds");
	}

	/**
	 * Reads training feature file, writing and updating feature counts
	 * to a database. 
	 * 
	 * 
	 * @param trainFile
	 * @param mappingFile
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	private static Mappings getMappingFromTrainingData(String trainFile,
			String mappingFile) throws IOException, SQLException {
		
		//step 1: Read features and store feature counts in separate database
		//keep as much in memory data as possible before dumping to database
		BufferedReader br = new BufferedReader(new FileReader(new File(trainFile)));
		Mappings m = new Mappings();
		String line;
		int count = 0 ;
		Map<String,Integer> featureCountMap = new HashMap<String,Integer>();
		
		//create database for feature counts
		CustomDerbyDB featureCountDB = CustomDerbyDB.createCustomDerbyDB("featureCountDB", "FEATURE", "VARCHAR(20000)", "COUNT", "int");
		
		batchInsertAllFeatures(br,featureCountDB);
		
		br = new BufferedReader(new FileReader(new File(trainFile)));
		while((line = br.readLine()) != null){
	    	count++;
	    	String[] values = line.split("\t");
	    	String rel = values[3];
	    	String []rels = rel.split("\\|");
	    	List<String> features = new ArrayList<>();
	    	//add all features
	    	for(int i = 4; i < values.length; i++){
	    		features.add(values[i]);
	    	}
	    	
	    	// update mappings file
	    	for(String r: rels){
	    	  m.getRelationID(r, true);
	    	}
	    	
	    	if(count % 3000 != 0){
		    	for(String feature: features){
		    		if(featureCountMap.containsKey(feature)){
		    			featureCountMap.put(feature,featureCountMap.get(feature)+1);
		    		}
		    		else{
		    			featureCountMap.put(feature,1);
		    		}
		    	}
	    	}
	    	else{
	    		//do batch updates to database
	    		System.out.println("Reached memory threshold:");
	    		printMemoryStatistics();
	    		System.out.println("Updating DB");
	    		//get current DB state
	    		Map<String,Integer> currentDBCounts = getDBValues(featureCountDB,featureCountMap);
	    		//Map<String,Integer> currentDBCounts = getDBValuesWithPreparedStatement(featureCountDB,featureCountMap);
	    		Map<String,Integer> newFeatureCountMap = new HashMap<String,Integer>();
	    		Map<String,Integer> updatedFeatureCountMap = new HashMap<String,Integer>();
	    		
	    		System.out.println("Current DB Values Retrieved");
	    		
	    		//update map
	    		for(String feat: featureCountMap.keySet()){
	    			if(currentDBCounts.containsKey(feat)){
	    				updatedFeatureCountMap.put(feat, featureCountMap.get(feat) + currentDBCounts.get(feat));
	    			}
	    			else{
	    				newFeatureCountMap.put(feat,featureCountMap.get(feat));
	    			}
	    		}
	    		
	    		//update DB
	    		updateDB(featureCountDB,updatedFeatureCountMap);
	    		insertDBWithPreparedStatement(featureCountDB,newFeatureCountMap);
	    		
	    		
	    		featureCountMap = new HashMap<String,Integer>();
	    	}
	    	if(count % 3000 == 0){
	    		System.out.println(count + " training instances processed");
	    	}
		}
		br.close();
		return m;
		
		
		//step 2: read new Database and only consider features
		//below the feature threshold count, and put into memory in Mappings object
//		
//
//		
//		
//		Mappings m = new Mappings();
//		CustomDerbyDB singleFeatureDB = CustomDerbyDB.createCustomDerbyDB("SingleFeatures","FEATURE","VARCHAR(20000)");
//		m.getRelationID("NA", true);
//		BufferedReader br = new BufferedReader(new FileReader(new File(trainFile)));
//		
//		String line;
//		int count = 0 ;
//		while((line = br.readLine()) != null){
//	    	String[] values = line.split("\t");
//	    	String rel = values[3];
//	    	String []rels = rel.split("\\|");
//	    	List<String> features = new ArrayList<>();
//	    	//add all features
//	    	for(int i = 4; i < values.length; i++){
//	    		features.add(values[i]);
//	    	}
//	    	
//	    	// update mappings file
//	    	for(String r: rels){
//	    	  m.getRelationID(r, true);
//	    	}
//	    	for(String feature: features){
//	    		Integer featId = m.getFeatureID(feature, false);
//	    		if(featId.equals(-1)){
//	    			if(singleFeatureDB.contains("FEATURE", feature)){
//	    				m.getFeatureID(feature, true);
//	    			}
//	    			else{
//	    				Object[] objectValues = new Object[1];
//	    				objectValues[0] = feature;
//	    				singleFeatureDB.putRow(objectValues);
//	    			}
//	    		}
//	    	}
//	    	count++;
//	    	if(count % 10 == 0){
//	    		System.out.println(count + " training instances processed");
//	    	}
//		}
//		singleFeatureDB.close();
//		br.close();
//		return m;

	}



	private static void batchInsertAllFeatures(BufferedReader br,
			CustomDerbyDB featureCountDB) throws IOException, SQLException {
		String line;
		int count = 0;
		Set<String> features = new HashSet<String> ();
		while((line = br.readLine()) != null){
	    	count++;
	    	String[] values = line.split("\t");
	    	List<String> featureList = new ArrayList<>();
	    	//add all features
	    	for(int i = 4; i < values.length; i++){
	    		features.add(values[i]);
	    	}

	    	
	    	if(!reachedMemoryThreshold(.1)){
		    	for(String feature: featureList){
		    		if(!features.contains(feature)){
		    			features.add(feature);
		    		}
		    	}
	    	}
	    	else{
	    		//do batch updates to database
	    		System.out.println("Reached memory threshold:");
	    		printMemoryStatistics();
	    		System.out.println("Inserting to  DB");
	    		insertDBWithPreparedStatement(featureCountDB,features);
	    		features = new HashSet<String>();
	    		System.gc();
	    	}
	    	if(count % 100000 == 0){
	    		System.out.println(count + " training instances processed");
	    	}
		}
		br.close();
	}

	private static void insertDBWithPreparedStatement(
			CustomDerbyDB featureCountDB, Set<String> features) throws SQLException {
		System.out.println("Inserting DB with " + features.size() + " values");
		
		
		StringBuilder insertDBCommand;
		insertDBCommand = new StringBuilder();
		insertDBCommand.append("INSERT INTO " + featureCountDB.getTableName() + " VALUES (?,?)");
		PreparedStatement ps = featureCountDB.getConnection().prepareStatement(insertDBCommand.toString());
		
		
		
		int insertCount  = 0;
		insertDBCommand = new StringBuilder();
		for(String f : features){
			ps.clearParameters();
			ps.setString(1, f);
			ps.setInt(2,0);
			ps.addBatch();
			insertCount++;
//			insertDBCommand.append("('"+f.replaceAll("'", "''")+"',"+newFeatureCountMap.get(f) + "),");
//			insertCount++;
//			if( insertCount % 1000 == 0){
//				insertDBCommand.setLength(insertDBCommand.length()-1);
//				featureCountDB.executeCommand(insertDBCommand.toString());
//				insertDBCommand = new StringBuilder();
//				insertDBCommand.append("INSERT INTO " + featureCountDB.getTableName() + " VALUES ");
//			}
			if(insertCount % 100000 == 0){
				ps.executeBatch();
				ps.clearBatch();
				System.out.println(insertCount + " insertions");
			}
		}
		ps.executeBatch();
		featureCountDB.getConnection().commit();
		
	}

	private static void insertDB(CustomDerbyDB featureCountDB,
			Map<String, Integer> newFeatureCountMap) throws SQLException {
		System.out.println("Inserting DB with " + newFeatureCountMap.size() + " values");
		
		
		StringBuilder insertDBCommand;
		
		int insertCount  = 0;
		insertDBCommand = new StringBuilder();
		insertDBCommand.append("INSERT INTO " + featureCountDB.getTableName() + " VALUES ");
		for(String f : newFeatureCountMap.keySet()){
			
			
			insertDBCommand.append("('"+f.replaceAll("'", "''")+"',"+newFeatureCountMap.get(f) + "),");
			insertCount++;
			if( insertCount % 1000 == 0){
				insertDBCommand.setLength(insertDBCommand.length()-1);
				featureCountDB.executeCommand(insertDBCommand.toString());
				insertDBCommand = new StringBuilder();
				insertDBCommand.append("INSERT INTO " + featureCountDB.getTableName() + " VALUES ");
			}
			if(insertCount % 50000 == 0){
				System.out.println(insertCount + " insertions");
			}
		}
		featureCountDB.getConnection().commit();
		
	}
	
	private static void insertDBWithPreparedStatement(CustomDerbyDB featureCountDB,
			Map<String, Integer> newFeatureCountMap) throws SQLException {
		System.out.println("Inserting DB with " + newFeatureCountMap.size() + " values");
		
		
		StringBuilder insertDBCommand;
		insertDBCommand = new StringBuilder();
		insertDBCommand.append("INSERT INTO " + featureCountDB.getTableName() + " VALUES (?,?)");
		PreparedStatement ps = featureCountDB.getConnection().prepareStatement(insertDBCommand.toString());
		
		
		
		int insertCount  = 0;
		insertDBCommand = new StringBuilder();
		for(String f : newFeatureCountMap.keySet()){
			ps.clearParameters();
			ps.setString(1, f);
			ps.setInt(2, newFeatureCountMap.get(f));
			ps.addBatch();
			insertCount++;
//			insertDBCommand.append("('"+f.replaceAll("'", "''")+"',"+newFeatureCountMap.get(f) + "),");
//			insertCount++;
//			if( insertCount % 1000 == 0){
//				insertDBCommand.setLength(insertDBCommand.length()-1);
//				featureCountDB.executeCommand(insertDBCommand.toString());
//				insertDBCommand = new StringBuilder();
//				insertDBCommand.append("INSERT INTO " + featureCountDB.getTableName() + " VALUES ");
//			}
			if(insertCount % 100000 == 0){
				ps.executeBatch();
				ps.clearBatch();
				System.out.println(insertCount + " insertions");
			}
		}
		ps.executeBatch();
		featureCountDB.getConnection().commit();
		
	}

	private static void updateDB(CustomDerbyDB featureCountDB,
			Map<String, Integer> featureCountMap) throws SQLException {
		System.out.println("Updating DB with " + featureCountMap.size() + " values");
		
		Map<Integer,List<String>> countToFeatureMap = new HashMap<Integer,List<String>>();
		
		for(String k : featureCountMap.keySet()){
			Integer count = featureCountMap.get(k);
			
			if(countToFeatureMap.containsKey(count)){
				countToFeatureMap.get(count).add(k);
			}
			else{
				List<String> features = new ArrayList<String>();
				features.add(k);
				countToFeatureMap.put(count,features);
			}
		}
		
		StringBuilder updateDBCommand;
		int queryLimit = 1000;
		int printLimit = 100000;
		for(Integer count: countToFeatureMap.keySet()){
			int queryCount  = 0;
			updateDBCommand = new StringBuilder();
			updateDBCommand.append("UPDATE " + featureCountDB.getTableName() + " SET COUNT=" + count);
		    updateDBCommand.append(" WHERE ");
			List<String> features = countToFeatureMap.get(count);
			for(String f: features){
				updateDBCommand.append("FEATURE='"+f.replaceAll("'","''")+"' OR ");
				queryCount++;
				if(queryCount % queryLimit == 0){
					if(queryCount % printLimit == 0){
					  System.out.println("Updated " + queryCount+ " entries");
					}
					updateDBCommand.setLength(updateDBCommand.length()-4);
					featureCountDB.executeCommand(updateDBCommand.toString());
					updateDBCommand = new StringBuilder();
					updateDBCommand.append("UPDATE " + featureCountDB.getTableName() + " SET COUNT=" + count);
				    updateDBCommand.append(" WHERE ");
				}
			}
			updateDBCommand.setLength(updateDBCommand.length()-4);
			featureCountDB.executeCommand(updateDBCommand.toString());
		}
		featureCountDB.getConnection().commit();
		
	}
	
	private static Map<String, Integer> getDBValuesWithPreparedStatement(
			CustomDerbyDB featureCountDB, Map<String, Integer> featureCountMap) throws SQLException {
				int queryLimit = 1000;

				System.out.println("Building query string from " + featureCountMap.size() + " features");

				StringBuilder psStringBuilder = new StringBuilder();
				psStringBuilder.append("SELECT * FROM " + featureCountDB.getTableName() + " WHERE FEATURE=");
				for(int i =0; i < queryLimit; i++){
					psStringBuilder.append("? OR FEATURE=");
				}
				psStringBuilder.setLength(psStringBuilder.length()-12);
				PreparedStatement ps = featureCountDB.getConnection().prepareStatement(psStringBuilder.toString());
				
				
				int featCount =0;
				Map<String,Integer> currentDBValues = new HashMap<String,Integer>();
				int queryCount = 1;
				for(String f: featureCountMap.keySet()){
					ps.setString(queryCount, f);
					if(queryCount == queryLimit){
						ResultSet rs = ps.executeQuery();
						System.out.println("Feature Count = " + featCount);
						while(rs.next()){
							String k = rs.getString(1);
							Integer count = rs.getInt(2);
							currentDBValues.put(k, count);
						}
						ps.clearParameters();
						queryCount =0;
					}
					featCount++;
					queryCount++;
				}

				String fakeString = "fakeString";
				for(int i = queryCount; i <= queryLimit; i++){
					ps.setString(i, fakeString);
				}
				ResultSet rs = ps.executeQuery();				
				while(rs.next()){
					String k = rs.getString(1);
					Integer count = rs.getInt(2);
					currentDBValues.put(k, count);
				}
				
//				for(String f : currentDBValues.keySet()){
//					System.out.println(f + "\t" + currentDBValues.get(f));
//				}
				
				return currentDBValues;
				

			}

	private static Map<String, Integer> getDBValues(
			CustomDerbyDB featureCountDB, Map<String, Integer> featureCountMap) throws SQLException {
		//PreparedStatement ps = featureCountDB.getConnection().prepareS
		int queryLimit = 1000;
		int printLimit = 100000;
		System.out.println("Building query string from " + featureCountMap.size() + " features");
		Map<String,Integer> currentDBValues = new HashMap<String,Integer>();
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("SELECT * FROM " + featureCountDB.getTableName() + " WHERE ");
		String targetColumnName = "FEATURE";
		int queryCount = 0;
		for(String f: featureCountMap.keySet()){
			queryBuilder.append(targetColumnName +"='" + f.replaceAll("'", "''")+ "' OR ");
			queryCount++;
			if(queryCount % queryLimit == 0){
				if(queryCount % printLimit == 0){
				  System.out.println("queryCount = " + queryCount);
				}
				queryBuilder.setLength(queryBuilder.length()-4);
				ResultSet rs = featureCountDB.executeQuery(queryBuilder.toString());
				while(rs.next()){
					String k = rs.getString(1);
					Integer count = rs.getInt(2);
					currentDBValues.put(k, count);
				}
				queryBuilder = new StringBuilder();
				queryBuilder.append("SELECT * FROM " + featureCountDB.getTableName() + " WHERE ");
			}
		}
		queryBuilder.setLength(queryBuilder.length()-4);
		System.out.println("Querying DB");
		ResultSet rs = featureCountDB.executeQuery(queryBuilder.toString());
		System.out.println("Converting Results to map");
		while(rs.next()){
			String k = rs.getString(1);
			Integer count = rs.getInt(2);
			currentDBValues.put(k, count);
		}
		
//		for(String f : currentDBValues.keySet()){
//			System.out.println(f + "\t" + currentDBValues.get(f));
//		}
		return currentDBValues;
	}

	private static boolean reachedMemoryThreshold(double threshold) {
		double ratio = getAllocatedMemory()/getMaxMemory();
		if(ratio > threshold){
			return true;
		}
		else{
			return false;
		}
	}

	/**
	 * Converts featuresTrain or featuresTest to train or test by aggregating
	 * the entity pairs into relations and their mentions.
	 * 
	 * @param input
	 *            - the test/train file in non-multir format
	 * @param output
	 *            - the test/train file in multir, MILDoc format
	 * @param m
	 *            - the mappings object that keeps track of relevant relations
	 *            and features
	 * @throws IOException
	 */
	private static void convertFeatureFileToMILDocument(String input,
			String output, Mappings m) throws IOException {
		// open input and output streams
		DataOutputStream os = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(output)));

		BufferedReader br = new BufferedReader(new FileReader(new File(input)));
		System.out.println("Set up buffered reader");

		// create MILDocument data map
		// load feature generation data into map from argument pair keys to
		// a Pair of List<Integer> relations and a List<List<Integer>> for
		// features
		// for each instance
		Map<Integer, Pair<List<Integer>, List<List<Integer>>>> relationMentionMap = new HashMap<>();

		String line;
		while ((line = br.readLine()) != null) {
			String[] values = line.split("\t");
			String arg1Id = values[1];
			String arg2Id = values[2];
			String relString = values[3];
			String[] rels = relString.split("\\|");
			// entity pair key separated by a delimiter
			String key = arg1Id + "%" + arg2Id;
			Integer intKey = getIntKey(key);
			List<String> features = new ArrayList<>();
			// add all features
			for (int i = 4; i < values.length; i++) {
				features.add(values[i]);
			}
			// convert to integer keys from the mappings m object
			List<Integer> featureIntegers = convertFeaturesToIntegers(features,
					m);

			// update map entry
			if (relationMentionMap.containsKey(intKey)) {
				Pair<List<Integer>, List<List<Integer>>> p = relationMentionMap
						.get(intKey);
				List<Integer> oldRelations = p.first;
				List<List<Integer>> oldFeatures = p.second;
				for (String rel : rels) {
					Integer relKey = getIntRelKey(rel, m);
					if (!oldRelations.contains(relKey)) {
						oldRelations.add(relKey);
					}
				}
				oldFeatures.add(featureIntegers);
			}

			// new map entry
			else {
				List<Integer> relations = new ArrayList<>();
				for (String rel : rels) {
					relations.add(getIntRelKey(rel, m));
				}
				List<List<Integer>> newFeatureList = new ArrayList<>();
				newFeatureList.add(featureIntegers);
				Pair<List<Integer>, List<List<Integer>>> p = new Pair<List<Integer>, List<List<Integer>>>(
						relations, newFeatureList);
				relationMentionMap.put(intKey, p);
			}
			if (relationMentionMap.size() % 100000 == 0) {
				System.out.println("Number of entity pairs read in ="
						+ relationMentionMap.size());
				printMemoryStatistics();
			}
		}

		br.close();
		System.out.println("LOADED MAP!");

		MILDocument doc = new MILDocument();

		// iterate over keys in the map and create MILDocuments
		int count = 0;
		for (Integer intKey : relationMentionMap.keySet()) {
			doc.clear();

			String[] keySplit = getStringKey(intKey).split("%");
			String arg1 = keySplit[0];
			String arg2 = keySplit[1];
			Pair<List<Integer>, List<List<Integer>>> p = relationMentionMap
					.get(intKey);
			List<Integer> intRels = p.first;
			List<List<Integer>> intFeatures = p.second;

			doc.arg1 = arg1;
			doc.arg2 = arg2;

			// System.out.println(arg1+"\t"+arg2);

			// set relations
			{
				int[] irels = new int[intRels.size()];
				for (int i = 0; i < intRels.size(); i++)
					irels[i] = intRels.get(i);
				Arrays.sort(irels);
				// ignore NA and non-mapped relations
				int countUnique = 0;
				for (int i = 0; i < irels.length; i++)
					if (irels[i] > 0 && (i == 0 || irels[i - 1] != irels[i]))
						countUnique++;
				doc.Y = new int[countUnique];
				int pos = 0;
				for (int i = 0; i < irels.length; i++)
					if (irels[i] > 0 && (i == 0 || irels[i - 1] != irels[i]))
						doc.Y[pos++] = irels[i];

				// System.out.println("Int rels");
				// for(int ir: irels){
				// System.out.print(ir + " ");
				// }
				// System.out.println("Original rels ");
				// for(Integer ir : intRels){
				// System.out.print(ir + " ");
				// }
				// System.out.println();
				// if((irels[0] !=0) && (intFeatures.size() ==1)){
				// System.out.println("Singleton =\t" + arg1 + "\t" + arg2);
				// }
			}

			// set mentions
			doc.setCapacity(intFeatures.size());
			doc.numMentions = intFeatures.size();

			for (int j = 0; j < intFeatures.size(); j++) {
				doc.Z[j] = -1;
				doc.mentionIDs[j] = j;
				SparseBinaryVector sv = doc.features[j] = new SparseBinaryVector();

				List<Integer> instanceFeatures = intFeatures.get(j);
				int[] fts = new int[instanceFeatures.size()];

				for (int i = 0; i < instanceFeatures.size(); i++)
					fts[i] = instanceFeatures.get(i);
				Arrays.sort(fts);
				int countUnique = 0;
				for (int i = 0; i < fts.length; i++)
					if (fts[i] != -1 && (i == 0 || fts[i - 1] != fts[i]))
						countUnique++;
				sv.num = countUnique;
				sv.ids = new int[countUnique];
				int pos = 0;
				for (int i = 0; i < fts.length; i++)
					if (fts[i] != -1 && (i == 0 || fts[i - 1] != fts[i]))
						sv.ids[pos++] = fts[i];

				// System.out.println("Int features");
				// for(int ft: fts){
				// System.out.print(ft + " ");
				// }
				// System.out.println();
			}
			doc.write(os);
			count++;

			if (count % 100000 == 0) {
				System.out.println(count + " entity pairs processed");
				printMemoryStatistics();
			}

		}
		os.close();
	}

	private static Integer getIntRelKey(String rel, Mappings m) {

		return m.getRelationID(rel, false);

	}

	private static List<Integer> convertFeaturesToIntegers(
			List<String> features, Mappings m) {

		List<Integer> intFeatures = new ArrayList<Integer>();

		for (String feature : features) {
			Integer intFeature = m.getFeatureID(feature, false);
			if (intFeature != -1) {
				intFeatures.add(intFeature);
			}
		}

		return intFeatures;
	}

	private static String getStringKey(Integer intKey) {
		if (intToKeyMap.containsKey(intKey)) {
			return intToKeyMap.get(intKey);
		} else {
			throw new IllegalStateException();
		}
	}

	private static Integer getIntKey(String key) {
		if (keyToIntegerMap.containsKey(key)) {
			return keyToIntegerMap.get(key);
		} else {
			Integer intKey = keyToIntegerMap.size();
			keyToIntegerMap.put(key, intKey);
			intToKeyMap.put(intKey, key);
			return intKey;
		}
	}

	private static void printMemoryStatistics() {
		double freeMemory = Runtime.getRuntime().freeMemory()
				/ GIGABYTE_DIVISOR;
		double allocatedMemory = Runtime.getRuntime().totalMemory()
				/ GIGABYTE_DIVISOR;
		double maxMemory = Runtime.getRuntime().maxMemory() / GIGABYTE_DIVISOR;
		System.out.println("MAX MEMORY: " + maxMemory);
		System.out.println("ALLOCATED MEMORY: " + allocatedMemory);
		System.out.println("FREE MEMORY: " + freeMemory);
	}
	
	private static double getMaxMemory(){
		return Runtime.getRuntime().maxMemory();
	}
	
	private static double getAllocatedMemory(){
		return Runtime.getRuntime().totalMemory();
	}
}