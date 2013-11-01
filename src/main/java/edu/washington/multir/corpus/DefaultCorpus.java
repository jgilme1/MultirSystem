package edu.washington.multir.corpus;

import java.sql.SQLException;

public class DefaultCorpus extends Corpus {
	
	//describe all the information types involed in this corpus type
	private static SentInformationI[] sentenceInformationTypes = {DefaultSentInformation.getSentTextInformation(),
																  DefaultSentInformation.getSentTokensInformation()};
	
	private static TokenInformationI[] tokenInformationTypes = {}
	
	
	public DefaultCorpus(boolean load, boolean train) throws SQLException{
		super(sentenceInformationTypes,tokenInformationTypes,load,train);
	}	
}
