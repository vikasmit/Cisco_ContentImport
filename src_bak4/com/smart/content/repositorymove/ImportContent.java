package com.smart.content.repositorymove;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.inquira.imwows.generated.ContentServices;
import com.inquira.imwows.generated.ContentServicesServiceLocator;
import com.inquira.imwows.generated.SecurityServices;
import com.inquira.imwows.generated.SecurityServicesServiceLocator;
/**
 * This class is the execution point to read the xml file and create the content in Info Manager and also copy the image/attachment at required path
 * 
 * @author Infogain
 * 
 */

public class ImportContent {
	private Logger logger = Logger.getLogger("ImportContent");
	Properties prop = null;
	private String inputXMLFileLocation = "",
	userName="",
	password="",
	repository="",
	imUrlSecurityService="",
	imUrlContentService="",
	destFilePathForImage="",documentCreatedPath="",pathForExistingImages="",finalViewName="",recordIDForNewDoc="",docIDForNewDoc="",newDestPathForImage="";
	private List<String> imageNameList=null,hrefList=null,hrefListTrack=null;
	private boolean DocPublishedFlag = true;
	
	private String AUTH_SOAP_MSG= "<AUTHENTICATE><USER>%s</USER><PASSWORD>%s</PASSWORD><REPOSITORY_REFERENCE_KEY>%s</REPOSITORY_REFERENCE_KEY></AUTHENTICATE>";
	RetainDocID listDAO=null;
	public static void main(String[] args) throws Exception {
		ImportContent classObj = new ImportContent();
		
		boolean response = classObj.loadResource();
		if(response){
			classObj.processXMLFile();
		}
		// closing the db connection used updating the doc ID
		if (RetainDocID.connection != null)
			RetainDocID.connection.close();

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
    		//InputStream inObj = getClass().getResourceAsStream("/webservice_resource.properties");
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
    		InputStream inObjTransformation =getClass().getResourceAsStream("/"+ getClass().getPackage().getName().replace('.', '/')+ "/parser_configuration.properties"); 
    			//getClass().getResourceAsStream("/parser_configuration.properties");
    		prop.load(inObjTransformation);

    		inputXMLFileLocation = prop.getProperty("XML_EXPORT_FILE_LOCATION");
    		pathForExistingImages = prop.getProperty("IM_LIBRARY_EXISTING_ROOT_PATH");
    		destFilePathForImage = prop.getProperty("IM_LIBRARY_ROOT_PATH");
    		
    		// Handling the class db connection for doc id update
    		listDAO = new RetainDocID();
    		response = true;
    		

    	} catch (IOException ex) {
    		ex.printStackTrace();
    		response = false;
    		logger.error("Error in loading the propery file in ImportContent "+ex.getMessage());
        }		
		return response;
	}
	
	private void processXMLFile() {

		File[] files = getFiles(inputXMLFileLocation);

		for (File file : files) {

			if (file.toString().endsWith(".xml")
					|| file.toString().endsWith(".XML")) {
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
		String oldDocIDRetain = "";
		try {

			if (fileAbsolutePath != null && !"".equals(fileAbsolutePath.trim())) {
			
				String xmlText = readXMLData(fileAbsolutePath);
				// Getting the view name
				finalViewName = readViewName(xmlText);
				newDestPathForImage = handlePathForView();
				
				// Content creation in IM
				if (xmlText != null && !"".equals(xmlText.trim())) {
					xmlText=replaceAuthorOwner(xmlText);
					response = createContent(xmlText,fileName);
					if(!response){
						//copyFailedXML(fileName);
					}else{
						// Not copying the attachment here now moved to next execution
						// copy images as well as attachment in href 
						copyFiles(pathForExistingImages, imageNameList);
						
						// Retaining the old doc id
						oldDocIDRetain = readDocID(xmlText);

						if (recordIDForNewDoc != null && !"".equals(recordIDForNewDoc) && docIDForNewDoc != null && !"".equals(docIDForNewDoc) && oldDocIDRetain != null && !"".equals(oldDocIDRetain)) {
							String docIDUpdatedStatus = listDAO.execute(recordIDForNewDoc,docIDForNewDoc,oldDocIDRetain);
							logger.info("Document ID "+oldDocIDRetain + " retained status is "+docIDUpdatedStatus);
							//if(docIDUpdatedStatus.equalsIgnoreCase("success") && isAttachment){
							if(docIDUpdatedStatus.equalsIgnoreCase("success")){
								// writing the file for folder structure handling in 3 execution
								logFileWrite(documentCreatedPath,oldDocIDRetain,readDocPath(xmlText));
							}
						}else{
							logger.info("Document ID retained is not done. Please see which is blank value for RecordID "+recordIDForNewDoc + " NewDocID "+docIDForNewDoc + " RetainDocID "+oldDocIDRetain);
						}

					}

					logger.info("\n"+"============================= End of Doc ID "+oldDocIDRetain + " for input file:: "+fileAbsolutePath +" ================================="+"\n");
					
				}

					
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Content migration is not successfully completed " + e.getMessage());
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
			bufferedReaderObj = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
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
	 * method responsible for creating content in InfoManager for particular
	 * channel
	 * 
	 * @param contentXML
	 * @return boolean
	 * 
	 */

	private boolean createContent(String contentXML, String fileName) {
		String  finalContentXML = ""; 
		
		try {
			if (contentXML != null && !"".equals(contentXML.trim())) {
				
				// Handle the path for im library folder for all the images
				finalContentXML = replaceImagePath(contentXML.substring(0,contentXML.indexOf("</CONTENT>")+("</CONTENT>").length()));
				// handle the path for attachment in href
				finalContentXML = replaceHrefPath(finalContentXML);
				//logger.info("Final Content XML ::: "+finalContentXML);

				
				SecurityServices securityService = null;
				SecurityServicesServiceLocator securityServiceLocator = new SecurityServicesServiceLocator();
				URL webserviceURL = new URL(imUrlSecurityService);
				securityService = securityServiceLocator.getSecurityServices(webserviceURL);
				String token=securityService.authenticate(AUTH_SOAP_MSG);
							
				ContentServices contentService = null;
				ContentServicesServiceLocator contentServiceLocator = new ContentServicesServiceLocator();
				
				webserviceURL = new URL(imUrlContentService);
				contentService = (ContentServices) contentServiceLocator.getContentServices(webserviceURL);
				
				String serverResponse=contentService.createContent(token, removeEmptyNodes(finalContentXML) , DocPublishedFlag);
				
				//logger.info("SERVER RESPONSE "+serverResponse);
				
				if (serverResponse.indexOf("<RESOURCEPATH><![CDATA[") != -1) {
					// finding the path created in IM for copy the attachment
					documentCreatedPath = serverResponse.substring(serverResponse.indexOf("<RESOURCEPATH><![CDATA[")+"<RESOURCEPATH><![CDATA[".length(), serverResponse.indexOf("]]></RESOURCEPATH>"));
					docIDForNewDoc = serverResponse.substring(serverResponse.indexOf("<DOCUMENTID><![CDATA[")+"<DOCUMENTID><![CDATA[".length(), serverResponse.indexOf("]]></DOCUMENTID>"));
					recordIDForNewDoc = serverResponse.substring(serverResponse.indexOf("<CONTENT RECORDID=\"")+"<CONTENT RECORDID=\"".length(), serverResponse.indexOf("\">"));
					logger.info("Content created in Info Manager for file "+fileName +" with New Document ID "+docIDForNewDoc + " having Record ID = "+recordIDForNewDoc +" And path is " + documentCreatedPath+ "......");
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
	 * method is used for copy the Images to $IM/library/image and attachmeent
	 * 
	 * @param ewr
	 * @return boolean
	 */

	private boolean copyFiles(String sourceImagePath,
			List<String> ImageFilesName) throws IOException {
		boolean result = false;

		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		File source=null,dest=null;
		String actualImageName="";
		try {			
			ImageFilesName.addAll(hrefList);
			for (String fileName : ImageFilesName) {
				try {
					 	source = new File(sourceImagePath + fileName);
					 	
						// fetching the image name from relative path of file name
					 	if(fileName.indexOf('/') != -1){
					 		actualImageName = (fileName.substring(fileName.lastIndexOf('/')+1));
					 	}else{
					 		actualImageName=fileName;
					 	}
					 	// ending for fetching the image name from relative path of file name
					 	// destination path with repository and adding viewname dir and then image name
						dest = new File(newDestPathForImage + actualImageName);

						inputChannel = new FileInputStream(source).getChannel();

						outputChannel = new FileOutputStream(dest).getChannel();

						outputChannel.transferFrom(inputChannel, 0, inputChannel
								.size());
					
				} catch (Exception e) {
					logger.error("Error in Copying the Image/File :: "
							+ e.getMessage());
					continue;
				}
			}
			result = true;

		} catch (Exception e) {
			logger.error("Error in copyFiles for image or file :: " + e.getMessage());
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
	 * method is used for parsing the href from content
	 * 
	 * @param xmlText
	 * @return final string
	 */
		
	private String replaceHrefPath(String htmlText) {
		String startText = " href=\"", endText = "\"",valueOfHref="",valueOfHrefWithOutSpace="";
		String splitFileName="",relativePathNewRep="";
		hrefList = new ArrayList<String>();
		hrefListTrack = new ArrayList<String>();
		try {
			if (htmlText != null && !"".equals(htmlText.trim())) {

				int lastIndex = 0, lastIndexforText = 0;
				if (newDestPathForImage.indexOf("library") != -1) {
					relativePathNewRep = (newDestPathForImage.substring(newDestPathForImage.indexOf("library")));
				}else{
					relativePathNewRep="";
				}

				while ((lastIndex = htmlText.indexOf(startText, lastIndex)) != -1) {
					lastIndex += startText.length();
					lastIndexforText = htmlText.indexOf(endText, lastIndex);
					valueOfHref = htmlText.substring(lastIndex,lastIndexforText);
					valueOfHrefWithOutSpace = valueOfHref.replaceAll("%20", " "); // removing the space if any
					// Adding file name with path in list
			        if (!hrefList.contains(valueOfHrefWithOutSpace) && valueOfHrefWithOutSpace.startsWith("/library")) {
			        	hrefList.add(valueOfHrefWithOutSpace);
						// idea absolute path + view name + image name need to be replaced in actual content
				        splitFileName = (valueOfHrefWithOutSpace.substring(valueOfHrefWithOutSpace.lastIndexOf('/')+1));
				        if (!hrefListTrack.contains(relativePathNewRep+splitFileName)) {
							htmlText   = htmlText.replace(valueOfHref.substring(valueOfHref.indexOf('/')+1), relativePathNewRep+splitFileName);
							hrefListTrack.add(relativePathNewRep+splitFileName);
				        }						
			        }
				}
				//logger.info("HREF files are  :: "+hrefList.toString());
			} 
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in replacing the text for final input xml for href Attachment Path "+e.getMessage());
		}
		return htmlText;
	}

	/**
	 * 
	 * method is used for handling the final input xml path for all the images
	 * 
	 * @param xmlText
	 * @return final string
	 */
		
	private String replaceImagePath(String htmlText) {
		String startText = " src=\"", endText = "\"",valueOfImage = "",valueOfImageWithOutSpace="";
		String splitImageName="",relativePathNewRep="";
		imageNameList = new ArrayList<String>();
		try {
			if (htmlText != null && !"".equals(htmlText.trim())) {

				int lastIndex = 0, lastIndexforText = 0;
				if (newDestPathForImage.indexOf("/library") != -1) {
					relativePathNewRep = (newDestPathForImage.substring(newDestPathForImage.indexOf("/library")));
				}
				else{
					relativePathNewRep="";
				}				
				while ((lastIndex = htmlText.indexOf(startText, lastIndex)) != -1) {
					lastIndex += startText.length();
					lastIndexforText = htmlText.indexOf(endText, lastIndex);
					valueOfImage = htmlText.substring(lastIndex,lastIndexforText);
					valueOfImageWithOutSpace =valueOfImage.replaceAll("%20", " "); // removing the space if any
					// Adding image name with path in list
			        if (!imageNameList.contains(valueOfImageWithOutSpace) && (valueOfImage.endsWith(".png")
							|| valueOfImage.endsWith(".jpg") || valueOfImage.endsWith(".jpeg")
							|| valueOfImage.endsWith(".gif") || valueOfImage.endsWith(".PNG") || valueOfImage.endsWith(".JPG") || valueOfImage.endsWith(".GIF") || valueOfImage.endsWith(".JPEG"))) {
			        		  
			        		imageNameList.add(valueOfImageWithOutSpace);
							// idea absolute path + view name + image name need to be replaced in actual content
							splitImageName = (valueOfImageWithOutSpace.substring(valueOfImageWithOutSpace.lastIndexOf('/')+1));
							htmlText   = htmlText.replace(valueOfImage, relativePathNewRep+splitImageName);
			        }
				}
				//logger.info("Images are  :: "+imageNameList.toString());
			} 
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in replacing the text for final input xml for Images Path "+e.getMessage());
		}
		return htmlText;
	}
	
	/**
	 * 
	 * method is used for reading view
	 * 
	 * @param fileName
	 * @return String
	 */
	private String readViewName(String xmlText) {

		String viewName = "";
		try {
			if(xmlText.indexOf("<VIEWS>") != -1){
				String dataToUse= xmlText.substring(xmlText.indexOf("<VIEWS>")+"<VIEWS>".length(),xmlText.indexOf("</VIEWS>"));
				viewName = dataToUse.substring(dataToUse.indexOf("<REFERENCE_KEY>")+"<REFERENCE_KEY>".length(),dataToUse.indexOf("</REFERENCE_KEY>"));
			}
		} catch (Exception e) {
			viewName = "";
			e.printStackTrace();
		} 
		return viewName;
	}
	/**
	 * 
	 * method is used for reading path for existing document in order to get the path for attachment
	 * 
	 * @param fileName
	 * @return String
	 */
	private String readDocPath(String xmlText) {
		String docPath = "";
		try {
			if(xmlText.indexOf("<RESOURCEPATH>") != -1){
				docPath = xmlText.substring(xmlText.indexOf("<RESOURCEPATH>")+"<RESOURCEPATH>".length(),xmlText.indexOf("</RESOURCEPATH>"));
			}
			
		} catch (Exception e) {
			docPath = "";
			e.printStackTrace();
		} 
		return docPath;
	}
	/**
	 * 
	 * method is used for reading DOC ID for existing document
	 * 
	 * @param fileName
	 * @return String
	 */
	private String readDocID(String xmlText) {
		String docID = "";
		try {
			if(xmlText.indexOf("<DOCUMENTID>") != -1){
				docID = xmlText.substring(xmlText.indexOf("<DOCUMENTID>")+"<DOCUMENTID>".length(),xmlText.indexOf("</DOCUMENTID>"));
			}
			
		} catch (Exception e) {
			docID = "";
			e.printStackTrace();
		} 
		return docID;
	}
	
	/**
	 * 
	 * method is used for reading User ID for author name/Owner Name
	 * 
	 * @param fileName
	 * @return String
	 */
	private String readUserLogin(String fullName) {
		String firstName="",lastName="",userLogin="";
		try {
			if (fullName.indexOf(' ') != -1) {
				firstName = fullName.substring(0, fullName.indexOf(' '));
				lastName = fullName.substring(fullName.lastIndexOf(' ')+1);
			}
			userLogin = listDAO.executeForUserID(firstName,lastName);
			if ("".equals(userLogin.trim())) {
				logger.info("User Login does not exist in IM for "+fullName);
			}
		} catch (Exception e) {
			userLogin = "";
			e.printStackTrace();
		} 
		return userLogin;
	}
	
	/**
	 * 
	 * method is used for handling the final input xml path for author/owner Tag
	 * 
	 * @param xmlText
	 * @return final string
	 */
		
	private String replaceAuthorOwner(String htmlText) {
		String authorName="",ownerName="";
		try {
			// handling authorName
			if(htmlText.indexOf("<AUTHORUSERNAME>") != -1){
				authorName = htmlText.substring(htmlText.indexOf("<AUTHORUSERNAME>")+"<AUTHORUSERNAME>".length(),htmlText.indexOf("</AUTHORUSERNAME>"));
			}
			htmlText = htmlText.replace("<AUTHORUSERNAME>"+authorName+"</AUTHORUSERNAME>","<AUTHORUSERNAME>"+readUserLogin(authorName)+"</AUTHORUSERNAME>");
			
			// handling ownerName
			if(htmlText.indexOf("<OWNERUSERNAME>") != -1){
				ownerName = htmlText.substring(htmlText.indexOf("<OWNERUSERNAME>")+"<OWNERUSERNAME>".length(),htmlText.indexOf("</OWNERUSERNAME>"));
			}
			htmlText = htmlText.replace("<OWNERUSERNAME>"+ownerName+"</OWNERUSERNAME>","<OWNERUSERNAME>"+readUserLogin(ownerName)+"</OWNERUSERNAME>");

			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in replacing the text for final input xml for Author/Owner User ID "+e.getMessage());
		}
		return htmlText;
	}
	
	/**
	 * 
	 * method is used for finding the absoulute path for views
	 * 
	 * @param 
	 * @return
	 */
	private String handlePathForView() {
		String destDirName = "";
		try {
			File sourceLocation = new File(destFilePathForImage);
			if (sourceLocation.isDirectory()) {
				// only for directory
				List<File> dirList = (List<File>) FileUtils.listFilesAndDirs(sourceLocation, new NotFileFilter(TrueFileFilter.INSTANCE), DirectoryFileFilter.DIRECTORY);
				
				for (File dir : dirList) {
					if(dir.getName().equalsIgnoreCase(finalViewName)){
						destDirName = (dir.getAbsolutePath()+"\\").replace('\\', '/');
						break;
					}
				}
				
			} // end of if
			// Alternate
			if ("".equals(destDirName)) {
				destDirName = destFilePathForImage+finalViewName+"/";
				//Creating view Name dir in the system if not exists
				try {
					File viewDirName = new File(destDirName);
					if(!viewDirName.exists()){
						viewDirName.mkdir();
					}
				} 
				catch (SecurityException Se) {
						System.out.println("Error while creating directory in for Copy files:" + Se.getMessage());
				}
				
			}


		} catch (Exception ex) {
			destDirName = "";
			logger.error("Error in finding the Dir " + ex.getMessage());
		}
		
		return destDirName;
	}
	
	/**
	 * 
	 * method is used for writing the file which will be input for attachment move after rebuild of xml
	 * 
	 * @param string
	 * @return Lsit
	 */
	
	private void logFileWrite(String path,String retainedDocID, String actualPath) {
		try {
			if (actualPath != null && !"".equals(actualPath) && path != null && !"".equals(path)) {
				// Create file
				FileWriter fstream = new FileWriter("src/attachmentMoveTrack.txt", true); // file
				BufferedWriter out = new BufferedWriter(fstream);
				out.newLine();
				out.write(path+","+retainedDocID+","+actualPath);
				// Close the output stream
				out.close();
			}
		} catch (Exception ex) {// Catch exception if any
			System.err.println("Error in writing the file " + ex.getMessage());
		}

	} // end of method

	private String removeEmptyNodes(String xml){
		String response=xml;
		try {
			
			// converting into doc type
			Document doc = convertStringToDocument(xml);
			// Removing the nodes which has attributes AND blank data
			NodeList nodeList = doc.getElementsByTagName("*");
			for (int i = 0; i < nodeList.getLength(); i++) {
		        Node currentNode = nodeList.item(i);
		        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
		            //System.out.println(currentNode.getNodeName());
			        if(currentNode.hasAttributes() && "".equalsIgnoreCase(currentNode.getTextContent())){
			        	currentNode.getParentNode().removeChild(currentNode);
			        }
		        }
		    }
			// Converting the doc to String 
			response = convertDocumentToString(doc);
			
		}
		catch(Exception e){
			logger.info("Error while cleaning the empty tag"+e.getMessage());
		}
		return response;
	}
    private Document convertStringToDocument(String xmlStr) {
    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory
        .newInstance();
      DocumentBuilder dBuilder=null;
      Document newDocument=null;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			newDocument = dBuilder.parse(new ByteArrayInputStream(xmlStr.getBytes("UTF-8")));
			return newDocument;
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    
		return null;
    }	
	 private String convertDocumentToString(Document doc) {
	        TransformerFactory tf = TransformerFactory.newInstance();
	        Transformer transformer;
	        try {
	            transformer = tf.newTransformer();
	            // below code to remove XML declaration
	            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	            StringWriter writer = new StringWriter();
	            transformer.transform(new DOMSource(doc), new StreamResult(writer));
	            String output = writer.getBuffer().toString();
	            return output;
	        } catch (TransformerException e) {
	            e.printStackTrace();
	        }
	         
	        return null;
	    }

} // end of class