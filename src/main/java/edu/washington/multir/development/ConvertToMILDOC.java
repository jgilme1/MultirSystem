package edu.washington.multir.development;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ConvertToMILDOC {
	
	public static void main(String[] args) throws IOException{
		
		BufferedReader in = new BufferedReader(new FileReader(new File(args[0])));
		String nextLine;
		
		while((nextLine = in.readLine()) != null){
			String[] values = nextLine.split("\t");
			
			
		}
		
		
		
		
		
		in.close();
		
		
	}

}
