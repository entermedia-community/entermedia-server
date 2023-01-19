package org.entermediadb.asset.orders;

import java.util.ArrayList;
import java.util.Collection;

import org.openedit.Data;

public class RenderTypeOptions
{
	protected Data fieldRenderType;
	protected Collection fieldPresetOptions = new ArrayList();
	
	public Collection getPresetOptions()
	{
		return fieldPresetOptions;
	}

	public void setPresetOptions(Collection inPresetOptions)
	{
		fieldPresetOptions = inPresetOptions;
	}

	public Data getRenderType()
	{
		return fieldRenderType;
	}

	public void setRenderType(Data inRenderType)
	{
		fieldRenderType = inRenderType;
	}
	public String getRenderTypeId()
	{
		if( getRenderType() != null)
		{
			return getRenderType().getId();
		}
		return "none";
	}

	public void addPresetOption(PresetOption inPresetoption)
	{
		getPresetOptions().add(inPresetoption);
	}
	
	
}
