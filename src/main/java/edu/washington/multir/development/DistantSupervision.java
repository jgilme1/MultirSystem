package edu.washington.multir.development;

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
import edu.washington.multir.argumentidentification.NERArgumentIdentification;
import edu.washington.multir.argumentidentification.NERRelationMatching;
import edu.washington.multir.argumentidentification.NERSententialInstanceGeneration;
import edu.washington.multir.argumentidentification.RelationMatching;
import edu.washington.multir.argumentidentification.SententialInstanceGeneration;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multir.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multir.data.Argument;
import edu.washington.multir.data.KBArgument;
import edu.washington.multir.knowledgebase.KnowledgeBase;

/**
 * An app for running distant supervision
 * @author jgilme1
 *
 */
public class DistantSupervision {
	
	/**
	 * 
	 * @param args
	 * 		args[0] should be name of corpus database
	 * 		args[1] should be relationKBFilePath
	 * 	    args[2] should be entityKBFielPath
	 * 	    args[3] should be targetRelationsFilePath
	 *      args[4] should be true / false for negative examples
	 * @throws SQLException
	 * @throws IOException
	 */
	
	//negative example flag
	private static boolean neFlag;
	private static List<Triple<KBArgument,KBArgument,String>> negativeExampleCandidates;
	private static final int NECONSTANT = 10;
	
	
	public static void main(String[] args) throws SQLException, IOException{
    	long start = System.currentTimeMillis();

		//initialize variables
		CorpusInformationSpecification cis =  new DefaultCorpusInformationSpecification();
		Corpus c = new Corpus(args[0],cis,true);
		String dsFileName = args[0]+"DS";
		ArgumentIdentification ai = NERArgumentIdentification.getInstance();
		SententialInstanceGeneration sig = NERSententialInstanceGeneration.getInstance();
		RelationMatching rm = new NERRelationMatching();
		

		
		//parse negative example flag
		if(args[4].equals("true")){
			neFlag = true;
			negativeExampleCandidates = new ArrayList<>();
		}
		else if(args[4].equals("false")){
			neFlag = false;
		}
		else{
			throw new IllegalArgumentException("The 5 argument should be true or false");
		}
		
		KnowledgeBase kb = new KnowledgeBase(args[1],args[2],args[3]);

		PrintWriter dsWriter = new PrintWriter(dsFileName);
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
				List<Pair<Argument,Argument>> sententialInstances= sig.generateSententialInstances(arguments, sentence);
				//relation matching
				List<Triple<KBArgument,KBArgument,String>> distantSupervisionAnnotations = 
						rm.matchRelations(sententialInstances,kb);
				//negative example annotations
				List<Triple<KBArgument,KBArgument,String>> negativeExampleAnnotations =
						findNegativeExampleAnnotations(sententialInstances,distantSupervisionAnnotations,
								kb.getEntityMap());
				
				//writeArguments(arguments,argumentWriter);
				writeDistantSupervisionAnnotations(distantSupervisionAnnotations,dsWriter,sentGlobalID);
				writeDistantSupervisionAnnotations(negativeExampleAnnotations,dsWriter,sentGlobalID);
			}
			count++;
			if( count % 1000 == 0){
				System.out.println(count + " documents processed");
			}
		}
		dsWriter.close();
    	long end = System.currentTimeMillis();
    	System.out.println("Distant Supervision took " + (end-start) + " millisseconds");
	}
	
	private static List<Triple<KBArgument, KBArgument, String>> findNegativeExampleAnnotations(
			List<Pair<Argument, Argument>> sententialInstances,
			List<Triple<KBArgument, KBArgument, String>> distantSupervisionAnnotations,
			Map<String,List<String>> entityMap) {
		
		List<Triple<KBArgument,KBArgument,String>> negativeExampleAnnotations = new ArrayList<>();
		
		if(neFlag){			
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
						negativeExampleCandidates.add(t);
						if(negativeExampleCandidates.size() == NECONSTANT){
							negativeExampleAnnotations.add(t);
							negativeExampleCandidates.clear();
						}
					}
				}
			}
		}
		return negativeExampleAnnotations;
	}

	/**
	 * Write out distant supervision annotation information
	 * @param distantSupervisionAnnotations
	 * @param dsWriter
	 * @param sentGlobalID
	 */
	private static void writeDistantSupervisionAnnotations(
			List<Triple<KBArgument, KBArgument, String>> distantSupervisionAnnotations, PrintWriter dsWriter,
			int sentGlobalID) {
		for(Triple<KBArgument,KBArgument,String> dsAnno : distantSupervisionAnnotations){
			KBArgument arg1 = dsAnno.first;
			KBArgument arg2 = dsAnno.second;
			String rel = dsAnno.third;
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
}
