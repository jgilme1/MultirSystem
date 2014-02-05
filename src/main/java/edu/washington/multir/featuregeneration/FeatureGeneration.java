package edu.washington.multir.featuregeneration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.util.BufferedIOUtils;

public class FeatureGeneration {
	
	private FeatureGenerator fg;
	private Corpus c;
	private static final int TRAINING_INSTANCES_IN_MEMORY_CONSTANT = 3000;
	public FeatureGeneration(FeatureGenerator fg){
		this.fg = fg;
	}	
	public void run(String dsFileName, String featureFileName, Corpus c, CorpusInformationSpecification cis) throws FileNotFoundException, IOException, SQLException{
    	long start = System.currentTimeMillis();
    	this.c = c;
    	int lines =0;
		//initialize variables
		BufferedReader in;
		BufferedWriter bw;
		in = BufferedIOUtils.getBufferedReader(new File(dsFileName));
		bw = BufferedIOUtils.getBufferedWriter(new File(featureFileName));

		String nextLine = in.readLine();
		List<SententialArgumentPair> saps = new ArrayList<>();
		int count =0;
		while(nextLine != null){
			SententialArgumentPair sap = SententialArgumentPair.parseSAP(nextLine);
			boolean mergeSap = false;
			if(saps.size()>0){
				if(saps.get(saps.size()-1).matchesSAP(sap)){
					mergeSap = true;
				}
			}
			if(mergeSap){
				saps.get(saps.size()-1).mergeSAP(sap);
			}
			else{
				//check if sap size is large enough
				if((saps.size() != 0) && (saps.size() % TRAINING_INSTANCES_IN_MEMORY_CONSTANT == 0)){
					//process saps
					processSaps(saps,bw);
					saps = new ArrayList<>();
				}
				saps.add(sap);
			}			
			nextLine = in.readLine();
			lines ++;
			if(lines % 3000 == 0){
				System.out.println("Lines read = " + lines);
			}
		}
		processSaps(saps,bw);
		bw.close();
		in.close();
		
    	long end = System.currentTimeMillis();
    	System.out.println("Feature Generation took " + (end-start) + " millisseconds");	
	}

	private void processSaps(List<SententialArgumentPair> saps,
			BufferedWriter bw) throws SQLException, IOException {
		
		Map<Integer,Pair<CoreMap,Annotation>> sentAnnotationMap = new HashMap<>();
		Set<Integer> sentIds = new HashSet<Integer>();
		for(SententialArgumentPair sap: saps){
			sentIds.add(sap.sentID);
		}
		List<Integer> sentIdList = new ArrayList<Integer>(sentIds);
		int count = sentIds.size() / 1000;
		for(int i =0; i <= count; i++){
			int startIndex = 1000*i;
			int endIndex = Math.min(startIndex + 1000,sentIdList.size());
			Map<Integer,Pair<CoreMap,Annotation>> smallSentAnnotationMap = c.getAnnotationPairsForEachSentence(
					new HashSet<Integer>(sentIdList.subList(startIndex, endIndex)));
			sentAnnotationMap.putAll(smallSentAnnotationMap);
			System.out.println(endIndex + " sentences collected");
		}
		
		for(SententialArgumentPair sap : saps){
			
			Pair<CoreMap,Annotation> senAnnoPair = sentAnnotationMap.get(sap.sentID);
			List<String> features = fg.generateFeatures(sap.arg1Offsets.first,sap.arg1Offsets.second
					,sap.arg2Offsets.first,sap.arg2Offsets.second,senAnnoPair.first,senAnnoPair.second);
			bw.write(makeFeatureString(sap,features)+"\n");
		}
		
	}

	private  String makeFeatureString(SententialArgumentPair sap,
			List<String> features) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.valueOf(sap.getSentID()));
		sb.append("\t");
		sb.append(sap.getArg1Id());
		sb.append("\t");
		sb.append(sap.getArg2Id());
		sb.append("\t");
		for(String rel : sap.getRelations()){
			sb.append(rel);
			sb.append("|");
		}
		sb.setLength(sb.length()-1);
		sb.append("\t");
		for(String f: features){
			sb.append(f);
			sb.append("\t");
		}
		return sb.toString().trim();
	}

	private  Map<Integer, Pair<CoreMap,Annotation>> getSentAnnotationsMap(
			List<SententialArgumentPair> saps) throws SQLException {
		
		Set<Integer> sentIds = new HashSet<Integer>();
		
		for(SententialArgumentPair sap : saps){
			sentIds.add(sap.sentID);
		}
		
		return c.getAnnotationPairsForEachSentence(sentIds);
	}
	
	public static class SententialArgumentPair{
		
		private Integer sentID;
		private Pair<Integer,Integer> arg1Offsets;
		private Pair<Integer,Integer> arg2Offsets;
		private List<String> relations;
		private String arg1ID;
		private String arg2ID;
		
		private SententialArgumentPair(Integer sentID, Pair<Integer,Integer> arg1Offsets,
										Pair<Integer,Integer> arg2Offsets, String relation,
										String arg1ID, String arg2ID){
			this.sentID = sentID;
			this.arg1Offsets = arg1Offsets;
			this.arg2Offsets = arg2Offsets;
			relations = new ArrayList<String>();
			relations.add(relation);
			this.arg1ID = arg1ID;
			this.arg2ID = arg2ID;
		}
		
		
		public boolean matchesSAP(SententialArgumentPair other){
			if( (other.sentID.equals(this.sentID))
				&& (other.arg1Offsets.equals(this.arg1Offsets))
				&& (other.arg2Offsets.equals(this.arg2Offsets))
				&& (other.arg1ID.equals(this.arg1ID))
				&& (other.arg2ID.equals(this.arg2ID))){
				return true;
			}
			return false;
		}
		
		public void mergeSAP(SententialArgumentPair other){
			if(this.matchesSAP(other)){
				
				for(String rel : other.relations){
					if(!this.relations.contains(rel)){
						this.relations.add(rel);
					}
				}
			}
			else{
				throw new IllegalArgumentException("SententialArgumentPair other must match this SententialArgumentPair");
			}
			
		}
		
		public static SententialArgumentPair parseSAP(String dsLine){
			try{
				String[] values = dsLine.split("\t");
				Integer arg1Start = Integer.parseInt(values[1]);
				Integer arg1End = Integer.parseInt(values[2]);
				Pair<Integer,Integer> arg1Offsets = new Pair<>(arg1Start,arg1End);
				Integer arg2Start = Integer.parseInt(values[5]);
				Integer arg2End = Integer.parseInt(values[6]);
				Pair<Integer,Integer> arg2Offsets = new Pair<>(arg2Start,arg2End);
				Integer sentId = Integer.parseInt(values[8]);
				
				
				return new SententialArgumentPair(sentId,arg1Offsets,arg2Offsets,values[9],values[0],values[4]);
			}
			catch(Exception e){
				throw new IllegalArgumentException("Line cannot be parsed into a SententialArgumentPair");
			}
		}
		
		
		public String getArg1Id(){return arg1ID;}
		public String getArg2Id(){return arg2ID;}
		public List<String> getRelations(){return relations;}
		public Integer getSentID(){return sentID;}
		public Pair<Integer,Integer> getArg1Offsets(){return arg1Offsets;}
		public Pair<Integer,Integer> getArg2Offsets(){return arg2Offsets;}
	}

}
