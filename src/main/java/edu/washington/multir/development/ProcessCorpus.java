package edu.washington.multir.development;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.Pair;
import edu.washington.multir.preprocess.CorpusPreprocessing;

public class ProcessCorpus {

	public static void main(String[] args) throws InterruptedException, ExecutionException, IOException{

		//CorpusPreprocessing.ProcessDocument.CorpusDirectory = new File("/scratch2/code/multir-reimplementation/PreprocessedCorpus/reformatted/small");

		
		File rawCorpusDirectory = new File(args[0]);
		Integer cores = Integer.parseInt(args[1]);

		int tempFileIndex =0;
		long start = System.currentTimeMillis();
		List<File> files = new ArrayList<>();
		System.out.println("HERE");
		for(File f : FileUtils.listFiles(rawCorpusDirectory, new String[] {"sgm"}, true)){
			files.add(f);
			System.out.println(f.getName());
			if(files.size() == 500){
				
				ExecutorService tp = java.util.concurrent.Executors.newFixedThreadPool(cores);
				CompletionService<List<Pair<Annotation,File>>> pool = new ExecutorCompletionService<>(tp);
				//split files and assign and submit callables.
				int lastIndex = 0;
				for(int j =1; j <= cores; j++){
			      int newIndex = lastIndex + (files.size()/cores);
			      List<File> part;
			      if(j == cores){
					  part = new ArrayList<File>(files.subList(lastIndex, files.size()));
			      }else{
					  part = new ArrayList<File>(files.subList(lastIndex,newIndex));			    	  
			      }
				  Callable<List<Pair<Annotation,File>>> c = new CorpusPreprocessing.ProcessDocuments(part,0,tempFileIndex);
				  tempFileIndex++;
			      pool.submit(c);
				  lastIndex = newIndex;
				}
				start = System.currentTimeMillis();
				
				try{
					for(int j =1; j <= cores; j++){
						Future<List<Pair<Annotation,File>>> fut = pool.poll(20, TimeUnit.MINUTES);
						List<Pair<Annotation,File>> r = fut.get();
					}
					long now = System.currentTimeMillis();
					long millis = now-start;
					System.out.println( "500  docs processed in " + millis);
					files.clear();
				}
				catch (Exception e){
					e.printStackTrace();
					System.err.println("CAUGHT EXCEPTION");
				}
				finally{
					tp.shutdownNow();
					System.exit(0);
				}
				
			}
		}
	}
}


