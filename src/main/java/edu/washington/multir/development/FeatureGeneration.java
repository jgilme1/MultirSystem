package edu.washington.multir.development;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.Pair;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multir.featuregeneration.DefaultFeatureGenerator;
import edu.washington.multir.featuregeneration.FeatureGenerator;

public class FeatureGeneration {
	
	public static void main(String[] args) throws SQLException, IOException{
		Corpus c = new Corpus(new DefaultCorpusInformationSpecification(),true,true);
		BufferedReader in = new BufferedReader(new FileReader("distantSupervision"));
		FeatureGenerator fg = new DefaultFeatureGenerator();
		
		int count =1;
		String nextLine = in.readLine();
		List<String> lines = new ArrayList<String>();
		Map<Integer,String> lineMap = new HashMap<Integer,String>();
		
		BufferedWriter bw =  new BufferedWriter(new FileWriter(new File("features")));
		while(nextLine != null){
			String [] values = nextLine.split("\t");
			Integer id = Integer.parseInt(values[8]);
			lines.add(nextLine);
			lineMap.put(id, nextLine);
			
			
			if(count % 1000 == 0){
				//issue Solr Query
				StringBuilder sb = new StringBuilder();
				Map<Integer,Pair<Annotation,Annotation>> sentAnnotationsMap = getSentAnnotationsMap(lines,c);
				for(Integer i : sentAnnotationsMap.keySet()){
					String line = lineMap.get(i);
					String [] lineValues = line.split("\t");
					int arg1StartOffset = Integer.parseInt(lineValues[1]);
					int arg1EndOffset = Integer.parseInt(lineValues[2]);
					int arg2StartOffset = Integer.parseInt(lineValues[5]);
					int arg2EndOffset = Integer.parseInt(lineValues[6]);
					Pair<Annotation,Annotation> annotations = sentAnnotationsMap.get(i);
					Annotation sentence = annotations.first;
					Annotation doc = annotations.second;
					
					List<String> features = fg.generateFeatures(arg1StartOffset,arg1EndOffset,arg2StartOffset,arg2EndOffset,sentence,doc);
					int globalSentID = sentence.get(CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID.class);
					sb.append(String.valueOf(globalSentID));
					for(String feature: features){
						sb.append("\t");
						sb.append(feature);
					}
					sb.append("\n");
//					System.out.print(globalSentID);
//					for(String feature : features){
//						System.out.print("\t" + feature);
//					}
//					System.out.print("\n");
				}
				lineMap.clear();
				lines.clear();
				System.out.println(count + " distant supervision annotations processed");
				bw.write(sb.toString());
			}
			nextLine = in.readLine();
			count ++;
		}
		bw.close();
	}

	private static Map<Integer, Pair<Annotation,Annotation>> getSentAnnotationsMap(
			List<String> lines, Corpus c) throws SQLException {
		
		Set<Integer> sentIds = new HashSet<Integer>();
		
		for(String line: lines){
			String[] values = line.split("\t");
			Integer sentID = Integer.parseInt(values[8]);
			sentIds.add(sentID);
		}
		
		return c.getAnnotationPairsForEachSentence(sentIds);
	}

}
