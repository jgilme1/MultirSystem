package edu.washington.multir.data;

public class Argument {
	int globalSentId;
	int sentOffset;
	String argString;
	String argID;
	
	public Argument(int globalSentId, int sentOffset, String argString, String argID){
		this.globalSentId = globalSentId;
		this.sentOffset = sentOffset;
		this.argString = argString;
		this.argID = argID;
	}
}
