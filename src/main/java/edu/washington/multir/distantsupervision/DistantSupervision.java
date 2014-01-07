package edu.washington.multir.distantsupervision;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.argumentidentification.ArgumentIdentification;
import edu.washington.multir.argumentidentification.RelationMatching;
import edu.washington.multir.argumentidentification.SententialInstanceGeneration;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multir.data.Argument;
import edu.washington.multir.data.KBArgument;
import edu.washington.multir.knowledgebase.KnowledgeBase;
import edu.washington.multir.util.BufferedIOUtils;

public class DistantSupervision {

	private ArgumentIdentification ai;
	private SententialInstanceGeneration sig;
	private RelationMatching rm;
	private boolean negativeExampleFlag;
	private List<Pair<Triple<KBArgument,KBArgument,String>,Integer>> globalNegativeExampleAnnotations = new ArrayList<>();
	private int distantSupervisionAnnotationCount = 0;
	private double positiveToNegativeRatio = 1.0;
	private static final int NEGATIVE_EXAMPLE_FLUSH_CONSTANT = 1000;

	public DistantSupervision(ArgumentIdentification ai, SententialInstanceGeneration sig, RelationMatching rm, boolean b){
		this.ai = ai;
		this.sig = sig;
		this.rm =rm;
		negativeExampleFlag = b;
	}
	
	public DistantSupervision(ArgumentIdentification ai, SententialInstanceGeneration sig, RelationMatching rm, boolean b, double ratio){
		this(ai,sig,rm,b);
		if(ratio > 0.0) positiveToNegativeRatio = ratio;
	}

	public void run(String outputFileName,KnowledgeBase kb, Corpus c) throws SQLException, IOException{
    	long start = System.currentTimeMillis();
		//PrintWriter dsWriter = new PrintWriter(new FileWriter(new File(outputFileName)));
    	PrintWriter dsWriter = new PrintWriter(BufferedIOUtils.getBufferedWriter(new File(outputFileName)));
		Iterator<Annotation> di = c.getDocumentIterator();
		int count =0;
		while(di.hasNext()){
			Annotation d = di.next();
			List<CoreMap> sentences = d.get(CoreAnnotations.SentencesAnnotation.class);
			for(CoreMap sentence : sentences){
				int sentGlobalID = sentence.get(SentGlobalID.class);
				
				//argument identification
				List<Argument> arguments =  ai.identifyArguments(d,sentence);
				//sentential instance generation
				List<Pair<Argument,Argument>> sententialInstances = sig.generateSententialInstances(arguments, sentence);
				//relation matching
				List<Triple<KBArgument,KBArgument,String>> distantSupervisionAnnotations = 
						rm.matchRelations(sententialInstances,kb);
				distantSupervisionAnnotationCount += distantSupervisionAnnotations.size();
				//adding sentence IDs
				List<Pair<Triple<KBArgument,KBArgument,String>,Integer>> dsAnnotationWithSentIDs = new ArrayList<>();
				for(Triple<KBArgument,KBArgument,String> trip : distantSupervisionAnnotations){
					Integer i = new Integer(sentGlobalID);
					Pair<Triple<KBArgument,KBArgument,String>,Integer> p = new Pair<>(trip,i);
					dsAnnotationWithSentIDs.add(p);
				}
				//negative example annotations
				List<Pair<Triple<KBArgument,KBArgument,String>,Integer>> negativeExampleAnnotations =
						findNegativeExampleAnnotations(sententialInstances,distantSupervisionAnnotations,
								kb.getEntityMap(),sentGlobalID);
				
				//writeArguments(arguments,argumentWriter);
				writeDistantSupervisionAnnotations(dsAnnotationWithSentIDs,dsWriter);
				writeDistantSupervisionAnnotations(negativeExampleAnnotations,dsWriter);
			}
			count++;
			if( count % 1000 == 0){
				System.out.println(count + " documents processed");
			}
		}
		Collections.shuffle(globalNegativeExampleAnnotations);
		writeDistantSupervisionAnnotations(globalNegativeExampleAnnotations.subList(0,(int)Math.floor(positiveToNegativeRatio*distantSupervisionAnnotationCount)),dsWriter);
		
		dsWriter.close();
    	long end = System.currentTimeMillis();
    	System.out.println("Distant Supervision took " + (end-start) + " millisseconds");
	}
	
	
	private  List<Pair<Triple<KBArgument, KBArgument, String>,Integer>> findNegativeExampleAnnotations(
			List<Pair<Argument, Argument>> sententialInstances,
			List<Triple<KBArgument, KBArgument, String>> distantSupervisionAnnotations,
			Map<String,List<String>> entityMap, Integer sentGlobalID) {
		
		List<Pair<Triple<KBArgument,KBArgument,String>,Integer>> negativeExampleAnnotations = new ArrayList<>();
		if(negativeExampleFlag){			
			for(Pair<Argument,Argument> p : sententialInstances){
				//check that at least one argument is not in distantSupervisionAnnotations
				Argument arg1 = p.first;
				Argument arg2 = p.second;
				boolean canBeNegativeExample = true;
				for(Triple<KBArgument,KBArgument,String> t : distantSupervisionAnnotations){
					Argument annotatedArg1 = t.first;
					Argument annotatedArg2 = t.second;
					
					//if sententialInstance is a distance supervision annotation
					//then it is not a negative example candidate
					if( (arg1.getStartOffset() == annotatedArg1.getStartOffset()) &&
						(arg1.getEndOffset() == annotatedArg1.getEndOffset()) &&
						(arg2.getStartOffset() == annotatedArg2.getStartOffset()) &&
						(arg2.getEndOffset() == annotatedArg2.getEndOffset())){
						canBeNegativeExample = false;
						break;
					}
				}
				if(canBeNegativeExample){
					//look for KBIDs, select a random pair
					List<String> arg1Ids = new ArrayList<>();
					if(entityMap.containsKey(arg1.getArgName())){
						arg1Ids = entityMap.get(arg1.getArgName());
					}
					List<String> arg2Ids = new ArrayList<>();
					if(entityMap.containsKey(arg2.getArgName())){
						arg2Ids = entityMap.get(arg2.getArgName());
					}
					if( (!arg1Ids.isEmpty()) && (!arg2Ids.isEmpty())){
						Collections.shuffle(arg1Ids);
						Collections.shuffle(arg2Ids);
						String arg1Id = arg1Ids.get(0);
						String arg2Id = arg2Ids.get(0);
						KBArgument kbarg1 = new KBArgument(arg1,arg1Id);
						KBArgument kbarg2 = new KBArgument(arg2,arg2Id);
						Triple<KBArgument,KBArgument,String> t = new Triple<>(kbarg1,kbarg2,"NA");
						Pair<Triple<KBArgument,KBArgument,String>,Integer> negativeAnnotationPair = new Pair<>(t,sentGlobalID);
						if(!containsNegativeAnnotation(negativeExampleAnnotations,t)) negativeExampleAnnotations.add(negativeAnnotationPair);
					}
				}
			}
		}
		//Collections.shuffle(negativeExampleAnnotations);
		globalNegativeExampleAnnotations.addAll(negativeExampleAnnotations);
		if(globalNegativeExampleAnnotations.size() > NEGATIVE_EXAMPLE_FLUSH_CONSTANT){
			negativeExampleAnnotations = globalNegativeExampleAnnotations;
			Collections.shuffle(negativeExampleAnnotations);
			int oldCount = distantSupervisionAnnotationCount;
			distantSupervisionAnnotationCount =0;
			globalNegativeExampleAnnotations = new ArrayList<>();
			return negativeExampleAnnotations.subList(0,(int)Math.floor(positiveToNegativeRatio*oldCount));			
		}
		else{
			return new ArrayList<>();
		}
	}
	
	private boolean containsNegativeAnnotation(
			List<Pair<Triple<KBArgument, KBArgument, String>,Integer>> negativeExampleAnnotations,
			Triple<KBArgument, KBArgument, String> t) {
		for(Pair<Triple<KBArgument,KBArgument,String>,Integer> p : negativeExampleAnnotations){
			Triple<KBArgument,KBArgument,String> trip = p.first;
			if( (trip.first.getStartOffset() == t.first.getStartOffset()) &&
				(trip.first.getEndOffset() == t.first.getEndOffset()) &&
				(trip.second.getStartOffset() == t.second.getStartOffset()) &&
				(trip.second.getEndOffset() == t.second.getEndOffset()) ){
				return true;
			}
		}	
		return false;
	}


	/**
	 * Write out distant supervision annotation information
	 * @param distantSupervisionAnnotations
	 * @param dsWriter
	 * @param sentGlobalID
	 */
	private void writeDistantSupervisionAnnotations(
			List<Pair<Triple<KBArgument, KBArgument, String>,Integer>> distantSupervisionAnnotations, PrintWriter dsWriter) {
		for(Pair<Triple<KBArgument,KBArgument,String>,Integer> dsAnno : distantSupervisionAnnotations){
			Triple<KBArgument,KBArgument,String> trip = dsAnno.first;
			Integer sentGlobalID = dsAnno.second;
			KBArgument arg1 = trip.first;
			KBArgument arg2 = trip.second;
			String rel = trip.third;
			dsWriter.write(arg1.getKbId());
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg1.getStartOffset()));
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg1.getEndOffset()));
			dsWriter.write("\t");
			dsWriter.write(arg1.getArgName());
			dsWriter.write("\t");
			dsWriter.write(arg2.getKbId());
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg2.getStartOffset()));
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg2.getEndOffset()));
			dsWriter.write("\t");
			dsWriter.write(arg2.getArgName());
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(sentGlobalID));
			dsWriter.write("\t");
			dsWriter.write(rel);
			dsWriter.write("\n");
		}
	}
	
	public static class DistantSupervisionAnnotation{
		KBArgument arg1;
		KBArgument arg2;
		String rel;
		Integer sentID;
	}
}
