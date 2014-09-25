package com.smart.content.extractor.html;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

public class ContentExtractor {
	private static Logger logger = Logger.getLogger("ContentExtractor");
	private String parserFileLocation = "", parserMasterAttFileName = "",
			parserExportFileLocation = "", parserExportFailedFileLocation = "",parserRemoteAuth = "";

	// For reading the master_attribute
	private Set<String> sourceContentAttribute = null;
	private HandleMasterAttribute okmHeaderList = null;

	private Map<String, String> contentSourceHeadersMap = null;
	
	// For attribute type handling
	private List<String> sourceAttributeTextField = new ArrayList<String>();
	private List<String> sourceAttributeNodeAttachment = new ArrayList<String>();
	private List<String> attachmentNameList = null;

	/**
	 * Constructor
	 */
	public ContentExtractor(String parserFileLocation,
			String parserMasterAttFileName, String parserExportFileLocation, String parserExportFailedFileLocation,
			String parserRemoteAuth) {
		this.parserFileLocation = parserFileLocation;
		this.parserMasterAttFileName = parserMasterAttFileName;
		this.parserExportFileLocation = parserExportFileLocation;
		this.parserExportFailedFileLocation=parserExportFailedFileLocation;
		this.parserRemoteAuth = parserRemoteAuth;
		try {
			// Reading all the master_attributes and putting the same in
			// LinkedHashSet
			readSourceContentAttribute(parserMasterAttFileName);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Error in reading the master_attribute property file"+e.getMessage());
		}
		
		// Actual content writing job here
		processSourceFile();

	}

	private void processSourceFile() {

		File[] files = getFiles(parserFileLocation);

		for (File file : files) {

			if (file.toString().endsWith(".htm")
					|| file.toString().endsWith(".HTM")
					|| file.toString().endsWith(".html")
					|| file.toString().endsWith(".HTML")) {
/*				System.out.println("Html file is ::: " + file.toString()
						+ " And Dir Name ::::" + file.getParentFile() + " And File Name ::: "+file.getName());
*/				
				logger.info("Html file is ::: "+file.getName());

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
	 * method is used for write content for given file.
	 * 
	 * @param fileName
	 * @return boolean Algorithm:
	 */

	private boolean writeFileContent(String fileAbsolutePath, String fileName) {
		boolean response = false;
		String contentXML = "";
		try {

			if (fileAbsolutePath != null && !"".equals(fileAbsolutePath.trim())) {
				// Actual logic for html content

				String htmlText = readHTMLData(fileAbsolutePath);

				// For HTML headers
				String[] stringArraySourceHeaders = Arrays
						.copyOf(sourceContentAttribute.toArray(),
								sourceContentAttribute.toArray().length,
								String[].class);
				contentSourceHeadersMap = new LinkedHashMap<String, String>();
				// Getting html content for each field and putting into MAP as key pair
				getString(stringArraySourceHeaders, htmlText);				
				

				// Fetching the key value pair for creating xml
				if (contentSourceHeadersMap != null
						&& contentSourceHeadersMap.size() > 0) {
					
					contentXML = getXmlString(contentSourceHeadersMap);
					logger.info("XML after content extraction ::: "+contentXML);
				}
				if (contentXML != null && !"".equals(contentXML.trim())) {
					String fileNamingConvention = fileName.substring(0,fileName.indexOf('.'))+".xml";				
					response  = writeTemporaryXMLFile(parserExportFileLocation+fileNamingConvention,contentXML);
				}
				// writing into log file if we are not able to create the xml for html content
				if(!response){
					copyFailedHTML(fileName);
				}

			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			//System.err.println(e);
			logger.error("Content Extraction => Content is not written in XML" + e.getMessage());
			return false;
		}

	}

	/**
	 * 
	 * method is used for read HTML content from given file.
	 * 
	 * @param fileName
	 * @return String
	 */

	private String readHTMLData(String fileName) {
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
	 * method is used for reading the master_attribute.properties
	 * 
	 * @param
	 * @return String
	 */

	private void readSourceContentAttribute(String properyFileName)
			throws InterruptedException {

		sourceContentAttribute = new LinkedHashSet<String>();

		// TODO Auto-generated method stub
		try {

			okmHeaderList = new HandleMasterAttribute(getRelativePath("RelativePathWithPackage")+ properyFileName);

		} catch (Exception e) {
			logger.error("Error in opening the file "+e.getMessage());
			return;
		}
		try {
			while (true) {
				String headerName = null;
				try {
					headerName = okmHeaderList.getNextLine();
					if (headerName == null) {
						return;
					} else if (headerName.startsWith("#")) {

					}
					else if (headerName.contains("_TYPE")) {
						if(headerName.substring(headerName.indexOf('=')+1).equalsIgnoreCase("TF")){
							sourceAttributeTextField.add(headerName.substring(0,headerName.indexOf('_')));
						} else if(headerName.substring(headerName.indexOf('=')+1).equalsIgnoreCase("Node")){
							sourceAttributeNodeAttachment.add(headerName.substring(0,headerName.indexOf('_')));
						}
					}
					else {
						sourceContentAttribute.add(headerName);
					}
				} catch (Exception e) {
					logger.error("Error processing Header :: "
							+ headerName);
					e.printStackTrace();
				}
			}// While

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in readSourceContentAttribute "+e.getMessage());

		}

	} // end of readSourceContentAttribute
	


	/**
	 * 
	 * method is used for get html content.
	 * 
	 * @param stringArray
	 * @param htmlText
	 */

	private boolean getString(String[] stringArrayHeader, String htmlText) {
		boolean result = true;
		String subString = null;
		// For handling text field type in order to remove html tags from content
		TextParser htmlTextParserObj = new TextParser();

		String acutalHeaderValue = "", actualFieldName = "", nexHeader = "";

		try {
			for (int countOfHeader = 0; countOfHeader < stringArrayHeader.length; countOfHeader++) {

				int lastCount = countOfHeader;

				acutalHeaderValue = stringArrayHeader[countOfHeader]
						.substring(stringArrayHeader[countOfHeader]
								.indexOf('=') + 1); // removing the key from
													// String

				actualFieldName = stringArrayHeader[countOfHeader].substring(0,
						stringArrayHeader[countOfHeader].indexOf('=')); // removing
																		// the
																		// value
																		// from
																		// String

				
				if(sourceAttributeNodeAttachment.contains(actualFieldName)){
					subString = handleAttachment(htmlText);
				}
				else{
				
					if (countOfHeader == stringArrayHeader.length - 1) {
						// To avoid error Checking if header exist in HTML content
						// then only do the substring else set it blank value.
						if (htmlText.contains(acutalHeaderValue)) {
							subString = htmlText.substring(htmlText
									.indexOf(acutalHeaderValue)
									+ acutalHeaderValue.length());
						} else {
							subString = "";
						}
	
					} else {
						int trackOfLastCount = ++lastCount;
						nexHeader = stringArrayHeader[trackOfLastCount]
								.substring(stringArrayHeader[trackOfLastCount]
										.indexOf('=') + 1); // removing the key from
															// String
						subString = getSubString(htmlText, acutalHeaderValue,
								nexHeader);
					}
					
					// checking for text field type of attribute
					if(sourceAttributeTextField.contains(actualFieldName) && subString != null && !"".equals(subString.trim())){
						subString = htmlTextParserObj.htmltoText(writeTexttoFile(
								subString, getRelativePath("RelativePathWithPackage")+"temp.txt")
								.getAbsolutePath());
					}

				}
				// Required if need to add target = Actual header as well
/*				System.out.println("Header Name :::::" + actualFieldName
						+ " target=" + acutalHeaderValue);
				System.out.println("Header Value:::::::::" + subString);
				putContentInMap(actualFieldName + " target="
						+ acutalHeaderValue, subString);				
*/
				//System.out.println("Header Name :::::" + actualFieldName);
				//System.out.println("Header Value:::::::::" + subString);
				//logger.info("Header Name :::::" + actualFieldName);
				//logger.info("Header Value:::::::::" + subString);

				putContentInMap(actualFieldName,subString);				

			} // end for for

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Error in getString data for html header "+e.getMessage());
			result = false;
		}
		return result;
	} // end of getString

	/**
	 * 
	 * method is used for read data from given file.
	 * 
	 * @param htmlText
	 * @param firstString
	 * @param lastString
	 * @return String
	 */

	private String getSubString(String htmlText, String firstString,
			String lastString) {
		if (htmlText != null && !"".equals(htmlText.trim())
				&& firstString != null && !"".equals(firstString.trim())
				&& lastString != null && !"".equals(lastString.trim())
				&& htmlText.contains(firstString)
				&& htmlText.contains(lastString)) {
/*			logger.info("SubString using First Header-- " + firstString
					+ " And Sec Header-- " + lastString);
*/
			return htmlText.substring(htmlText.indexOf(firstString)
					+ firstString.length(), htmlText.indexOf(lastString));
		}

		return "";// if header is not matched in HTML content then setting blank
					// value for the fiel
	}

	/**
	 * 
	 * method is used for store value content attribute in Map.
	 * 
	 * @param attributeName
	 * @param htmlToText
	 */

	private boolean putContentInMap(String attributeName, String htmlToText) {
		boolean result = true;
		try {
			if (attributeName != null && !"".equals(attributeName.trim())
					&& htmlToText != null && !"".equals(htmlToText)) {

/*				logger.info("Final Map Key:::::::: "
						+ attributeName.toUpperCase() + " And Value - "
						+ htmlToText);

				logger.info("---------------------------------------------------------------------");
*/
				contentSourceHeadersMap.put(attributeName.toUpperCase(),
						htmlToText);

			} // end for if
		} catch (Exception e) {
			e.printStackTrace();
			//System.err.println(e);
			logger.error("Error in putting header and data in MAP "+e.getMessage());
			result = false;
		}
		return result;
	} // end of method

	/**
	 * 
	 * method is used for creating xml string for writing in FILE.
	 * 
	 * @param contentMap
	 * @return xmlString
	 */

	private String getXmlString(Map<String, String> contentMap) {
		String response = null;
		try {
			StringBuilder xmlString = new StringBuilder("<").append("CONTENT")
					.append(">");
			Iterator entries = contentMap.entrySet().iterator();
			while (entries.hasNext()) {
				
				Entry thisEntry = (Entry) entries.next();
				Object key = thisEntry.getKey();
				Object value = thisEntry.getValue();
				if(value.toString().startsWith("[")){
					for( String attachmentFileName : attachmentNameList) {
						  xmlString.append("<").append(key).append(">")
						  .append("<").append(key+"_NODE_ATTRIBUTE").append(">")
						  .append("<![CDATA[").append(attachmentFileName)
						  .append("]]>")
						  .append("</").append(key+"_NODE_ATTRIBUTE").append(">")
						  .append("</").append(key).append(">");
					}
				}
				else{
				xmlString.append("<").append(key).append(">").append(
						"<![CDATA[").append(value).append("]]>").append("</")
						.append(key).append(">");
				}
			} // end of while
			


			xmlString.append("</").append("CONTENT").append(">");
			response = xmlString.toString();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Error in creating the contentXML "+e.getMessage());
			response = null;
		}

		return response;
	} // END OF getXML STRING
	
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
			logger.error("Error in writing the file " + ex.getMessage());
			response = false;
		}
		return response;

	}

	/**
	 * 
	 * method is used for write data in file.
	 * 
	 * @param htmlText
	 * @param fileName
	 * @return file 
	 *
	 */ 
	
	   public static File writeTexttoFile(String htmlText, String fileName) { 
		    if(htmlText != null  && !"".equals(htmlText) && fileName != null && !"".equals(fileName.trim())){
		    	 
		        try {  
					File file = new File(fileName);
					FileOutputStream fop = null;
					fop = new FileOutputStream(file, false);
					if (!file.exists()) {						
						file.createNewFile();
					}
					byte[] contentInBytes = htmlText.getBytes();
					fop.write(contentInBytes);
					fop.flush();
					fop.close();
					return file;
		        } catch (Exception e) {  
		            //System.err.println("An exception occurred in writing the html text to file.");
		            logger.error("An exception occurred in writing the html text to file. "+e.getMessage());
		            e.printStackTrace();  
		        }  
		    }
	        return null;
	    }	

	/**
	 * 
	 * method is used for fetching the relative path
	 * 
	 * @param String for path
	 * @return string with path
	 */	
	private String getRelativePath(String requiredPath) {
		String response = "";
    	try {
    		// Building the relative path
    		// properties file in classes folder
    		if(requiredPath.equalsIgnoreCase("RelativePath")){
    			//relative path with out package name
    			response=getClass().getClassLoader().getResource(".").getPath();
    		} else {
    			//relative path with package name
    			response=getClass().getClassLoader().getResource(".").getPath()+ getClass().getPackage().getName().replace('.','/')+ "/";
    		}
    		
    	} catch (Exception ex) {
    		ex.printStackTrace();
    		logger.error("Error in finding the relative path "+ex.getMessage());
    		response="";
        }
    	return response;
	}	

	/**
	 * 
	 * method is used for parsing the attachment from html content
	 * 
	 * @param attributeName
	 * @param htmlToText
	 */

	private String handleAttachment(String htmlText) {
		String result="";
			try {
			if (htmlText != null && !"".equals(htmlText.trim())){
				attachmentNameList = new ArrayList<String>();
				String valueOfHref = "";
			    int lastIndex = 0,lastIndexForAttachment=0;

			    while ((lastIndex = htmlText.indexOf("href=\"", lastIndex)) != -1) {
		       
			        lastIndex += "href=\"".length();

			        lastIndexForAttachment = htmlText.indexOf("\"", lastIndex);
			        
			        valueOfHref = htmlText.substring(lastIndex, lastIndexForAttachment);
			        if(!valueOfHref.startsWith("http") && !valueOfHref.startsWith("www") && !valueOfHref.startsWith("#") && !valueOfHref.startsWith("mailto") && !valueOfHref.endsWith("htm")
			        		&& !valueOfHref.endsWith("html") && !valueOfHref.endsWith("HTM") && !valueOfHref.endsWith("HTML") && !valueOfHref.endsWith("css")){
			        	attachmentNameList.add(valueOfHref);
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
			//System.err.println(e);
		}
		return result;
	} // end of method
	/**
	 * 
	 * method is used for copy the failed html to archieve to
	 * 
	 * 
	 * @param 
	 * @return boolean
	 */

	private boolean copyFailedHTML(String fileName) throws IOException {
		boolean result = true;
		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		try {
				File source = new File(parserFileLocation+fileName); // source for html
				
				File dest = new File(parserExportFailedFileLocation+fileName); // archeive

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
} // END OF CLASS

