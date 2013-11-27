package edu.washington.multir.preprocess;

import java.io.IOException;

import edu.stanford.nlp.pipeline.Annotation;

public class PrintTestDocument {

	
	public static void main(String[] args) throws IOException, InterruptedException{
		
		Annotation a = CorpusPreprocessing.getTestDocument("/homes/gws/jgilme1/XIN_ENG_20021028.0184.LDC2007T07.sgm");
	}
}
