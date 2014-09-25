package com.smart.content.repositorymove;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
/**
 * This class is the execution point to parse the existing data and create the new required xml for content creation in IM
 * 
 * @author Infogain
 * 
 */

public class TransformXML {
	private Logger logger = Logger.getLogger("TransformXML");
	private Properties prop = new Properties();
	
	private static String parserFileLocation = "",parserMasterAttFileName="",parserExportFileLocation="";
	private static String channelName="",generalContentXML="",generalXMLTemplate="",generalContentXMLEndTag="",defaultView="";

	/**
	 * Blank constructor
	 */
	public TransformXML() {
	}
	
	/**
	 * 
	 * method is used for extracting key pair from property file parser_configuration from folder.
	 * 
	 * @param 
	 * @return String (with Parser Type)
	 */
    public boolean loadFileConfiguration(){
    	boolean response = false;
		try {
    		// properties file in classes folder
    		//InputStream inObj = getClass().getResourceAsStream("/parser_configuration.properties");
			InputStream inObj = getClass().getResourceAsStream("/"+ getClass().getPackage().getName().replace('.', '/')+ "/parser_configuration.properties");
    		
    		prop.load(inObj);
			parserFileLocation = prop.getProperty("XML_PARSER_FILELOCATION");
			parserMasterAttFileName= prop.getProperty("XML_PARSER_MASTER_ATT_FILE_NAME");
			parserExportFileLocation=prop.getProperty("XML_EXPORT_FILE_LOCATION");
			
    		generalContentXML = prop.getProperty("CONTENT_GENERAL_XML_FORMAT_STARTTAG");
    		generalContentXMLEndTag = prop.getProperty("CONTENT_GENERAL_XML_FORMAT_ENDTAG");
    		channelName = prop.getProperty("IM_CHANNEL_NAME");
    		defaultView = prop.getProperty("IM_VIEW_REFKEY");

			
			response =  true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			response= false;
			e.printStackTrace();
			logger.error("ParserConfiguration => File Not Found in check Parser Type "+e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			response= false;
			e.printStackTrace();
			logger.error("ParserConfiguration => IO exception in check Parser Type "+e.getMessage());
		}
		return response;
    }
	
	
	public static void main(String[] args) throws Exception {
		TransformXML classObj = new TransformXML();
		// Load the required attribute
		boolean loadConf = classObj.loadFileConfiguration();
		
		if (loadConf) {
			
			HandleGeneralXML xmlObj = new HandleGeneralXML(classObj.prop);
			
			generalXMLTemplate = xmlObj.generalXML(generalContentXML);

			if(generalXMLTemplate != null && !"".equals(generalXMLTemplate.trim())){
				//Pointing for content extraction process for XML
				ContentReader contentExtObj = new ContentReader(generalXMLTemplate,generalContentXMLEndTag,channelName,defaultView,parserFileLocation,parserMasterAttFileName,parserExportFileLocation);
			}
			else{
				classObj.logger.error("XML Template is Blank");
				throw new RuntimeException("XML Template is Blank");				
			}			
		}	

		System.out.println("===============================Terminating and exiting app!!!===============================");
		classObj.logger.info("===============================Terminating and exiting app!!!===============================");

	} // end of main
} // end of class
