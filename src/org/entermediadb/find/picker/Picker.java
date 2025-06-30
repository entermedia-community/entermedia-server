package org.entermediadb.find.picker;

public class Picker
{

	protected String fieldTargetType = "asset";
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
	
	public void setTargetType(String inTargetType) {
		fieldTargetType = inTargetType;
	}
	
	public String getTargetType() {
		return fieldTargetType;
	}
	
	
}
