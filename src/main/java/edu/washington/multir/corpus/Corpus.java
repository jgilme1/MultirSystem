package edu.washington.multir.corpus;

import java.sql.SQLException;

import edu.washington.multir.database.CorpusDatabase;

public abstract class Corpus {
	
	CorpusDatabase cd;
	public Corpus(boolean load, boolean train) throws SQLException{
      cd = load ? CorpusDatabase.loadCorpusDatabase(train) : CorpusDatabase.newCorpusDatabase(getSentenceTableSQLSpecification(), getDocumentTableSQLSpecification(), train);
	}
	abstract String getSentenceTableSQLSpecification();
	abstract String getDocumentTableSQLSpecification();
}
