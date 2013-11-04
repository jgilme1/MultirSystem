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
	public static CorpusDatabase loadCorpusDatabase(boolean train) throws SQLException{
		String databaseName = train ? trainingDatabaseName : testDatabaseName;
		DerbyDb db = new DerbyDb(databaseName,true);
		return new CorpusDatabase(databaseName,db);
	}
	public static CorpusDatabase newCorpusDatabase(String sentenceTableSQLSpecification, String documentTableSQLSpecification, boolean train) throws SQLException{
		String databaseName = train ? trainingDatabaseName : testDatabaseName;
		DerbyDb db = new DerbyDb(databaseName,false);
		deleteTable(db.connection,sentenceInformationTableName);
		deleteTable(db.connection,documentInformationTableName);
		createTable(db.connection,sentenceInformationTableName,sentenceTableSQLSpecification);
		createTable(db.connection,documentInformationTableName,documentTableSQLSpecification);
		return new CorpusDatabase(databaseName,db);
	}
	private static void deleteTable(Connection connection, String tableName) throws SQLException{
		try{
			connection.prepareStatement("DROP TABLE " + tableName).execute();
		}
		catch(SQLException e){
			return;
		}
	}
	private static void createTable(Connection connection, String tableName, String tableSpecification) throws SQLException{
		System.out.println(tableSpecification);
		connection.prepareStatement("CREATE TABLE " + tableName + " " + tableSpecification).execute();
	}
	
	public ResultSet getDocumentRows() throws SQLException {
		return issueQuery("SELECT * FROM " + documentInformationTableName);
	}
	
	public ResultSet getSentenceRows(String columnName, String value) throws SQLException{
		return issueQuery("SELECT * FROM " + sentenceInformationTableName + " WHERE " + columnName + "='" + value+"'");
	}
	public ResultSet issueQuery(String queryString) throws SQLException{
		System.out.println(queryString);
		return db.connection.prepareStatement(queryString).executeQuery();
	}
	
	private void insert(String tableName, List<String> columnNames, List<Object> values) throws SQLException{
		StringBuilder command = new StringBuilder();
		command.append("INSERT INTO " + tableName + " (");
		for(String columnName: columnNames){
			command.append(" " + columnName + ",");
		}
		command.deleteCharAt(command.length()-1);
		command.append(") VALUES (");
	    for(Object value: values){
	    	command.append(" ");
	    	if(value instanceof String){
	    		command.append("'");
	    		command.append(value.toString().replaceAll("'", "''"));
	    		command.append("'");
	    	}
	    	else{
	    		command.append(value.toString());
	    	}
	    	command.append(",");
	    }
	    command.deleteCharAt(command.length()-1);
		command.append(")");
		try{
		 db.connection.prepareStatement(command.toString()).execute();		
		}
		catch(SQLException e){
			System.err.println(command.toString());
			throw e;
		}
	}
	
	public void insertToSentenceTable(List<String> columnNames, List<Object> values) throws SQLException{
		insert(sentenceInformationTableName,columnNames,values);
	}
	public void insertToDocumentTable(List<String> columnNames, List<Object> values) throws SQLException{
		insert(documentInformationTableName,columnNames,values);
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
	public void turnOffAutoCommit() throws SQLException {
		db.connection.setAutoCommit(false);
	}
	public void turnOnAutoCommit() throws SQLException{
		db.connection.setAutoCommit(true);
	}
	
}
