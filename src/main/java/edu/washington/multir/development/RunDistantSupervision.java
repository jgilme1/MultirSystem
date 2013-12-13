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
import edu.washington.multir.distantsupervision.DistantSupervision;
import edu.washington.multir.knowledgebase.KnowledgeBase;

/**
 * An app for running distant supervision
 * @author jgilme1
 *
 */
public class RunDistantSupervision {
	
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

	
	
	public static void main(String[] args) throws SQLException, IOException{
    	long start = System.currentTimeMillis();

		//initialize variables
		CorpusInformationSpecification cis =  new DefaultCorpusInformationSpecification();
		Corpus c = new Corpus(args[0],cis,true);
		String dsFileName = args[0]+"DS";
		
		
		ArgumentIdentification ai = NERArgumentIdentification.getInstance();
		SententialInstanceGeneration sig = NERSententialInstanceGeneration.getInstance();
		RelationMatching rm = new NERRelationMatching();
		KnowledgeBase kb = new KnowledgeBase(args[1],args[2],args[3]);
		
		boolean neFlag = (args[4].equals("true"))? true : false;
		
		DistantSupervision ds = new DistantSupervision(ai,sig,rm,neFlag);
		ds.run(dsFileName, kb, c);
		
	}
}
