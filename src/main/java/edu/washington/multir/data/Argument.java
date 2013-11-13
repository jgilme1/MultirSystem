package edu.washington.multir.data;

/**
 * Argument represents an offset interval
 * with optional meta information like
 * name and id.
 * @author jgilme1
 *
 */
public class Argument {
	int startOffset;
	int endOffset;
	String argName;
	String argID;

	public int getStartOffset(){return startOffset;}
	public String getArgName(){return argName;}
	public String getArgID(){return argID;}
	public int getEndOffset(){return endOffset;}
	
	public Argument(String name, int startOffset, int endOffset, String argID){
		this.argName = name;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.argID = argID;
	}
	
	public Argument(String name, int startOffset, int endOffset){
		this(name,startOffset,endOffset,null);
	}
}
