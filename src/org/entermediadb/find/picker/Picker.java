package org.entermediadb.find.picker;

public class Picker
{

	protected String fieldTargetModuleId;
	protected String fieldTargetFieldId;

	public String getTargetModuleId()
	{
		return fieldTargetModuleId;
	}

	public void setTargetModuleId(String inTargetModuleId)
	{
		fieldTargetModuleId = inTargetModuleId;
	}

	public void setTargetFieldId(String inTargetFieldId)
	{
		fieldTargetFieldId = inTargetFieldId;
	}
	
	public String getTargetFieldId() {
		return fieldTargetFieldId;
	}
	
	
}
