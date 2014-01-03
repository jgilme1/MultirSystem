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

	public int getStartOffset(){return startOffset;}
	public String getArgName(){return argName;}
	public int getEndOffset(){return endOffset;}
	
	public Argument(String name, int startOffset, int endOffset){
		this.argName = name;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
	}
	
	protected Argument (Argument a){
		this.startOffset = a.startOffset;
		this.endOffset = a.endOffset;
		this.argName = a.argName;
	}
}
