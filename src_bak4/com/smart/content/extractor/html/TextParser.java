package com.smart.content.extractor.html;

import java.io.File;  
import java.io.FileInputStream;  
import org.w3c.dom.DocumentFragment;  
import org.w3c.dom.Node;  
import org.w3c.dom.NodeList;  
import org.xml.sax.InputSource;  
import org.cyberneko.html.parsers.DOMFragmentParser;  
import org.apache.log4j.Logger;
import org.apache.xerces.dom.CoreDocumentImpl;  
  
  
public class TextParser{  
	
    private FileInputStream fin = null;  
    private StringBuffer textBuffer = null;  
    private InputSource inSource = null;  
    private Logger logger = Logger.getLogger("TextParser");
   
    //Gets the text content from Nodes recursively  
    
    /**
  		 * 
  		 * method is used gets the text content from Nodes recursively 
  		 * 
  		 * @param node
  		 */
    
    public void processNode(Node node) {  
        if (node == null) return;  
  
        //Process a text node  
        if (node.getNodeType() == node.TEXT_NODE) { 
        	
        	//System.out.println("value of node is::::::::"+node.toString());
        	
        	textBuffer.append(node.getNodeValue());  
        } else if (node.hasChildNodes()) {  
            //Process the Node's children  
  
            NodeList childList = node.getChildNodes();  
            int childLen = childList.getLength();  
  
            for (int count = 0; count < childLen; count ++)  
                processNode(childList.item(count));  
        }  
        else return;  
    }  
  
    // Extracts text from HTML Document  
    
    
    /**
		 * 
		 * method is used  extracts text from HTML Document  
		 * 
		 * @param fileName
		 * @param textBuffer
		 * @since 09-Sep-2013
		 * @author chandan1.kumar
		 *
		 */
    
    
    
    public String htmltoText(String fileName) {  
  
        DOMFragmentParser parser = new DOMFragmentParser();  
  
        //System.out.println("Parsing text from HTML file " + fileName + "....");  
        File f = new File(fileName);  
  
        if (!f.isFile()) {  
            //System.out.println("File " + fileName + " does not exist.");
        	logger.error("File " + fileName + " does not exist.");
            return null;  
        }  
  
        try {  
            fin = new FileInputStream(f);  
        } catch (Exception e) {  
            logger.error("Unable to open HTML file " + fileName + " for reading.");  
            return null;  
        }  
  
        try {  
            inSource = new InputSource(fin);  
        } catch (Exception e) {  
        	logger.error("Unable to open Input source from HTML file " + fileName);  
            return null;  
        }  
  
        CoreDocumentImpl codeDoc = new CoreDocumentImpl();  
        DocumentFragment doc = codeDoc.createDocumentFragment();  
  
        try {  
            parser.parse(inSource, doc);  
        } catch (Exception e) {  
        	logger.error("Unable to parse HTML file " + fileName + " Error Message is "+e.getMessage());  
            return null;  
        }  
  
        textBuffer = new StringBuffer();  
  
        //Node is a super interface of DocumentFragment, so no typecast needed  
        processNode(doc);  
  
        //System.out.println("Done.");  
  
        return textBuffer.toString();  
    }  
}  