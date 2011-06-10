package org.openedit.modules.spreadsheet;

import java.util.Iterator;

public class ColorRule
{
	protected String fieldContains;
	protected String fieldColor;
	protected boolean fieldRow; //should apply to entire row
	
	public boolean isRow()
	{
		return fieldRow;
	}

	public void setRow(boolean inRow)
	{
		fieldRow = inRow;
	}

	public boolean matches(String inString)
	{
		return inString.toLowerCase().contains(getContains().toLowerCase());
	}
	
	public boolean matches(Row inRow)
	{
		if (isRow())
		{
			for (Iterator iterator = inRow.getCells().iterator(); iterator
					.hasNext();)
			{
				Cell cell = (Cell) iterator.next();
				if (matches(cell.getText()))
				{
					return true;					
				}
			}
		}
		return false;
	}
	
	public String getContains()
	{
		return fieldContains;
	}
	public void setContains(String inContains)
	{
		fieldContains = inContains;
	}
	public String getColor()
	{
		return fieldColor;
	}
	public void setColor(String inColor)
	{
		fieldColor = inColor;
	}
	
}
