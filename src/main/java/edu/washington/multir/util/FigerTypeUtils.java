package edu.washington.multir.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Set;

public class FigerTypeUtils {
	public static void main(String[] args) {
		//init();
//		Set<String> fbTypes = getFreebaseTypes("Barack Obama");
//		for (String fbType : fbTypes) {
//			System.out.println("FB:"+fbType);
//		}
//		Set<String> figerTypes = getFigerTypes("Barack Obama");
//		for (String type : figerTypes) {
//			System.out.println("FIGER:"+type);
//		}
//		close();
	}

	private static Connection conn = null;
	private static String guidQueryPrefix = "select guid from freebase_names where name=?";
	private static String typeQueryPrefix = "select type from freebase_types where guid=?";
//	private PreparedStatement guidQuery = null;
//	private PreparedStatement typeQuery = null;
	public final static String typeFile = "types.map";
	public static Hashtable<String, String> mapping = null;

	public static void init() {
		try {
			// initialize the db connection
			conn = DriverManager
					.getConnection("jdbc:postgresql://pardosa05.cs.washington.edu:5432/wex?user=jgilme1"
							+ "&charSet=UTF8");
//			guidQuery = conn
//					.prepareStatement("select guid from freebase_names where name=?");
//			typeQuery = conn
//					.prepareStatement("select type from freebase_types where guid=?");

			// initialize the freebase-figer type mapping
			if (mapping == null) {
				mapping = new Hashtable<String, String>();
				Scanner scanner = new Scanner(
						FigerTypeUtils.class.getResourceAsStream(typeFile),
						"UTF-8");
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					String arg = line.substring(0, line.indexOf("\t")), newType = line
							.substring(line.indexOf("\t") + 1).trim()
							.replace("\t", "/");
					mapping.put(arg, newType);
				}
				scanner.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static  void close() {
		try {
//			guidQuery.close();
//			typeQuery.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	/**
//	 * return a list of FIGER types for the entityName or null if the
//	 * entityName string is missing in the database.
//	 * 
//	 * @param entityName: space separated
//	 * @return
//	 */
//	public  Set<String> getFigerTypes(String entityName) {
//		Set<String> fbTypes = getFreebaseTypes(entityName);
//		if (fbTypes == null) {
//			return null;
//		}
//		Set<String> types = new HashSet<String>();
//		for (String fbType : fbTypes) {
//			String figerType = mapToFigerType(fbType);
//			if (figerType != null) {
//				types.add(figerType);
//			}
//		}
//		return types;
//	}
	
	/**
	 * return a list of FIGER types for the entityID or null if the
	 * entityID string is missing in the database.
	 * 
	 * @param entityName: space separated
	 * @return
	 */
	public static Set<String> getFigerTypesFromID(String entityID) {
		Set<String> fbTypes = getFreebaseTypesFromID(entityID);
		if (fbTypes == null) {
			return null;
		}
		Set<String> types = new HashSet<String>();
		for (String fbType : fbTypes) {
			String figerType = mapToFigerType(fbType);
			if (figerType != null) {
				types.add(figerType);
			}
		}
		return types;
	}

//	/**
//	 * return a list of Freebase types for the entityName or null if the
//	 * entityName string is missing in the database.
//	 * 
//	 * @param entityName: space separated
//	 * @return
//	 */
//	public  Set<String> getFreebaseTypes(String entityName) {
//		Set<String> types = new HashSet<String>();
//		try {
//			guidQuery.setString(1, entityName);
//			ResultSet rs = guidQuery.executeQuery();
//			String guid = null;
//			if (rs.next()) {
//				guid = rs.getString(1);
//				rs.close();
//			} else {
//				// entityName not found!
//				rs.close();
//				return null;
//			}
//			typeQuery.setString(1, guid);
//			rs = typeQuery.executeQuery();
//			while (rs.next()) {
//				types.add(rs.getString(1));
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return types;
//	}
	
	/**
	 * return a list of Freebase types for the entityID or null if the
	 * entityID string is missing in the database.
	 * 
	 * @param entityName: space separated
	 * @return
	 */
	public  static Set<String> getFreebaseTypesFromID(String entityID) {
		Set<String> types = new HashSet<String>();
		try {
			PreparedStatement typeQuery = conn.prepareStatement(typeQueryPrefix);
			typeQuery.setString(1, entityID);
			ResultSet rs = typeQuery.executeQuery();
			while (rs.next()) {
				types.add(rs.getString(1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return types;
	}

	/**
	 * It returns a FIGER type for the given Freebase type or a null if there is
	 * no mapping (i.e. the Freebase type should probably be discarded anyways).
	 * 
	 * @param freebaseType
	 * @return
	 */
	public static String mapToFigerType(String freebaseType) {
		if (mapping.containsKey(freebaseType)) {
			return mapping.get(freebaseType);
		} else {
			return null;
		}
	}
}
