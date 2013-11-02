package edu.washington.multir.corpus;

import java.io.File;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
	
	protected Corpus(CorpusInformationSpecification cis, boolean load, boolean train) throws SQLException{
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
    		a.set(si.getAnnotationKey(), si.read(sentenceResults.getObject(index)));
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
    
    
    public void loadCorpus(File path, CorpusInformationSpecification ci){
    	if(path.isDirectory()){
    		File[] filesInDirectory = path.listFiles();
    		//check for all required files
    		if(requiredFilesExist(Arrays.asList(filesInDirectory), ci)){
    			
    		}
    	}
    	
    }
    
    private boolean requiredFilesExist(List<File> files, CorpusInformationSpecification cis){
    	List<String> requiredFileNames = new ArrayList<String>();
    	requiredFileNames.add("sentences.meta");
    	//sentences.meta will be sentID docname tokens 
    	for(SentInformationI si :cis.sentenceInformation){
    		if(si.name() != "DOCNAME" && si.name() != "SENTID" && si.name()!="SentTokensInformation"){
    			requiredFileNames.add(si.name());
    		}
    	}
    	for(TokenInformationI ti: cis.tokenInformation){
    		requiredFileNames.add(si.name());
    	}
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
