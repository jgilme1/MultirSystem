package edu.washington.multir.data;

import java.util.List;

public class NegativeAnnotation {
	
	private KBArgument arg1;
	private KBArgument arg2;
	private Integer sentNum;
	private List<String> negativeRelations;
	
	
	public NegativeAnnotation(KBArgument arg1, KBArgument arg2, Integer sentNum, List<String> negativeRelations){
		this.arg1=arg1;
		this.arg2=arg2;
		this.sentNum =sentNum;
		this.negativeRelations=negativeRelations;
	}
	
	
	public KBArgument getArg1(){return arg1;}
	public KBArgument getArg2(){return arg2;}
	public Integer getSentNum(){return sentNum;}
	public List<String> getNegativeRelations(){return negativeRelations;}


}
