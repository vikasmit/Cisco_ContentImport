package com.smart.content.extractor.intercept;

import java.io.FileNotFoundException;
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
		
		String packageName = "/"+getClass().getPackage().getName().replace('.', '/');
		
		// properties file in classes folder
		InputStream in = ParserConfigurationUtility.class
				.getResourceAsStream(packageName+"/parser_configuration.properties");
		prop.load(in);
		return prop;
	}
}
