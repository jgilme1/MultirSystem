package edu.washington.multir.development;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;

/**
 * An app for taking a prespecified corpus
 * directory an inputting the information
 * into the derby db.
 * 
 * args[0] - path to the corpus information directory
 * args[1] - path 
 * @author jgilme1
 */

public class LoadCorpus {
	/**
	 * args[0] - "train" or "test"
	 * args[0] - path to the corpus information directory
	 * args[1] - name of temporary sentence file for batch insertion into Derby DB
	 * args[2] - name of temporary document file for batch insertion intro Derby DB
	 * @param args
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void main(String[] args) throws SQLException, IOException{
		CorpusInformationSpecification cis = new DefaultCorpusInformationSpecification();
    	Corpus c;
    	if(args[0].equals("train")){
    		c = new Corpus(cis,false,true);
    	}
    	else if(args[0].equals("test")){
    		c = new Corpus(cis,false,false);
    	}
    	else{
    		throw new IllegalArgumentException("Bad argument, arg0 should be train or test");
    	}
    	long start = System.currentTimeMillis();
    	c.loadCorpus(new File(args[1]), args[2], args[3]);
    	long end = System.currentTimeMillis();
    	System.out.println("Loading DB took " + (end-start) + " millisseconds");
	}
}
