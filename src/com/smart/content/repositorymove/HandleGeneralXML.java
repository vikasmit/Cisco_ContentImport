package com.smart.content.repositorymove;


import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * This class is responsible for adding the value like channelname,repository etc in general xml
 * 
 * @author Infogain
 * 
 */

public class HandleGeneralXML {
	private Logger logger = Logger.getLogger("HandleGeneralXML");

	private String CHANNEL_NAME = "",CHANNEL_GUID = "",REPOSITORY = "",REPOSITORY_GUID = "",AUTHORID = "",AUTHOR_USERNAME = "",OWNERID = "",OWNERUSERNAME = "",LOCALECODE = "",DATECREATED_MILLIS="",REVIEW_TIMESTAMP = "",
	REVIEW_TIMESTAMP_MILLIS = "",STARTTIMESTAMP = "",STARTTIMESTAMP_MILLIS = "",ENDTIMESTAMP = "",ENDTIMESTAMP_MILLIS = "",EVENTSTARTTIMESTAMP = "",EVENTSTARTTIMESTAMP_MILLIS = "",EVENTENDTIMESTAMP = "",
	EVENTENDTIMESTAMP_MILLIS = "";

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
	}

	public String generalXML(String XMLTemplate) {
		String standardXML = "";
		try {
			if ("".equals(CHANNEL_NAME)	|| "".equals(REPOSITORY) || "".equals(LOCALECODE)) {
				standardXML = "";
				logger.info("Required value is/are null "+" Channel Name "+CHANNEL_NAME+" REPOSITORY "+REPOSITORY + " LOCALECODE "+LOCALECODE);
			} else {
/*				standardXML = String.format(XMLTemplate, CHANNEL_NAME, CHANNEL_GUID,
						REPOSITORY, REPOSITORY_GUID,AUTHORID, AUTHOR_USERNAME,
						OWNERID, OWNERUSERNAME, LOCALECODE, DATECREATED_MILLIS,
						REVIEW_TIMESTAMP, REVIEW_TIMESTAMP_MILLIS,
						STARTTIMESTAMP, STARTTIMESTAMP_MILLIS, ENDTIMESTAMP,
						ENDTIMESTAMP_MILLIS, EVENTSTARTTIMESTAMP,
						EVENTSTARTTIMESTAMP_MILLIS, EVENTENDTIMESTAMP,
						EVENTENDTIMESTAMP_MILLIS);
*/			
				standardXML = String.format(XMLTemplate, CHANNEL_NAME, CHANNEL_GUID,
						REPOSITORY, REPOSITORY_GUID,"", "",
						"", "", LOCALECODE, DATECREATED_MILLIS,
						REVIEW_TIMESTAMP, REVIEW_TIMESTAMP_MILLIS,
						STARTTIMESTAMP, STARTTIMESTAMP_MILLIS, ENDTIMESTAMP,
						ENDTIMESTAMP_MILLIS, EVENTSTARTTIMESTAMP,
						EVENTSTARTTIMESTAMP_MILLIS, EVENTENDTIMESTAMP,
						EVENTENDTIMESTAMP_MILLIS);

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
} // end of class
