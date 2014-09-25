package com.smart.content.extractor.intercept;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class ParserConfiguration {
	private Logger logger = Logger.getLogger("ParserConfiguration");
	private static Properties prop = new Properties();
	
	public static String parserFileLocation = "",parserMasterAttFileName="",parserExportFileLocation="",parserExportFailedFileLocation="",parserRemoteAuth="";

	/**
	 * Blank constructor
	 */
	public ParserConfiguration() {
		// FOR LOG FILE
		ClassLoader classLoader =  
			   Thread.currentThread().getContextClassLoader();  
			PropertyConfigurator.configure(classLoader.getResource("log4j.properties"));  
	}
	
	/**
	 * 
	 * method is used for extracting key pair from property file parser_configuration from folder.
	 * 
	 * @param 
	 * @return String (with Parser Type)
	 */
    public String checkParserType(){
    	String parserType = "";
		ParserConfigurationUtility upu = new ParserConfigurationUtility();
		try {
			prop = upu.readProp();
			Enumeration enuKeys = prop.keys();
			while (enuKeys.hasMoreElements()) {
				String key = (String) enuKeys.nextElement();
				String value = prop.getProperty(key);
				if(key.endsWith("_PARSER_FLAG") && value.equalsIgnoreCase("true")){
					parserType = key.substring(0,key.indexOf('_'));
				}
			}

			parserFileLocation = prop.getProperty(parserType+"_PARSER_FILELOCATION");
			parserMasterAttFileName= prop.getProperty(parserType+"_PARSER_MASTER_ATT_FILE_NAME");
			parserExportFileLocation=prop.getProperty(parserType+"_EXPORT_FILE_LOCATION");
			parserExportFailedFileLocation=prop.getProperty(parserType+"_FAILED_EXPORT_FILE_LOCATION");
			parserRemoteAuth = prop.getProperty(parserType+"_REMOTE_AUTHENTICATION");
			

			//logger.info("parserFileLocation "+parserFileLocation+ " parserMasterAttFileName "+parserMasterAttFileName + " parserExportFileLocation "+parserExportFileLocation +" parserRemoteAuth "+parserRemoteAuth);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("ParserConfiguration => File Not Found in check Parser Type "+e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("ParserConfiguration => IO exception in check Parser Type "+e.getMessage());
		}
    	return parserType;
    }
	
	
	public static void main(String[] args) throws Exception {
		ParserConfiguration classObj = new ParserConfiguration();
		// Check ParserType and load the required attribute
		String parserType = classObj.checkParserType();
		classObj.logger.info("Parser Type is "+parserType);

		if (parserType != null && !"".equals(parserType.trim())) {
			if(parserType.equalsIgnoreCase("HTML")){
				//Pointing for content extraction process for HTML
				com.smart.content.extractor.html.ContentExtractor contentExtObj = new com.smart.content.extractor.html.ContentExtractor(parserFileLocation,parserMasterAttFileName,parserExportFileLocation,parserExportFailedFileLocation,parserRemoteAuth);
			}
			else if(parserType.equalsIgnoreCase("XML")){
				//Pointing for content extraction process for XML
				com.smart.content.extractor.xml.ContentExtractor contentExtObj = new com.smart.content.extractor.xml.ContentExtractor(parserFileLocation,parserMasterAttFileName,parserExportFileLocation,parserExportFailedFileLocation,parserRemoteAuth);
			}
		}
		
		classObj.logger.info("Terminating and exiting app!!! ");
		classObj.logger.info("===============================ENDING OF CONTENT EXTRACTION===============================");

	} // end of main

} // end of class
