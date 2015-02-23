package com.smart.content.transformation;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class ContentTransform {
	
	private Logger logger = Logger.getLogger("ContentTransform");
	
	private static String inputXMLFileLocation = "",outputXMLFileLocation="",failedXMLFileLocation="",channelName="",generalContentXML="",generalContentXMLEndTag="";

	/**
	 * Blank constructor
	 */
	public ContentTransform() {
		// FOR LOG FILE
		ClassLoader classLoader =  
			   Thread.currentThread().getContextClassLoader();  
			PropertyConfigurator.configure(classLoader.getResource("log4j.properties"));  

	}
	
	/**
	 * 
	 * method is used for extracting key pair from property file content_import.properties from folder.
	 * 
	 * @param 
	 * @return String 
	 */
	Properties prop = new Properties();
    private String loadResources(){
    	String response = "SUCCESS";
    	
		try {
    		// properties file in classes folder
    		//InputStream inObj = getClass().getResourceAsStream("/"+ getClass().getPackage().getName().replace('.', '/')+ "/content_import.properties");
    		//prop.load(inObj);
			File file = new File((System.getProperty("SMART_HOME")+"content_import.properties"));

			FileReader reader = new FileReader(file) ; 
			prop.load(reader);


    		inputXMLFileLocation = prop.getProperty("IM_CONTENT_FILE_INPUTXMLPATH");
    		outputXMLFileLocation= prop.getProperty("IM_CONTENT_IMPORT_INPUTXMLPATH");
    		failedXMLFileLocation=prop.getProperty("IM_CONTENT_FAILED_INPUTXMLPATH");
    		generalContentXML = prop.getProperty("CONTENT_GENERAL_XML_FORMAT_STARTTAG");
    		generalContentXMLEndTag = prop.getProperty("CONTENT_GENERAL_XML_FORMAT_ENDTAG");
    		channelName = prop.getProperty("IM_CHANNEL_NAME");

    	} catch (IOException ex) {
    		response = "FAILURE";
    		ex.printStackTrace();
    		logger.error("Error in reading the Attribute type file in content transform "+ex.getMessage());
        }		

    	return response;
    }
	
	public static void main(String[] args) throws Exception {
		ContentTransform classObj = new ContentTransform();
		String response = classObj.loadResources();
		if(response.equalsIgnoreCase("SUCCESS") && generalContentXML != null && !"".equals(generalContentXML.trim())){
			HandleGeneralXML xmlObj = new HandleGeneralXML(classObj.prop);
			String xmlTemplate = xmlObj.generalXML(generalContentXML);

			if(xmlTemplate != null && !"".equals(xmlTemplate.trim())){
				//Pointing for create content input process
				CreateContentInput createContentInputObj = new CreateContentInput(inputXMLFileLocation,outputXMLFileLocation,failedXMLFileLocation,channelName,xmlTemplate,generalContentXMLEndTag);
			}
			else{
				classObj.logger.error("XML Template is Blank");
				throw new RuntimeException("XML Template is Blank");				
			}
		}
		// closing the db connection used for finding the list data items
		if (ListTypeAttributeDBDao.connection != null)
			ListTypeAttributeDBDao.connection.close();

		classObj.logger.info("Terminating and exiting app!!! ");
		classObj.logger.info("===============================ENDING OF CONTENT TRANSFORMATION===============================");

	} // end of main
	
} // end of class
