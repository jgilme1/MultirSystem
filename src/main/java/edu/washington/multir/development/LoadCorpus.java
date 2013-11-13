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
 * @author jgilme1
 *
 */
public class LoadCorpus {
	public static void main(String[] args) throws SQLException, IOException{
		CorpusInformationSpecification cis = new DefaultCorpusInformationSpecification();
    	Corpus c = new Corpus(cis,false,true);
    	long start = System.currentTimeMillis();
    	c.loadCorpus(new File("/scratch2/code/multir-reimplementation/PreprocessedCorpus/reformatted/train"), "dbsentencestrain", "dbdocumentstrain");
    	long end = System.currentTimeMillis();
    	System.out.println("Loading DB took " + (end-start) + " millisseconds");
	}
}
