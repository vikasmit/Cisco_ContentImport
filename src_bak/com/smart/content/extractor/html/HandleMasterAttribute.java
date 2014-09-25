package com.smart.content.extractor.html;
import java.io.*;


public class HandleMasterAttribute {
	
	public HandleMasterAttribute (String file)throws FileNotFoundException{
		fileName = new String (file);
		bFileReader = new BufferedReader (new FileReader (fileName));
	
	};
	
	public String getNextLine ()throws IOException{
		return bFileReader.readLine();
		
	}
	
	public String fileName (){
		return fileName;
	}
	public void done () throws IOException{
		bFileReader.close();
	}
	
	
	private BufferedReader bFileReader; 
	private String fileName;
}
