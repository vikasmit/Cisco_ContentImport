package com.smart.content.imimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.inquira.imwows.generated.ContentServices;
import com.inquira.imwows.generated.ContentServicesServiceLocator;
import com.inquira.imwows.generated.SecurityServices;
import com.inquira.imwows.generated.SecurityServicesServiceLocator;

/*
 * Issue - Replace the final XML content for image/ attachment . Example ../abc.txt or ../abc.jpg
 * �  Class is responsible for creating the content in InfoManager and also copy the image/attachment at required path
*/
public class ImportContent {
	private Logger logger = Logger.getLogger("ImportContent");
	Properties prop = null;
	private String inputXMLFileLocation = "",failedXMLFileLocation="",
	userName="",
	password="",
	repository="",
	imUrlSecurityService="",
	imUrlContentService="",
	destFilePathForAttachment="",	destFileForImage="",documentCreatedPath="",parserFileLocation="";
	private List<String> attachmentNameList = null,imageNameList=null;
	private String okmAttributeFileName = "";
	private StringBuilder findStrUsedInAttachment = null;
	private boolean DocPublishedFlag = true;
	
	private String AUTH_SOAP_MSG= "<AUTHENTICATE><USER>%s</USER><PASSWORD>%s</PASSWORD><REPOSITORY_REFERENCE_KEY>%s</REPOSITORY_REFERENCE_KEY></AUTHENTICATE>";
	
	public static void main(String[] args) throws Exception {
		ImportContent classObj = new ImportContent();
		boolean response = classObj.loadResource();
		if(response){
			classObj.processXMLFile();
		}
		classObj.logger.info("Terminating and exiting app!!! ");
		classObj.logger.info("===============================ENDING OF CONTENT IMPORT===============================");
				
	} // end of main
	
	private boolean loadResource(){
		boolean response = false;
		
		try {
			// FOR LOG FILE
			ClassLoader classLoader =  
				   Thread.currentThread().getContextClassLoader();  
				PropertyConfigurator.configure(classLoader.getResource("log4j.properties"));  
				
			prop = new Properties();
    		// properties file in classes folder
    		InputStream inObj = getClass().getResourceAsStream("/"+ getClass().getPackage().getName().replace('.', '/')+ "/webservice_resource.properties");
    		prop.load(inObj);
			userName = prop.getProperty("userName");
			password = prop.getProperty("password");
			repository = prop.getProperty("repository");
			imUrlSecurityService = prop.getProperty("imUrlForSecurtiyService");
			imUrlContentService = prop.getProperty("imUrlForContentService");
			DocPublishedFlag = Boolean.valueOf(prop.getProperty("DocPublished"));

			AUTH_SOAP_MSG = String.format(AUTH_SOAP_MSG, userName,password,repository);
			
    		// properties file content_import.properties
			prop = new Properties();
    		InputStream inObjTransformation = getClass().getResourceAsStream("/com/smart/content/transformation/content_import.properties");
    		prop.load(inObjTransformation);

    		inputXMLFileLocation = prop.getProperty("IM_CONTENT_IMPORT_INPUTXMLPATH");
    		failedXMLFileLocation=prop.getProperty("IM_IMPORT_FAILED_XMLPATH");
    		parserFileLocation = prop.getProperty("ACTUAL_PARSER_FILELOCATION");
			destFilePathForAttachment = prop.getProperty("IM_RESOURCE_ROOT_PATH");
			destFileForImage = prop.getProperty("IM_LIBRARY_ROOT_PATH");
    		
    		loadResourceForAttachment();
    		response = true;
    		

    	} catch (IOException ex) {
    		ex.printStackTrace();
    		response = false;
    		logger.error("Error in loading the propery file in ImportContent "+ex.getMessage());
        }		
		return response;
	}
	
	private boolean loadResourceForAttachment(){
		boolean response = false;
		try{
    		// handling the attachment tag search in handleAttachment()
    		prop = new Properties();
    		InputStream okmMapping = getClass().getResourceAsStream("/com/smart/content/transformation/okm_attribute_mapping.properties");
    		prop.load(okmMapping);
			Enumeration enuKeys = prop.keys();
			while (enuKeys.hasMoreElements()) {
				String key = (String) enuKeys.nextElement();
				String value = prop.getProperty(key);
				if (value.indexOf(',') != -1) {
					okmAttributeFileName = value.toUpperCase();
					break;
				}
			}
			findStrUsedInAttachment = new StringBuilder();
			if (okmAttributeFileName != null && !"".equals(okmAttributeFileName.trim())) {				
				findStrUsedInAttachment.append("<").append(okmAttributeFileName.substring(0,okmAttributeFileName.indexOf(','))).append(">").append("<").append(okmAttributeFileName.substring(okmAttributeFileName.indexOf(',')+1)).append("><![CDATA[");
			}else{
				findStrUsedInAttachment.append(""); 
			}
			response = true;
		}
		catch(IOException ex){
    		ex.printStackTrace();
    		response = false;
    		logger.error("Error in loading the okm attribute mapping propery file in ImportContent "+ex.getMessage());
			
		}
		return response;
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
			
				
				String xmlText = readXMLData(fileAbsolutePath);
				//logger.info("XML data for file "+fileName+ " is ::: "+xmlText);
				
				
				// Content creation in IM
				if (xmlText != null && !"".equals(xmlText.trim())) {
					response = createContent(xmlText,fileName);
					if(!response){
						copyFailedXML(fileName);
					}else{
						//Handle Images
						String images = handleImages(xmlText);
						logger.info("Images are  :: "+images);
						// handle Attachment
						String attachments = handleAttachment(xmlText);
						logger.info("Attachments are :: "+attachments);
						copyAttachmentFile(parserFileLocation, attachmentNameList);
						copyImageFile(parserFileLocation, imageNameList);
					}
				}

					
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Content is not written in XML during import in IM " + e.getMessage());
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
				File source = new File(inputXMLFileLocation+fileName); // source for xml
				
				File dest = new File(failedXMLFileLocation+fileName); // archeive

				inputChannel = new FileInputStream(source).getChannel(); 

				outputChannel = new FileOutputStream(dest).getChannel();

				outputChannel
						.transferFrom(inputChannel, 0, inputChannel.size());

			result = true;

		} catch (Exception e) {
			result = false;
			logger.error("Error in copying the failed XML file "+e.getMessage());	
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
	 * method responsible for creating content in InfoManager for particular
	 * channel
	 * 
	 * @param contentXML
	 * @return boolean
	 * 
	 */

	private boolean createContent(String contentXML, String fileName) {

		
		try {
			if (contentXML != null && !"".equals(contentXML.trim())) {
				
				
				// Handle the path for im library folder for all the images

				String finalContentXML = handleFinalInputXml(contentXML).replaceAll("img src=\"","img src=\"/library/"+ destFileForImage.substring(destFileForImage.indexOf("library/")+ "library/".length()));
				logger.info("Final Content XML ::: "+finalContentXML);
				
				// FINAL CODE
				SecurityServices securityService = null;
				SecurityServicesServiceLocator securityServiceLocator = new SecurityServicesServiceLocator();
				URL webserviceURL = new URL(imUrlSecurityService);
				securityService = securityServiceLocator.getSecurityServices(webserviceURL);
				String token=securityService.authenticate(AUTH_SOAP_MSG);
							
				ContentServices contentService = null;
				ContentServicesServiceLocator contentServiceLocator = new ContentServicesServiceLocator();
				
				webserviceURL = new URL(imUrlContentService);
				contentService = (ContentServices) contentServiceLocator.getContentServices(webserviceURL);
				
				String serverResponse=contentService.createContent(token, finalContentXML , DocPublishedFlag);
				
				logger.info("SERVER RESPONSE "+serverResponse);
				
				if (serverResponse.indexOf("<RESOURCEPATH><![CDATA[") != -1) {
					// finding the path created in IM for copy the attachment
					documentCreatedPath = serverResponse.substring(serverResponse.indexOf("<RESOURCEPATH><![CDATA[")+"<RESOURCEPATH><![CDATA[".length(), serverResponse.indexOf("]]></RESOURCEPATH>"));
					String docID = serverResponse.substring(serverResponse.indexOf("<DOCUMENTID><![CDATA[")+"<DOCUMENTID><![CDATA[".length(), serverResponse.indexOf("]]></DOCUMENTID>"));
					logger.info("Content created in Info Manager for file "+fileName +" with Document ID "+docID +" And path is " + documentCreatedPath+ "......");
					return true;
				}
				if (serverResponse.indexOf("<ERRORS><ERROR><CODE>") != -1) {
					logger.error("Content is not created in Info Manager for "+fileName+ " due to ::: " + serverResponse.substring(serverResponse.indexOf("<ERRORS><ERROR><CODE>")+"<ERRORS><ERROR><CODE>".length(), serverResponse.indexOf("</CODE>")));
					return false;
				}

				

			} else {
				throw new RuntimeException("contentXML can not be null::::::");
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception in creating content in InfoManager "
					+ e.getMessage());
		}
		return false;
	}

	/**
	 * 
	 * method is used for copy the attachment to
	 * \webapps\ROOT\resources\sites\BROADCOM\content\staging\ALERT\0 path
	 * 
	 * @param ewr
	 * @return boolean
	 */

	private  boolean copyAttachmentFile(String sourceAtachmentPath,
			List<String> attachmentFilesName) throws IOException {
		boolean result = true;
		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		FileChannel outputChannelLive = null;
		try {
			String livePath = documentCreatedPath.replaceFirst("staging",
					"live").replaceFirst("1.0/", ""); // making path for live
														// directory.

			for (String fileName : attachmentFilesName) {
				try {
					File source = new File(sourceAtachmentPath + fileName); // source
					
					//File source = new File("D:\\SMART\\XML\\content_folder\\" + fileName);// for
																			// attachment

					File dest = new File(destFilePathForAttachment
							+ documentCreatedPath + (fileName.substring(fileName.lastIndexOf('/')+1))); // Staging Assuming path will be /

					inputChannel = new FileInputStream(source).getChannel();

					outputChannel = new FileOutputStream(dest).getChannel();

					outputChannel.transferFrom(inputChannel, 0, inputChannel
							.size());

					// LIVE
					File liveDest = new File(destFilePathForAttachment
							+ livePath + fileName);
					outputChannelLive = new FileOutputStream(liveDest)
							.getChannel();
					outputChannelLive.transferFrom(inputChannel, 0,
							inputChannel.size());
				} catch (Exception e) {
					logger.error("Error in Copying the Attachment at path "
							+ documentCreatedPath + " Reason :: "
							+ e.getMessage());
					continue;
				}

			}
			result = true;

		} catch (Exception e) {
			logger.error("Error in copyAttachmentFile() :: " + e.getMessage());

		} finally {
			if (inputChannel != null) {
				inputChannel.close();
			}
			if (outputChannel != null) {
				outputChannel.close();
			}
			if (outputChannelLive != null) {
				outputChannelLive.close();
			}

		}

		return result;
	}

	/**
	 * 
	 * method is used for copy the Images to $IM/library/image
	 * 
	 * @param ewr
	 * @return boolean
	 */

	private boolean copyImageFile(String sourceImagePath,
			List<String> ImageFilesName) throws IOException {
		boolean result = true;

		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		try {

			for (String fileName : ImageFilesName) {
				try {

					File source = new File(sourceImagePath + fileName); // source
																		// for
																		// image
					
					File dest = new File(destFileForImage + (fileName.substring(fileName.lastIndexOf('/')+1))); // for
																		// $IM/library/image

					inputChannel = new FileInputStream(source).getChannel();

					outputChannel = new FileOutputStream(dest).getChannel();

					outputChannel.transferFrom(inputChannel, 0, inputChannel
							.size());
					
				} catch (Exception e) {
					logger.error("Error in Copying the Image :: "
							+ e.getMessage());
					continue;
				}
			}
			result = true;

		} catch (Exception e) {
			logger.error("Error in copyImageFile :: " + e.getMessage());
			result = false;
		} finally {
			if (inputChannel != null) {
				inputChannel.close();
			}
			if (outputChannel != null) {
				outputChannel.close();
			}
		}
		return result;
	}

	/**
	 * 
	 * method is used for parsing the image from content
	 * 
	 * @param xmlText
	 * @return String with images name
	 */

	private String handleImages(String xmlText) {
		String result="";
			try {
			if (xmlText != null && !"".equals(xmlText.trim())){
				imageNameList = new ArrayList<String>();
				String valueOfIMG = "";
			    int lastIndex = 0,lastIndexForImage=0;
			    
			    
			    while ((lastIndex = xmlText.indexOf("<img src=\"", lastIndex)) != -1) {
		       
			        lastIndex += "<img src=\"".length();

			        lastIndexForImage = xmlText.indexOf("\"", lastIndex);
			        
			        valueOfIMG = xmlText.substring(lastIndex, lastIndexForImage);
			        
			        if (!imageNameList.contains(valueOfIMG) && (valueOfIMG.endsWith(".png")
							|| valueOfIMG.endsWith(".jpg")
							|| valueOfIMG.endsWith(".gif"))) {
			        		imageNameList.add(valueOfIMG);
			        }
			        
			    }
			    result = imageNameList.toString();
			    if(imageNameList != null && imageNameList.size() > 0){
			    	result = imageNameList.toString();
				} else{
					result="";
				}
				

			} // end for if
		} catch (Exception e) {
			result ="";
			e.printStackTrace();
			logger.error("Error in parsing the Images "+e.getMessage());
		}
		return result;
	} // end of method	
	
	/**
	 * 
	 * method is used for parsing the attachment from content
	 * 
	 * @param xmlText
	 */

	private String handleAttachment(String xmlText) {
		String result="";
			try {
			attachmentNameList = new ArrayList<String>();	
			if (xmlText != null && !"".equals(xmlText.trim()) && findStrUsedInAttachment.length()>0){				
				String nameOfFile = "";
			    int lastIndex = 0,lastIndexForAttachment=0;
			    while ((lastIndex = xmlText.indexOf(findStrUsedInAttachment.toString(), lastIndex)) != -1) {
			        lastIndex += findStrUsedInAttachment.toString().length();
			        lastIndexForAttachment = xmlText.indexOf("]]>", lastIndex);
			        nameOfFile = xmlText.substring(lastIndex, lastIndexForAttachment);
			        if(!attachmentNameList.contains(nameOfFile)){
			        	attachmentNameList.add(nameOfFile);
			        }
			    }
			    result = attachmentNameList.toString();
			    if(attachmentNameList != null && attachmentNameList.size() > 0){
			    	result = attachmentNameList.toString();
				} else{
					result="";
				}
				

			} // end for if
		} catch (Exception e) {
			result ="";
			e.printStackTrace();
			logger.error("Error in parsing the attachment "+e.getMessage());
		}
		return result;
	} // end of method

	/**
	 * 
	 * method is used for handling the final input xml for image and attachment path
	 * 
	 * @param xmlText
	 * @return final string
	 */

	private String handleFinalInputXml(String xmlText) {
		try {
			HashMap<String, String> hmap = new HashMap<String, String>();
			
			hmap.put("<img src=\"", "\"");
			
			hmap.put(findStrUsedInAttachment.toString(), "]]");

			for (String key : hmap.keySet()) {
				if(key != null && !"".equals(key.trim()))
				xmlText = replaceText(xmlText, key, hmap.get(key));
			}
			
			return xmlText;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in handling the final input xml for Images/Attachment Path "+e.getMessage());
		}
		return "";
	} // end of method
	
	private String replaceText(String htmlText, String startText,
			String endText) {
		String trimString = "";
		try {
			if (htmlText != null && !"".equals(htmlText.trim())) {

				String replacementStr = "";
				int lastIndex = 0, lastIndexforText = 0;

				while ((lastIndex = htmlText.indexOf(startText, lastIndex)) != -1) {

					lastIndex += startText.length();
					lastIndexforText = htmlText.indexOf(endText, lastIndex);
					replacementStr = htmlText.substring(lastIndex,
							lastIndexforText);
					
					trimString = replacementStr.substring(replacementStr.lastIndexOf("/")+1);
					htmlText   = htmlText.replace(replacementStr, trimString);
				}
			} 
			return htmlText;
		} catch (Exception e) {
			trimString="";
			e.printStackTrace();
			logger.error("Error in replacing the text for final input xml for Images/Attachment Path "+e.getMessage());
		}
		return trimString;
	}
	
	
} // end of class

