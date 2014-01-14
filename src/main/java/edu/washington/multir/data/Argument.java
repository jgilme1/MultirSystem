package edu.washington.multir.data;

import java.util.List;

import edu.stanford.nlp.util.Interval;

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
	public boolean intersectsWithList(List<Argument> args) {
		
		for(Argument arg: args){
			if(this.intersects(arg)){
				return true;
			}
		}
		return false;
	}
	private boolean intersects (Argument other){
		Interval<Integer> thisInterval = Interval.toInterval(this.startOffset,this.endOffset);
		Interval<Integer> otherInterval = Interval.toInterval(other.startOffset, other.endOffset);
		if(thisInterval.intersect(otherInterval) != null){
			return true;
		}
		else{
			return false;
		}
	}
	
	public boolean containedInList(List<Argument> args) {
		
		for(Argument other : args){
			if(this.hasSameOffsets(other)){
				return true;
			}
		}
		return false;

	}
	
	private boolean hasSameOffsets(Argument other){
		if(
		   (this.getStartOffset() == other.getStartOffset()) &&
		   (this.getEndOffset()  == other.getEndOffset()) && 
		   (this.getArgName().equals(other.getArgName()))){
			return true;
		}
		else{
			return false;
		}
	}
}
