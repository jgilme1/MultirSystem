package edu.washington.multir.development;

import java.io.IOException;
import java.sql.SQLException;

import edu.washington.multir.argumentidentification.ArgumentIdentification;
import edu.washington.multir.argumentidentification.NELArgumentIdentification;
import edu.washington.multir.argumentidentification.NELRelationMatching;
import edu.washington.multir.argumentidentification.NERArgumentIdentification;
import edu.washington.multir.argumentidentification.NERRelationMatching;
import edu.washington.multir.argumentidentification.NERSententialInstanceGeneration;
import edu.washington.multir.argumentidentification.RelationMatching;
import edu.washington.multir.argumentidentification.SententialInstanceGeneration;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecificationWithNEL;
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
	 *      args[5] is optional, and is a ratio of positive to negative examples
	 * @throws SQLException
	 * @throws IOException
	 */

	
	
	public static void main(String[] args) throws SQLException, IOException{
		//initialize variables
		CorpusInformationSpecification cis =  new DefaultCorpusInformationSpecification();
		Corpus c = new Corpus(args[0],cis,true);
		String dsFileName = args[0]+"DS";
		
		
		ArgumentIdentification ai = NERArgumentIdentification.getInstance();
		SententialInstanceGeneration sig = NERSententialInstanceGeneration.getInstance();
		RelationMatching rm = NERRelationMatching.getInstance();
		KnowledgeBase kb = new KnowledgeBase(args[1],args[2],args[3]);
		
		boolean neFlag = (args[4].equals("true"))? true : false;
		
		double ratio = 0.0;
		if(neFlag){
			if(args.length == 6){
				ratio = Double.parseDouble(args[5]);
			}
		}
		
		DistantSupervision ds = new DistantSupervision(ai,sig,rm,neFlag,ratio);
		ds.run(dsFileName, kb, c);
		
	}
}
