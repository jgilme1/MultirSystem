package edu.washington.multir.corpus;

import java.sql.SQLException;

public class DefaultCorpus extends Corpus {
	
	public DefaultCorpus(boolean load, boolean train) throws SQLException{
		super(load,train);
	}

	@Override
	final String getSentenceTableSQLSpecification() {
		return "(sentenceId int, docName VARCHAR(3200), text VARCHAR(32000))";
	}

	@Override
	final String getDocumentTableSQLSpecification() {
		return "(docName VARCHAR(3200), coref VARCHAR(3200)";
	}

}
