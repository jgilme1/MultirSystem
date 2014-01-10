package edu.washington.multir.development;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;

import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;

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
	 * there should be an option value for the CorpusInformationSpecification class
	 * @param args
	 * @throws SQLException
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ParseException{
		
		Options options = new Options();
		options.addOption("cis",true,"corpusInformationSpecification algorithm class");
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		List<String> remainingArgs = cmd.getArgList();
		CorpusInformationSpecification cis = null;
		
		String corpusInformationSpecificationClass = cmd.getOptionValue("cis");
		if(corpusInformationSpecificationClass != null){
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			String corpusInformationClassPrefix = "edu.washington.multir.corpus.";
			Class<?> corpusInformationClass = cl.loadClass(corpusInformationClassPrefix+corpusInformationSpecificationClass);
			cis = (CorpusInformationSpecification) corpusInformationClass.newInstance();
		}
		else{
			throw new IllegalArgumentException("Option -corpusInformationSpecification must be set with the name of a concrete CorpusInformationSpecification class");
		}
		
		
    	Corpus c = new Corpus(remainingArgs.get(0),cis,false);
    	long start = System.currentTimeMillis();
    	c.loadCorpus(new File(remainingArgs.get(1)), remainingArgs.get(2), remainingArgs.get(3));
    	long end = System.currentTimeMillis();
    	System.out.println("Loading DB took " + (end-start) + " millisseconds");    	
	}
}
