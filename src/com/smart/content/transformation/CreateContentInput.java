package com.smart.content.transformation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.smart.content.extractor.intercept.ParserConfiguration;


/*
• 	Will read all the xml file from HTML_EXPORT_FILE_LOCATION : Done 	
• 	Create the input xml file in IM_CONTENT_IMPORT_INPUTXMLPATH : Need to add the required data for view , user group and category in the top of xml
• 	Move failed xml to archieve dir - Done
*/
public class CreateContentInput {
	
	private Logger logger = Logger.getLogger("CreateContentInput");
	
	private String inputXMLFileLocation = "",outputXMLFileLocation="",failedXMLFileLocation="",channelName="",generalXMLTemplate="",generalContentXMLEndTag="";
	

	/**
	 * constructor
	 */
	public CreateContentInput(String inputXMLFileLocation, String outputXMLFileLocation, String failedXMLFileLocation,String channelName, String generalXMLTemplate, String generalContentXMLEndTag) {
		this.inputXMLFileLocation=inputXMLFileLocation;
		this.outputXMLFileLocation = outputXMLFileLocation;
		this.failedXMLFileLocation = failedXMLFileLocation;
		this.channelName = channelName;
		this.generalXMLTemplate = generalXMLTemplate;
		this.generalContentXMLEndTag=generalContentXMLEndTag;
		processXMLFile();		
	}
			
	
	private void processXMLFile() {

		File[] files = getFiles(inputXMLFileLocation);

		for (File file : files) {

			if (file.toString().endsWith(".xml")
					|| file.toString().endsWith(".XML")) {
				
				//logger.info("XML file is ::: "+file.getName());

				if (!writeFileContent(file.getAbsolutePath(),file.getName()))
					throw new RuntimeException(
							"Exception occured during creating content of file:::::"
									+ file.getAbsolutePath());

			}

		}

	}

	/**
	 * 
	 * method is used for extracting files from folder.
	 * 
	 * @param directoryPath
	 * @return listOfFiles
	 */

	private File[] getFiles(String directoryPath) {
		try {
			if (directoryPath != null && !"".equals(directoryPath.trim())) {
				File folder = new File(directoryPath);
				File[] listOfFiles = folder.listFiles();
				return listOfFiles;
			}

		} catch (Exception e) {
			e.printStackTrace();
			//System.err.println(e);
			logger.error("File Not Found " + e.getMessage());
		}

		return null;

	}

	/**
	 * 
	 * method is used for write content as xml for given file.
	 * 
	 * @param fileName
	 * @return boolean Algorithm:
	 */
	private boolean writeFileContent(String fileAbsolutePath, String fileName) {
		boolean response = false;
		try {
			if (fileAbsolutePath != null && !"".equals(fileAbsolutePath.trim())) {
				// Actual logic for html content
				// modify the xml with OKM channel schema		
				//
				
				String xmlData = readXMLData(fileAbsolutePath);
				
				String authName = xmlData.substring(xmlData.indexOf("<AUTHORUSERNAME>"), xmlData.indexOf("</AUTHORUSERNAME>")+ "</AUTHORUSERNAME>".length());
				
				//String ownerName = xmlData.substring(xmlData.indexOf("<OWNERUSERNAME>"), xmlData.indexOf("</OWNERUSERNAME>")+ "</OWNERUSERNAME>".length());
				
				String xmlText = handleOKMAttributeInXML(xmlData.replace(authName, ""));
				
				StringBuilder builderCompleteXML = new StringBuilder();
				
				// combining xml for xml with view etc and xml with OKM channel schema attributes

				builderCompleteXML.append(generalXMLTemplate.replace("<AUTHORUSERNAME></AUTHORUSERNAME>", authName)).append(xmlText).append(generalContentXMLEndTag);

				logger.info("XML data for file "+fileName+ " is ::: "+builderCompleteXML);
				if (builderCompleteXML != null && !"".equals(builderCompleteXML.toString().trim())) {
					response  = writeTemporaryXMLFile(outputXMLFileLocation+fileName,builderCompleteXML.toString());
				}
				// writing into log file if we are not able to create the xml for html content
				if(!response){
					copyFailedXML(fileName);
				}
				
					
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			//System.err.println(e);
			logger.error("Content is not written in XML" + e.getMessage());
			return false;
		}

	} //end of writeFileContent
	
	/**
	 * 
	 * method is used for read XML content from given file.
	 * 
	 * @param fileName
	 * @return String
	 */

	private String readXMLData(String fileName) {
		BufferedReader bufferedReaderObj = null;
		StringBuilder builder = new StringBuilder("");
		try {
			String sCurrentLine;
			bufferedReaderObj = new BufferedReader(new FileReader(fileName));
			while ((sCurrentLine = bufferedReaderObj.readLine()) != null) {
				builder.append(sCurrentLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bufferedReaderObj != null)
					bufferedReaderObj.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return builder.toString();
	}

	/**
	 * 
	 * method is used for copy the failed XML to archieve to
	 * 
	 * 
	 * @param 
	 * @return boolean
	 */

	private boolean copyFailedXML(String fileName) throws IOException {
		boolean result = true;
		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		try {
				File source = new File(inputXMLFileLocation+fileName); // source for html
				
				File dest = new File(failedXMLFileLocation+fileName); // archeive

				inputChannel = new FileInputStream(source).getChannel(); 

				outputChannel = new FileOutputStream(dest).getChannel();

				outputChannel
						.transferFrom(inputChannel, 0, inputChannel.size());

			result = true;

		} catch (Exception e) {
			result = false;
			logger.error("Error in copying the failed HTML file "+e.getMessage());	
		} finally {
			if (inputChannel != null){
				inputChannel.close();
			}
			if (outputChannel != null){
				outputChannel.close();
			}

		}
		return result;
	}	

	/**
	 * 
	 * method is used for reading property file for okm_attribute_mapping
	 * resource
	 */

	private String handleOKMAttributeInXML(String xmlText) {
		
		String transformedString = xmlText; 
		
		Properties prop = new Properties();
		Properties attrprop = new Properties();
		try {
			
			// find parser type and load master_attribute.properties related to that parser type
			ParserConfiguration pc = new ParserConfiguration();
			String parserType = pc.checkParserType();

			if (parserType != null && !"".equals(parserType.trim())) {
				InputStream inObj = null ;			
				if(parserType.equalsIgnoreCase("HTML")){
					inObj = getClass().getResourceAsStream("/com/smart/content/extractor/html/master_attribute.properties");
				}
				else if(parserType.equalsIgnoreCase("XML")){
					 inObj = getClass().getResourceAsStream("/com/smart/content/extractor/xml/master_attribute.properties");
				}
				attrprop.load(inObj);
			}
    		// properties file in classes folder
    		InputStream inObj = getClass().getResourceAsStream("/"+ getClass().getPackage().getName().replace('.', '/')+ "/okm_attribute_mapping.properties");
    		prop.load(inObj);
			Enumeration enuKeys = prop.keys();
			while (enuKeys.hasMoreElements()) {
				String key = (String) enuKeys.nextElement();
				String value = prop.getProperty(key);
				
				if(xmlText.toString().contains(key)){
					
					if (value.toString().indexOf(',') != -1) {
						transformedString = transformedString.replace(key,(value.substring(0,value.indexOf(','))).toUpperCase());
						//for list type attribute
						if("LIST".equals(attrprop.get(key+"_TYPE"))){
								String startText = value.substring(0,value.indexOf(','))+">";
								String endText = "</";
								String replacementStr = "";
								int lastIndex = 0, lastIndexforText = 0;
								while ((lastIndex = transformedString.indexOf(startText, lastIndex)) != -1) {
									lastIndex += startText.length();
									lastIndexforText = transformedString.indexOf(endText, lastIndex);
									replacementStr = transformedString.substring(lastIndex,
											lastIndexforText);
									ListTypeAttributeDBDao listDAO = new ListTypeAttributeDBDao();
									String xmlToAppend = listDAO.execute(value.substring(value.indexOf(',')+1), stripCDATA(replacementStr));
									transformedString  = transformedString.replace(replacementStr, xmlToAppend);
									break;
								} // end of while
								// for attachment
						} else if("NODE".equals(attrprop.get(key+"_TYPE"))){
							transformedString = transformedString.replace((value.substring(0,value.indexOf(','))).toUpperCase()+"_NODE_ATTRIBUTE",(value.substring(value.indexOf(',')+1)).toUpperCase());
						}
					} // end of if
					else {
						transformedString = transformedString.replace(key,value.toUpperCase());
					}
									
				}
				// Handle merging of fields
				if (key.indexOf('+') != -1) {
					StringBuffer tempXML = new StringBuffer("<![CDATA[");
					StringTokenizer stringTokenizerForKey = new StringTokenizer(key, "+");
					
					while (stringTokenizerForKey.hasMoreElements()) {
						String keyData = stringTokenizerForKey.nextElement().toString();
						if(xmlText.toString().contains(keyData)){
							tempXML.append(xmlText.substring(xmlText.indexOf("<"+keyData+"><![CDATA[")+("<"+keyData+"><![CDATA[").length(), xmlText.indexOf("]]></"+keyData+">")));
							tempXML.append('\n');
						}
					}
					tempXML.append("]]>");
					String dataPrepared = "<"+value+">"+tempXML.toString()+"</"+value+">";
					transformedString = transformedString.replace("</CONTENT>", dataPrepared+"</CONTENT>");
				} // end of merging fields
				
			} // end of while

    		transformedString = transformedString.replace("CONTENT", channelName);
			
		} catch (IOException ex) {
			transformedString = "";
			ex.printStackTrace();
			logger
					.error("Error in reading the reading property file for okm_attribute_mapping "
							+ ex.getMessage());
		}
		return transformedString;
	}
	
	/**
	 * 
	 * method is used for store xml in FILE or write the log file in case of fail
	 * 
	 * @param String for content XML
	 * @return boolean
	 */	
	private boolean writeTemporaryXMLFile(String location,String contentXML) {
		boolean response = false;
		try {
			// Create file
			FileWriter fstream = new FileWriter(
					location, false); // file

			BufferedWriter out = new BufferedWriter(fstream);
			out.write(contentXML);
			// Close the output stream
			out.close();
			response = true;
		} catch (Exception ex) {// Catch exception if any
			//System.err.println("Error in writing the file " + ex.getMessage());
			logger.error("Error in writing the file during content trasform " + ex.getMessage());
			response = false;
		}
		return response;

	}
	// method provided by Medha
	private String stripCDATA(String type) {
	    type = type.trim();
	    if (type.startsWith("<![CDATA[")) {
	      type = type.substring(9);
	      int indexCount = type.indexOf("]]>");
	      if (indexCount == -1) {
	        throw new IllegalStateException(
	            "argument starts with <![CDATA[ but cannot find pairing ]]&gt;");
	      }
	      type = type.substring(0, indexCount);
	    }
	    return type;
	  }
	
} // end of class
