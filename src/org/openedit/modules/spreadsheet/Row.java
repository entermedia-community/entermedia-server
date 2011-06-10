package org.openedit.modules.spreadsheet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;

public class Row extends BaseComponent
{
	protected List fieldCells;

	public List getCells()
	{
		if (fieldCells == null)
		{
			fieldCells = new ArrayList();
			
			for (Iterator iterator = getData().elementIterator(); iterator.hasNext();)
			{
				Element	data = (Element) iterator.next();
				Cell cell = new Cell();
				cell.setData(data);
				fieldCells.add( cell);
			}
		}

		return fieldCells;
	}

	public void setCells(List inCells)
	{
		fieldCells = inCells;
	}
	public Cell getCell(String inCellId)
	{
		List cells = getCells();
		for (Iterator iterator = cells.iterator(); iterator.hasNext();)
		{
			Cell cell = (Cell) iterator.next();
			if(cell.getId().equals(inCellId))
			{
				return cell;
			}
		}
		return null;
	}

	public Cell setValue(String inCellId, String inValue)
	{
		Cell cell = getCell(inCellId);
		cell.setText(inValue);
		return cell;
	}

	public void setHeight(String inValue)
	{
		if( inValue != null)
		{
			if( inValue.endsWith("px"))
			{
				inValue = inValue.substring(0, inValue.length() - 2);
			}
			getData().addAttribute("height", inValue);
		}
	}
	public int getHeight()
	{
		String h = get("height");
		if( h == null)
		{
			return -1;
		}
		return Integer.parseInt(h);
	}
	public int getEditHeight()
	{
		String h = get("height");
		if( h == null)
		{
			return 5;
		}
		return Integer.parseInt(h)/11;
	}

	public void insertCol(int inColIndex)
	{
		List cells = new ArrayList( getData().elements("cell") );
		for (int i = inColIndex; i < cells.size(); i++)
		{
			//remove all the old ones
			Element old = (Element)cells.get(i);
			getData().remove(old);
			old.setParent(null);
		}
		//add in new one
		Element cell = getData().addElement("cell");
		cell.addAttribute("ID", getId() + "_"  +cells.size()); //TODO: Make unique
		
		
		//add in old ones again
		for (int i = inColIndex; i < cells.size(); i++)
		{
			Element old = (Element)cells.get(i);
			getData().add(old);
		}
		fieldCells = null;
	}
}
