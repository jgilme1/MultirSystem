package edu.washington.multir.development;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.cli.ParseException;

import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.featuregeneration.FeatureGeneration;
import edu.washington.multir.featuregeneration.FeatureGenerator;
import edu.washington.multir.featuregeneration.SortedFeatureGeneration;
import edu.washington.multir.util.CLIUtils;
import edu.washington.multir.util.FigerTypeUtils;

public class RunSortedFeatureGeneration {
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
		
		
		List<String> arguments  = new ArrayList<String>();
		for(String arg: args){
			arguments.add(arg);
		}
		CorpusInformationSpecification cis = CLIUtils.loadCorpusInformationSpecification(arguments);
		FeatureGenerator fg = CLIUtils.loadFeatureGenerator(arguments);
		
		Corpus c = new Corpus(arguments.get(0),cis,true);
		
		SortedFeatureGeneration featureGeneration = new SortedFeatureGeneration(fg);
		FigerTypeUtils.init();
		featureGeneration.run(arguments.get(1),arguments.get(2),c,cis);
		FigerTypeUtils.close();
	}
}
