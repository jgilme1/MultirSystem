package edu.washington.multir.corpus;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import edu.stanford.nlp.pipeline.Annotation;
import edu.washington.multir.database.CorpusDatabase;

public abstract class Corpus {
	
	private CorpusDatabase cd;
	private SentenceInformationI[] sentenceInformationTypes;
	private DocumentIterator di;
	protected Corpus(SentenceInformationI[] sentenceInformationTypes, boolean load, boolean train) throws SQLException{
	  this.sentenceInformationTypes = sentenceInformationTypes;
      cd = load ? CorpusDatabase.loadCorpusDatabase(train) : CorpusDatabase.newCorpusDatabase(getSentenceTableSQLSpecification(), getDocumentTableSQLSpecification(), train);
      this.di = new DocumentIterator();
	}
	private String getSentenceTableSQLSpecification(){
		StringBuilder sqlTableSpecificationBuilder = new StringBuilder();
		sqlTableSpecificationBuilder.append("( SENTID int, ");
		for(SentenceInformationI sentenceInformation : sentenceInformationTypes){
			String columnName = sentenceInformation.getClass().getName();
		}
		sqlTableSpecificationBuilder.append("PRIMARY KEY (SENTID))");
		return sqlTableSpecificationBuilder.toString();
	}
	private String getDocumentTableSQLSpecification(){
		StringBuilder sqlTableSpecificationBuilder = new StringBuilder();
		sqlTableSpecificationBuilder.append("( DOCNAME VARCHAR(128),");
		
		sqlTableSpecificationBuilder.append("PRIMARY KEY (DOCNAME))");
		return sqlTableSpecificationBuilder.toString();
	}
	
    private class DocumentIterator implements Iterator<Annotation>{
    	
    	String currentDocument = cd.firstDocument;
		@Override
		public boolean hasNext() {
			if(currentDocument != null){
				return true;
			}
			else{
				return false;
			}
		}

		@Override
		public Annotation next() {
			//query for first document and build Annotation
			
			
			currentDocument = cd.nextDocument(currentDocument);
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub
			
		}    	
    }
    
    public DocumentIterator getDocumentIterator(){
    	return this.di;
    }
    
    
}
