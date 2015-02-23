package com.smart.content.transformation;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.inquira.client.serviceclient.IQServiceClient;
import com.inquira.client.serviceclient.IQServiceClientManager;
import com.inquira.client.serviceclient.request.IQCategoryRequest;
import com.inquira.im.ito.CategoryITO;
import com.inquira.im.ito.CategoryKeyITO;

public class CategoriesUtilServiceProvider {
	
	private static Map<String,String> categoryNameKeyMap= new HashMap<String,String>();
	static IQServiceClient client=null;
	private final static String DELIM="~~!~~"; 
	static{
		try{
				
		 client = IQServiceClientManager.connect("praveen", "praveen", "CISCO", "CISCO",
		                "http://localhost:8226/imws/WebObjects/IMWebServicesNG.woa/ws/RequestProcessor",
		                null
		                , true);	
         IQCategoryRequest categoryRequest= client.getCategoryRequest();
         CategoryITO level1CategoryITO=null;
         CategoryITO level2CategoryITO=null;
         CategoryITO level3CategoryITO=null;
         List<CategoryKeyITO> level2List=null;
         List<CategoryKeyITO> level3List=null;
         
         CategoryITO categoryITOMain=categoryRequest.getCategoryByReferenceKey("Service_Owner_10");
         categoryNameKeyMap.put(categoryITOMain.getName()+DELIM+"BLANK",categoryITOMain.getReferenceKey());
         List<CategoryKeyITO> level1List=categoryITOMain.getChildren();
         
         //iterating level1 categories
         for(CategoryKeyITO level1Key:level1List){
        	 
        	 level1CategoryITO=categoryRequest.getCategoryByReferenceKey(level1Key.getReferenceKey());
        	 categoryNameKeyMap.put(level1CategoryITO.getName()+DELIM+categoryITOMain.getName(), level1CategoryITO.getReferenceKey());
        	 
         }
         
       System.out.println(categoryNameKeyMap.size()+"<>");  
		
	}catch(Exception ee){
		
	}finally{
		if(client!=null)
		client.close();
	}
	
	}	
	public static String getCategoryKey(String keyAndParentCat) {
		
		
		
		return categoryNameKeyMap.get(keyAndParentCat);
		
	}
	
	
	public static void main(String[] args) {
		System.out.println(getCategoryKey("Service Owner"+DELIM+"BLANK"));
		
		System.out.println(getCategoryKey("Directory Services"+DELIM+"Service Owner"));
		
		System.out.println(getCategoryKey("ace video - jabber video"+DELIM+"Advanced Cisco Experience (ACE)"));
		
		System.out.println(getCategoryKey("advanced deployment environment (new)"+DELIM+"Advanced Cisco Experience (ACE)"));
		
		System.out.println(getCategoryKey("aironet-airespace-alpha"+DELIM+"Wireless LAN"));
		
		System.out.println(getCategoryKey("altiris agent"+DELIM+"Altiris Agent"));
		
		
	}
	
	

}
