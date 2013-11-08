package edu.washington.multir.development;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.argumentidentification.ArgumentIdentification;
import edu.washington.multir.argumentidentification.DefaultArgumentIdentification;
import edu.washington.multir.argumentidentification.DefaultRelationMatching;
import edu.washington.multir.argumentidentification.RelationMatching;
import edu.washington.multir.corpus.Corpus;
import edu.washington.multir.corpus.CorpusInformationSpecification;
import edu.washington.multir.corpus.CorpusInformationSpecification.SentDocNameInformation.SentDocName;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multir.corpus.CorpusInformationSpecification.SentGlobalIDInformation.SentGlobalID;
import edu.washington.multir.data.Argument;
import edu.washington.multir.knowledgebase.KnowledgeBase;

public class DistantSupervision {
	
	public static void main(String[] args) throws SQLException, IOException{
		CorpusInformationSpecification cis =  new DefaultCorpusInformationSpecification();
		Corpus c = new Corpus(cis,true,false);
		Iterator<Annotation> di = c.getCachedDocumentIterator();
		KnowledgeBase kb = new KnowledgeBase(args[0],args[1],args[2]);
		ArgumentIdentification ai = DefaultArgumentIdentification.getInstance();
		RelationMatching rm = new DefaultRelationMatching();
		ai.setKB(kb);

		PrintWriter dsWriter = new PrintWriter("distantSupervisionTest");
		int count =0;
		while(di.hasNext()){
			Annotation d = di.next();
			List<CoreMap> sentences = d.get(CoreAnnotations.SentencesAnnotation.class);
			for(CoreMap sentence : sentences){
				int sentGlobalID = sentence.get(SentGlobalID.class);
				String docName = sentence.get(SentDocName.class);
				//argument identification
				List<Argument> arguments =  ai.identifyArguments(d,sentence);
				//relation matching
				List<Triple<Argument,Argument,String>> distantSupervisionAnnotations = rm.matchRelations(arguments,kb.getEntityPairRelationMap());
				//writeArguments(arguments,argumentWriter);
				writeDistantSupervisionAnnotations(distantSupervisionAnnotations,dsWriter,sentGlobalID);
			}
			count ++;
			if(count % 100 == 0){
				System.out.println(count +" documents read");
			}
		}
		dsWriter.close();
	}
	
	private static void writeDistantSupervisionAnnotations(
			List<Triple<Argument, Argument, String>> distantSupervisionAnnotations, PrintWriter dsWriter,
			int sentGlobalID) {
		for(Triple<Argument,Argument,String> dsAnno : distantSupervisionAnnotations){
			Argument arg1 = dsAnno.first;
			Argument arg2 = dsAnno.second;
			String rel = dsAnno.third;
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
			dsWriter.write(String.valueOf(sentGlobalID));
			dsWriter.write("\t");
			dsWriter.write(rel);
			dsWriter.write("\n");
		}
	}
}
