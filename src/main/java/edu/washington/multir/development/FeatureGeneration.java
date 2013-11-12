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
		CorpusInformationSpecification cis = new DefaultCorpusInformationSpecification();
		FeatureGenerator fg = new DefaultFeatureGenerator();
		Corpus c;
		BufferedReader in;
		BufferedWriter bw;

		if(args[0].equals("train")){
			in = new BufferedReader(new FileReader(new File("distantSupervisionTrain")));
			bw =  new BufferedWriter(new FileWriter(new File("featuresTrain")));
			c = new Corpus(cis,true,true);
		}
		else if(args[0].equals("test")){
			in = new BufferedReader(new FileReader(new File("distantSupervisionTest")));
			bw =  new BufferedWriter(new FileWriter(new File("featuresTest")));
			c = new Corpus(cis,true,false);
		}
		else{
			throw new IllegalArgumentException("Argument incorrect");
		}

		int count =1;
		String nextLine = in.readLine();
		List<String> lines = new ArrayList<String>();
		
		while(nextLine != null){
			String [] values = nextLine.split("\t");
			lines.add(nextLine);
			System.out.println("Line " + count + "\t" + nextLine);
			
			if(count % 1000 == 0){
				//issue Solr Query
				StringBuilder sb = new StringBuilder();
				Map<Integer,Pair<Annotation,Annotation>> sentAnnotationsMap = getSentAnnotationsMap(lines,c);
				//System.out.println(sentAnnotationsMap.size());
				for(String line : lines){
					
					try{
						String [] lineValues = line.split("\t");
						int arg1StartOffset = Integer.parseInt(lineValues[1]);
						int arg1EndOffset = Integer.parseInt(lineValues[2]);
						int arg2StartOffset = Integer.parseInt(lineValues[5]);
						int arg2EndOffset = Integer.parseInt(lineValues[6]);
						int sentId = Integer.parseInt(lineValues[8]);
						Pair<Annotation,Annotation> annotations = sentAnnotationsMap.get(sentId);
						Annotation sentence = annotations.first;
						Annotation doc = annotations.second;
						System.out.println("GENERATING FEATURES");
						System.out.println("SENTID: " +sentId);
						System.out.println("ARG1 = " + lineValues[3]);
						List<String> features = fg.generateFeatures(arg1StartOffset,arg1EndOffset,arg2StartOffset,arg2EndOffset,sentence,doc);
						System.out.println("GENERATED FEATURES");
						int globalSentID = sentence.get(CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID.class);
						sb.append(String.valueOf(globalSentID));
						String arg1Id = lineValues[0];
						String arg2Id = lineValues[4];
						String rel = lineValues[9];
						sb.append("\t");
						sb.append(arg1Id);
						sb.append("\t");
						sb.append(arg2Id);
						sb.append("\t");
						sb.append(rel);
						for(String feature: features){
							sb.append("\t");
							sb.append(feature);
						}
						sb.append("\n");
					}
					catch(NumberFormatException e){
						e.printStackTrace();
					}
				}
				lines.clear();
				System.out.println(count + " distant supervision annotations processed");
				bw.write(sb.toString());
			}
			nextLine = in.readLine();
			count ++;
		}
		bw.close();
		in.close();
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
