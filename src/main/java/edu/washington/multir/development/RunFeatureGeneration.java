package edu.washington.multir.development;


import java.io.IOException;
import java.sql.SQLException;

import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multir.featuregeneration.DefaultFeatureGenerator;
import edu.washington.multir.featuregeneration.FeatureGeneration;
import edu.washington.multir.featuregeneration.FeatureGenerator;

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
	 */
	public static void main(String[] args) throws SQLException, IOException{
		//initialize variables
		CorpusInformationSpecification cis = new DefaultCorpusInformationSpecification();
		FeatureGenerator fg = new DefaultFeatureGenerator();
		Corpus c = new Corpus(args[0],cis,true);
		
		FeatureGeneration featureGeneration = new FeatureGeneration(fg);
		featureGeneration.run(args[1],args[2],c,cis);
	}
}
