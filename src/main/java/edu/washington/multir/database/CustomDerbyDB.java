package edu.washington.multir.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CustomDerbyDB {
	
	private DerbyDb db;
	private String tableSpecification;
	private String tableName;
	private String[] columns;
	
	
	
	public static CustomDerbyDB createCustomDerbyDB(String name, String... columnNamesAndValues) throws SQLException{
		CustomDerbyDB cdb = new CustomDerbyDB();
		StringBuilder tableSpecificationBuilder = new StringBuilder();
		cdb.columns = new String[columnNamesAndValues.length/2];
		cdb.tableName = name+"Table";
		int columnIndex =0;
		tableSpecificationBuilder.append("( ");
		for(int i =0; i < columnNamesAndValues.length; i+=2){
			tableSpecificationBuilder.append(columnNamesAndValues[i] + " " + columnNamesAndValues[i+1]+",\n");
			cdb.columns[columnIndex] = columnNamesAndValues[i];
			columnIndex++;
		}
		//tableSpecificationBuilder.append("PRIMARY KEY (" + columnNamesAndValues[0] + " ))");
		tableSpecificationBuilder.setLength(tableSpecificationBuilder.length()-2);
		tableSpecificationBuilder.append(")");
		cdb.tableSpecification = tableSpecificationBuilder.toString();
		//System.out.println(cdb.tableSpecification);
		
		
		cdb.db = new DerbyDb(name);
		try{
			cdb.db.connection.prepareStatement("CREATE TABLE " + name+"Table" + " " + cdb.tableSpecification).execute();
		}
		catch(SQLException e){
			e.printStackTrace();
			cdb.db.connection.prepareStatement("DROP TABLE " + cdb.tableName).execute();
			cdb.db.connection.prepareStatement("CREATE TABLE " + name+"Table" + " " + cdb.tableSpecification).execute();

		}
		cdb.db.connection.setAutoCommit(false);
		//cdb.db.connection.prepareStatement("CREATE INDEX FEATUREINDEX ON " + cdb.tableName + "(" + columnNamesAndValues[0] + ")").execute();
		return cdb;
	}
	
	
	public void putRow(Object[] values) throws SQLException{
		StringBuilder sqlCommand = new StringBuilder();
		sqlCommand.append("INSERT INTO " + tableName + "\nVALUES (");
		for(Object v : values){
			sqlCommand.append("'"+v.toString().replace("'", "''")+"',");
		}
		sqlCommand.setLength(sqlCommand.length()-1);
		sqlCommand.append(")");
		//System.out.println(sqlCommand.toString());
		db.connection.prepareStatement(sqlCommand.toString()).execute();
	}
	
	
	public Map<String,Object> getRow(String columnName, String value) throws SQLException{
		StringBuilder sqlQuery = new StringBuilder();
		sqlQuery.append("SELECT * FROM " + tableName);
		sqlQuery.append(" WHERE " + columnName + "='" + value+"'");
		ResultSet rs = db.connection.prepareStatement(sqlQuery.toString()).executeQuery();
		Map<String,Object> result = new HashMap<String,Object>();
		int columnCount = rs.getMetaData().getColumnCount();
		for(int i =1; i<= columnCount; i++){
			result.put(columns[i-1],rs.getObject(i));
		}
		return result;
	}

	
	public boolean contains(String columnName, String value) throws SQLException{
		StringBuilder sqlQuery = new StringBuilder();
		sqlQuery.append("SELECT * FROM " + tableName);
		sqlQuery.append(" WHERE " + columnName + "='" + value.replaceAll("'", "''") +"'");
		//System.out.println(sqlQuery.toString());
		ResultSet rs = db.connection.prepareStatement(sqlQuery.toString()).executeQuery();
		if(rs.next()){
			return true;
		}
		else{
			return false;
		}
		
	}
	
	public ResultSet executeQuery(String sqlQuery) throws SQLException{
		return db.connection.prepareStatement(sqlQuery).executeQuery();
	}
	
	public boolean executeCommand(String sqlCommand) throws SQLException{
		return db.connection.prepareStatement(sqlCommand).execute();
	}
	
    public void close() {
        try {
          db.connection.close();
        } catch (SQLException e) {
          throw new RuntimeException("Error closing DB connection.", e);
        }
      }
    
    public static void main(String[] args) throws SQLException{
    	CustomDerbyDB cdb = CustomDerbyDB.createCustomDerbyDB("SingleFeatures","FEATURE","VARCHAR(20000)");
    	
		Object[] objectValues = new Object[1];
		objectValues[0] = "inverse_true|Rebel|/people/ethnicity|President|/person|'s";
		cdb.putRow(objectValues);
    	System.out.println(cdb.contains("FEATURE", "inverse_true|Rebel|/people/ethnicity|President|/person|'s"));
    }
    
    
    
    public String getTableName(){return tableName;}


	public Connection getConnection() {
		return db.connection;
	}
}
