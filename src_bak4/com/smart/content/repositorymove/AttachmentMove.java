package com.smart.content.repositorymove;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * This class is the execution point to read the txt file in order to move all
 * the attachment to new folder structure created after Rebuild of xml from IM
 * console
 * 
 * @author Infogain
 * 
 */

public class AttachmentMove {
	private Logger logger = Logger.getLogger("AttachmentMove");
	private FileUtil pathList = null;	
	//private int numOfRecords = 0;
	Properties prop = null;
	private String channelName="",localeCode="",versionNumber="",actualSourcePath="",olderDocID="",attachmentInputLocation="",destDirName="",docCreatedPath="";
	private boolean isAttachment=false;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int sleep = 0;
		int batchsize = 1;
		
		AttachmentMove classObj = new AttachmentMove();
		try {
			classObj.pathList = new FileUtil("src/attachmentMoveTrack.txt");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		boolean response = classObj.loadResource();
		if(response){
			try {
				classObj.processFile(sleep, batchsize);
			} catch (Exception e) {
				classObj.logger.info(e.getMessage());
				return;
			}
		}
		classObj.logger.info("Terminating and exiting app");
	}
	/**
	 * 
	 * method is used for loading the property files.
	 * 
	 * @param 
	 * @return
	 */

	private boolean loadResource(){
		boolean response = false;
		
		try {
			// FOR LOG FILE
			ClassLoader classLoader =  
				   Thread.currentThread().getContextClassLoader();  
				PropertyConfigurator.configure(classLoader.getResource("log4j.properties"));  
				
    		// properties file parser_configuration.properties
			prop = new Properties();
    		InputStream inObjTransformation =getClass().getResourceAsStream("/"+ getClass().getPackage().getName().replace('.', '/')+ "/parser_configuration.properties"); 
    			//getClass().getResourceAsStream("/parser_configuration.properties");
    		prop.load(inObjTransformation);

    		channelName = prop.getProperty("IM_CHANNEL_NAME");
    		localeCode = prop.getProperty("IM_LOCALECODE");
    		docCreatedPath = prop.getProperty("IM_RESOURCE_ROOT_PATH");
    		response = true;
    		

    	} catch (IOException ex) {
    		ex.printStackTrace();
    		response = false;
    		logger.error("Error in loading the propery file "+ex.getMessage());
        }		
		return response;
	}
	
	/**
	 * 
	 * method is used for processing input file for all records
	 * 
	 * @param 
	 * @return
	 */

	private void processFile(int sleep, int batchsize)
			throws InterruptedException {
		boolean delStageFlag=false,delLiveFlag=false;
		try {

			while (true) {
				String processData = null;
				try {
					processData = pathList.getNextLine();
					if (processData == null) {
						return;
					} 
					else 
					{						
						if(!"".equals(processData.trim())){
							long startTime = System.nanoTime();
							String[] fields = processData.split(",");
							// Required this for getting the path for finding the new folder structure, version etc. also for deletion
							actualSourcePath = docCreatedPath+fields[0];
							// Help for getting the new folder structure as this will be there as sub dir
							olderDocID = fields[1];
							// Actual path of original content.xml where all attachment exists.
							attachmentInputLocation = fields[2];
							
							isAttachment = false;
							
							// Checking whether records has any attachment
							attachmentCheck(attachmentInputLocation);
							
							// processing furthur only when there is attachment else simple deleting the older folder structure
							if(isAttachment){
								
								versionNumber = actualSourcePath.substring(actualSourcePath.indexOf(localeCode+"/")+(localeCode+"/").length());
								
								// Getting the parent dir to search the new folder structure
								File filterPath = new File(actualSourcePath.substring(0,actualSourcePath.indexOf(channelName)+channelName.length()));
								handlePathForRetainedDocID(filterPath);
								
								logger.info("destDirName "+destDirName);
								
								if(!"".equals(destDirName)){
									// Staging
									delStageFlag = copyFiles(new File(attachmentInputLocation),new File(destDirName));
									// Live
									delLiveFlag = copyFiles(new File(attachmentInputLocation),new File(destDirName.replace("staging","live").replace(versionNumber, "")));
									if(delStageFlag && delLiveFlag){
										logger.info("Success "+olderDocID);
										deleteFolderStructure();
									}
									else{
										logger.info("Failure for "+olderDocID + ". Please manually copy the attachment from dir "+actualSourcePath + " to new path");
									}
								}
								else {
									
								}
							} // end of isAttachment
							else{
								logger.info("Success "+olderDocID);
								deleteFolderStructure();
							}
							long endTime = System.nanoTime();
							long duration = endTime - startTime;
							logger.info("Time Taken in NanoSeconds for "+olderDocID+ " --- "+duration);
							
						}
					} // end of else
				} catch (Exception e) {
					logger.error("Error processing data file :: " + processData);
					e.printStackTrace();
				}
			}// While
		} catch (Exception e) {
			logger.error("error in processing the data");
		} finally {
		}
	}
	
	/**
	 * 
	 * method is used for searching the new folder structure created by IM during rebuild xml
	 * 
	 * @param 
	 * @return
	 */
	private String handlePathForRetainedDocID(File sourceLocation) {
		destDirName = "";
		
		try {
			if (sourceLocation.isDirectory()) {

				File[] listOfFiles = sourceLocation.listFiles((FileFilter) FileFilterUtils.directoryFileFilter());
				
				for (File file : listOfFiles) {
					// Sec level Iteration
					File folderSecLevel = new File(file.getAbsolutePath());
					File[] listOfFilesSecLevel = folderSecLevel.listFiles((FileFilter) FileFilterUtils.directoryFileFilter());
					for (File fileSecLevel : listOfFilesSecLevel) {
						if(fileSecLevel.getName().equalsIgnoreCase(olderDocID)){
							destDirName = fileSecLevel.getAbsolutePath() + "\\"+localeCode+"\\"+versionNumber;
							break;
						}										
					}
					if(!"".equals(destDirName)){
						break;
					}
				} // end for 

				
			} // end of if
			
			
		} catch (Exception ex) {
			destDirName = "";
			logger.error("Error in finding the Dir " + ex.getMessage());
		}
		
		return destDirName;
	}

	
	/**
	 * 
	 * method is responsible for copying the file to new folder structure
	 * 
	 * @param 
	 * @return
	 */
	
	private boolean copyFiles(File srcDir, File destDir) throws IOException {
		boolean result = false;
		try {
			FileUtils.copyDirectory(srcDir, destDir, new FileFilter() {
			    public boolean accept(File pathname) {
			        String name = pathname.getName();
			        if (name.endsWith(".xml") || name.endsWith(".XML"))
			            return false;
			        //return !(name.equals("Sub3") && pathname.isDirectory());
			        return true;
			    }
			});
			result = true;
		} catch (Exception e) {
			result = false;
			logger.error("Error in copyAttachment :: "
					+ e.getMessage());

		} 
		return result;
	} // end of method
	
	private boolean attachmentCheck(String path)  {
		boolean result = true;
		
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();
		for (File file : listOfFiles) {
			if (!file.toString().endsWith(".xml") && file.isFile()) {
				isAttachment = true;
				logger.info("file "+file.getName());
			}
			// In case of secure dir
			else if(file.isDirectory()){
				attachmentCheck(file.getAbsolutePath());
			}
		}

		return result;
	}
	
	private void deleteFolderStructure(){
		String dirDeleteStaging = "",dirDeleteLive="";
		dirDeleteStaging = actualSourcePath.substring(0,actualSourcePath.indexOf(localeCode)-1);
		FileUtils.deleteQuietly(new File(dirDeleteStaging));
		dirDeleteLive = dirDeleteStaging.replace("staging", "live");
		FileUtils.deleteQuietly(new File(dirDeleteLive));

	}

} // end of class
