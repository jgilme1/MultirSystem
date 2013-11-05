package edu.washington.multir.data;

public class Argument {
	int globalSentId;
	String argString;
	String argID;
	
	public int getGlobalSentId(){return globalSentId;}
	public String getArgString(){return argString;}
	public String getArgID(){return argID;}
	
	public Argument(int globalSentId, String argString, String argID){
		this.globalSentId = globalSentId;
		this.argString = argString;
		this.argID = argID;
	}
}
