package org.openedit.modules.spreadsheet;

public class Col extends BaseComponent
{
	public void setWidth(String inValue)
	{
		if( inValue != null)
		{
			if( inValue.endsWith("px"))
			{
				inValue = inValue.substring(0, inValue.length() - 2);
			}
			getData().addAttribute("width", inValue);
		}
	}
	public int getWidth()
	{
		String h = get("width");
		if( h == null)
		{
			return 100;
		}
		return Integer.parseInt(h);
	}

}
