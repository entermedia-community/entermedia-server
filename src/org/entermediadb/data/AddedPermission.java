package org.entermediadb.data;

import org.openedit.Data;

public class AddedPermission
{
	public String getPermissionType()
	{
		return fieldPermissionType;
	}
	public void setPermissionType(String inPermissionType)
	{
		fieldPermissionType = inPermissionType;
	}
	public Data getData()
	{
		return fieldData;
	}
	public void setData(Data inData)
	{
		fieldData = inData;
	}
	public boolean isEditor()
	{
		return fieldEditor;
	}
	public void setEditor(boolean inEditor)
	{
		fieldEditor = inEditor;
	}
	protected String fieldPermissionType;
	protected Data fieldData;
	protected boolean fieldEditor;
	
}
