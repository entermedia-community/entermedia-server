package org.entermediadb.asset.upload;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.openedit.page.Page;

public class FileUploadItem
{
	protected String fieldName;
	protected FileItem fieldFileItem;
	protected Page fieldSavedPage;
	protected Map fieldProperties;
	protected int fieldCount;
	protected boolean base64;
	public boolean isBase64() {
		return base64;
	}
	public void setBase64(boolean base64) {
		this.base64 = base64;
	}
	public String getName()
	{
		return fieldName;
	}
	public void setName(String inName)
	{
		fieldName = inName;
	}
	public FileItem getFileItem()
	{
		return fieldFileItem;
	}
	public void setFileItem(FileItem inFileItem)
	{
		fieldFileItem = inFileItem;
	}
	public Page getSavedPage()
	{
		return fieldSavedPage;
	}
	public void setSavedPage(Page inSavedPage)
	{
		fieldSavedPage = inSavedPage;
	}
	public Map getProperties()
	{
		if (fieldProperties == null)
		{
			fieldProperties = new HashMap();
		}
		return fieldProperties;
	}
	public void setProperties(Map inProperties)
	{
		fieldProperties = inProperties;
	}
	public String get(String inString)
	{
		return (String)getProperties().get(inString);
	}
	public void putProperty(String inFieldName, String inString) {
		// TODO Auto-generated method stub
		getProperties().put(inFieldName, inString);
	}
	public int getCount() {
		return fieldCount;
	}
	public void setCount(int inCount) {
		fieldCount = inCount;
	}
	public String getFieldName() {
	return getFileItem().getFieldName();
	}
}
