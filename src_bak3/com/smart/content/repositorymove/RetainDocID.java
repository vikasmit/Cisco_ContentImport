package com.smart.content.repositorymove;

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
/**
 * This class is responsible to update the IM table for DOC ID,
 * 
 * @author Infogain Date: 15 May, 14
 * 
 */

public class RetainDocID {
	static Connection connection;
	Properties prop = null;
	private Logger logger = Logger.getLogger("RetainDocID");
	String jdbcDriver="",jdbcURL = "",jdbcUser ="",jdbcPwd = "";

/*	*//**
	 * @param args
	 */
	
/*	public static void main(String[] args) {
		// TODO Auto-generated method stub
		// Reading database details for entry in table
		try {
			RetainDocID classObj = new RetainDocID();
			classObj.loadResource();
			classObj.connectDatabase();
			classObj.execute("3f2c8d59064c4f0290323f57fb32dffe","AB10","AB11");
			classObj.execute("3f2c8d59064c4f0290323f57fb32dffe","AB11","AB12");
			String str = classObj.executeForUserID("Rakesh", "Dadwal");
			System.out.println("DONE "+str);
		}  catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

	}*/
	
	// Constructor
	public RetainDocID(){
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

	/**
	 * Execute the update statement in CONTENT, CONTENTTEXT and CONTENTTEXTPUB table
	 * @param String record id of the newly created document, String doc id which needs to retain
	 * @return Success or Failure
	 */

	public String execute(String NewDocRecordID,String newDocID, String retainDocID) {
		String opsFlag = "failure";
		PreparedStatement stmtContent = null,stmtContentText = null,stmtContentTextPub=null;
		
		try {
			String updateQueryContent = "UPDATE CONTENT SET DOCUMENTID = '" + retainDocID + "'" + " WHERE RECORDID = '" + NewDocRecordID + "'" + " AND DOCUMENTID = '"+newDocID+"'";
			String updateQueryContentText  = "UPDATE CONTENTTEXT SET DOCUMENTID = '" + retainDocID + "'" + " WHERE CONTENTID = '" + NewDocRecordID + "'" + " AND DOCUMENTID = '"+newDocID+"'";
			String updateQueryContentTextPub = "UPDATE CONTENTTEXTPUB SET DOCUMENTID = '" + retainDocID + "'" + " WHERE CONTENTID = '" + NewDocRecordID + "'" + " AND DOCUMENTID = '"+newDocID+"'";

			
			stmtContent = connection.prepareStatement(updateQueryContent);
			int contentRowUpdated = stmtContent.executeUpdate();
			
			stmtContentText = connection.prepareStatement(updateQueryContentText);
			int contentTextRowUpdated = stmtContentText.executeUpdate();

			stmtContentTextPub = connection.prepareStatement(updateQueryContentTextPub);
			int contentTextPubRowUpdated = stmtContentTextPub.executeUpdate();

			
			if (contentRowUpdated == 1 && contentTextRowUpdated == 1 && contentTextPubRowUpdated ==1 ) {
				opsFlag = "success";
			} else {
				opsFlag = "failure";
			}
			
		} catch (SQLException e) {
			logger.error("Database Read Error");
			logger.error(e.toString(), e);
			return "failure";
		} finally {
			try {
				if (stmtContent != null)
					stmtContent.close();
				if (stmtContentText != null)
					stmtContentText.close();
				if (stmtContentTextPub != null)
					stmtContentTextPub.close();

			} catch (SQLException e) {
				logger.error("Database Close Error");
			}

		}
		return opsFlag;
	} // end of function
	
	/**
	 * Execute the select statement in USERINFORMATION table
	 * @param String first Name and String last name
	 * @return userid/login. It can be blank
	 */

	public String executeForUserID(String firstName, String lastName) {
		String userLogin = "";
		ResultSet rs = null;
		PreparedStatement stmt = null;
		String sqlQuery="";
		try {	
				sqlQuery = "SELECT LOGIN FROM USERINFORMATION where FIRSTNAME LIKE '"+firstName+"%' AND LASTNAME LIKE '%"+lastName+"'";
				stmt = connection.prepareStatement(sqlQuery);
				rs = stmt.executeQuery();
				if (rs != null) {
					while (rs.next()) {
						userLogin = rs.getString("LOGIN");
					}
					
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
		return userLogin;
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
			
    		//InputStream inObj = getClass().getResourceAsStream("/db_resource.properties");
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