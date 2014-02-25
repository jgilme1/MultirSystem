package edu.washington.multir.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.washington.multir.argumentidentification.ArgumentIdentification;
import edu.washington.multir.argumentidentification.RelationMatching;
import edu.washington.multir.argumentidentification.SententialInstanceGeneration;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.CustomCorpusInformationSpecification;
import edu.washington.multir.corpus.DocumentInformationI;
import edu.washington.multir.corpus.SentInformationI;
import edu.washington.multir.corpus.TokenInformationI;
import edu.washington.multir.distantsupervision.NegativeExampleCollection;

public class CLIUtils {
	
	/**
	 * Returns A CorpusInformationSpecification object using the proper 
	 * command line options -si, -di, or -ti. These options can have 
	 * a list of arguments so any non-option arguments should occur first
	 * @return
	 * @throws ParseException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static CorpusInformationSpecification loadCorpusInformationSpecification(List<String> args) throws ParseException, ClassNotFoundException, InstantiationException, IllegalAccessException{
		Options options = new Options();
		OptionBuilder.hasArgs(10);
		OptionBuilder.withDescription("List of All SentInformationI to be added to CorpusSpecification");
		options.addOption(OptionBuilder.create("si"));
		OptionBuilder.hasArgs(10);
		OptionBuilder.withDescription("List of All DocInformationI to be added to CorpusSpecification");
		options.addOption(OptionBuilder.create("di"));
		OptionBuilder.hasArgs(10);
		OptionBuilder.withDescription("List of All TokenInformationI to be added to CorpusSpecification");
		options.addOption(OptionBuilder.create("ti"));
		
		List<Integer> relevantArgIndices = getContiguousArgumentsForOptions(args,"si","di","ti");
		List<String> relevantArguments = new ArrayList<String>();
		List<String> remainingArguments = new ArrayList<String>();
		for(Integer i: relevantArgIndices){
			relevantArguments.add(args.get(i));
		}
		for(Integer i =0; i < args.size(); i++){
			if(!relevantArgIndices.contains(i)){
				remainingArguments.add(args.get(i));
			}
		}
		
		
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, relevantArguments.toArray(new String[relevantArguments.size()]));
		CustomCorpusInformationSpecification cis = new CustomCorpusInformationSpecification();
		
		String[] sentInformationSpecificationClasses = cmd.getOptionValues("si");
		if(sentInformationSpecificationClasses != null){
			List<SentInformationI> sentenceInformation = new ArrayList<>();
			for(String sentInformationSpecificationClass : sentInformationSpecificationClasses){
				ClassLoader cl = ClassLoader.getSystemClassLoader();
				String corpusInformationClassPrefix = "edu.washington.multir.corpus.";
				Class<?> sentInformationClass = cl.loadClass(corpusInformationClassPrefix+sentInformationSpecificationClass);
				sentenceInformation.add((SentInformationI)sentInformationClass.newInstance());
			}
			cis.addSentenceInformation(sentenceInformation);
		}
		
		String[] documentInformationSpecificationClasses = cmd.getOptionValues("di");
		if(documentInformationSpecificationClasses!=null){
			List<DocumentInformationI> docInformation = new ArrayList<>();
			for(String docInformationSpecification : documentInformationSpecificationClasses){
				ClassLoader cl = ClassLoader.getSystemClassLoader();
				String corpusInformationClassPrefix = "edu.washington.multir.corpus.";
				Class<?> documentInformationClass = cl.loadClass(corpusInformationClassPrefix+docInformationSpecification);
				docInformation.add((DocumentInformationI)documentInformationClass.newInstance());
			}
			cis.addDocumentInformation(docInformation);
		}
		
		
		String[] tokenInformationSpecificationClasses = cmd.getOptionValues("ti");
		if(tokenInformationSpecificationClasses != null){
			List<TokenInformationI> tokInformation = new ArrayList<>();
			for(String tokenInformationSpecificationClass : tokenInformationSpecificationClasses){
				ClassLoader cl = ClassLoader.getSystemClassLoader();
				String corpusInformationClassPrefix = "edu.washington.multir.corpus.";
				Class<?> tokenInformationClass = cl.loadClass(corpusInformationClassPrefix+tokenInformationSpecificationClass);
				tokInformation.add((TokenInformationI)tokenInformationClass.newInstance());
			}
			cis.addTokenInformation(tokInformation);
		}
		
		removeUsedArguments(remainingArguments,args);
		
		
		return cis;
	}

	private static List<Integer> getContiguousArgumentsForOptions(
			List<String> args, String ... options) {
		List<String> optionList = new ArrayList<String>();
		List<Integer> relevantTokens = new ArrayList<Integer>();
		for(String opt: options){
			optionList.add(opt);
		}
		
		boolean foundTargetOption = false;
		for(Integer i = 0; i < args.size(); i++){
			String iString = args.get(i);
			if(isOption(iString)){
				if(optionList.contains(iString.substring(1))){
					relevantTokens.add(i);
					foundTargetOption = true;
				}
				else{
					foundTargetOption = false;
				}
			}
			else{
				if(foundTargetOption){
					relevantTokens.add(i);
				}
			}
		}
		
		return relevantTokens;
	}
	
	private static boolean isOption(String str){
		if(str.startsWith("-")){
			return true;
		}
		else{
			return false;
		}
	}


	public static ArgumentIdentification loadArgumentIdentification(
			List<String> arguments) throws ParseException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Options options = new Options();
		options.addOption("ai",true,"argumentIdentification algorithm class");
		
		
		List<Integer> relevantArgIndices = getContiguousArgumentsForOptions(arguments,"ai");
		List<String> relevantArguments = new ArrayList<String>();
		List<String> remainingArguments = new ArrayList<String>();
		for(Integer i: relevantArgIndices){
			relevantArguments.add(arguments.get(i));
		}
		for(Integer i =0; i < arguments.size(); i++){
			if(!relevantArgIndices.contains(i)){
				remainingArguments.add(arguments.get(i));
			}
		}
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, relevantArguments.toArray(new String[relevantArguments.size()]));
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		ArgumentIdentification ai = null;
		String argumentIdentificationName = cmd.getOptionValue("ai");
		if(argumentIdentificationName != null){
			String argumentIdentificationClassPrefix = "edu.washington.multir.argumentidentification.";
			Class<?> c = cl.loadClass(argumentIdentificationClassPrefix+argumentIdentificationName);
			Method m = c.getMethod("getInstance");
			ai = (ArgumentIdentification) m.invoke(null);
		}
		else{
			throw new IllegalArgumentException("argumentIdentification Class Argument is invalid");
		}

		removeUsedArguments(remainingArguments,arguments);
		return ai;
	}
	
	

	private static void removeUsedArguments(List<String> remainingArgs,
			List<String> arguments) {
		//update parameter args
		arguments.clear();
		arguments.addAll(remainingArgs);
	}

	public static SententialInstanceGeneration loadSententialInformationGeneration(
			List<String> arguments) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ParseException {
		Options options = new Options();
		options.addOption("sig",true,"sententialInstanceGeneration algorithm class");
		
		List<Integer> relevantArgIndices = getContiguousArgumentsForOptions(arguments,"sig");
		List<String> relevantArguments = new ArrayList<String>();
		List<String> remainingArguments = new ArrayList<String>();
		for(Integer i: relevantArgIndices){
			relevantArguments.add(arguments.get(i));
		}
		for(Integer i =0; i < arguments.size(); i++){
			if(!relevantArgIndices.contains(i)){
				remainingArguments.add(arguments.get(i));
			}
		}
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, relevantArguments.toArray(new String[relevantArguments.size()]));
		
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		SententialInstanceGeneration sig = null;

		String sententialInstanceGenerationName = cmd.getOptionValue("sig");

		if(sententialInstanceGenerationName != null){
			String sententialInstanceClassPrefix = "edu.washington.multir.argumentidentification.";
			Class<?> c = cl.loadClass(sententialInstanceClassPrefix+sententialInstanceGenerationName);
			Method m = c.getMethod("getInstance");
			sig = (SententialInstanceGeneration) m.invoke(null);
		}
		else{
			throw new IllegalArgumentException("sententialInstanceGeneration Class Argument is invalid");
		}
		
		removeUsedArguments(remainingArguments,arguments);
		
		return sig;
	}

	public static RelationMatching loadRelationMatching(List<String> arguments) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, ParseException, NoSuchMethodException, SecurityException {
		Options options = new Options();
		options.addOption("rm",true,"relationMatching algorithm class");
		
		List<Integer> relevantArgIndices = getContiguousArgumentsForOptions(arguments,"rm");
		List<String> relevantArguments = new ArrayList<String>();
		List<String> remainingArguments = new ArrayList<String>();
		for(Integer i: relevantArgIndices){
			relevantArguments.add(arguments.get(i));
		}
		for(Integer i =0; i < arguments.size(); i++){
			if(!relevantArgIndices.contains(i)){
				remainingArguments.add(arguments.get(i));
			}
		}
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, relevantArguments.toArray(new String[relevantArguments.size()]));
		
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		RelationMatching rm = null;
		
		String relationMatchingName = cmd.getOptionValue("rm");

		
		if(relationMatchingName != null){
			String relationMatchingClassPrefix = "edu.washington.multir.argumentidentification.";
			Class<?> c = cl.loadClass(relationMatchingClassPrefix+relationMatchingName);
			Method m = c.getMethod("getInstance");
			rm = (RelationMatching) m.invoke(null);
		}
		else{
			throw new IllegalArgumentException("relationMatching Class Argument is invalid");
		}
		
		removeUsedArguments(remainingArguments,arguments);
		return rm;
	}

	public static NegativeExampleCollection loadNegativeExampleCollection(
			List<String> arguments) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ParseException {
		Options options = new Options();
		options.addOption("nec",true,"negativeExample collection algorithm class");
		options.addOption("ratio",true,"negative Example to positive example ratio");
		
		List<Integer> relevantArgIndices = getContiguousArgumentsForOptions(arguments,"nec","ratio");
		List<String> relevantArguments = new ArrayList<String>();
		List<String> remainingArguments = new ArrayList<String>();
		for(Integer i: relevantArgIndices){
			relevantArguments.add(arguments.get(i));
		}
		for(Integer i =0; i < arguments.size(); i++){
			if(!relevantArgIndices.contains(i)){
				remainingArguments.add(arguments.get(i));
			}
		}
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, relevantArguments.toArray(new String[relevantArguments.size()]));
		
		
		
		
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		NegativeExampleCollection nec = null;
		double ratio;
		
		String negativeExampleCollectionName = cmd.getOptionValue("nec");
		String ratioString = cmd.getOptionValue("ratio");
		
		if(ratioString != null){
			ratio =Double.parseDouble(ratioString);
		}
		else{
			throw new IllegalArgumentException("ratio argument is invalid");
		}
		
		if(negativeExampleCollectionName != null){
			String relationMatchingClassPrefix = "edu.washington.multir.distantsupervision.";
			Class<?> c = cl.loadClass(relationMatchingClassPrefix+negativeExampleCollectionName);
			Method m = c.getMethod("getInstance",double.class);
			nec = (NegativeExampleCollection) m.invoke(null,ratio);
		}
		else{
			throw new IllegalArgumentException("negativeExampleCollection Class Argument is invalid");
		}
		
		removeUsedArguments(remainingArguments,arguments);
		return nec;
	}
	
	

}
