package edu.washington.multir.development;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.argumentidentification.ArgumentIdentification;
import edu.washington.multir.argumentidentification.DefaultArgumentIdentification;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multir.data.Argument;
import edu.washington.multir.featuregeneration.DefaultFeatureGenerator;
import edu.washington.multir.featuregeneration.FeatureGenerator;
import edu.washington.multir.knowledgebase.KnowledgeBase;

public class AddNegativeExamples {
	
	public static void main(String[] args) throws SQLException, IOException{
		String dsFileName = "";
		String outputFile = "";
		Corpus c;
		KnowledgeBase KB = new KnowledgeBase(args[1],args[2],args[3]);
		Map<String,List<String>> entityPairRelationMap = KB.getEntityPairRelationMap();
		ArgumentIdentification ai = DefaultArgumentIdentification.getInstance();
		ai.setKB(KB);
		CorpusInformationSpecification cis = new DefaultCorpusInformationSpecification();
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
					for(int j =i; j < filteredArguments.size();j++){
						if( j != i){
							//check that there is no relation between these.
							Argument arg1 = filteredArguments.get(i);
							Argument arg2 = filteredArguments.get(j);
							String key = arg1.getArgID()+arg2.getArgID();
							if(!entityPairRelationMap.containsKey(key)){
								// can be negative example
								Triple t = new Triple(arg1,arg2,sentId);
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
		
		
	}
	
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
