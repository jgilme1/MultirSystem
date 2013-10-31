package edu.washington.multir.corpus;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.washington.multir.database.CorpusDatabase;

public abstract class Corpus {
	
	private CorpusDatabase cd;
	private SentInformationI[] sentenceInformationTypes;
	private static String documentColumnName = "DOCNAME";
	private static String sentIDColumnName = "SENTID";
	
    public static class SentGlobalID implements CoreAnnotation<Integer>{
		@Override
		public Class<Integer> getType() {
			return Integer.class;
		}
    }
    public static class SentDocName implements CoreAnnotation<String>{
		@Override
		public Class<String> getType() {
			return String.class;
		}
    }
	
	protected Corpus(SentInformationI[] sentenceInformationTypes, boolean load, boolean train) throws SQLException{
	  this.sentenceInformationTypes = sentenceInformationTypes;
      cd = load ? CorpusDatabase.loadCorpusDatabase(train) : CorpusDatabase.newCorpusDatabase(getSentenceTableSQLSpecification(), getDocumentTableSQLSpecification(), train);
	}
	private String getSentenceTableSQLSpecification(){
		StringBuilder sqlTableSpecificationBuilder = new StringBuilder();
		sqlTableSpecificationBuilder.append("( " + sentIDColumnName + " int,\n");
		sqlTableSpecificationBuilder.append(documentColumnName +" VARCHAR(128),\n");
		// add SentenceInformation types
		for(SentInformationI sentenceInformation : sentenceInformationTypes){
			String columnName = sentenceInformation.getClass().getSimpleName();
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
    	//add annotation for global sent Id
    	Integer globalId = sentenceResults.getInt(1);
    	a.set(SentGlobalID.class, globalId);
    	
    	//add annotation for document Name
    	String docName = sentenceResults.getString(2);
    	a.set(SentDocName.class,docName);
    	
    	//add all other sent information types as specified during object construction
    	int startIndex = 3;
    	for(SentInformationI si : sentenceInformationTypes){
    		Class key = si.getAnnotationKey();
    		a.set(key, si.read(sentenceResults.getString(startIndex)));
    		startIndex++;
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
    	Corpus c = new DefaultCorpus(false,true);
    	List<String> columnNames = new ArrayList<String>();
    	List<Object> values = new ArrayList<Object>();
    	columnNames.add(sentIDColumnName);
    	columnNames.add(documentColumnName);
    	for(SentInformationI info : c.sentenceInformationTypes){
    		columnNames.add(info.getClass().getSimpleName());
    	}
    	values.add(new Integer(0));
    	values.add("DOC1");
    	values.add("This is the sentence text.");
    	values.add("This is the sentence text .");
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
    }
}
