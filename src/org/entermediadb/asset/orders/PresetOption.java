package org.entermediadb.asset.orders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openedit.Data;

public class PresetOption
{
	protected Collection fieldOrderItems = new ArrayList();
	protected Collection fieldDowloadPaths = new ArrayList();

	public Collection getDowloadPaths()
	{
		return fieldDowloadPaths;
	}

	public void setDowloadPaths(Collection inDowloadPaths)
	{
		fieldDowloadPaths = inDowloadPaths;
	}
	public void addDownloadPath(String inPath)
	{
		getDowloadPaths().add(inPath);
	}
	
	public void addDownloadPath(String inDownloadPath, String inSavePath, Data inAsset)
	{
		Map<String, Object> object = new HashMap<String, Object>();
		object.put("asset", inAsset);
		object.put("downloadpath", inDownloadPath);
		object.put("savetopath", inSavePath);
		getDowloadPaths().add(object);
	}
	
	
	protected Data fieldPreset; //render type
	public Data getPreset()
	{
		return fieldPreset;
	}

	public void setPreset(Data inPreset)
	{
		fieldPreset = inPreset;
	}
	public Collection getOrderItems()
	{
		return fieldOrderItems;
	}

	public void setOrderItems(Collection inItems)
	{
		fieldOrderItems = inItems;
	}
	public void addOrderItem(Data inItem)
	{
		getOrderItems().add(inItem);
	}

}
