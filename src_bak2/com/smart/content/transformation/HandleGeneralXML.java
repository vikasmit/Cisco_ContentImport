package com.smart.content.transformation;

import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

/* This class is responsible for adding the value like view,category,user groups etc in general xml
  */
public class HandleGeneralXML {
	private Logger logger = Logger.getLogger("HandleGeneralXML");

	private String CHANNEL_NAME = "",CHANNEL_GUID = "",REPOSITORY = "",REPOSITORY_GUID = "",AUTHORID = "",AUTHOR_USERNAME = "",OWNERID = "",OWNERUSERNAME = "",LOCALECODE = "",DATECREATED_MILLIS="",REVIEW_TIMESTAMP = "",
	REVIEW_TIMESTAMP_MILLIS = "",STARTTIMESTAMP = "",STARTTIMESTAMP_MILLIS = "",ENDTIMESTAMP = "",ENDTIMESTAMP_MILLIS = "",EVENTSTARTTIMESTAMP = "",EVENTSTARTTIMESTAMP_MILLIS = "",EVENTENDTIMESTAMP = "",
	EVENTENDTIMESTAMP_MILLIS = "",VIEW_REFKEY= "",USERGROUP_REFKEY = "",CATEGORY_REFKEY = "";

	// Construtor
	public HandleGeneralXML(Properties prop) {

		CHANNEL_NAME = prop.getProperty("IM_CHANNEL_NAME");
		CHANNEL_GUID = prop.getProperty("IM_CHANNEL_GUID");
		REPOSITORY = prop.getProperty("IM_REPOSITORY");
		REPOSITORY_GUID = prop.getProperty("IM_REPOSITORY_GUID");
		AUTHORID = prop.getProperty("IM_AUTHORID");
		AUTHOR_USERNAME = prop.getProperty("IM_AUTHOR_USERNAME");
		OWNERID = prop.getProperty("IM_OWNERID");
		OWNERUSERNAME = prop.getProperty("IM_OWNERUSERNAME");
		LOCALECODE = prop.getProperty("IM_LOCALECODE");
		DATECREATED_MILLIS = prop.getProperty("IM_DATECREATED_MILLIS");
		REVIEW_TIMESTAMP = prop.getProperty("IM_REVIEW_TIMESTAMP");
		REVIEW_TIMESTAMP_MILLIS = prop.getProperty("IM_REVIEW_TIMESTAMP_MILLIS");
		STARTTIMESTAMP = prop.getProperty("IM_STARTTIMESTAMP");
		STARTTIMESTAMP_MILLIS = prop.getProperty("IM_STARTTIMESTAMP_MILLIS");
		ENDTIMESTAMP = prop.getProperty("IM_ENDTIMESTAMP");
		ENDTIMESTAMP_MILLIS = prop.getProperty("IM_ENDTIMESTAMP_MILLIS");
		EVENTSTARTTIMESTAMP = prop.getProperty("IM_EVENTSTARTTIMESTAMP");
		EVENTSTARTTIMESTAMP_MILLIS = prop.getProperty("IM_EVENTSTARTTIMESTAMP_MILLIS");
		EVENTENDTIMESTAMP = prop.getProperty("IM_EVENTENDTIMESTAMP");
		EVENTENDTIMESTAMP_MILLIS = prop.getProperty("IM_EVENTENDTIMESTAMP_MILLIS");
		VIEW_REFKEY= prop.getProperty("IM_VIEW_REFKEY");
		USERGROUP_REFKEY = prop.getProperty("IM_USERGROUP_REFKEY");
		CATEGORY_REFKEY = prop.getProperty("IM_CATEGORY_REFKEY");

	}

	public String generalXML(String XMLTemplate) {
		String standardXML = "";
		try {
			if ("".equals(CHANNEL_NAME)	|| "".equals(REPOSITORY) || "".equals(LOCALECODE) || "".equals(VIEW_REFKEY)) {
				standardXML = "";
				logger.info("Required value is/are null "+" Channel Name "+CHANNEL_NAME+" REPOSITORY "+REPOSITORY + " LOCALECODE "+LOCALECODE+ " VIEW REF KEY "+VIEW_REFKEY);
			} else {
				StringBuilder xmlFormat = new StringBuilder();
				standardXML = String.format(XMLTemplate, CHANNEL_NAME, CHANNEL_GUID,
						REPOSITORY, REPOSITORY_GUID,AUTHORID, "",
						OWNERID, "", LOCALECODE, DATECREATED_MILLIS,
						REVIEW_TIMESTAMP, REVIEW_TIMESTAMP_MILLIS,
						STARTTIMESTAMP, STARTTIMESTAMP_MILLIS, ENDTIMESTAMP,
						ENDTIMESTAMP_MILLIS, EVENTSTARTTIMESTAMP,
						EVENTSTARTTIMESTAMP_MILLIS, EVENTENDTIMESTAMP,
						EVENTENDTIMESTAMP_MILLIS);
				standardXML = xmlFormat.append(standardXML).append(handleMultiTag()).toString();
			}
		} catch (Exception ex) {
			standardXML = "";
			ex.printStackTrace();
			logger
					.error("Error in handling the general xml "
							+ ex.getMessage());

		}
		return standardXML;
	} // end of generalXML
	
	public String handleMultiTag() {
		
		StringBuffer tempXML = new StringBuffer();

		// Adding Tags for View
		tempXML.append("<VIEWS>");
		StringTokenizer stringTokenizer = new StringTokenizer(VIEW_REFKEY, "+");
		
		while (stringTokenizer.hasMoreElements()) {
			tempXML.append("<VIEW>").append("<REFERENCE_KEY>")
					.append(stringTokenizer.nextElement().toString())
					.append("</REFERENCE_KEY>").append("<GUID></GUID>")
					.append("</VIEW>");
		}		
		tempXML.append("</VIEWS>");
		
		
		// Adding Tags for Category
		
		if(CATEGORY_REFKEY != null && !"".equals(CATEGORY_REFKEY.trim())){
			tempXML.append("<CATEGORIES>");
			stringTokenizer = new StringTokenizer(CATEGORY_REFKEY, "+");		 
			while (stringTokenizer.hasMoreElements()) {	
				tempXML.append("<CATEGORY>").append("<REFERENCE_KEY>")
						.append(stringTokenizer.nextElement().toString())
						.append("</REFERENCE_KEY>").append("<GUID></GUID>")
						.append("</CATEGORY>");
			}
			tempXML.append("</CATEGORIES>");
		}// end of if

		//Adding Tags User Group
		if(USERGROUP_REFKEY != null && !"".equals(USERGROUP_REFKEY.trim())){
			tempXML.append("<SECURITY>");
			stringTokenizer = new StringTokenizer(USERGROUP_REFKEY, "+");		 
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
} // end of class
