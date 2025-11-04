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
	
	public String getTargetFieldId() {
		return fieldTargetFieldId;
	}

	public void setTargetFieldId(String inTargetFieldId)
	{
		fieldTargetFieldId = inTargetFieldId;
	}
	
	public String getTargetType() {
		return fieldTargetType;
	}
	
	public void setTargetType(String inTargetType) {
		fieldTargetType = inTargetType;
	}
	
	public boolean isPickerEnabled(String inTargetType, String inModuleId)
	{
		if (inModuleId != null && inTargetType != null)
			{
			if (inModuleId.equals(getTargetModuleId()) && inTargetType.equals(getTargetType()))
			{
				return true;
			}
		}
		return false;
	}
	
	
}
