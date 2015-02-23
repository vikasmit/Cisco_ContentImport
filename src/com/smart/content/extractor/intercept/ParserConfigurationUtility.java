package com.smart.content.extractor.intercept;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ParserConfigurationUtility {
	/**
	 * Load the resource file in application scope
	 * 
	 * @return Properties
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Properties readProp() throws FileNotFoundException, IOException {
		Properties prop = new Properties();
		
		/*String packageName = "/"+getClass().getPackage().getName().replace('.', '/');
		
		// properties file in classes folder
		InputStream in = ParserConfigurationUtility.class
				.getResourceAsStream(packageName+"/parser_configuration.properties");*/
		System.out.println("SMART_HOME>>>>>>>>>>>"+System.getProperty("SMART_HOME"));
		File file = new File((System.getProperty("SMART_HOME")+"parser_configuration.properties"));

		FileReader reader = new FileReader(file) ; 
		prop.load(reader);

		//prop.load(in);
		return prop;
	}
}
