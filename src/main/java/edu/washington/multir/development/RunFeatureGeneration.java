package edu.washington.multir.development;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.washington.multir.argumentidentification.ArgumentIdentification;
import edu.washington.multir.argumentidentification.RelationMatching;
import edu.washington.multir.argumentidentification.SententialInstanceGeneration;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecificationWithNEL;
import edu.washington.multir.featuregeneration.DefaultFeatureGenerator;
import edu.washington.multir.featuregeneration.FeatureGeneration;
import edu.washington.multir.featuregeneration.FeatureGenerator;
import edu.washington.multir.util.CLIUtils;
import edu.washington.multir.util.FigerTypeUtils;

/**
 * App for doing feature generation. Before this is run
 * DistantSupervision and AddNegativeExamples should have
 * been run.
 * @author jgilme1
 *
 */
public class RunFeatureGeneration {
	/**
	 * 
	 * @param args
	 * 			args[0] is path to DB file
	 * 			args[1] is path to Distant Supervision file
	 * 			args[2] is path to output features file
	 * @throws SQLException
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 * @throws ParseException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 */
	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException, ParseException, InstantiationException, IllegalAccessException, InterruptedException, ExecutionException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException{
//		Options options = new Options();
//		options.addOption("cis",true,"corpusInformationSpecification algorithm class");
//		options.addOption("fg",true,"featureGeneration algorithm class");
//		CommandLineParser parser = new BasicParser();
//		CommandLine cmd = parser.parse(options, args);
//		List<String> remainingArgs = cmd.getArgList();
//		
//		ClassLoader cl = ClassLoader.getSystemClassLoader();
//		CorpusInformationSpecification cis = null;
//		FeatureGenerator fg = null;
//		
//		String corpusInformationSpecificationName = cmd.getOptionValue("cis");
//		String featureGenerationName = cmd.getOptionValue("fg");
//		
//		if(corpusInformationSpecificationName != null){
//			String corpusInformationSpecificationClassPrefix = "edu.washington.multir.corpus.";
//			Class<?> c = cl.loadClass(corpusInformationSpecificationClassPrefix+corpusInformationSpecificationName);
//			cis = (CorpusInformationSpecification) c.newInstance();
//		}
//		else{
//			throw new IllegalArgumentException("corpusInformationSpecification Class Argument is invalid");
//		}
//		
//		if(featureGenerationName != null){
//			String featureGenerationClassPrefix = "edu.washington.multir.featuregeneration.";
//			Class<?> c = cl.loadClass(featureGenerationClassPrefix+featureGenerationName);
//			fg = (FeatureGenerator) c.newInstance();
//		}
//		else{
//			throw new IllegalArgumentException("argumentIdentification Class Argument is invalid");
//		}
//		
//		Corpus c = new Corpus(remainingArgs.get(0),cis,true);
//		
//		FeatureGeneration featureGeneration = new FeatureGeneration(fg);
//		FigerTypeUtils.init();
//		featureGeneration.run(remainingArgs.get(1),remainingArgs.get(2),c,cis);
//		FigerTypeUtils.close();
		
		
		List<String> arguments  = new ArrayList<String>();
		for(String arg: args){
			arguments.add(arg);
		}
		CorpusInformationSpecification cis = CLIUtils.loadCorpusInformationSpecification(arguments);
		FeatureGenerator fg = CLIUtils.loadFeatureGenerator(arguments);
		
		Corpus c = new Corpus(arguments.get(0),cis,true);
		
		FeatureGeneration featureGeneration = new FeatureGeneration(fg);
		FigerTypeUtils.init();
		featureGeneration.run(arguments.get(1),arguments.get(2),c,cis);
		FigerTypeUtils.close();
	}
}
