package edu.washington.multir.development;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecificationWithNEL;

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
	 * args[0] - name of corpus database
	 * args[1] - path to the corpus information directory
	 * args[2] - name of temporary sentence file for batch insertion into Derby DB
	 * args[3] - name of temporary document file for batch insertion into Derby DB
	 * args[4] - name of the Corpus Information class to load in e.g. "DefaultCorpusInformationSpecification"
	 * @param args
	 * @throws SQLException
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException{
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		String corpusInformationClassPrefix = "edu.washington.multir.corpus.";
		Class<?> corpusInformationClass = cl.loadClass(corpusInformationClassPrefix+args[4]);
		CorpusInformationSpecification cis = (CorpusInformationSpecification) corpusInformationClass.newInstance();
    	Corpus c = new Corpus(args[0],cis,false);
    	long start = System.currentTimeMillis();
    	c.loadCorpus(new File(args[1]), args[2], args[3]);
    	long end = System.currentTimeMillis();
    	System.out.println("Loading DB took " + (end-start) + " millisseconds");    	
	}
}
