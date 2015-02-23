package com.smart.content.transformation;

public class ProductInfoDTO {
	
	private String documentId;
	private String productName;
	private String secondLevel;
	private String parentCategory;
	private String serviceOwner;
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public String getSecondLevel() {
		return secondLevel;
	}
	public void setSecondLevel(String secondLevel) {
		this.secondLevel = secondLevel;
	}
	public String getParentCategory() {
		return parentCategory;
	}
	public void setParentCategory(String parentCategory) {
		this.parentCategory = parentCategory;
	}
	public String getServiceOwner() {
		return serviceOwner;
	}
	public void setServiceOwner(String serviceOwner) {
		this.serviceOwner = serviceOwner;
	}

}
