package org.entermediadb.asset.modules;

import org.openedit.Data;

public class ParentChildPair
{
	String fieldParentModuleId;
	public String getParentModuleId()
	{
		return fieldParentModuleId;
	}
	public void setParentModuleId(String inParentModuleId)
	{
		fieldParentModuleId = inParentModuleId;
	}
	public Data getChildModule()
	{
		return fieldChildModule;
	}
	public void setChildModule(Data inChildModule)
	{
		fieldChildModule = inChildModule;
	}
	Data fieldChildModule;
	
}
