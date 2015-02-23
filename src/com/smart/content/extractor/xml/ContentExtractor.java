package com.smart.content.extractor.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ContentExtractor {
	private static Logger logger = Logger.getLogger("ContentExtractor");
	private String parserFileLocation = "", parserFailLocation = "",
			parserMasterAttFileName = "", parserExportFileLocation = "",
			parserRemoteAuth = "", parserMultipleContentNode = "";
	private HashMap<String, String> contentAttribute, tempMap;
	private StringBuffer multipleTagsContainer = null;
	/**
	 * Constructor
	 */
	public ContentExtractor(String parserFileLocation,
			 String parserMasterAttFileName,
			String parserExportFileLocation,String parserFailLocation, String parserRemoteAuth) {
		this.parserFileLocation = parserFileLocation;
		this.parserFailLocation = parserFailLocation;
		this.parserMasterAttFileName = parserMasterAttFileName;
		this.parserExportFileLocation = parserExportFileLocation;
		this.parserRemoteAuth = parserRemoteAuth;
		
		try {
			// properties file content_import.properties
			Properties prop = new Properties();
			InputStream inObjTransformation = getClass().getResourceAsStream("/com/smart/content/extractor/xml/xml_multipleTag.properties");
			prop.load(inObjTransformation);
			parserMultipleContentNode = prop.getProperty("XML_MULTIPLE_CONTENT_NODE");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("Error in loading the xml_multipleTag.properties");
		}

		
		processSourceFile();
	}

	/**
	 * 
	 * 
	 * @param
	 * @return
	 */

	public void processSourceFile() {

		File[] files = getFiles(parserFileLocation);

		for (File file : files) {

			if (file.toString().endsWith(".htm")
					|| file.toString().endsWith(".HTM")
					|| file.toString().endsWith(".html")
					|| file.toString().endsWith(".HTML")
					|| file.toString().endsWith(".xml")
					|| file.toString().endsWith(".XML")) {
				// System.out.println("Filename is::: " + file.toString()
				// + " And Dir Name ::::" + file.getParentFile());
				if (!writeFileContent(file.getName())) {
					try {
						copyFileIfFailed(file, new File(parserFailLocation
								+ file.getName()));
					} catch (IOException ie) {
						throw new RuntimeException(
								"Error while copying failed file:"
										+ file.getName());
					}
				}
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
			System.err.println(e);
			logger.error("File Not Found " + e.getMessage());
		}
		return null;
	}

	/**
	 * 
	 * method is used for write content for given file.
	 * 
	 * @param fileName
	 * @return boolean
	 */
	private boolean writeFileContent(String fileName) {
		boolean response = false;
		try {
			if (fileName != null && !"".equals(fileName.trim())) {
				String xmlText = readDataFromFile(parserFileLocation + fileName);
				response = parseAndWriteXML(readContentAttribute(),
						fileName);
				return response;
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
	 * method is used for read data from given file.
	 * 
	 * @param fileName
	 * @return String
	 */
	public String readDataFromFile(String fileName) {
		// System.out.println("reading file :::::" + fileName);
		BufferedReader br = null;
		StringBuilder builder = new StringBuilder("");
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(fileName));
			
			while ((sCurrentLine = br.readLine()) != null) {
				builder.append(sCurrentLine);
			}
			// System.out.println("end of reading file::::::::::");
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
	public HashMap<String, String> readContentAttribute() {
		Properties prop = new Properties();
		try {
			/*prop.load(new FileInputStream(getRelativePath("RelativePathWithPackage")
					+ parserMasterAttFileName));
			*/
			File file = new File("C:\\SmartToolConfig\\master_attribute.properties");

			FileReader reader = new FileReader(file) ; 
			prop.load(reader);
			
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
	 * method is used for parse content for given file.
	 * 
	 * @param
	 * @return
	 */
	public boolean parseAndWriteXML(HashMap<String, String> attributeMap,
			String fileName) throws Exception {
		boolean response = false;
		boolean multiNode = false;
		try {
			tempMap = new HashMap<String, String>();
			
			FileInputStream file = new FileInputStream(parserFileLocation
					+ fileName);
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			
			Document xmlDocument = builder.parse(file);
			
			XPath xPath = XPathFactory.newInstance().newXPath();
			XPathExpression expr = xPath.compile("count("+ parserMultipleContentNode + ")");
			Object result = expr.evaluate(xmlDocument, XPathConstants.NUMBER);
			int count = ((Double) result).intValue();

			if (count == 0) {
				System.out.println("Source file does not contain the recurring node mentioned in parser configuration file");
				return false;
			}
			if (count > 1) {
				multiNode = true;
			}
			
			for (int i = 1; i <= count; i++) {
				for (String attKey : attributeMap.keySet()) {
					
					if (!attKey.contains("_TYPE")) {

						String expression = "(" + parserMultipleContentNode
								+ ")[" + i + "]" + attributeMap.get(attKey);


						NodeList nodeList = (NodeList) xPath
								.compile(expression).evaluate(xmlDocument,
										XPathConstants.NODESET);
						
						//System.out.println("Length of node "+nodeList.getLength());
						
						Node node = (Node) xPath.compile(expression).evaluate(
								xmlDocument, XPathConstants.NODE);
						
						if (null != node) {
							
							// for attachment - Rakesh commented this line
							//nodeList = node.getChildNodes();
							
							if (attributeMap.containsKey((attKey + "_TYPE"))
									&& attributeMap.get(attKey + "_TYPE").equalsIgnoreCase(ContentConstant.FIELD_TYPE_ATTACHMENT)) {
								
								for (int j = 0; null != nodeList
										&& j < nodeList.getLength(); j++) {
									Node nod = nodeList.item(j);
									
									if (nod.getNodeType() == Node.ELEMENT_NODE){
											//System.out.println("Dummy:::::::::::::"+nod.getFirstChild().getNodeValue());
										//tempMap.put(attKey + "_NODE_ATTRIBUTE_"+j, nod.getFirstChild().getNodeValue());
										tempMap.put(attKey + "_NODE_ATTRIBUTE_"+j, "");
									}
								} // for
							}
							// new code for IRAND . can not use above if code as adding repeating tag for attachment
							
							else if (attributeMap.containsKey((attKey + "_TYPE"))
									&& (attributeMap.get(attKey + "_TYPE").substring(attributeMap.get(attKey + "_TYPE").indexOf(',')+1)).equalsIgnoreCase(ContentConstant.FIELD_TYPE_MULTIPLE_TAGS)) {
								multipleTagsContainer = new StringBuffer("<ul>");
								for (int k = 0; null != nodeList && k < nodeList.getLength(); k++) {
									Node nodForMultipleTags = nodeList.item(k);
									if (nodForMultipleTags.getNodeType() == Node.ELEMENT_NODE && nodForMultipleTags.getNodeValue() !=null){
										multipleTagsContainer.append("<li>"+nodForMultipleTags.getFirstChild().getNodeValue());
										//multipleTagsContainer.append("\n");
										
									}
								} // for
								multipleTagsContainer.append("</ul>");
								tempMap.put(attKey,multipleTagsContainer.toString());
							}
							
							// Cisco ---- handling author
							else if (attributeMap.containsKey((attKey + "_TYPE"))
									&& attributeMap.get(attKey + "_TYPE").equalsIgnoreCase(ContentConstant.FIELD_TYPE_AUTHOR)) {
								tempMap.put("AUTHORUSERNAME", xPath.compile(expression)
										.evaluate(xmlDocument));
							}
							else if (attributeMap.containsKey((attKey + "_TYPE"))
									&& attributeMap.get(attKey + "_TYPE").equalsIgnoreCase(ContentConstant.FIELD_TYPE_OWNER)) {
								tempMap.put("OWNERUSERNAME", xPath.compile(expression)
										.evaluate(xmlDocument));
							}
							
							// end for IRAND
							// Else for all remaining attributes
							else {
								tempMap.put(attKey, xPath.compile(expression)
										.evaluate(xmlDocument));
							}
						}
					}
				}

				response = writeTemporaryXMLFile(fileName, i, multiNode);
			}
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e);
			logger.error("Content is not written in XML" + e.getMessage());
			return false;
		}

	}

	/**
	 * 
	 * method is used for copying file from source location to failed location.
	 * 
	 * @param
	 * @return
	 */
	private void copyFileIfFailed(File sourceFile, File destFile)
			throws IOException {
		if (!sourceFile.exists()) {
			return;
		}
		if (!destFile.exists()) {
			destFile.createNewFile();
		}
		
		FileChannel source = null;
		FileChannel destination = null;
		source = new FileInputStream(sourceFile).getChannel();
		destination = new FileOutputStream(destFile).getChannel();
		
		if (destination != null && source != null) {
			destination.transferFrom(source, 0, source.size());
		}
		if (source != null) {
			source.close();
		}
		if (destination != null) {
			destination.close();
		}
	}

	/**
	 * 
	 * method is used to write temporary XML File.
	 * 
	 * @param
	 * @return
	 */
	public boolean writeTemporaryXMLFile(String fileName, int increment,
			Boolean multipleNode) {
		boolean response = false;
		try {

			DocumentBuilderFactory documentFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder documentBuilder = documentFactory
					.newDocumentBuilder();

			// define root elements
			Document document = documentBuilder.newDocument();
			Element rootElement = document
					.createElement(ContentConstant.TEMPXML_ROOT);
			document.appendChild(rootElement);

			TreeSet<String> keys = new TreeSet<String>(tempMap.keySet());
			Iterator<String> iterator = keys.iterator();

			while (iterator.hasNext()) {
				String elem = iterator.next();
				// for attachment
				if(elem.contains("_NODE_ATTRIBUTE")){
					Element elemattach = document.createElement(elem.substring(0,(elem.indexOf("_NODE_ATTRIBUTE"))));
					Element firstname = document.createElement(elem.substring(0,elem.lastIndexOf('_')));
					firstname.appendChild(document.createCDATASection(tempMap
							.get(elem)));
					elemattach.appendChild(firstname);
					rootElement.appendChild(elemattach);
				}
				else{
					Element firstname = document.createElement(elem);
					if(elem.equals("FIELD_18")){
						String  temp = tempMap.get("FIELD_18");
						temp = "_" + tempMap.get(elem);
						firstname.appendChild(document.createCDATASection(temp));
						rootElement.appendChild(firstname);
						continue;
					}else if(elem.equals("FIELD2")){
						String  temp = tempMap.get("FIELD2");
						temp = "<br/>~~QUESTION~~<br/>" + tempMap.get(elem);
						firstname.appendChild(document.createCDATASection(temp));
						rootElement.appendChild(firstname);
						continue;
					}else if(elem.equals("FIELD4")){
						String  temp = tempMap.get("FIELD4");
						temp = "<br/>~~ANSWER~~<br/>" + tempMap.get(elem);
						firstname.appendChild(document.createCDATASection(temp));
						rootElement.appendChild(firstname);
						continue;
					}else if(elem.equals("FIELD_13")){
						String  temp = tempMap.get("FIELD_13");
						temp = "<br/>~~PROBLEM~~<br/>" + tempMap.get(elem);
						firstname.appendChild(document.createCDATASection(temp));
						rootElement.appendChild(firstname);
						continue;
					}else if(elem.equals("FIELD_12")){
						String  temp = tempMap.get("FIELD_12");
						temp = "<br/>~~SOLUTION~~<br/>" + tempMap.get(elem);
						firstname.appendChild(document.createCDATASection(temp));
						rootElement.appendChild(firstname);
						continue;
					}else if(elem.equals("FIELD_14")){
						String  temp = tempMap.get("FIELD_14");
						temp = "<br/>~~ERROR~~<br/>" + tempMap.get(elem);
						firstname.appendChild(document.createCDATASection(temp));
						rootElement.appendChild(firstname);
						continue;
					}else if(elem.equals("FIELD09")){
						String  temp = tempMap.get("FIELD09");
						temp = "<br/>~~ROOT-CAUSE~~<br/>" + tempMap.get(elem);
						firstname.appendChild(document.createCDATASection(temp));
						rootElement.appendChild(firstname);
						continue;
					}else if(elem.equals("FIELD10")){
						String  temp = tempMap.get("FIELD10");
						temp = "<br/>~~WORKAROUND~~<br/>" + tempMap.get(elem);
						firstname.appendChild(document.createCDATASection(temp));
						rootElement.appendChild(firstname);
						continue;
					}else if(elem.equals("FIELD_11")){
						String  temp = tempMap.get("FIELD_11");
						temp = "<br/>~~REFRENCE~~<br/>" + tempMap.get(elem);
						firstname.appendChild(document.createCDATASection(temp));
						rootElement.appendChild(firstname);
						continue;
					}
					firstname.appendChild(document.createCDATASection(tempMap
							.get(elem)));
					rootElement.appendChild(firstname);
				}
			} // end of while

			String filenamewithoutextn = fileName.substring(0,
					fileName.indexOf('.'));

			// creating and writing to xml file
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			 document.setXmlStandalone(true);
			DOMSource domSource = new DOMSource(document);
			
			File flname;
			if (multipleNode) {
				flname = new File(parserExportFileLocation
						+ ContentConstant.TEMPXML_FILE_NAME_PREFIX
						+ filenamewithoutextn + "_" + increment + ".xml");
			} else {
				flname = new File(parserExportFileLocation
						+ ContentConstant.TEMPXML_FILE_NAME_PREFIX
						+ filenamewithoutextn + ".xml");
			}
			
			StreamResult streamResult = new StreamResult(flname);
			transformer.transform(domSource, streamResult);
			// rakesh
			tempMap.clear();
			response = true;

		} catch (Exception ex) {
			System.err.println("Error in writing the file " + ex.getMessage());
			logger.error("Error in writing the file " + ex.getMessage());
			response = false;
		}
		return response;

	}
	public String getRelativePath(String requiredPath) {
		String response = "";
		try {
			// Building the relative path
			// properties file in classes folder
			if (requiredPath.equalsIgnoreCase("RelativePath")) {
				// relative path with out package name
				response = getClass().getClassLoader().getResource(".").getPath();
			} else {
				// relative path with package name
				response = getClass().getClassLoader().getResource(".")
						.getPath()
						+ getClass().getPackage().getName().replace('.', '/')
						+ "/";
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("Error in finding the relative path "
			 + ex.getMessage());
			response = "";
		}
		return response;
	}
} // END OF CLASS

