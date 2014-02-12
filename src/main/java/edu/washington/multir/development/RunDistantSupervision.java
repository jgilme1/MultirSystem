package edu.washington.multir.development;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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
import edu.washington.multir.distantsupervision.NegativeExampleCollection;
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
	 * @throws ClassNotFoundException 
	 * @throws ParseException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InstantiationException 
	 */

	
	
	public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException, ParseException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, InstantiationException{		
		
		Options options = new Options();
		options.addOption("cis",true,"corpusInformationSpecification algorithm class");
		options.addOption("ai",true,"argumentIdentification algorithm class");
		options.addOption("sig",true,"sententialInstanceGeneration algorithm class");
		options.addOption("rm",true,"relationMatching algorithm class");
		options.addOption("nec",true,"negative example collection algorithm class");
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		List<String> remainingArgs = cmd.getArgList();
		
		
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		CorpusInformationSpecification cis = null;
		ArgumentIdentification ai = null;
		SententialInstanceGeneration sig = null;
		RelationMatching rm = null;
		NegativeExampleCollection nec = null;
		
		String corpusInformationSpecificationName = cmd.getOptionValue("cis");
		String argumentIdentificationName = cmd.getOptionValue("ai");
		String sententialInstanceGenerationName = cmd.getOptionValue("sig");
		String relationMatchingName = cmd.getOptionValue("rm");
		String negativeExampleCollectionName = cmd.getOptionValue("nec");
		
		if(corpusInformationSpecificationName != null){
			String corpusInformationSpecificationClassPrefix = "edu.washington.multir.corpus.";
			Class<?> c = cl.loadClass(corpusInformationSpecificationClassPrefix+corpusInformationSpecificationName);
			cis = (CorpusInformationSpecification) c.newInstance();
		}
		else{
			throw new IllegalArgumentException("corpusInformationSpecification Class Argument is invalid");
		}
		
		if(argumentIdentificationName != null){
			String argumentIdentificationClassPrefix = "edu.washington.multir.argumentidentification.";
			Class<?> c = cl.loadClass(argumentIdentificationClassPrefix+argumentIdentificationName);
			Method m = c.getMethod("getInstance");
			ai = (ArgumentIdentification) m.invoke(null);
		}
		else{
			throw new IllegalArgumentException("argumentIdentification Class Argument is invalid");
		}
		
		if(sententialInstanceGenerationName != null){
			String sententialInstanceClassPrefix = "edu.washington.multir.argumentidentification.";
			Class<?> c = cl.loadClass(sententialInstanceClassPrefix+sententialInstanceGenerationName);
			Method m = c.getMethod("getInstance");
			sig = (SententialInstanceGeneration) m.invoke(null);
		}
		else{
			throw new IllegalArgumentException("sententialInstanceGeneration Class Argument is invalid");
		}
		
		if(relationMatchingName != null){
			String relationMatchingClassPrefix = "edu.washington.multir.argumentidentification.";
			Class<?> c = cl.loadClass(relationMatchingClassPrefix+relationMatchingName);
			Method m = c.getMethod("getInstance");
			rm = (RelationMatching) m.invoke(null);
		}
		else{
			throw new IllegalArgumentException("relationMatching Class Argument is invalid");
		}
		
		Corpus c = new Corpus(remainingArgs.get(0),cis,true);
		String dsFileName = remainingArgs.get(1);
		KnowledgeBase kb = new KnowledgeBase(remainingArgs.get(2),remainingArgs.get(3),remainingArgs.get(4));
		
		boolean neFlag = (remainingArgs.get(5).equals("true"))? true : false;
		
		double ratio = 0.0;
		if(neFlag){
			if(remainingArgs.size() == 7){
				ratio = Double.parseDouble(remainingArgs.get(6));
			}
		}
		
		if(negativeExampleCollectionName != null){
			String negativeExampleCollectionClassPrefix = "edu.washington.multir.distantsupervision.";
			Class<?> necClass = cl.loadClass(negativeExampleCollectionClassPrefix+negativeExampleCollectionName);
			Method m = necClass.getMethod("getInstance", double.class);
			nec =  (NegativeExampleCollection) m.invoke(null,ratio);
		}
		else{
			throw new IllegalArgumentException("negativeExampleCollection Class Argument is invalid");
		}
		
		DistantSupervision ds = new DistantSupervision(ai,sig,rm,nec,neFlag);
		ds.run(dsFileName,kb,c);
	}
}
