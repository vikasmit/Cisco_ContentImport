package com.smart.content.transformation;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class ListTypeAttributeDBDao {
	static Connection connection;
	Properties prop = null;
	private Logger logger = Logger.getLogger("ListTypeAttributeDBDao");
	String jdbcDriver="",jdbcURL = "",jdbcUser ="",jdbcPwd = "";

	/**
	 * @param args
	 */
/*	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// Reading database details for entry in table
		try {
			ListTypeAttributeDBDao classObj = new ListTypeAttributeDBDao();
			classObj.loadResource();
			classObj.connectDatabase();
			classObj.execute("FAQ_TYPE","Diagnosis");
		}  catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

	}*/
	// COnstructor
	public ListTypeAttributeDBDao(){
		if (connection==null) {
			boolean loadResFlag = loadResource();
			if(loadResFlag){
				connection = connectDatabase();
			}
		}		
	}
	/**
	 * Get the connection to database Responsibility table
	 * 
	 * @return Connection
	 */
	private Connection connectDatabase() {

		try {
			Class.forName(jdbcDriver);
			connection = DriverManager
					.getConnection(jdbcURL, jdbcUser, jdbcPwd);
			return connection;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String execute(String refKey, String type) {
		String recordSetData = "failure";
		ResultSet rs = null;
		PreparedStatement stmt = null;
		String sqlQuery="";
		String display = "",value="",guid="";
		try {
				 
				sqlQuery = "SELECT VALUE,NAME,DATALISTITEMID FROM INQUIRAIMDB.DATALISTITEMRESOURCE where DATALISTITEMID IN (SELECT RECORDID from INQUIRAIMDB.DATALISTITEM where DATALISTID = (SELECT RECORDID from INQUIRAIMDB.DATALIST where REFERENCEKEY = '"+refKey+"' AND NAME = '"+type+"'))";
				stmt = connection.prepareStatement(sqlQuery);
				rs = stmt.executeQuery();
				if (rs != null) {
						while (rs.next()) {
							display = rs.getString("NAME");
							value = rs.getString("VALUE");
							guid = rs.getString("DATALISTITEMID");
						}
						recordSetData = "<VALUE><![CDATA["+value+"]]></VALUE><DISPLAY><![CDATA["+display+"]]></DISPLAY><GUID><![CDATA["+guid+"]]></GUID>";
				} else {
					recordSetData = "failure";
				}
		} catch (SQLException e) {
			logger.error("Database Read Error");
			logger.error(e.toString(), e);
			return "failure";
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();

			} catch (SQLException e) {
				logger.error("Database Close Error");
			}

		}
		return recordSetData;
	} // end of function
	
	private boolean loadResource(){
		boolean response = false;
		
		try {
			// FOR LOG FILE
			ClassLoader classLoader =  
				   Thread.currentThread().getContextClassLoader();  
				PropertyConfigurator.configure(classLoader.getResource("log4j.properties"));  
				
			prop = new Properties();
    		// properties file in classes folder
    		InputStream inObj = getClass().getResourceAsStream("/"+ getClass().getPackage().getName().replace('.', '/')+ "/db_resource.properties");
    		prop.load(inObj);
    		
    		jdbcDriver = prop.getProperty("jdbcDriver");
			jdbcURL = prop.getProperty("jdbcURL");
			jdbcUser = prop.getProperty("jdbcUser");
			jdbcPwd = prop.getProperty("jdbcPwd");
			
    		response = true;
    		

    	} catch (IOException ex) {
    		ex.printStackTrace();
    		response = false;
    		logger.error("Error in loading the propery file for db resource "+ex.getMessage());
        }		
		return response;
	}
	
} // end of class
