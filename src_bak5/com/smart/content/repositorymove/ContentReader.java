package com.smart.content.repositorymove;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;
/**
 * This class is responsible for writing the new transformed xml
 * 
 * @author Infogain
 * 
 */

public class ContentReader {
	private static Logger logger = Logger.getLogger("ContentReader");
	private String parserMasterAttFileName = "", parserExportFileLocation = "",multiNodeName="",multiNodeNameNew="",okmXml="",existingDocID="",existingAuthorName="",
	existingOwnerName="",existingStartDate="",existingEndDate="";
	private HashMap<String, String> contentAttribute;
	private String categoryMapping = "",viewMapping="",userGroupMapping="",generalXMLTemplate="",generalXMLTemplateNew="",generalContentXMLEndTag="",channelName="",defaultView="";
	private Properties propForCategory = new Properties(),propForView = new Properties(),propForUserGroup = new Properties();
	private List<String> securityTagList= new ArrayList<String>();

	/**
	 * Constructor
	 */
	public ContentReader(String generalXMLTemplate, String generalContentXMLEndTag, String channelName,String defaultView,String parserFileLocation,
			 String parserMasterAttFileName,
			String parserExportFileLocation) {
		this.generalContentXMLEndTag=generalContentXMLEndTag;
		this.channelName = channelName;
		this.generalXMLTemplate = generalXMLTemplate;
		this.defaultView = defaultView;
		this.parserMasterAttFileName = parserMasterAttFileName;
		this.parserExportFileLocation = parserExportFileLocation;
		try {
			// properties file category_mapping
			InputStream inObjCatMapping =getClass().getResourceAsStream("/"+ getClass().getPackage().getName().replace('.', '/')+ "/category_mapping.properties"); 
				//getClass().getResourceAsStream("/category_mapping.properties");
			propForCategory.load(inObjCatMapping);

			// properties file view_mapping.properties
			InputStream inObjViewMapping =getClass().getResourceAsStream("/"+ getClass().getPackage().getName().replace('.', '/')+ "/view_mapping.properties"); 
				//getClass().getResourceAsStream("/view_mapping.properties");
			propForView.load(inObjViewMapping);

			// properties file usergroup_mapping.properties
			InputStream inObjUserGroupMapping = getClass().getResourceAsStream("/"+ getClass().getPackage().getName().replace('.', '/')+ "/usergroup_mapping.properties");
			//getClass().getResourceAsStream("/usergroup_mapping.properties");
			propForUserGroup.load(inObjUserGroupMapping);
			
			// Reading okm attributes mapping file
			// getting the OKM data
			okmXml = readOkmAttributeFile();;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("Error in loading the properties file");
		}
		// handling the logic for getting the MAIN NODE 
		try{
			readContentAttribute();
			String multiNodeCheck = contentAttribute.get("MAINNODE");
			if(!"".equals(multiNodeCheck) && multiNodeCheck.toString().indexOf(',') != -1){
				multiNodeName = "<"+multiNodeCheck.substring(0,multiNodeCheck.indexOf(','))+">";
				multiNodeNameNew = "<"+(multiNodeCheck.substring(multiNodeCheck.indexOf(',')+1))+">";
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("Error in loading the properties file");
		}
		processSourceFile(parserFileLocation);
	}

	/*
	 * List all files from a directory and its subdirectories
	 * @param directoryName to be listed
	 */
	
	private void processSourceFile(String directoryName){
		
		File directory = new File(directoryName);
		//get all the files from a directory
		File[] fList = directory.listFiles();
		for (File file : fList){
			if (file.isFile() && (file.toString().endsWith(".xml") || file.toString().endsWith(".XML"))){
				if (!writeFileContent(file.getName(),file.getAbsolutePath(),file.getParent()))
					throw new RuntimeException(
							"Exception occured during creating content of file:::::"
									+ file.getAbsolutePath());
			}
			else if (file.isDirectory()){
				processSourceFile(file.getAbsolutePath());
			}
		} // for loop ending
	}

	/**
	 * 
	 * method is used for write content for given file.
	 * 
	 * @param fileName
	 * @return boolean
	 */
	private boolean writeFileContent(String fileName,String directoryName, String existingAttachmentPath) {
		boolean response = false;
		try {
			if (fileName != null && !"".equals(fileName.trim())) {
				// Handle the category, views and groups here
				String xmlText = readDataFromFile(directoryName);
				
				if(!"".equals(xmlText.trim()) && xmlText != null){
					categoryMapping = processMappingConfiguration(xmlText,propForCategory,"<CATEGORIES>","<CATEGORY>");
					//view_mapping
					viewMapping = processMappingConfiguration(xmlText,propForView,"<VIEWS>","<VIEW>");
					//usergroup_mapping
					userGroupMapping = processMappingConfiguration(xmlText,propForUserGroup,"<SECURITY>","<USERGROUP>");
					
					//Existing Doc ID used for retaining in new doc
					existingDocID = readExistingDocID(xmlText);
					
					//Starting for retaining the older author id and owner ID
					//Existing Author ID used for retaining in new doc
					existingAuthorName = readExistingAuthorName(xmlText);
					
					//Existing Owner ID used for retaining in new doc
					existingOwnerName = readExistingOwnerName(xmlText);
					//Starting for retaining the older author id and owner ID
					
					//Existing start date for document used for retaining in new doc
					existingStartDate = readExistingStartDate(xmlText);
					
					//Existing endt date for document used for retaining in new doc
					existingEndDate = readExistingEndDate(xmlText);

					DataLayer dataLayerObj= new DataLayer(propForUserGroup);
					// Getting to be xml
					xmlText = dataLayerObj.parseExistingXML(xmlText, multiNodeName, multiNodeNameNew, okmXml, contentAttribute,securityTagList);
					response = writeTemporaryXMLFile(fileName,xmlText,existingAttachmentPath);	
					
				}
				response = true;
			}
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e);
			logger.error("Content is not written " + e.getMessage());
			return false;
		}
	}

	/**
	 * 
	 * method is used for read categories,views and user group.
	 * 
	 * @param fileName
	 * @return String
	 */
	private List<String> readCategoryViewGroup(String xmlText,String headerValue, String subHeaderValue) {
		
		List<String> mappingList = new ArrayList<String>();
		String dataToUse="";
		try {
			if(xmlText.indexOf(headerValue) != -1){
				dataToUse= xmlText.substring(xmlText.indexOf(headerValue)+headerValue.length(),xmlText.indexOf(headerValue.replace("<", "</")));
			}			
			
			String valueOfMapping = "";
			
		    int lastIndex = 0,lastIndexForImage=0;
		    
		    while ((lastIndex = dataToUse.indexOf(subHeaderValue, lastIndex)) != -1) {
		    			    	
		        lastIndex += subHeaderValue.length();
		    	
		        lastIndexForImage= dataToUse.indexOf("<REFERENCE_KEY>",lastIndex)+"<REFERENCE_KEY>".length();

		        valueOfMapping = dataToUse.substring(lastIndexForImage,dataToUse.indexOf(("</REFERENCE_KEY>"), lastIndexForImage));
		        
		        // As may reference key contains like <![CDATA[WINDOWS_2008]]> so need to parse from this 
		        // Start tag <![CDATA[ And End tag ]]>
		        if (valueOfMapping.toString().indexOf("<![CDATA[") != -1) {
		        	valueOfMapping = valueOfMapping.substring(valueOfMapping.indexOf("<![CDATA[")+"<![CDATA[".length(), valueOfMapping.indexOf("]]>"));
		        }
		        if (!mappingList.contains(valueOfMapping)) {
		        	mappingList.add(valueOfMapping);
		        }
		    } // end of while

		} catch (Exception e) {
			mappingList = null;
			e.printStackTrace();
		} 
		return mappingList;
	}
	
	/**
	 * 
	 * method is used for read data from given file.
	 * 
	 * @param fileName
	 * @return String
	 */
	private String readDataFromFile(String fileName) {

		BufferedReader br = null;
		StringBuilder builder = new StringBuilder("");
		try {
			//br = new BufferedReader(new FileReader(fileName));
			br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
			String sCurrentLine;
			
			while ((sCurrentLine = br.readLine()) != null) {
				builder.append(sCurrentLine);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return builder.toString();
	}

	/**
	 * 
	 * method is used for read data from Okm attribute mapping configuration file.
	 * 
	 * @param 
	 * @return String
	 */
	private String readOkmAttributeFile() {

		BufferedReader br = null;
		StringBuilder builder = new StringBuilder("");
		try {
			InputStream in =getClass().getResourceAsStream("/"+ getClass().getPackage().getName().replace('.', '/')+ "/okm_attribute_mapping.properties"); 
				//ContentReader.class.getResourceAsStream("/okm_attribute_mapping.properties");
			br = new BufferedReader(new InputStreamReader(in));
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				if(!(sCurrentLine.startsWith("#")) && sCurrentLine != null && !"".equals(sCurrentLine.trim())){
					builder.append(sCurrentLine);
					if(sCurrentLine.contains("SECURITY=\"")){
						securityTagList.add(sCurrentLine.substring(sCurrentLine.indexOf("\">")+2,sCurrentLine.indexOf("</")));
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return builder.toString();
	}
	
	/**
	 * 
	 * method is used for read attributes from master attribute file.
	 * 
	 * @param
	 * @return
	 */
	private HashMap<String, String> readContentAttribute() {
		Properties prop = new Properties();
		try {
			//String path = getClass().getClassLoader().getResource(".").getPath();
		
			prop.load(new FileInputStream(getClass().getClassLoader().getResource(".").getPath()+ getClass().getPackage().getName().replace('.', '/')	+ "/" +parserMasterAttFileName));
			
			contentAttribute = new HashMap<String, String>();
			
			for (Entry<Object, Object> e : prop.entrySet()) {
				contentAttribute.put(e.getKey().toString(),
						prop.getProperty(e.getKey().toString()));
			}
			return contentAttribute;
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * method to get the new category,views and groups for document
	 * 
	 * @param file Name,properties,header value,sub header
	 * @return String
	 */
	private String processMappingConfiguration(String xmlText,Properties prop,String headerValue, String subHeaderValue) {
		String response = "";

		try {
			List<String> mappingConf = readCategoryViewGroup(xmlText,headerValue,subHeaderValue);
			
			StringBuilder builderForMapping = new StringBuilder();
			
			for (String mappingValue : mappingConf) {
				if(prop.getProperty(mappingValue)!= null){
					builderForMapping.append(prop.getProperty(mappingValue));
					builderForMapping.append("+");
				}
			}
			if(builderForMapping.length() > 0){
				response = builderForMapping.toString().substring(0,builderForMapping.lastIndexOf("+"));
			}else {
				response = "";
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return response;
	}

	/**
	 * 
	 * method is used for store xml in FILE 
	 * 
	 * @param String for content XML
	 * @return boolean
	 */	
	private boolean writeTemporaryXMLFile(String fileName,String contentXML, String existingAttachmentPath) {
		boolean response = false;
		try {
			StringBuilder builderCompleteXML = new StringBuilder();
			//Starting for retaining the older author name and owner name
			generalXMLTemplateNew = generalXMLTemplate.replace("<AUTHORUSERNAME></AUTHORUSERNAME>", existingAuthorName).
			replace("<OWNERUSERNAME></OWNERUSERNAME>", existingOwnerName).replace("<STARTTIMESTAMP_MILLIS></STARTTIMESTAMP_MILLIS>", existingStartDate).replace("<ENDTIMESTAMP_MILLIS></ENDTIMESTAMP_MILLIS>", existingEndDate);
			// Ending for retaining the older author name and owner name
			
			// combining xml for xml with view etc and xml with OKM channel schema attributes			
			builderCompleteXML.append(generalXMLTemplateNew).append(handleMultiTags()).append("<"+channelName+">").append(contentXML).append("</"+channelName+">").append(generalContentXMLEndTag).append("<RESOURCEPATH>"+existingAttachmentPath+"\\</RESOURCEPATH>").append(existingDocID);
			//logger.info("Final XML written for file "+fileName + " ---" +builderCompleteXML.toString());
			
			PrintWriter outObj = new PrintWriter(new File(parserExportFileLocation+fileName), "UTF-8");
			//outObj.write("\uFEFF");
			outObj.write(builderCompleteXML.toString());
			outObj.flush();
			outObj.close();			
			
			response = true;
		} catch (Exception ex) {// Catch exception if any
			//System.err.println("Error in writing the file " + ex.getMessage());
			logger.error("Error in writing the file during content extraction " + ex.getMessage());
			response = false;
		}
		return response;

	}
	
	/**
	 * 
	 * method is used for reading DOC ID for existing document
	 * 
	 * @param fileName
	 * @return String
	 */
	private String readExistingDocID(String xmlText) {
		String docID = "";
		try {
			if (xmlText.indexOf("<DOCUMENTID><![CDATA[") != -1) {
				docID = xmlText.substring(xmlText.indexOf("<DOCUMENTID><![CDATA[")+"<DOCUMENTID><![CDATA[".length(), xmlText.indexOf("]]></DOCUMENTID>"));
				
			}else if (xmlText.indexOf("<DOCUMENTID>") != -1) {
				docID = xmlText.substring(xmlText.indexOf("<DOCUMENTID>")+"<DOCUMENTID>".length(),xmlText.indexOf("</DOCUMENTID>"));
			}
			docID = "<DOCUMENTID>"+docID+"</DOCUMENTID>";
			
		} catch (Exception e) {
			docID = "";
			e.printStackTrace();
		} 
		return docID;
	}
	/*/
	 * Starting for retaining the older author id and owner ID
	 */
	/**
	 * 
	 * method is used for reading Author ID for existing document
	 * 
	 * @param fileName
	 * @return String
	 */
	private String readExistingAuthorName(String xmlText) {
		String authorName = "";
		try {
			if (xmlText.indexOf("<AUTHOR><![CDATA[") != -1) {
				authorName = xmlText.substring(xmlText.indexOf("<AUTHOR><![CDATA[")+"<AUTHOR><![CDATA[".length(), xmlText.indexOf("]]></AUTHOR>"));
				
			}else if (xmlText.indexOf("<AUTHOR>") != -1) {
				authorName = xmlText.substring(xmlText.indexOf("<AUTHOR>")+"<AUTHOR>".length(),xmlText.indexOf("</AUTHOR>"));
			}
			authorName = "<AUTHORUSERNAME>"+authorName+"</AUTHORUSERNAME>";
		} catch (Exception e) {
			authorName = "";
			e.printStackTrace();
		} 
		return authorName;
	}
	/**
	 * 
	 * method is used for reading Owner ID for existing document
	 * 
	 * @param fileName
	 * @return String
	 */
	private String readExistingOwnerName(String xmlText) {
		String ownerName = "";
		try {
			if (xmlText.indexOf("<OWNER><![CDATA[") != -1) {
				ownerName = xmlText.substring(xmlText.indexOf("<OWNER><![CDATA[")+"<OWNER><![CDATA[".length(), xmlText.indexOf("]]></OWNER>"));
				
			}else if (xmlText.indexOf("<OWNER>") != -1) {
				ownerName = xmlText.substring(xmlText.indexOf("<OWNER>")+"<OWNER>".length(),xmlText.indexOf("</OWNER>"));
			}
			ownerName = "<OWNERUSERNAME>"+ownerName+"</OWNERUSERNAME>";
		} catch (Exception e) {
			ownerName = "";
			e.printStackTrace();
		} 
		return ownerName;
	}
	/**
	 * 
	 * method is used for reading Start date of existing document
	 * 
	 * @param fileName
	 * @return String
	 */
	private String readExistingStartDate(String xmlText) {
		String startDate = "";
		try {
			if (xmlText.indexOf("<STARTTIMESTAMP_MILLIS><![CDATA[") != -1) {
				startDate = xmlText.substring(xmlText.indexOf("<STARTTIMESTAMP_MILLIS><![CDATA[")+"<STARTTIMESTAMP_MILLIS><![CDATA[".length(), xmlText.indexOf("]]></STARTTIMESTAMP_MILLIS>"));
				
			}else if (xmlText.indexOf("<STARTTIMESTAMP_MILLIS>") != -1) {
				startDate = xmlText.substring(xmlText.indexOf("<STARTTIMESTAMP_MILLIS>")+"<STARTTIMESTAMP_MILLIS>".length(),xmlText.indexOf("</STARTTIMESTAMP_MILLIS>"));
			}
			else if (xmlText.indexOf("<CREATEDATE_MILLIS><![CDATA[") != -1) {
				startDate = xmlText.substring(xmlText.indexOf("<CREATEDATE_MILLIS><![CDATA[")+"<CREATEDATE_MILLIS><![CDATA[".length(),xmlText.indexOf("]]></CREATEDATE_MILLIS>"));
			}
			else if (xmlText.indexOf("<CREATEDATE_MILLIS>") != -1) {
				startDate = xmlText.substring(xmlText.indexOf("<CREATEDATE_MILLIS>")+"<CREATEDATE_MILLIS>".length(),xmlText.indexOf("</CREATEDATE_MILLIS>"));
			}
			startDate = "<STARTTIMESTAMP_MILLIS>"+startDate+"</STARTTIMESTAMP_MILLIS>";
		} catch (Exception e) {
			startDate = "";
			e.printStackTrace();
		} 
		return startDate;
	}

	/**
	 * 
	 * method is used for reading End date of existing document
	 * 
	 * @param fileName
	 * @return String
	 */
	private String readExistingEndDate(String xmlText) {
		String endDate = "";
		try {
			if (xmlText.indexOf("<ENDTIMESTAMP_MILLIS><![CDATA[") != -1) {
				endDate = xmlText.substring(xmlText.indexOf("<ENDTIMESTAMP_MILLIS><![CDATA[")+"<ENDTIMESTAMP_MILLIS><![CDATA[".length(), xmlText.indexOf("]]></ENDTIMESTAMP_MILLIS>"));
				
			}else if (xmlText.indexOf("<ENDTIMESTAMP_MILLIS>") != -1) {
				endDate = xmlText.substring(xmlText.indexOf("<ENDTIMESTAMP_MILLIS>")+"<ENDTIMESTAMP_MILLIS>".length(),xmlText.indexOf("</ENDTIMESTAMP_MILLIS>"));
			}
			endDate = "<ENDTIMESTAMP_MILLIS>"+endDate+"</ENDTIMESTAMP_MILLIS>";
		} catch (Exception e) {
			endDate = "";
			e.printStackTrace();
		} 
		return endDate;
	}
	
	/*/
	 * Ending for retaining the older author id and owner ID
	 */
	
	/**
	 * 
	 * method is used for creating xml part for view,category and usergroup
	 * 
	 * @param fileName
	 * @return String
	 */
	private String handleMultiTags() {
		StringBuffer tempXML = new StringBuffer();
		
		// Adding Tags for View
		if("".equals(viewMapping.trim())){
			viewMapping = defaultView;
		}
		tempXML.append("<VIEWS>");
		StringTokenizer stringTokenizer = new StringTokenizer(viewMapping, "+");
		
		while (stringTokenizer.hasMoreElements()) {
			tempXML.append("<VIEW>").append("<REFERENCE_KEY>")
					.append(stringTokenizer.nextElement().toString())
					.append("</REFERENCE_KEY>").append("<GUID></GUID>")
					.append("</VIEW>");
		}		
		tempXML.append("</VIEWS>");
		
		
		// Adding Tags for Category
		
		if(categoryMapping != null && !"".equals(categoryMapping.trim())){
			tempXML.append("<CATEGORIES>");
			stringTokenizer = new StringTokenizer(categoryMapping, "+");		 
			while (stringTokenizer.hasMoreElements()) {	
				tempXML.append("<CATEGORY>").append("<REFERENCE_KEY>")
						.append(stringTokenizer.nextElement().toString())
						.append("</REFERENCE_KEY>").append("<GUID></GUID>")
						.append("</CATEGORY>");
			}
			tempXML.append("</CATEGORIES>");
		}// end of if

		//Adding Tags User Group
		if(userGroupMapping != null && !"".equals(userGroupMapping.trim())){
			tempXML.append("<SECURITY>");
			stringTokenizer = new StringTokenizer(userGroupMapping, "+");		 
			while (stringTokenizer.hasMoreElements()) {	
				tempXML.append("<USERGROUP>").append("<REFERENCE_KEY>")
						.append(stringTokenizer.nextElement().toString())
						.append("</REFERENCE_KEY>").append("<GUID></GUID>")
						.append("</USERGROUP>");
			}
			tempXML.append("</SECURITY>");
		}
		return tempXML.toString();
	}

} // END OF CLASS

