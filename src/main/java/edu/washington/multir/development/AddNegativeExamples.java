package edu.washington.multir.development;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.argumentidentification.ArgumentIdentification;
import edu.washington.multir.argumentidentification.NERArgumentIdentification;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multir.data.Argument;
import edu.washington.multir.knowledgebase.KnowledgeBase;

/**
 * This is just an app for adding negative examples to
 * a distant supervision file. This should only be run
 * after running distant supervision.
 * 
 * Run the main method with the arguments
 * 1. "train" or "test"
 * 2. relationKBFilePath
 * 3. entityKBFilePath
 * 4. targetRelationsFilePath
 * 
 * After running this main method, the file
 * distantSupervisionTrain or
 * distantSupervisionTest will be modified to include
 * negative examples
 * 
 * @author jgilme1
 *
 */
public class AddNegativeExamples {
	
	
	public static void main(String[] args) throws SQLException, IOException{
    	long start = System.currentTimeMillis();

		//initialize variables
		String dsFileName = "";
		Corpus c;
		KnowledgeBase KB = new KnowledgeBase(args[1],args[2],args[3]);
		Map<String,List<String>> entityPairRelationMap = KB.getEntityPairRelationMap();
		ArgumentIdentification ai = NERArgumentIdentification.getInstance();
		CorpusInformationSpecification cis = new DefaultCorpusInformationSpecification();
		
		//choose corpus based on first argument
		if(args[0].equals("train")){
			dsFileName = "distantSupervisionTrain";
			c = new Corpus(cis,true,true);
		}
		else if(args[0].equals("test")){
			dsFileName = "distantSupervisionTest";
			c = new Corpus(cis,true,false);
		}
		else{
			throw new IllegalArgumentException("Argument incorrect");
		}
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(dsFileName),true));
		
		
		//iterate over documents
		Iterator<Annotation> di = c.getDocumentIterator();
		List<Triple<Argument,Argument,Integer>> nonRelatedEntityPairs = new ArrayList<>();
		int count =1;
		while(di.hasNext()){
			Annotation d= di.next();
			List<CoreMap> sentences = d.get(CoreAnnotations.SentencesAnnotation.class);
			for(CoreMap s : sentences){
				int sentId =s.get(SentGlobalID.class);
				List<Argument> arguments = ai.identifyArguments(d, s);
				//now ignore links and only consider unique pairs of offsets..
				List<Argument> filteredArguments = new ArrayList<Argument>();
				
				for(Argument arg: arguments){
					int offset = arg.getStartOffset();
					boolean add = true;
					for(Argument filteredarg : filteredArguments){
						if(filteredarg.getStartOffset() == offset){
							add = false;
						}
					}
					if(add){
						filteredArguments.add(arg);
					}
				}
				//shuffle list
				java.util.Collections.shuffle(filteredArguments);
				for(int i =0; i < filteredArguments.size(); i ++){
					for(int j =0; j < filteredArguments.size();j++){
						if( j != i){
							//check that there is no relation between these.
							Argument arg1 = filteredArguments.get(i);
							Argument arg2 = filteredArguments.get(j);
							
							List<String> arg1Ids = new ArrayList<>();
							if(KB.getEntityMap().containsKey(arg1.getArgName())){
								arg1Ids = KB.getEntityMap().get(arg1);
							}
							List<String> arg2Ids = new ArrayList<>();
							if(KB.getEntityMap().containsKey(arg2.getArgName())){
								arg2Ids = KB.getEntityMap().get(arg1.getArgName());
							}
							
							String key = arg1.getArgID()+arg2.getArgID();
							if(!entityPairRelationMap.containsKey(key)){
								// can be negative example
								Triple<Argument,Argument,Integer> t = new Triple<>(arg1,arg2,sentId);
								nonRelatedEntityPairs.add(t);
								//check if the size of the nonRelatedPairs is 10, if so add to output
								if(nonRelatedEntityPairs.size() == 10){
									writeDistantSupervisionNoRelationAnnotations(nonRelatedEntityPairs.get(9), bw);
									nonRelatedEntityPairs.clear();
								}
							}
						}
					}
				}
			}
			
			if(count % 1000 == 0){
				System.out.println(count + " document processed");
			}
			count++;
		}
		bw.close();
		
    	long end = System.currentTimeMillis();
    	System.out.println("Add Negative Examples took " + (end-start) + " millisseconds");
		
	}
	
	/**
	 * Writing NA distant supervision training instances
	 * @param triple
	 * @param dsWriter
	 * @throws IOException
	 */
	private static void writeDistantSupervisionNoRelationAnnotations(
			Triple<Argument, Argument, Integer> triple, BufferedWriter dsWriter) throws IOException {
			Argument arg1 = triple.first;
			Argument arg2 = triple.second;
			String rel = "NA";
			dsWriter.write(arg1.getArgID());
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg1.getStartOffset()));
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg1.getEndOffset()));
			dsWriter.write("\t");
			dsWriter.write(arg1.getArgName());
			dsWriter.write("\t");
			dsWriter.write(arg2.getArgID());
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg2.getStartOffset()));
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(arg2.getEndOffset()));
			dsWriter.write("\t");
			dsWriter.write(arg2.getArgName());
			dsWriter.write("\t");
			dsWriter.write(String.valueOf(triple.third));
			dsWriter.write("\t");
			dsWriter.write(rel);
			dsWriter.write("\n");
	}

}
