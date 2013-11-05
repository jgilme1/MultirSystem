package edu.washington.multir.corpus;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
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
			sqlTableSpecificationBuilder.append(columnName + " VARCHAR(10000),\n");
		}
		for(TokenInformationI tokenInformation : cis.tokenInformation){
			String columnName = tokenInformation.name();
			sqlTableSpecificationBuilder.append(columnName + " VARCHAR(10000),\n");
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
	
	private Annotation getDocument(String docName) throws SQLException{
		List<CoreMap> sentences = getSentences(docName);
		if(sentences.size() > 0)
		  return new Annotation(sentences);
		else
		  return null;
	}
	
	private List<CoreMap> getSentences(String docName) throws SQLException{
		List<CoreMap> sentences = new ArrayList<CoreMap>();
		ResultSet sentenceResults = cd.getSentenceRows(documentColumnName,docName);
		//iterate over sentence results
		while(sentenceResults.next()){
			    System.out.println("Has one sentence");
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
    		Object x = sentenceResults.getObject(index);
    		a.set(si.getAnnotationKey(), si.readFromDb(sentenceResults.getObject(index)));
    		index++;
    	}
    	for(TokenInformationI ti: cis.tokenInformation){
    		List<String> tokenSeparatedValues = ti.getTokenSeparatedValues(sentenceResults.getString(index));
    		List<CoreLabel> tokens = a.get(CoreAnnotations.TokensAnnotation.class);
    		for(int i =0; i < tokenSeparatedValues.size(); i++){
    			String tokenSeparatedValue = tokenSeparatedValues.get(i);
    			CoreLabel token = tokens.get(i);
    			token.set(ti.getAnnotationKey(), ti.read(tokenSeparatedValue));
    		}
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
			// TODO Auto-generated method stub
			
		}    	
    }
    public DocumentIterator getDocumentIterator() throws SQLException{
    	return new DocumentIterator();
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
    	DocumentIterator di = c.getDocumentIterator();
    	
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
    
    public void loadCorpus2(File path, CorpusInformationSpecification ci) throws IOException, SQLException{
    	cd.turnOffAutoCommit();
    	File dbsentencesFile = new File("dbsentences");
    	File dbdocumentsFile = new File("dbdocuments");
    	if(!(dbsentencesFile.exists() && dbdocumentsFile.exists())){
    	if(path.isDirectory()){
    		File[] filesInDirectory = path.listFiles();
    		//check for all required files
    		if(requiredFilesExist(Arrays.asList(filesInDirectory), ci)){
    			//load in data from each file iteratively as rows into db.
    			List<LineIterator> sentenceDataLineIterators = new ArrayList<LineIterator>();
    			List<LineIterator> tokenDataLineIterators = new ArrayList<LineIterator>();
    			List<LineIterator> documentDataLineIterators = new ArrayList<LineIterator>();
    			LineIterator metaLineIterator = FileUtils.lineIterator(new File(path.getPath()+"/sentences.meta"));
    			
    			List<String> sentColumnNames = ci.getSentenceTableColumnNames();
    			List<String> docColumnNames = ci.getDocumentTableColumnNames();
    			
    			List<SentInformationI> sentenceInformationSpecifications = new ArrayList<SentInformationI>();
    			List<TokenInformationI> tokenInformationSpecifications = new ArrayList<TokenInformationI>();
    			
    			for(SentInformationI si : ci.sentenceInformation){
    	    		if(!(si.name().equals("DOCNAME") || si.name().equals("SENTID") ||  si.name().equals("SENTTOKENSINFORMATION"))){
    	    			sentenceInformationSpecifications.add(si);
    	    			sentenceDataLineIterators.add(FileUtils.lineIterator(new File(path+"/"+si.name())));
    	    		}
    			}
    			for(TokenInformationI ti : ci.tokenInformation){
    				tokenInformationSpecifications.add(ti);
    				tokenDataLineIterators.add(FileUtils.lineIterator(new File(path+"/"+ti.name())));
    			}
    			
    			
    			SentInformationI globalSentIdInformationSpecification = ci.sentenceInformation.get(0);
    			SentInformationI docNameInformationSpecification = ci.sentenceInformation.get(1);
    			SentInformationI tokensInformationSpecification = ci.sentenceInformation.get(2);
    			
    			int linesProcessed = 0;

    			//iterate over corpus
    			String previousDocumentName = "";
    			PrintWriter sentenceWriter = new PrintWriter(new File("dbsentences"));
    			PrintWriter documentWriter = new PrintWriter(new File("dbdocuments"));
    			List<String> cachedSentenceLines = new ArrayList<String>();
    			List<String> cachedDocumentLines = new ArrayList<String>();
    			while(metaLineIterator.hasNext()){
    				List<Object> sentenceValues = new ArrayList<Object>();
    				List<Object> documentValues = null;

    				String metaLine = metaLineIterator.nextLine();
    				String[] metaLineValues = metaLine.split("\t");
    				String docName = metaLineValues[1];
    				
    				StringBuilder newLine =  new StringBuilder();
    				for(String metaLineValue: metaLineValues){
    					newLine.append(formatValue(metaLineValue));
    					newLine.append("\t");
    				}

    				
    				int sentLineIteratorIndex = 0;
    				//get remaining SentInformationI values
    				while(sentLineIteratorIndex < sentenceDataLineIterators.size()){
    					String nextLine = sentenceDataLineIterators.get(sentLineIteratorIndex).nextLine();
    					String values = nextLine.split("\t")[1];
    					newLine.append(formatValue(values));
    					newLine.append("\t");
    					sentLineIteratorIndex++;
    				}
    				
    				//get tokenInformation values
    				int tokenLineIteratorIndex = 0;
    				while(tokenLineIteratorIndex < tokenDataLineIterators.size()){
    					String nextLine = tokenDataLineIterators.get(tokenLineIteratorIndex).nextLine();
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
    		}
    		else{
    		}
    	}
    	cd.turnOnAutoCommit();
    	}
    	// after files are converted to db format, batch load them into derby
    	cd.batchSentenceTableLoad(ci,dbsentencesFile);

    }
    
    public void loadCorpus(File path, CorpusInformationSpecification ci) throws IOException, SQLException{
    	cd.turnOffAutoCommit();
    	if(path.isDirectory()){
    		File[] filesInDirectory = path.listFiles();
    		//check for all required files
    		if(requiredFilesExist(Arrays.asList(filesInDirectory), ci)){
    			//load in data from each file iteratively as rows into db.
    			List<LineIterator> sentenceDataLineIterators = new ArrayList<LineIterator>();
    			List<LineIterator> tokenDataLineIterators = new ArrayList<LineIterator>();
    			List<LineIterator> documentDataLineIterators = new ArrayList<LineIterator>();
    			LineIterator metaLineIterator = FileUtils.lineIterator(new File(path.getPath()+"/sentences.meta"));
    			
    			List<String> sentColumnNames = ci.getSentenceTableColumnNames();
    			List<String> docColumnNames = ci.getDocumentTableColumnNames();
    			
    			List<SentInformationI> sentenceInformationSpecifications = new ArrayList<SentInformationI>();
    			List<TokenInformationI> tokenInformationSpecifications = new ArrayList<TokenInformationI>();
    			
    			for(SentInformationI si : ci.sentenceInformation){
    	    		if(!(si.name().equals("DOCNAME") || si.name().equals("SENTID") ||  si.name().equals("SENTTOKENSINFORMATION"))){
    	    			sentenceInformationSpecifications.add(si);
    	    			sentenceDataLineIterators.add(FileUtils.lineIterator(new File(path+"/"+si.name())));
    	    		}
    			}
    			for(TokenInformationI ti : ci.tokenInformation){
    				tokenInformationSpecifications.add(ti);
    				tokenDataLineIterators.add(FileUtils.lineIterator(new File(path+"/"+ti.name())));
    			}
    			
    			
    			SentInformationI globalSentIdInformationSpecification = ci.sentenceInformation.get(0);
    			SentInformationI docNameInformationSpecification = ci.sentenceInformation.get(1);
    			SentInformationI tokensInformationSpecification = ci.sentenceInformation.get(2);
    			
    			int linesProcessed = 0;

    			//iterate over corpus
    			String previousDocumentName = "";
    			while(metaLineIterator.hasNext()){
    				List<Object> sentenceValues = new ArrayList<Object>();
    				List<Object> documentValues = null;

    				String metaLine = metaLineIterator.nextLine();
    				String[] metaLineValues = metaLine.split("\t");
    				String docName = metaLineValues[1];
    				sentenceValues.add(globalSentIdInformationSpecification.readFromString(metaLineValues[0]));
    				sentenceValues.add(docNameInformationSpecification.readFromString(metaLineValues[1]));
    				sentenceValues.add(tokensInformationSpecification.readFromString(metaLineValues[2]));
    				
    				int sentLineIteratorIndex = 0;
    				//get remaining SentInformationI values
    				while(sentLineIteratorIndex < sentenceDataLineIterators.size()){
    					String nextLine = sentenceDataLineIterators.get(sentLineIteratorIndex).nextLine();
    					String[] lineValues = nextLine.split("\t");
    					Integer sentId = Integer.parseInt(lineValues[0]);
    					
    					SentInformationI si = sentenceInformationSpecifications.get(sentLineIteratorIndex);
    					sentenceValues.add(si.readFromString(lineValues[1]));
    					
    					sentLineIteratorIndex++;
    				}
    				
    				//get tokenInformation values
    				int tokenLineIteratorIndex = 0;
    				while(tokenLineIteratorIndex < tokenDataLineIterators.size()){
    					TokenInformationI ti = tokenInformationSpecifications.get(tokenLineIteratorIndex);
    					String nextLine = tokenDataLineIterators.get(tokenLineIteratorIndex).nextLine();
    					String [] values = nextLine.split("\t");
    					sentenceValues.add(values[1]);
    					tokenLineIteratorIndex++;
    				}

    				//get DocumentInformation values if applicable
    				if(!docName.equals(previousDocumentName)){
    					documentValues = new ArrayList<Object>();
    					documentValues.add(docName);
    					previousDocumentName = docName;
    				}

//    				//insert data to sentence table
//    				for(String name: sentColumnNames){
//    					System.out.println(name);
//    				}
//    				for(Object o : sentenceValues){
//    					System.out.println(o.toString());
//    				}
//    				//insert data to sentence table
//    				for(String name: docColumnNames){
//    					System.out.println(name);
//    				}
//    				for(Object o : documentValues){
//    					System.out.println(o.toString());
//    				}
    				cd.bulkInsertToSentenceTable(sentColumnNames, sentenceValues);
    				if(documentValues != null) {
    					cd.bulkInsertToDocumentTable(docColumnNames, documentValues);
    					documentValues = null;
    				}
    				
    				
    				if(linesProcessed % 1000 == 0){
    					System.out.println("Processed " + linesProcessed + " lines");
    				}
    				linesProcessed++;
    			}
    			
    		}
    		else{
    		}
    	}
    	cd.turnOnAutoCommit();
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
}
