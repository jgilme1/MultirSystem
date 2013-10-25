package edu.washington.multir.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public final class CorpusDatabase {
	
	private String name;
	private DerbyDb db;
	private static final String sentenceInformationTableName = "SentenceTable";
	private static final String documentInformationTableName = "DocumentTable";
	private static final String trainingDatabaseName = "TrainingCorpusDatabase";
	private static final String testDatabaseName = "TestCorpusDatabase";
	
	private CorpusDatabase(String name, DerbyDb db){
		this.name = name;
		this.db =db;
	}
	public static CorpusDatabase loadCorpusDatabase(boolean train){
		String databaseName = train ? trainingDatabaseName : testDatabaseName;
		DerbyDb db = new DerbyDb(databaseName);
		return new CorpusDatabase(databaseName,db);
	}
	public static CorpusDatabase newCorpusDatabase(String sentenceTableSQLSpecification, String documentTableSQLSpecification, boolean train) throws SQLException{
		String databaseName = train ? trainingDatabaseName : testDatabaseName;
		DerbyDb db = new DerbyDb(databaseName);
		deleteTable(db.connection,sentenceInformationTableName);
		deleteTable(db.connection,documentInformationTableName);
		createTable(db.connection,sentenceInformationTableName,sentenceTableSQLSpecification);
		createTable(db.connection,documentInformationTableName,documentTableSQLSpecification);
		return new CorpusDatabase(databaseName,db);
	}
	private static void deleteTable(Connection connection, String tableName) throws SQLException{
		connection.prepareStatement("DROP TABLE " + tableName).execute();
	}
	private static void createTable(Connection connection, String tableName, String tableSpecification) throws SQLException{
		connection.prepareStatement("CREATE TABLE " + tableName + " " + tableSpecification).execute();
	}
	public List<List<String>> issueQuery(String queryStatement, List<Integer> columnIds) throws SQLException{
		ResultSet results = db.connection.prepareStatement(queryStatement).executeQuery();
		List<List<String>> resultList = new ArrayList<List<String>>();
		while(results.next()){
			List<String> information = new ArrayList<String>();
			for(Integer i : columnIds){
				information.add(results.getString(i));
			}
			resultList.add(information);
		}
		return resultList;
	}
}
