package edu.washington.multir.development;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;

import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.CustomCorpusInformationSpecification;
import edu.washington.multir.corpus.DocumentInformationI;
import edu.washington.multir.corpus.SentInformationI;
import edu.washington.multir.corpus.TokenInformationI;
import edu.washington.multir.util.CLIUtils;

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
	 * args[4-...] - Use option -si to declare a list of SentInformationI class names, option -di 
	 * for DocumentInformationI class names and -ti for TokenInformationI class names, these
	 * classes will be used to extend the DefaultCorpusInformationSpecification
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
		
//		Options options = new Options();
//		OptionBuilder.hasArgs(10);
//		OptionBuilder.withDescription("List of All SentInformationI to be added to CorpusSpecification");
//		options.addOption(OptionBuilder.create("si"));
//		OptionBuilder.hasArgs(10);
//		OptionBuilder.withDescription("List of All DocInformationI to be added to CorpusSpecification");
//		options.addOption(OptionBuilder.create("di"));
//		OptionBuilder.hasArgs(10);
//		OptionBuilder.withDescription("List of All TokenInformationI to be added to CorpusSpecification");
//		options.addOption(OptionBuilder.create("ti"));
////		
////		options.addOption("cis",true,"corpusInformationSpecification algorithm class");
////		options.addOptionGroup(arg0)
//		
//		CommandLineParser parser = new BasicParser();
//		CommandLine cmd = parser.parse(options, args);
//		List<String> remainingArgs = cmd.getArgList();
//		CustomCorpusInformationSpecification cis = new CustomCorpusInformationSpecification();
//		
////		String corpusInformationSpecificationClass = cmd.getOptionValue("cis");
////		if(corpusInformationSpecificationClass != null){
////			ClassLoader cl = ClassLoader.getSystemClassLoader();
////			String corpusInformationClassPrefix = "edu.washington.multir.corpus.";
////			Class<?> corpusInformationClass = cl.loadClass(corpusInformationClassPrefix+corpusInformationSpecificationClass);
////			cis = (CorpusInformationSpecification) corpusInformationClass.newInstance();
////		}
////		else{
////			throw new IllegalArgumentException("Option -cis must be set with the name of a concrete CorpusInformationSpecification class");
////		}
//		
//		String[] sentInformationSpecificationClasses = cmd.getOptionValues("si");
//		if(sentInformationSpecificationClasses != null){
//			List<SentInformationI> sentenceInformation = new ArrayList<>();
//			for(String sentInformationSpecificationClass : sentInformationSpecificationClasses){
//				ClassLoader cl = ClassLoader.getSystemClassLoader();
//				String corpusInformationClassPrefix = "edu.washington.multir.corpus.";
//				Class<?> sentInformationClass = cl.loadClass(corpusInformationClassPrefix+sentInformationSpecificationClass);
//				sentenceInformation.add((SentInformationI)sentInformationClass.newInstance());
//			}
//			cis.addSentenceInformation(sentenceInformation);
//		}
//		
//		String[] documentInformationSpecificationClasses = cmd.getOptionValues("di");
//		if(documentInformationSpecificationClasses!=null){
//			List<DocumentInformationI> docInformation = new ArrayList<>();
//			for(String docInformationSpecification : documentInformationSpecificationClasses){
//				ClassLoader cl = ClassLoader.getSystemClassLoader();
//				String corpusInformationClassPrefix = "edu.washington.multir.corpus.";
//				Class<?> documentInformationClass = cl.loadClass(corpusInformationClassPrefix+docInformationSpecification);
//				docInformation.add((DocumentInformationI)documentInformationClass.newInstance());
//			}
//			cis.addDocumentInformation(docInformation);
//		}
//		
//		
//		String[] tokenInformationSpecificationClasses = cmd.getOptionValues("ti");
//		if(tokenInformationSpecificationClasses != null){
//			List<TokenInformationI> tokInformation = new ArrayList<>();
//			for(String tokenInformationSpecificationClass : tokenInformationSpecificationClasses){
//				ClassLoader cl = ClassLoader.getSystemClassLoader();
//				String corpusInformationClassPrefix = "edu.washington.multir.corpus.";
//				Class<?> tokenInformationClass = cl.loadClass(corpusInformationClassPrefix+tokenInformationSpecificationClass);
//				tokInformation.add((TokenInformationI)tokenInformationClass.newInstance());
//			}
//			cis.addTokenInformation(tokInformation);
//		}
//		
		List<String> arguments  = new ArrayList<String>();
		for(String arg: args){
			arguments.add(arg);
		}
		CorpusInformationSpecification cis = CLIUtils.loadCorpusInformationSpecification(arguments);
    	Corpus c = new Corpus(arguments.get(0),cis,false);
    	long start = System.currentTimeMillis();
    	c.loadCorpus(new File(arguments.get(1)), arguments.get(2), arguments.get(3));
    	long end = System.currentTimeMillis();
    	System.out.println("Loading DB took " + (end-start) + " millisseconds");    	
	}
}
