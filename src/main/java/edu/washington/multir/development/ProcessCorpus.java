package edu.washington.multir.development;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multir.corpus.DefaultCorpusInformationSpecification;
import edu.washington.multir.preprocess.CorpusPreprocessing;

public class ProcessCorpus {

	public static void main(String[] args) throws InterruptedException, ExecutionException, IOException{

		//CorpusPreprocessing.ProcessDocument.CorpusDirectory = new File("/scratch2/code/multir-reimplementation/PreprocessedCorpus/reformatted/small");

		
		File rawCorpusDirectory = new File(args[0]);
		Integer cores = Integer.parseInt(args[1]);

		int tempFileIndex =0;
		int i =0;
		long start = System.currentTimeMillis();
		List<File> files = new ArrayList<>();
		System.out.println("HERE");
		for(File f : FileUtils.listFiles(rawCorpusDirectory, new String[] {"sgm"}, true)){
			files.add(f);
			System.out.println(f.getName());
			if(files.size() == 500){
				
				start = System.currentTimeMillis();
				ExecutorService tp = java.util.concurrent.Executors.newFixedThreadPool(cores);
				CompletionService<List<Pair<Annotation,File>>> pool = new ExecutorCompletionService<>(tp);
				//split files and assign and submit callables.
				int lastIndex = 0;
				for(int j =1; j <= cores; j++){
			      int newIndex = lastIndex + (files.size()/cores);
			      List<File> part;
			      if(j == cores){
					  part = files.subList(lastIndex,files.size());			    	  
			      }else{
					  part = files.subList(lastIndex,newIndex);			    	  
			      }
				  Callable<List<Pair<Annotation,File>>> c = new CorpusPreprocessing.ProcessDocuments(part,0,tempFileIndex);
				  tempFileIndex++;
			      pool.submit(c);
				  lastIndex = newIndex;
				}
				
				try{
					for(int j =1; j <= cores; j++){
						Future<List<Pair<Annotation,File>>> fut = pool.poll(20, TimeUnit.MINUTES);
						List<Pair<Annotation,File>> r = fut.get();
					}
				}
				catch (Exception e){
					e.printStackTrace();
					System.err.println("CAUGHT EXCEPTION");
				}
				finally{
					tp.shutdownNow();
				}
				
				long now = System.currentTimeMillis();
				long millis = now-start;
				System.out.println( "500  docs processed in " + millis);
				files.clear();
			}
		}
	}
}


