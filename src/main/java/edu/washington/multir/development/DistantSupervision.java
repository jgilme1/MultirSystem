package edu.washington.multir.development;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.argumentidentification.ArgumentIdentification;
import edu.washington.multir.argumentidentification.NERArgumentIdentification;
import edu.washington.multir.argumentidentification.DefaultRelationMatching;
import edu.washington.multir.argumentidentification.RelationMatching;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multir.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multir.data.Argument;
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
	 * 		args[0] should be train or test
	 * 		args[1] should be relationKBFilePath
	 * 	    args[2] should be entityKBFielPath
	 * 	    args[3] should be targetRelationsFilePath
	 * @throws SQLException
	 * @throws IOException
	 */
	
	
	public static void main(String[] args) throws SQLException, IOException{
    	long start = System.currentTimeMillis();

		//initialize variables
		CorpusInformationSpecification cis =  new DefaultCorpusInformationSpecification();
		Corpus c;
		String dsFileName;
		KnowledgeBase kb = new KnowledgeBase(args[1],args[2],args[3]);
		ArgumentIdentification ai = NERArgumentIdentification.getInstance();
		RelationMatching rm = new DefaultRelationMatching();
		ai.setKB(kb);
		
		//choose corpus based on first argument
		if(args[0].equals("train")){
			dsFileName = "distantSupervisionTrain";
			//load train corpus
			c = new Corpus(cis,true,true);
		}
		else if(args[0].equals("test")){
			dsFileName = "distantSupervisionTest";
			//load test corpus
			c = new Corpus(cis,true,false);
		}
		else{
			throw new IllegalArgumentException("Argument incorrect");
		}

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
				
				//relation matching
				List<Triple<Argument,Argument,String>> distantSupervisionAnnotations = rm.matchRelations(arguments,kb.getEntityPairRelationMap());
				
				//writeArguments(arguments,argumentWriter);
				writeDistantSupervisionAnnotations(distantSupervisionAnnotations,dsWriter,sentGlobalID);
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
	
	/**
	 * Write out distant supervision annotation information
	 * @param distantSupervisionAnnotations
	 * @param dsWriter
	 * @param sentGlobalID
	 */
	private static void writeDistantSupervisionAnnotations(
			List<Triple<Argument, Argument, String>> distantSupervisionAnnotations, PrintWriter dsWriter,
			int sentGlobalID) {
		for(Triple<Argument,Argument,String> dsAnno : distantSupervisionAnnotations){
			Argument arg1 = dsAnno.first;
			Argument arg2 = dsAnno.second;
			String rel = dsAnno.third;
			dsWriter.write(arg1.getArgID());
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg1.getStartOffset()));
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg1.getEndOffset()));
			dsWriter.write("\t");
			dsWriter.write(arg1.getArgName());
			dsWriter.write("\t");
			dsWriter.write(arg2.getArgID());
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
