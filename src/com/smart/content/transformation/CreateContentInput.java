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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.impl.xb.xsdschema.TopLevelAttribute;

import com.inquira.client.serviceclient.IQServiceClient;
import com.inquira.client.serviceclient.IQServiceClientManager;
import com.inquira.client.serviceclient.request.IQCategoryRequest;
import com.inquira.im.ito.CategoryITO;
import com.inquira.im.ito.CategoryKeyITO;
import com.inquira.im.ito.impl.CategoryITOImpl;
import com.inquira.im.ito.impl.CategoryKeyITOImpl;
import com.smart.content.extractor.intercept.ParserConfiguration;

/*
 • 	Will read all the xml file from HTML_EXPORT_FILE_LOCATION : Done 	
 • 	Create the input xml file in IM_CONTENT_IMPORT_INPUTXMLPATH : Need to add the required data for view , user group and category in the top of xml
 • 	Move failed xml to archieve dir - Done
 */
public class CreateContentInput {

	private Logger logger = Logger.getLogger("CreateContentInput");

	private String inputXMLFileLocation = "", outputXMLFileLocation = "",
			failedXMLFileLocation = "", channelName = "",
			generalXMLTemplate = "", generalContentXMLEndTag = "";

	private final static String DELIM = "~~!~~";

	/**
	 * constructor
	 */

	//static{
		
	//}
	
	public CreateContentInput(String inputXMLFileLocation,
			String outputXMLFileLocation, String failedXMLFileLocation,
			String channelName, String generalXMLTemplate,
			String generalContentXMLEndTag) {
		this.inputXMLFileLocation = inputXMLFileLocation;
		this.outputXMLFileLocation = outputXMLFileLocation;
		this.failedXMLFileLocation = failedXMLFileLocation;
		this.channelName = channelName;
		this.generalXMLTemplate = generalXMLTemplate;
		this.generalContentXMLEndTag = generalContentXMLEndTag;
		initializeMap();
		processXMLFile();
	}

	private void processXMLFile() {

		File[] files = getFiles(inputXMLFileLocation);

		for (File file : files) {

			if (file.toString().endsWith(".xml")
					|| file.toString().endsWith(".XML")) {

				// logger.info("XML file is ::: "+file.getName());

				if (!writeFileContent(file.getAbsolutePath(), file.getName()))
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
			// System.err.println(e);
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

				String authName = xmlData.substring(
						xmlData.indexOf("<AUTHORUSERNAME>"),
						xmlData.indexOf("</AUTHORUSERNAME>")
								+ "</AUTHORUSERNAME>".length());

				String xmlText = handleOKMAttributeInXML(xmlData.replace(
						authName, ""));

				String visibilityStatus = handleRuntimeData(xmlText, fileName);

				if (xmlText.contentEquals("~!~Internal~!~Self-Help~!~")
						|| xmlText.contentEquals("~!~Internal")
						|| xmlText.contentEquals("~Self-Help~!~")) {

					String visibiltyGroup = xmlText.substring(
							xmlText.indexOf("<VISIBILITY_GROUP>"),
							xmlText.indexOf("</VISIBILITY_GROUP>"));
					if (visibiltyGroup
							.contentEquals("~!~Internal~!~Self-Help~!~")
							|| visibiltyGroup.contentEquals("~!~Internal")
							|| visibiltyGroup.contentEquals("~Self-Help~!~")) {

						String finalVisibility = visibiltyGroup
								.substring(visibiltyGroup.indexOf("[~!~"));

						if (finalVisibility
								.contains("~!~Internal~!~Self-Help~!~")) {
							// Based on visibility, deciding the reference type
							xmlText = xmlText.replaceAll(
									"<ANALYST_ANSWERSOLUTION>",
									"<CLIENT_ANSWERSOLUTION>").replaceAll(
									"</ANALYST_ANSWERSOLUTION>",
									"</CLIENT_ANSWERSOLUTION>");
						}
					}

				}
				StringBuilder builderCompleteXML = new StringBuilder();

				// combining xml for xml with view etc and xml with OKM channel
				// schema attributes

				builderCompleteXML
						.append(generalXMLTemplate.replace(
								"<AUTHORUSERNAME></AUTHORUSERNAME>", authName))
						.append(visibilityStatus).append(xmlText)
						.append(generalContentXMLEndTag);

				logger.info("XML data for file " + fileName + " is ::: "
						+ builderCompleteXML);
				if (builderCompleteXML != null
						&& !"".equals(builderCompleteXML.toString().trim())) {
					response = writeTemporaryXMLFile(outputXMLFileLocation
							+ fileName, builderCompleteXML.toString());
				}
				// writing into log file if we are not able to create the xml
				// for html content
				if (!response) {
					copyFailedXML(fileName);
				}

			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			// System.err.println(e);
			logger.error("Content is not written in XML" + e.getMessage());
			return false;
		}

	} // end of writeFileContent

	public String handleRuntimeData(String xmlText, String file) {

		StringBuffer tempXML = new StringBuffer();
		ArrayList<String> temp = new ArrayList<String>();

		if (xmlText.contentEquals("~!~Internal~!~Self-Help~!~")
				|| xmlText.contentEquals("~!~Internal")
				|| xmlText.contentEquals("~Self-Help~!~")) {

			String testVisibility = xmlText.substring(
					xmlText.lastIndexOf("<VISIBILITY_GROUP><![CDATA"),
					xmlText.lastIndexOf("</VISIBILITY_GROUP>"));
			String visibility = xmlText.substring(
					xmlText.lastIndexOf("CDATA[~!~"),
					xmlText.lastIndexOf("~!~]]></VISIBILITY_GROUP>"));

			if (visibility.contains("~!~Internal~!~Self-Help")) {
				String test1 = visibility.substring(
						visibility.indexOf("~") + 3,
						visibility.lastIndexOf("~!~"));
				String test = visibility.substring(
						visibility.lastIndexOf("~") + 1).replaceAll("-", "_");
				temp.add(test);
				temp.add(test1);
			} else {
				String visibile = visibility.substring(visibility
						.lastIndexOf("~") + 1);
				temp.add(visibile);
			}

			tempXML.append("<SECURITY>");
			Iterator<String> test123 = temp.iterator();
			while (test123.hasNext()) {

				tempXML.append("<USERGROUP>").append("<REFERENCE_KEY>")
						.append(test123.next().toUpperCase())
						.append("</REFERENCE_KEY>").append("<GUID></GUID>")
						.append("</USERGROUP>");
			}

			tempXML.append("</SECURITY>");
		} else {

			tempXML.append("<SECURITY>");
			tempXML.append("<USERGROUP>").append("<REFERENCE_KEY>")
					.append("</REFERENCE_KEY>").append("<GUID></GUID>")
					.append("</USERGROUP>");
			tempXML.append("</SECURITY>");
		}

		//HashMap<String, String> data = readCategoryFromExcel();

		//HashMap<String, String> parentData = readParentCategoryFromExcel();

		//HashMap<String, String> serviceProvider = readServiceProviderParentCategory();

		String fileName = file.substring(file.lastIndexOf("_") + 1)
				.replaceAll("_", "").replaceAll(".xml", "");
		String documentID = null;
		if (file.contains("Extracted_faq")) {
			documentID = "faq_" + fileName;

		} else {
			documentID = fileName;
		}

		//CategoriesUtil util = new CategoriesUtil();

		//CategoriesUtilServiceProvider servcProvider = new CategoriesUtilServiceProvider();

		// Adding Tags for Category
		tempXML.append("<CATEGORIES>");

		//for (Map.Entry<String, String> entry : data.entrySet()) {

			//for (Map.Entry<String, String> topEntry : parentData.entrySet()) {

//				if (entry.getKey().equals(documentID)) {

					//String[] lines = entry.getValue().split("\\n");
					//String[] linesCat = topEntry.getValue().split("\\n");

//					String refKey = topEntry.getValue();
		           ProductInfoDTO dto=getProductDetail(documentID);
		           if(dto!=null){
		        	   String[] lines = dto.getProductName().split("\\n");
		        	   for (int i = 0; i < lines.length ; i++) {
		           
						tempXML.append("<CATEGORY>")
								.append("<REFERENCE_KEY>")
								.append(CategoriesUtil.getCategoryKey(lines[i]
										+ DELIM + dto.getSecondLevel()))
								.append("</REFERENCE_KEY>")
								.append("<GUID></GUID>").append("</CATEGORY>");
						
						}
		//        	}

					//break;
				//}
			//}
		//}

		String provider = "Service Owner";
	//	for (Map.Entry<String, String> serviceEntry : serviceProvider
		//		.entrySet()) {

			//Added entries from the Service Column 
			//if (serviceEntry.getKey().equals(documentID)) {
				String serValue = dto.getServiceOwner();
				if (!(serValue.equals("NA") || serValue.equals("0"))) {
					tempXML.append("<CATEGORY>")
							.append("<REFERENCE_KEY>")
							 .append(CategoriesUtilServiceProvider.getCategoryKey(serValue
							 + DELIM + provider))
							.append("</REFERENCE_KEY>").append("<GUID></GUID>")
							.append("</CATEGORY>");
					}
			//}
		}
		tempXML.append("</CATEGORIES>");

		String catDet = tempXML.substring(tempXML.indexOf("<CATEGORIES>"),
				tempXML.lastIndexOf("</CATEGORIES>"));
		if (catDet.equals("<CATEGORIES>")) {

			tempXML.replace(
					tempXML.indexOf("<CATEGORIES>"),
					tempXML.lastIndexOf("</CATEGORIES>"),
					"<CATEGORIES><CATEGORY><REFERENCE_KEY>DEFAULT</REFERENCE_KEY><GUID></GUID></CATEGORY>");

		}

		return tempXML.toString();

	}

	public HashMap<String, String> readServiceProviderParentCategory() {
		// TODO Auto-generated method stub

		HashMap<String, String> serviceValue = new HashMap<String, String>();

		for (int i = 0; i < docID.size(); i++) {
			serviceValue.put(docID.get(i), serviceProvider.get(i));
			//System.out.println(serviceValue);
		}

		return serviceValue;
	}

	ArrayList<String> docID;
	ArrayList<String> prodName;
	ArrayList<String> secCat;
	ArrayList<String> serviceProvider;
	private static Map<String,ProductInfoDTO> xlsMap=new HashMap<String, ProductInfoDTO>(); 
	private ProductInfoDTO getProductDetail(String documentId){
		
		return xlsMap.get(documentId);
		
		
	}

	private void initializeMap(){
		Row row;
		Cell cell;
		ProductInfoDTO dto;
		try {
			FileInputStream file = new FileInputStream(new File(System.getProperty("SMART_HOME")+"new.xlsx"));

			XSSFWorkbook workbook = new XSSFWorkbook(file);
			
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			Iterator<Row> rowIterator = sheet.iterator();

			while (rowIterator.hasNext()) {
				row = rowIterator.next();
				//doc id
				cell=row.getCell(0);
				dto=new ProductInfoDTO();
				if(Cell.CELL_TYPE_NUMERIC==cell.getCellType())
				dto.setDocumentId((int)cell.getNumericCellValue()+"");
				else
				dto.setDocumentId(cell.getStringCellValue());	
				
				
				//product name 
				cell=row.getCell(4);
				dto.setProductName(cell.getStringCellValue());
				
				
				//second level 
				cell=row.getCell(9);
				dto.setSecondLevel(cell.getStringCellValue());
				
				//parent 
				cell=row.getCell(7);
				dto.setParentCategory(cell.getStringCellValue());
				
				
				//service owner 
				cell=row.getCell(16);
				if(Cell.CELL_TYPE_STRING==cell.getCellType()){
					dto.setServiceOwner(cell.getStringCellValue());

				}else if(Cell.CELL_TYPE_ERROR==cell.getCellType()){
					dto.setServiceOwner("NA");
				}else if(Cell.CELL_TYPE_NUMERIC==cell.getCellType()){
					dto.setServiceOwner("0");

				}


				
				
				xlsMap.put(dto.getDocumentId(),dto);
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	

	public HashMap<String, String> readCategoryFromExcel() {

		docID = new ArrayList<String>();

		prodName = new ArrayList<String>();

		ArrayList<String> topCat = new ArrayList<String>();

		secCat = new ArrayList<String>();

		serviceProvider = new ArrayList<String>();

		HashMap<String, String> catDetail = new HashMap<String, String>();

		Row row;
		try {
			FileInputStream file = new FileInputStream(new File("C:\\samplexmls\\new.xlsx"));

			XSSFWorkbook workbook = new XSSFWorkbook(file);
			
			XSSFSheet sheet1 = workbook.getSheet("All RKM Docs");
			
			XSSFSheet sheet = workbook.getSheetAt(0);

			Iterator<Row> rowIterator = sheet.iterator();

			while (rowIterator.hasNext()) {
				row = rowIterator.next();
				//row.get
				// For each row, iterate through all the columns
				Iterator<Cell> cellIterator = row.cellIterator();

				while (cellIterator.hasNext()) {

					Cell cell = cellIterator.next();

					int index = cell.getColumnIndex();
					//System.out.println(index);

					// For all the 5 columns of the Excel sheet
					if (index == 0 || index == 4 || index == 7 || index == 9
							|| index == 16) {

						switch (index) {

						case 0:
							switch (cell.getCellType()) {
							case Cell.CELL_TYPE_NUMERIC:
								Integer text = (int) cell.getNumericCellValue();

								docID.add(text.toString());

								break;
							case Cell.CELL_TYPE_STRING:
								if (cell.getStringCellValue().equals("Doc ID")) {
									continue;
								} else {
									docID.add(cell.getStringCellValue());
									break;
								}
								
							}

							break;
							
							/**
							 * 
							 *case 16:
							switch (cell.getCellType()) {
							case Cell.CELL_TYPE_STRING:
								if ((cell.getStringCellValue()
										.equals("Service"))) {
									continue;
								} else {
									serviceProvider.add(cell
											.getStringCellValue());
									//System.out.println(serviceProvider);
								}
								break;
							case Cell.CELL_TYPE_ERROR:
								serviceProvider.add("NA");
								break;
							}
							break;
 
							 * 
							 * 
							 * 
							 * **/
							

						case 4:
							switch (cell.getCellType()) {
							case Cell.CELL_TYPE_STRING:
								if (cell.getStringCellValue().equals(
										"Product Name (manual adds)")) {
									continue;
								} else {
									prodName.add(cell.getStringCellValue());

								}
								break;
							}

							//break;

						case 7:
							switch (cell.getCellType()) {
							case Cell.CELL_TYPE_STRING:
								if (cell.getStringCellValue().equals(
										"Top level Category ")) {
									continue;
								} else {
									topCat.add(cell.getStringCellValue());

								}
								break;
							}

							break;

						case 9:
							switch (cell.getCellType()) {
							case Cell.CELL_TYPE_STRING:
								if ((cell.getStringCellValue()
										.equals("manually checked against SRM report"))
										|| (cell.getStringCellValue()
												.equals("2nd level category"))) {
									continue;
								} else {
									secCat.add(cell.getStringCellValue());
								}
							}
							break;

						case 16:
							switch (cell.getCellType()) {
							case Cell.CELL_TYPE_STRING:
								if ((cell.getStringCellValue()
										.equals("Service"))) {
									continue;
								} else {
									serviceProvider.add(cell
											.getStringCellValue());
									break;
								}
								
							case Cell.CELL_TYPE_ERROR:
								serviceProvider.add("NA");
								break;

							case Cell.CELL_TYPE_NUMERIC:
							Integer text = (int) cell.getNumericCellValue();

							serviceProvider.add(text.toString());

								
							}

							break;


						default:
							break;
						}
					}
				}

			}

			while (docID != null && prodName != null) {

				for (int i = 0; i < docID.size(); i++) {
					catDetail.put(docID.get(i), prodName.get(i));

				}
				break;
			}
			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return catDetail;
	}

	public HashMap<String, String> readParentCategoryFromExcel() {

		HashMap<String, String> parentCatDetail = new HashMap<String, String>();

		try {

			for (int i = 0; i < prodName.size() || i < secCat.size(); i++) {
				parentCatDetail.put(prodName.get(i), secCat.get(i));
				//System.out.println(parentCatDetail);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return parentCatDetail;
	}

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
			File source = new File(inputXMLFileLocation + fileName); // source
																		// for
																		// html

			File dest = new File(failedXMLFileLocation + fileName); // archeive

			inputChannel = new FileInputStream(source).getChannel();

			outputChannel = new FileOutputStream(dest).getChannel();

			outputChannel.transferFrom(inputChannel, 0, inputChannel.size());

			result = true;

		} catch (Exception e) {
			result = false;
			logger.error("Error in copying the failed HTML file "
					+ e.getMessage());
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
	 * method is used for reading property file for okm_attribute_mapping
	 * resource
	 */

	private String handleOKMAttributeInXML(String xmlText) {

		String transformedString = xmlText;

		Properties prop = new Properties();
		Properties attrprop = new Properties();
		try {

			// find parser type and load master_attribute.properties related to
			// that parser type
			ParserConfiguration pc = new ParserConfiguration();
			String parserType = pc.checkParserType();

			if (parserType != null && !"".equals(parserType.trim())) {
				InputStream inObj = null;
				if (parserType.equalsIgnoreCase("HTML")) {
					inObj = getClass()
							.getResourceAsStream(
									"/com/smart/content/extractor/html/master_attribute.properties");
				} else if (parserType.equalsIgnoreCase("XML")) {
					inObj = getClass()
							.getResourceAsStream(
									"/com/smart/content/extractor/xml/master_attribute.properties");
				}
				attrprop.load(inObj);
			}
			// properties file in classes folder
			InputStream inObj = getClass().getResourceAsStream(
					"/" + getClass().getPackage().getName().replace('.', '/')
							+ "/okm_attribute_mapping.properties");
			prop.load(inObj);
			Enumeration enuKeys = prop.keys();
			while (enuKeys.hasMoreElements()) {
				String key = (String) enuKeys.nextElement();
				String value = prop.getProperty(key);

				if (xmlText.toString().contains(key)) {

					if (value.toString().indexOf(',') != -1) {
						transformedString = transformedString.replace(key,
								(value.substring(0, value.indexOf(',')))
										.toUpperCase());
						// for list type attribute
						if ("LIST".equals(attrprop.get(key + "_TYPE"))) {
							String startText = value.substring(0,
									value.indexOf(','))
									+ ">";
							String endText = "</";
							String replacementStr = "";
							int lastIndex = 0, lastIndexforText = 0;
							while ((lastIndex = transformedString.indexOf(
									startText, lastIndex)) != -1) {
								lastIndex += startText.length();
								lastIndexforText = transformedString.indexOf(
										endText, lastIndex);
								replacementStr = transformedString.substring(
										lastIndex, lastIndexforText);
								ListTypeAttributeDBDao listDAO = new ListTypeAttributeDBDao();
								String xmlToAppend = listDAO
										.execute(value.substring(value
												.indexOf(',') + 1),
												stripCDATA(replacementStr));
								transformedString = transformedString.replace(
										replacementStr, xmlToAppend);
								break;
							} // end of while
								// for attachment
						} else if ("NODE".equals(attrprop.get(key + "_TYPE"))) {
							transformedString = transformedString.replace(
									(value.substring(0, value.indexOf(',')))
											.toUpperCase() + "_NODE_ATTRIBUTE",
									(value.substring(value.indexOf(',') + 1))
											.toUpperCase());
						}
					} // end of if
					else {
						transformedString = transformedString.replace(key,
								value.toUpperCase());
					}

				}
				// Handle merging of fields
				if (key.indexOf('+') != -1) {
					StringBuffer tempXML = new StringBuffer("<![CDATA[");
					StringTokenizer stringTokenizerForKey = new StringTokenizer(
							key, "+");

					while (stringTokenizerForKey.hasMoreElements()) {
						String keyData = stringTokenizerForKey.nextElement()
								.toString();
						if (xmlText.toString().contains(keyData)) {
							if (xmlText.contains("<" + keyData + "><![CDATA[")) {
								tempXML.append(xmlText.substring(
										xmlText.indexOf("<" + keyData
												+ "><![CDATA[")
												+ ("<" + keyData + "><![CDATA[")
														.length(), xmlText
												.indexOf("]]></" + keyData
														+ ">")));
								tempXML.append('\n');
							}
						}
					}
					tempXML.append("]]>");
					String dataPrepared = "<" + value + ">"
							+ tempXML.toString() + "</" + value + ">";
					if(dataPrepared.contains("<LEGACY_DOCUMENT_ID><![CDATA[_")){
					continue;	
					}else{
					transformedString = transformedString.replace("</CONTENT>",
							dataPrepared + "</CONTENT>");
					}
				} // end of merging fields

			} // end of while

			transformedString = transformedString.replace("CONTENT",
					channelName);

		} catch (IOException ex) {
			transformedString = "";
			ex.printStackTrace();
			logger.error("Error in reading the reading property file for okm_attribute_mapping "
					+ ex.getMessage());
		}
		return transformedString;
	}

	/**
	 * 
	 * method is used for store xml in FILE or write the log file in case of
	 * fail
	 * 
	 * @param String
	 *            for content XML
	 * @return boolean
	 */
	private boolean writeTemporaryXMLFile(String location, String contentXML) {
		boolean response = false;
		try {
			// Create file
			FileWriter fstream = new FileWriter(location, false); // file

			BufferedWriter out = new BufferedWriter(fstream);
			out.write(contentXML);
			// Close the output stream
			out.close();
			response = true;
		} catch (Exception ex) {// Catch exception if any
			// System.err.println("Error in writing the file " +
			// ex.getMessage());
			logger.error("Error in writing the file during content trasform "
					+ ex.getMessage());
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
