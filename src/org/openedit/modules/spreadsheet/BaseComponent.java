package org.openedit.modules.spreadsheet;

import org.dom4j.Element;

public class BaseComponent
{
	Element fieldData;
	
	public String getId()
	{
		return get("ID");
	}

	public void setId(String inId)
	{
		setProperty("ID", inId);
	}
	
	public void setProperty(String inId, String inValue )
	{
		getData().addAttribute(inId, inValue);
	}

	public String get(String inKey)
	{
		return getData().attributeValue(inKey);
	}
	
	public Element getData()
	{
		return fieldData;
	}

	public void setData(Element inData)
	{
		fieldData = inData;
	}

	public String getLabel()
	{
		return get("label");
	}

	public void setLabel(String inLabel)
	{
		setProperty("label", inLabel);
	}
	public String getText()
	{
		return getData().getTextTrim();
	}

	public void setText(String inValue)
	{
		if( inValue == null)
		{
			getData().clearContent();
		}
		else
		{
			getData().setText(inValue);
		}
	}
}
