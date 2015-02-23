package com.smart.content.repositorymove;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
/**
 * This class is responsible for parsing the required attributes from input xml
 * 
 * @author Infogain
 * 
 */

public class DataLayer {

	
	boolean multiNodeflag=false;
	private static Logger logger = Logger.getLogger("DataLayer");
	private Properties propForSecurity = new Properties();
	/**
	 * Constructor
	 */
	public DataLayer(Properties propForSecurity) {
		this.propForSecurity=propForSecurity;
	}
	
	/**
	 * 
	 * method is used for parse content for given file.
	 * 
	 * @param dataToExtract,multiNodeName,multiNodeNameNew, okmXml,contentAttribute
	 * @return String
	 */
	public String parseExistingXML(String dataToExtract,String multiNodeName,String multiNodeNameNew, String okmXml,HashMap<String, String> contentAttribute,List<String> securityTagList) throws Exception {
		String response = "";
		try {
			multiNodeflag=false;
			int countForMultiNode = 0;
			
			if(!"".equals(multiNodeName)){
				Pattern pattern = Pattern.compile(multiNodeName);
				Matcher matcher = pattern.matcher(dataToExtract);
			    while (matcher.find()) {
			    	countForMultiNode++;
			    }
			}
		    if(countForMultiNode>1){
		    	multiNodeflag=true;
		    }
		    
		    String fileHandlingNode = "",fileHandlingNodeAttribute="",nodeHandle="",okmXmlTagManipulate="",completeOkmXmlData="",multiTagController="",
		    securityRemoveData="",securityData="",keyData="",fieldHandle = "",fieldHandleFromXML="",fieldHandleInMultiTags = "",fieldOutsideNode="",requiredField = "";
		    StringBuilder newOkmXml = new StringBuilder(okmXml);
		    StringBuilder attachmentTracker = new StringBuilder();
		    
			if(multiNodeflag){
				// Handle xml for okm mapping
				if(okmXml.indexOf(multiNodeNameNew) != -1){
					okmXmlTagManipulate = okmXml.substring(okmXml.indexOf(multiNodeNameNew), okmXml.indexOf(multiNodeNameNew.replace("<", "</"))+multiNodeNameNew.replace("<", "</").length());
					for(int countMultiTag=1;countMultiTag<countForMultiNode;countMultiTag++){
						newOkmXml.append(okmXmlTagManipulate);
					}
					completeOkmXmlData = newOkmXml.toString();
				}else{
					completeOkmXmlData = okmXml;
				}
				
				int startIndexForMultiNode=0,lastIndexForMultiNode=0;
				for(int countMultiTagHandle=0;countMultiTagHandle<countForMultiNode;countMultiTagHandle++){
			    	
					startIndexForMultiNode = dataToExtract.indexOf(multiNodeName, startIndexForMultiNode);
					
					startIndexForMultiNode += multiNodeName.length();
					
					lastIndexForMultiNode = dataToExtract.indexOf(multiNodeName.replace("<", "</"), startIndexForMultiNode);
					
					multiTagController = dataToExtract.substring(startIndexForMultiNode, lastIndexForMultiNode);	   
					
					for (Entry<String, String> entry : contentAttribute.entrySet()) {
						
						attachmentTracker.delete(0, attachmentTracker.length());
					    if(entry.getKey().startsWith("FIELD") && !entry.getKey().endsWith("ATTACHMENT")){
					    	if (entry.getValue().indexOf('+') != -1) {
								StringBuffer tempXML = new StringBuffer();
								StringTokenizer stringTokenizerForKey = new StringTokenizer(entry.getValue(), "+");
								while (stringTokenizerForKey.hasMoreElements()) {
									keyData = stringTokenizerForKey.nextElement().toString();
									if(multiTagController.indexOf("<"+keyData+">") != -1){
										tempXML.append(multiTagController.substring(multiTagController.indexOf("<"+keyData+">")+("<"+keyData+">").length(),multiTagController.indexOf("</"+keyData+">")));
										tempXML.append('\n');
									}
							    	//rakesh - this is for tag type closing like <SECTIE_NAAM/>
							    	else if (multiTagController.indexOf("<"+keyData+"/>") != -1){
							    		tempXML.append(""); // setting blank in case of no data
							    	}
							    	//end

							    	else if(multiTagController.indexOf("<"+keyData) != -1){
							    		fieldHandle = "";
							    		if(multiTagController.indexOf("</"+keyData+">") != -1){
							    			fieldHandle = multiTagController.substring(multiTagController.indexOf("<"+keyData)+("<"+keyData).length(),multiTagController.indexOf("</"+keyData+">"));
							    		}else{
											fieldHandle = multiTagController.substring(multiTagController.indexOf("<"+keyData)+("<"+keyData).length());
											fieldHandle = fieldHandle.substring(0,fieldHandle.indexOf("/>"));

							    		}								    	
								    	if(fieldHandle.contains("SECURITY=") && fieldHandle.indexOf('>') != -1){
								    		securityRemoveData = fieldHandle.substring(fieldHandle.indexOf('>')+1);
								    		securityData = fieldHandle.substring(0,fieldHandle.indexOf('>'));
								    		tempXML.append(securityRemoveData).append('\n');
								    	}else{
								    		tempXML.append("");
								    	}
								    	
							    	}
									else if(dataToExtract.indexOf("<"+keyData+">") != -1){
										tempXML.append(dataToExtract.substring(dataToExtract.indexOf("<"+keyData+">")+("<"+keyData+">").length(),dataToExtract.indexOf("</"+keyData+">")));
										tempXML.append('\n');
									}
							    	//rakesh - this is for tag type closing like <SECTIE_NAAM/>
							    	else if (dataToExtract.indexOf("<"+keyData+"/>") != -1){
							    		tempXML.append(""); // setting blank in case of no data
							    	}
							    	//end

									else if(dataToExtract.indexOf("<"+keyData) != -1){
										
										fieldHandleFromXML = "";
										if(dataToExtract.indexOf("</"+keyData+">") != -1){
											fieldHandleFromXML = dataToExtract.substring(dataToExtract.indexOf("<"+keyData)+("<"+keyData).length(),dataToExtract.indexOf("</"+keyData+">"));
										}else{
											fieldHandleFromXML = dataToExtract.substring(dataToExtract.indexOf("<"+keyData)+("<"+keyData).length());
											fieldHandleFromXML = fieldHandleFromXML.substring(0,fieldHandleFromXML.indexOf("/>"));
										}										
										
										if(fieldHandleFromXML.contains("SECURITY=") && fieldHandleFromXML.indexOf('>') != -1){
								    		securityRemoveData = fieldHandleFromXML.substring(fieldHandleFromXML.indexOf('>')+1);
								    		securityData = fieldHandleFromXML.substring(0,fieldHandleFromXML.indexOf('>'));
								    		tempXML.append(securityRemoveData).append('\n');								    		
										}else{
											tempXML.append("");
										}
										
									}
								}
								completeOkmXmlData = completeOkmXmlData.replaceFirst(entry.getKey(),tempXML.toString());
					    	} // if for checking merging of attributes
					    	else {
					    		
						    	if(multiTagController.indexOf("<"+entry.getValue()+">") != -1){
							    	fieldHandle = multiTagController.substring(multiTagController.indexOf("<"+entry.getValue()+">")+("<"+entry.getValue()+">").length(),multiTagController.indexOf("</"+entry.getValue()+">"));
							    	completeOkmXmlData = completeOkmXmlData.replaceFirst(entry.getKey(),fieldHandle);
						    	}
						    	//rakesh - this is for tag type closing like <SECTIE_NAAM/>
						    	else if (multiTagController.indexOf("<"+entry.getValue()+"/>") != -1){
						    		completeOkmXmlData = completeOkmXmlData.replaceFirst(entry.getKey(),""); // setting blank in case of no data
						    	}
						    	//end

						    	// handle SECURITY TAG
						    	else if(multiTagController.indexOf("<"+entry.getValue()) != -1){
						    		
							    	fieldHandleInMultiTags = "";
						    		if(multiTagController.indexOf("</"+entry.getValue()+">") != -1){
						    			fieldHandleInMultiTags = multiTagController.substring(multiTagController.indexOf("<"+entry.getValue())+("<"+entry.getValue()).length(),multiTagController.indexOf("</"+entry.getValue()+">"));
						    		}else{
						    			fieldHandleInMultiTags = multiTagController.substring(multiTagController.indexOf("<"+entry.getValue())+("<"+entry.getValue()).length());
						    			fieldHandleInMultiTags = fieldHandleInMultiTags.substring(0,fieldHandleInMultiTags.indexOf("/>"));
						    		}
						    								    		
							    	if(fieldHandleInMultiTags.contains("SECURITY=") && fieldHandleInMultiTags.indexOf('>') != -1 ){
							    		securityRemoveData = fieldHandleInMultiTags.substring(fieldHandleInMultiTags.indexOf('>')+1);
							    		securityData = fieldHandleInMultiTags.substring(0,fieldHandleInMultiTags.indexOf('>'));
							    		securityData = getDataWithSecurity(securityData); // giving new value for group
							    	}else{
							    		securityRemoveData = "";
							    		securityData="";
							    	}
						    		if(securityData == null){
						    			securityData="";
						    		}
						    		if(securityTagList.contains(entry.getKey())){
						    			completeOkmXmlData = completeOkmXmlData.replaceFirst(entry.getKey(),securityRemoveData);
						    		}else
						    			completeOkmXmlData = completeOkmXmlData.replaceFirst(">"+entry.getKey(),securityData+">"+securityRemoveData);
						    		
							    	
						    	}
						    	else if(dataToExtract.indexOf("<"+entry.getValue()+">") != -1){
							    	fieldOutsideNode = dataToExtract.substring(dataToExtract.indexOf("<"+entry.getValue()+">")+("<"+entry.getValue()+">").length(),dataToExtract.indexOf("</"+entry.getValue()+">"));
							    	completeOkmXmlData = completeOkmXmlData.replaceFirst(entry.getKey(),fieldOutsideNode);
						    	}
						    	//rakesh - this is for tag type closing like <SECTIE_NAAM/>
						    	else if (dataToExtract.indexOf("<"+entry.getValue()+"/>") != -1){
						    		completeOkmXmlData = completeOkmXmlData.replaceFirst(entry.getKey(),""); // setting blank in case of no data
						    	}
						    	//end

						    	// handle SECURITY TAG outside the multinode tags
						    	else if(dataToExtract.indexOf("<"+entry.getValue()) != -1){
						    		
							    	fieldOutsideNode = "";
						    		if(dataToExtract.indexOf("</"+entry.getValue()+">") != -1){
						    			fieldOutsideNode = dataToExtract.substring(dataToExtract.indexOf("<"+entry.getValue())+("<"+entry.getValue()).length(),dataToExtract.indexOf("</"+entry.getValue()+">"));
						    		}else{
						    			fieldOutsideNode = dataToExtract.substring(dataToExtract.indexOf("<"+entry.getValue())+("<"+entry.getValue()).length());
						    			fieldOutsideNode = fieldOutsideNode.substring(0,fieldOutsideNode.indexOf("/>"));
						    		}
						    								    		
							    	if(fieldOutsideNode.contains("SECURITY=") && fieldOutsideNode.indexOf('>') != -1 ){
							    		securityRemoveData = fieldOutsideNode.substring(fieldOutsideNode.indexOf('>')+1);
							    		securityData = fieldOutsideNode.substring(0,fieldOutsideNode.indexOf('>'));
							    		securityData = getDataWithSecurity(securityData); // giving new value for group
							    	}else{
							    		securityRemoveData = "";
							    		securityData="";
							    	}
						    		if(securityData == null){
						    			securityData="";
						    		}
						    		if(securityTagList.contains(entry.getKey())){
						    			completeOkmXmlData = completeOkmXmlData.replaceFirst(entry.getKey(),securityRemoveData);
						    		}else
						    			completeOkmXmlData = completeOkmXmlData.replaceFirst(">"+entry.getKey(),securityData+">"+securityRemoveData);

						    	}

						    	
					    	} // end of else no merging of attributes
					    	
					    } // end of if
				    	else if(entry.getKey().endsWith("ATTACHMENT")){
					    	fileHandlingNode = entry.getValue().substring(0,entry.getValue().indexOf('/'));
					    	fileHandlingNodeAttribute = entry.getValue().substring(entry.getValue().indexOf('/')+1);
					    	nodeHandle = "<"+fileHandlingNode+">"+"<"+fileHandlingNodeAttribute;
						    int lastIndex = 0,lastIndexForAttachment=0;
						    while ((lastIndex = multiTagController.indexOf(nodeHandle, lastIndex)) != -1) {
						        lastIndex += nodeHandle.length();						    	
						        lastIndexForAttachment= multiTagController.indexOf("</"+fileHandlingNode,lastIndex);
						        String requiredNode = multiTagController.substring(lastIndex,lastIndexForAttachment);
						        // handling the security value for new value from user group mapping
						        if(requiredNode.indexOf("SECURITY=\"") != -1){
						        	requiredNode = getDataWithSecurity(requiredNode);
						        }// end of handling the security value in tag

						        attachmentTracker.append(nodeHandle+requiredNode+"</"+fileHandlingNode+">");
						    } // end of while
						    completeOkmXmlData = completeOkmXmlData.replaceFirst(entry.getKey(),attachmentTracker.toString());
				    	}
					} // end for loop for replacing
					
				} // for loop for count so that multiple tags can be replaced
				okmXml = completeOkmXmlData;

			}
			// else of multiNodeFlag
			else{
				for (Entry<String, String> entry : contentAttribute.entrySet()) {
					attachmentTracker.delete(0, attachmentTracker.length());
										
					    if(entry.getKey().startsWith("FIELD") && !entry.getKey().endsWith("ATTACHMENT")){
					    	
					    	if (entry.getValue().indexOf('+') != -1) {
								StringBuffer tempXML = new StringBuffer();
								StringTokenizer stringTokenizerForKey = new StringTokenizer(entry.getValue(), "+");
								while (stringTokenizerForKey.hasMoreElements()) {
									keyData = stringTokenizerForKey.nextElement().toString();
									if(dataToExtract.indexOf("<"+keyData+">") != -1){
										tempXML.append(dataToExtract.substring(dataToExtract.indexOf("<"+keyData+">")+("<"+keyData+">").length(),dataToExtract.indexOf("</"+keyData+">")));
										tempXML.append('\n');
									}
							    	//rakesh - this is for tag type closing like <SECTIE_NAAM/>
							    	else if (dataToExtract.indexOf("<"+keyData+"/>") != -1){
							    		tempXML.append(""); // setting blank in case of no data
							    	}
							    	//end
							    	else if(dataToExtract.indexOf("<"+keyData) != -1){
							    		requiredField = "";
							    		if(dataToExtract.indexOf("</"+keyData+">") != -1){
							    			requiredField = dataToExtract.substring(dataToExtract.indexOf("<"+keyData)+("<"+keyData).length(),dataToExtract.indexOf("</"+keyData+">"));
							    		}else{
							    			requiredField = dataToExtract.substring(dataToExtract.indexOf("<"+keyData)+("<"+keyData).length());
							    			requiredField = requiredField.substring(0,requiredField.indexOf("/>"));
							    		}							    								    		
								    	if(requiredField.contains("SECURITY=") && requiredField.indexOf('>') != -1 ){
								    		securityRemoveData = requiredField.substring(requiredField.indexOf('>')+1);
								    		securityData = requiredField.substring(0,requiredField.indexOf('>'));
								    		tempXML.append(securityRemoveData).append('\n');
								    	}else{
								    		tempXML.append("");
								    	}
							    	}						    	

								} // end of while
								okmXml = okmXml.replace(entry.getKey(),tempXML.toString());
					    	} // if for multiattributes check
					    	else {					    		
						    	if(dataToExtract.indexOf("<"+entry.getValue()+">") != -1){
							    	requiredField = dataToExtract.substring(dataToExtract.indexOf("<"+entry.getValue()+">")+("<"+entry.getValue()+">").length(),dataToExtract.indexOf("</"+entry.getValue()+">"));
							    	okmXml = okmXml.replace(entry.getKey(),requiredField);
						    	}
						    	//rakesh - this is for tag type closing like <SECTIE_NAAM/>
						    	else if (dataToExtract.indexOf("<"+entry.getValue()+"/>") != -1){
						    		okmXml = okmXml.replace(entry.getKey(),""); // setting blank in case of no data
						    	}
						    	//end
						    	// handle SECURITY TAG
						    	else if(dataToExtract.indexOf("<"+entry.getValue()) != -1){
						    		requiredField = "";
						    		if(dataToExtract.indexOf("</"+entry.getValue()+">") != -1){
						    			requiredField = dataToExtract.substring(dataToExtract.indexOf("<"+entry.getValue())+("<"+entry.getValue()).length(),dataToExtract.indexOf("</"+entry.getValue()+">"));
						    		}else{
						    			requiredField = dataToExtract.substring(dataToExtract.indexOf("<"+entry.getValue())+("<"+entry.getValue()).length());
						    			requiredField = requiredField.substring(0,requiredField.indexOf("/>"));
						    		}
							    	if(requiredField.contains("SECURITY=") && requiredField.indexOf('>') != -1 ){
							    		securityRemoveData = requiredField.substring(requiredField.indexOf('>')+1);
							    		securityData = requiredField.substring(0,requiredField.indexOf('>')); // giving as SECURITY="DATA";
							    		securityData = getDataWithSecurity(securityData); // giving new value for group
							    	} else{
							    		securityRemoveData = "";
							    		securityData="";
							    	}
						    		if(securityData == null){
						    			securityData="";
						    		}
						    		if(securityTagList.contains(entry.getKey())){
						    			okmXml = okmXml.replace(entry.getKey(),securityRemoveData);
						    		}else
						    			okmXml = okmXml.replace(">"+entry.getKey(),securityData+">"+securityRemoveData);
						    	}						    	
					    	} // else for multi attributes check
					    	
					    }
					    else if(entry.getKey().endsWith("ATTACHMENT")){
					    	fileHandlingNode = entry.getValue().substring(0,entry.getValue().indexOf('/'));
					    	fileHandlingNodeAttribute = entry.getValue().substring(entry.getValue().indexOf('/')+1);					    	
					    	nodeHandle = "<"+fileHandlingNode+">"+"<"+fileHandlingNodeAttribute;
						    int lastIndex = 0,lastIndexForAttachment=0;
						    while ((lastIndex = dataToExtract.indexOf(nodeHandle, lastIndex)) != -1) {						    			    	
						        lastIndex += nodeHandle.length();						    	
						        lastIndexForAttachment= dataToExtract.indexOf("</"+fileHandlingNode,lastIndex);
						        String mappingValue = dataToExtract.substring(lastIndex,lastIndexForAttachment);

						        // handling the security value for new value from user group mapping
						        if(mappingValue.indexOf("SECURITY=\"") != -1){
						        	mappingValue = getDataWithSecurity(mappingValue);
						        }// end of handling the security value in tag
						        
						        attachmentTracker.append(nodeHandle+mappingValue+"</"+fileHandlingNode+">");
						        
						    } // end of while
						    okmXml = okmXml.replaceFirst(entry.getKey(),attachmentTracker.toString());
					    }
				}
			} // end of else
			// Need to remove all FIELD from final remaining data which has no data set
			for(int i=1;i<=15;i++){
				okmXml = okmXml.replace("FIELD"+i, "");
			}			
			response = okmXml;
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e);
			logger.error("Content is not extracted from XML" + e.getMessage());
			response="";
		}
		return response;
	} // end of method
	
	/**
	 * 
	 * method to get the new security value
	 * 
	 * @param file Name,properties,header value,sub header
	 * @return String
	 */
	private String getDataWithSecurity(String mappingValue) {
		String response = "";
		String securityValue = "",securityNewValue="";
		try {
	        // handling the security value for new value from user group mapping
	        securityValue = mappingValue.substring(mappingValue.indexOf("SECURITY=\"")+"SECURITY=\"".length()); 
	        securityValue = securityValue.substring(0,securityValue.indexOf('\"'));
	        
	        StringTokenizer stringTokenizerForSecurity = new StringTokenizer(securityValue, "+");
	        StringBuffer tempSecurity = new StringBuffer();
	        
	        while (stringTokenizerForSecurity.hasMoreElements()) {
	        	String tokenValueFromConf=stringTokenizerForSecurity.nextToken();
	        	if(propForSecurity.getProperty(tokenValueFromConf)!= null){
	        		tempSecurity.append(propForSecurity.getProperty(tokenValueFromConf)).append("+");
	        	}
	        }
			if(tempSecurity.length() > 0){
		        securityNewValue = tempSecurity.toString().substring(0,tempSecurity.lastIndexOf("+"));
			}else{
				securityNewValue = "";
			}
	        response = mappingValue.replace("\""+securityValue+"\"", "\""+securityNewValue+"\"");
		
		} catch (Exception e) {
			response="";
			e.printStackTrace();
		} 
		return response;
	}

}
