package edu.washington.multir.corpus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.washington.multir.database.CorpusDatabase;

public class Corpus {
	
	private CorpusDatabase cd;
	private CorpusInformationSpecification cis;
	private static String documentColumnName = "DOCNAME";
	private static String sentIDColumnName = "SENTID";
	
	public Corpus(CorpusInformationSpecification cis, boolean load, boolean train) throws SQLException{
	  this.cis = cis;
      cd = load ? CorpusDatabase.loadCorpusDatabase(train) : CorpusDatabase.newCorpusDatabase(getSentenceTableSQLSpecification(), getDocumentTableSQLSpecification(), train);
	}
	private String getSentenceTableSQLSpecification(){
		StringBuilder sqlTableSpecificationBuilder = new StringBuilder();
		sqlTableSpecificationBuilder.append("( " + sentIDColumnName + " int,\n");
		sqlTableSpecificationBuilder.append(documentColumnName +" VARCHAR(128),\n");
		// add SentenceInformation types
		
		for(int i =2; i < cis.sentenceInformation.size(); i++){
			SentInformationI sentenceInformation = cis.sentenceInformation.get(i);
			String columnName = sentenceInformation.name();
			sqlTableSpecificationBuilder.append(columnName + " VARCHAR(20000),\n");
		}
		for(TokenInformationI tokenInformation : cis.tokenInformation){
			String columnName = tokenInformation.name();
			sqlTableSpecificationBuilder.append(columnName + " VARCHAR(20000),\n");
		}
		sqlTableSpecificationBuilder.append("PRIMARY KEY (" + sentIDColumnName+"))");
		return sqlTableSpecificationBuilder.toString();
	}
	private String getDocumentTableSQLSpecification(){
		StringBuilder sqlTableSpecificationBuilder = new StringBuilder();
		sqlTableSpecificationBuilder.append("( " + documentColumnName + " VARCHAR(128),\n");
		//add Document Information types
		sqlTableSpecificationBuilder.append("PRIMARY KEY (DOCNAME))");
		return sqlTableSpecificationBuilder.toString();
	}
	
	private List<Annotation> getDocuments(List<String> docNames) throws SQLException{
		ResultSet sentenceResults = cd.getSentenceRows(documentColumnName, docNames);
		
		List<Annotation> documents = new ArrayList<Annotation>();
		List<CoreMap> sentences = new ArrayList<CoreMap>();
		String lastDocument = "";
		if(sentenceResults.next()){
			lastDocument = sentenceResults.getString(documentColumnName);
			sentences.add(parseSentence(sentenceResults));
		}
		
		
		while(sentenceResults.next()){
			String document = sentenceResults.getString(documentColumnName);
			if(!lastDocument.equals(document)){
				Annotation a = new Annotation(sentences);
				documents.add(a);
				sentences = new ArrayList<CoreMap>();
				lastDocument = document;
			}
			sentences.add(parseSentence(sentenceResults));				
		}
		if(!sentences.isEmpty()){
			Annotation a = new Annotation(sentences);
			documents.add(a);
		}
		return documents;
	}
	
	private Annotation getDocument(String docName) throws SQLException{
		List<CoreMap> sentences = getSentences(docName);
		if(sentences.size() > 0)
		  return new Annotation(sentences);
		else
		  return null;
	}
	
	private List<CoreMap> getSentences(String docName) throws SQLException{
		List<CoreMap> sentences = new ArrayList<CoreMap>();
		List<String> docNames = new ArrayList<String>();
		docNames.add(docName);
		ResultSet sentenceResults = cd.getSentenceRows(documentColumnName,docNames);
		//iterate over sentence results
		while(sentenceResults.next()){
				sentences.add(parseSentence(sentenceResults));
		}
			
		return sentences;
	}
	
	/** 
	 * sentenceResults should already be at the correct cursor
	 * @param sentenceResults
	 * @return
	 * @throws SQLException 
	 */
    private CoreMap parseSentence(ResultSet sentenceResults) throws SQLException,IllegalArgumentException {
    	ResultSetMetaData metaData = sentenceResults.getMetaData();
    	Annotation a = new Annotation("");
    	int index =1;
    	for(SentInformationI si : cis.sentenceInformation){
    		String x = sentenceResults.getString(index);
    		si.read(x,a);
    		index++;
    	}
 
    	List<CoreLabel> tokens = a.get(CoreAnnotations.TokensAnnotation.class);

    	for(TokenInformationI ti: cis.tokenInformation){
    		String tokenInformation =sentenceResults.getString(index);
    		ti.read(tokenInformation, tokens);
    		index++;
    	}
    	return a;
	}


	private class DocumentIterator implements Iterator<Annotation>{
    	private ResultSet documents;
    	private boolean didNext = false;
    	private boolean hasNext = false;
    	
    	public DocumentIterator() throws SQLException{
    		documents = cd.getDocumentRows();
    	}
		@Override
		public boolean hasNext() {
			try{
				if(!didNext){
					hasNext = documents.next();
					didNext = true;
				}
				return hasNext;
			}
			catch(SQLException e){
				e.printStackTrace();
				return false;
			}
		}
		@Override
		public Annotation next() {
			try{
			if(!didNext){
				documents.next();
			}
			didNext = false;
			return getDocument(documents.getString(documentColumnName));
			}
			catch(SQLException e){
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public void remove() {
		}    	
    }
	
	private class CachingDocumentIterator implements Iterator<Annotation>{
    	private ResultSet documents;
    	private List<Annotation> cachedDocuments;
    	private int documentCount =0;
    	
    	private static final int CACHED_LIMIT = 1000;
    	
    	private boolean doNext() throws SQLException{
    		int i =0;
    		List<String> docNames = new ArrayList<String>();
    		while((i < CACHED_LIMIT) && documents.next()){
    			String docName = documents.getString(documentColumnName);
    			docNames.add(docName);
    			i++;
    			documentCount++;
    		}
    		if(i == 0){
    			return false;
    		}
    		else{
    		  cachedDocuments.addAll(getDocuments(docNames));
    		  return true;
    		}
    	}
    	
    	public CachingDocumentIterator() throws SQLException{
    		documents = cd.getDocumentRows();
    		cachedDocuments = new ArrayList<Annotation>();
    	}
		@Override
		public boolean hasNext() {
			if(!cachedDocuments.isEmpty()){
				return true;
			}
			else{
				try{
						//doing next involves doing up to a limit of nexts
						//and doing one Derby query....
					return doNext();
				}
				catch(SQLException e){
					e.printStackTrace();
					return false;
				}
			}
		}
		@Override
		public Annotation next() {
			if(cachedDocuments.isEmpty()){
				try{
					if(doNext()){
						return next();
					}
					else{
						return null;
					}
				}
				catch(SQLException e){
					e.printStackTrace();
					return null;
				}
			}
			else{
				Annotation d = cachedDocuments.remove(0);
				return d;
			}
		}

		@Override
		public void remove() {
		}    
	}
	
    public Iterator<Annotation> getDocumentIterator() throws SQLException{
    	return new DocumentIterator();
    }
    
    public Iterator<Annotation> getCachedDocumentIterator() throws SQLException{
    	return new CachingDocumentIterator();
    }
    
    
    public static void main(String[] args) throws SQLException{
    	//load db with stuff
    	Corpus c = new Corpus(new DefaultCorpusInformationSpecification(),false,true);
    	List<String> columnNames = new ArrayList<String>();
    	List<Object> values = new ArrayList<Object>();
    	for(SentInformationI info : c.cis.sentenceInformation){
    		columnNames.add(info.name());
    	}
    	for(TokenInformationI tokenInfo : c.cis.tokenInformation){
    		columnNames.add(tokenInfo.name());
    	}
    	values.add(new Integer(0));
    	values.add("DOC1");
    	values.add("This is the sentence text.");
    	values.add("This is the sentence text .");
    	values.add("DT VB DT NN NN P");
    	c.cd.insertToSentenceTable(columnNames, values);
    	
    	values = new ArrayList<Object>();
    	values.add(new Integer(1));
    	values.add("DOC1");
    	values.add("A test is a beautiful thing.");
    	values.add("A test is a beautiful thing .");
    	values.add("DT NN VB DT JJ NN P");
    	c.cd.insertToSentenceTable(columnNames, values);

    	columnNames = new ArrayList<String>();
    	columnNames.add(documentColumnName);
    	values = new ArrayList<Object>();
    	values.add("DOC1");
    	c.cd.insertToDocumentTable(columnNames, values);
    	
    	//use Document Iterator
    	Iterator<Annotation> di = c.getDocumentIterator();
    	
    	while(di.hasNext()){
    		Annotation a = di.next();
    		printAnnotation(a);
    	}
    	
    }
    
    private static String formatValue(String s){
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append(s.replaceAll("%", "%%"));
    	sb.insert(0,"%");
    	sb.append("%");
    	return sb.toString();
    }
    
    public void loadCorpus2(File path, CorpusInformationSpecification ci, String sentenceDBFileName, String documentDBFileName ) throws IOException, SQLException{
    	cd.turnOffAutoCommit();
    	File dbsentencesFile = new File(sentenceDBFileName);
    	File dbdocumentsFile = new File(documentDBFileName);
    	if(!(dbsentencesFile.exists() && dbdocumentsFile.exists())){
    	if(path.isDirectory()){
    		File[] filesInDirectory = path.listFiles();
    		//check for all required files
    		if(requiredFilesExist(Arrays.asList(filesInDirectory), ci)){
    			//load in data from each file iteratively as rows into db.
    			List<BufferedReader> sentenceDataLineReaders = new ArrayList<BufferedReader>();
    			List<BufferedReader> tokenDataLineReaders = new ArrayList<BufferedReader>();
    			List<BufferedReader> documentDataLineReaders = new ArrayList<BufferedReader>();
    			BufferedReader metaLineReader = new BufferedReader(new FileReader(new File(path.getPath()+"/sentences.meta")));
    			
    			List<String> sentColumnNames = ci.getSentenceTableColumnNames();
    			List<String> docColumnNames = ci.getDocumentTableColumnNames();
    			
    			List<SentInformationI> sentenceInformationSpecifications = new ArrayList<SentInformationI>();
    			List<TokenInformationI> tokenInformationSpecifications = new ArrayList<TokenInformationI>();
    			
    			for(SentInformationI si : ci.sentenceInformation){
    	    		if(!(si.name().equals("DOCNAME") || si.name().equals("SENTID") ||  si.name().equals("SENTTOKENSINFORMATION"))){
    	    			sentenceInformationSpecifications.add(si);
    	    			sentenceDataLineReaders.add(new BufferedReader(new FileReader(new File(path+"/"+si.name()))));
    	    		}
    			}
    			for(TokenInformationI ti : ci.tokenInformation){
    				tokenInformationSpecifications.add(ti);
    				tokenDataLineReaders.add(new BufferedReader(new FileReader(new File(path+"/"+ti.name()))));
    			}
    			
    			
    			SentInformationI globalSentIdInformationSpecification = ci.sentenceInformation.get(0);
    			SentInformationI docNameInformationSpecification = ci.sentenceInformation.get(1);
    			SentInformationI tokensInformationSpecification = ci.sentenceInformation.get(2);
    			
    			int linesProcessed = 0;

    			//iterate over corpus
    			String previousDocumentName = "";
    			BufferedWriter sentenceWriter = new BufferedWriter(new PrintWriter(new File(sentenceDBFileName)));
    			BufferedWriter documentWriter = new BufferedWriter(new PrintWriter(new File(documentDBFileName)));
    			List<String> cachedSentenceLines = new ArrayList<String>();
    			List<String> cachedDocumentLines = new ArrayList<String>();
    			String nextMetaLine = metaLineReader.readLine();
    			while(nextMetaLine != null){
    				List<Object> sentenceValues = new ArrayList<Object>();
    				List<Object> documentValues = null;

    				String[] metaLineValues = nextMetaLine.split("\t");
    				String docName = metaLineValues[1];
    				
    				StringBuilder newLine =  new StringBuilder();
    				for(String metaLineValue: metaLineValues){
    					newLine.append(formatValue(metaLineValue));
    					newLine.append("\t");
    				}

    				
    				int sentLineIteratorIndex = 0;
    				//get remaining SentInformationI values
    				while(sentLineIteratorIndex < sentenceDataLineReaders.size()){
    					String nextLine = sentenceDataLineReaders.get(sentLineIteratorIndex).readLine();
    					String [] splitValues = nextLine.split("\t");
    					String values = "";
    					if(splitValues.length >1){
    						values = splitValues[1];
    					}
    					newLine.append(formatValue(values));
    					newLine.append("\t");
    					sentLineIteratorIndex++;
    				}
    				
    				//get tokenInformation values
    				int tokenLineIteratorIndex = 0;
    				while(tokenLineIteratorIndex < tokenDataLineReaders.size()){
    					String nextLine = tokenDataLineReaders.get(tokenLineIteratorIndex).readLine();
    			        String [] splitValues = nextLine.split("\t");
    			        String values = " ";
    			        if(splitValues.length > 1){
    			        	values = splitValues[1];
    			        }
    					newLine.append(formatValue(values));
    					newLine.append("\t");
    					tokenLineIteratorIndex++;
    				}
    				newLine.deleteCharAt(newLine.length()-1);
    				newLine.append("\n");
    				cachedSentenceLines.add(newLine.toString());

    				//get DocumentInformation values if applicable
    				if(!docName.equals(previousDocumentName)){
    					cachedDocumentLines.add(formatValue(docName)+"\n");
    					previousDocumentName = docName;
    				}

    				
    				if(linesProcessed % 500000 == 0){
    					System.out.println("Processed " + linesProcessed + " lines");
    					StringBuilder sentenceBuilder = new StringBuilder();
    					StringBuilder documentBuilder = new StringBuilder();
    					for(String sentenceLine: cachedSentenceLines){
    						sentenceBuilder.append(sentenceLine);
    					}
    					for(String documentLine: cachedDocumentLines){
    						documentBuilder.append(documentLine);
    					}
    					sentenceWriter.write(sentenceBuilder.toString());
    					documentWriter.write(documentBuilder.toString());
    					cachedSentenceLines.clear();
    					cachedDocumentLines.clear();
    					
    				}
    				linesProcessed++;
        			nextMetaLine = metaLineReader.readLine();
    			}
				StringBuilder sentenceBuilder = new StringBuilder();
				StringBuilder documentBuilder = new StringBuilder();
				for(String sentenceLine: cachedSentenceLines){
					sentenceBuilder.append(sentenceLine);
				}
				for(String documentLine: cachedDocumentLines){
					documentBuilder.append(documentLine);
				}
				sentenceWriter.write(sentenceBuilder.toString());
				documentWriter.write(documentBuilder.toString());
				cachedSentenceLines.clear();
				cachedDocumentLines.clear();
    			sentenceWriter.close();
    			documentWriter.close();
    			metaLineReader.close();
    			for(BufferedReader br : sentenceDataLineReaders){
    				br.close();
    			}
    			for(BufferedReader br: tokenDataLineReaders){
    				br.close();
    			}
    			for(BufferedReader br: documentDataLineReaders){
    				br.close();
    			}
    		}
    		else{
    		}
    	}
    	cd.turnOnAutoCommit();
    	}
    	
    	// after files are converted to db format, batch load them into derby
    	cd.batchSentenceTableLoad(ci,dbsentencesFile);
    	cd.batchDocumentTableLoad(ci,dbdocumentsFile);
    }
    
    private boolean requiredFilesExist(List<File> files, CorpusInformationSpecification cis){
    	List<String> requiredFileNames = new ArrayList<String>();
    	List<String> fileNames = new ArrayList<String>();
    	for(File f : files){
    		fileNames.add(f.getName());
    	}
    	requiredFileNames.add("sentences.meta");
    	//sentences.meta will be sentID docname tokens 
    	for(SentInformationI si :cis.sentenceInformation){
    		if(!(si.name().equals("DOCNAME") || si.name().equals("SENTID") ||  si.name().equals("SENTTOKENSINFORMATION"))){
    			requiredFileNames.add(si.name());
    		}
    	}
    	for(TokenInformationI ti: cis.tokenInformation){
    		requiredFileNames.add(ti.name());
    	}
    	for(String name : requiredFileNames){
    		if(!fileNames.contains(name)){
    			System.err.println(name + "file does not exist");
    			return false;
    		}
    	}
    	return true;
    }
    
    private static void printAnnotation(Annotation a){	
    	Set<Class<?>> keys = a.keySet();
    	System.out.println("KEYS:");
    	for(Class k : keys){
    		System.out.println(k.getName());
    	}
    	System.out.println("Annotations:");
    	for(Class k : keys){
    		System.out.println("Key: "+ k.getName());
    		System.out.println("Value: " + a.get(k));
    	}
    	
    	System.out.println("Sentences:");
    	List<CoreMap> sentences = a.get(CoreAnnotations.SentencesAnnotation.class);
    	for(CoreMap sentence: sentences){
    		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
    		for(CoreLabel token : tokens){
    			System.out.println(token.value());
    			System.out.println(token.get(CoreAnnotations.NamedEntityTagAnnotation.class));
    		}
    	}
    	
    }
	public Map<Integer,Pair<Annotation,Annotation>> getAnnotationPairsForEachSentence(Set<Integer> sentIds) throws SQLException {
		if(sentIds.size() > 1000){
			throw new IllegalArgumentException("sentIds should have less than 1000 sentences in order for the large SQL query to work");
		}
		Map<Integer,Pair<Annotation,Annotation>> sentIdToAnnotationsMap = new HashMap<>();
		List<Integer> values = new ArrayList<Integer>();
		for(Integer sentID: sentIds){
			values.add(sentID);
		}
		ResultSet sentenceResults = cd.getSentenceRowsByID(values);
		
		Set<String> relevantDocuments = new HashSet<String>();
		
		Map<Integer,Pair<Annotation,String>> mapFromSentToAnnotationAndDocName = new HashMap<>();
		
		while(sentenceResults.next()){
			//keep track of needed documents.
			String docName = sentenceResults.getString(documentColumnName);
			Integer sentId = sentenceResults.getInt(sentIDColumnName);
			relevantDocuments.add(docName);
			CoreMap s = parseSentence(sentenceResults);
			if(!mapFromSentToAnnotationAndDocName.containsKey(sentId)){
				mapFromSentToAnnotationAndDocName.put(sentId, new Pair(s,docName));
			}
		}
		
		//get document map
		Map<String,Annotation> docNameToAnnoMap = new HashMap<String,Annotation>();
		List<String> docNames = new ArrayList<>();
		for(String docName: relevantDocuments){
			docNames.add(docName);
		}
		List<Annotation> docAnnotations = getDocuments(docNames);
		for(int i =0; i < docAnnotations.size(); i++){
			String docName = docNames.get(i);
			Annotation doc = docAnnotations.get(i);
			docNameToAnnoMap.put(docName,doc);
		}
		
		for(Integer key : mapFromSentToAnnotationAndDocName.keySet()){
			Pair<Annotation,String> s = mapFromSentToAnnotationAndDocName.get(key);
			Annotation sent = s.first;
			Annotation doc = docNameToAnnoMap.get(s.second);
			Pair<Annotation,Annotation> newPair = new Pair(sent,doc);
			sentIdToAnnotationsMap.put(key, newPair);
		}
		return sentIdToAnnotationsMap;
	}
}
