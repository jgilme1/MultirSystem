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
import edu.washington.multir.argumentidentification.ArgumentIdentification;
import edu.washington.multir.argumentidentification.DefaultArgumentIdentification;
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
		Corpus c = new Corpus(cis,true,true);
		Iterator<Annotation> di = c.getCachedDocumentIterator();
		KnowledgeBase kb = new KnowledgeBase(args[0],args[1],args[2]);
		ArgumentIdentification ai = DefaultArgumentIdentification.getInstance();
		ai.setKB(kb);

		PrintWriter argumentWriter = new PrintWriter("argumentsCached1000");
		int count =0;
		while(di.hasNext()){
			Annotation d = di.next();
			List<CoreMap> sentences = d.get(CoreAnnotations.SentencesAnnotation.class);
			if(sentences.size() > 0){
				argumentWriter.write(sentences.get(0).get(SentDocName.class));
			}
			for(CoreMap sentence : sentences){
				int sentGlobalID = sentence.get(SentGlobalID.class);
				argumentWriter.write(" " + sentGlobalID);
				String docName = sentence.get(SentDocName.class);
				List<Argument> arguments =  ai.identifyArguments(d,sentence);
				writeArguments(arguments,argumentWriter);
			}
			if(sentences.size()> 0){
				argumentWriter.write("\n");
			}
			count ++;
			if(count % 100 == 0){
				System.out.println(count +" documents read");
			}
		}
		argumentWriter.close();
	}
	
	private static void writeArguments(List<Argument> arguments,
			PrintWriter argumentWriter) {
		for(Argument arg: arguments){
			argumentWriter.write("\t" + arg.getArgString() + "\t" + arg.getArgID());
		}
	}
}
