package org.openedit.modules.spreadsheet;

import java.io.BufferedReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.entermedia.util.ImportFile;
import org.openedit.xml.XmlFile;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.util.PathUtilities;

public class SpreadSheet
{
	protected List fieldRows;
	protected List fieldColumns;
	protected Row fieldHeader;
	protected String fieldId;
	protected List fieldColorRules;
	
	public List getColorRules()
	{
		if (fieldColorRules == null)
		{
			fieldColorRules = new ArrayList();
			for (Iterator iterator = getXmlFile().getElements("colorrule"); iterator.hasNext();)
			{
				Element ruleElement = (Element) iterator.next();
				ColorRule rule = new ColorRule();
				rule.setColor(ruleElement.attributeValue("color"));
				rule.setContains(ruleElement.attributeValue("contains"));
				rule.setRow(Boolean.parseBoolean(ruleElement.attributeValue("row")));
				fieldColorRules.add(rule);
			}

		}
		return fieldColorRules;
	}

	public String pickColor(String inString)
	{
		for (Iterator iterator = getColorRules().iterator(); iterator.hasNext();)
		{
			ColorRule rule = (ColorRule) iterator.next();
			if (rule.matches(inString))
			{
				return rule.getColor();
			}
		}
		return null;
	}
	
	public String pickColorForRow(Row inRow)
	{
		for (Iterator iterator = getColorRules().iterator(); iterator.hasNext();)
		{
			ColorRule rule = (ColorRule) iterator.next();
			if (rule.matches(inRow))
			{
				return rule.getColor();
			}
		}
		return null;
	}
	
	public String getPath()
	{
		return getXmlFile().getPath();
	}
	public String getName()
	{
		return PathUtilities.extractFileName(getPath());
	}
	protected XmlFile fieldXmlFile;

	public List getRows(int inStarting)
	{
		return getRows().subList(inStarting, getRows().size());
	}
	
	public void clear()
	{
		fieldRows = null;
		fieldHeader = null;
		fieldColumns = null;
		
	}
	public List getRows()
	{
		if (fieldRows == null)
		{
			fieldRows = new ArrayList();
			int rowid = 1;
			for (Iterator iterator = getXmlFile().getElements("row"); iterator.hasNext();)
			{
				Element row = (Element) iterator.next();
				Row arow = new Row();
				arow.setData(row);
				arow.setLabel(String.valueOf(rowid));
				rowid++;
				fieldRows.add(arow);
			}
			//pad out empty rows
			int max = 30 - fieldRows.size();
			for (int i = 0; i < max; i++)
			{
				Row arow = addRowTo( getXmlFile().getRoot());
				fieldRows.add(arow);
			}
			checkColumns();
		}
		return fieldRows;
	}
	
	public Row addRow(int inNear)
	{
		Element data = getXmlFile().getRoot().addElement("row");
		Row therow = new Row();
		therow.setData(data);
		return therow;
	}

	public void setValue( String inRowId, String inCellId, String inValue)
	{
		Row row = getRow( inRowId);
		row.setValue(inCellId, inValue);
		//Element row = getXmlFile().getElementById(inRowId);
		//Element cell = row.elementByID(inCellId);
		//cell.setText(inValue);
		//System.out.println(inRowId + " " + inCellId + " " + inValue);
	}
	
	public Row getRow(String inRowId)
	{
		List rows = getRows();
		for (Iterator iterator = rows.iterator(); iterator.hasNext();)
		{
			Row row = (Row) iterator.next();
			if(row.getId().equals(inRowId))
			{
				return row;
			}
		}
		return null;
	}
	
	public XmlFile getXmlFile()
	{
		return fieldXmlFile;
	}

	public void setXmlFile(XmlFile inXmlFile)
	{
		fieldXmlFile = inXmlFile;
	}

	public List getColumns()
	{
		if (fieldColumns == null)
		{
			fieldColumns = new ArrayList();
			//pad out empty rows
			Element columns = getXmlFile().getRoot().element("columns");
			if( columns == null)
			{
				columns = getXmlFile().getRoot().addElement("columns");
				for (int i = 0; i < 10; i++)
				{
					addColumn(columns);
				}
			}
			for (Iterator iterator = columns.elementIterator("cell"); iterator.hasNext();)
			{
				Element row = (Element) iterator.next();
				Col	 acol = new Col();
				acol.setData(row);
				fieldColumns.add(acol);
			}
			relabelColumns();
		}

		return fieldColumns;
	}
	protected void addColumn(Element columns)
	{
		char label = (char)('A' + getColumnCount());
		String val = String.valueOf( label );
		Element cell = columns.addElement("cell");
		cell.setText(val);
		cell.addAttribute("ID", "col_"+ val );
	}
	public int getColumnCount()
	{
		int count = getColumns().size();
		return count;
	}

	
	public Row getHeader()
	{
		if( fieldHeader == null)
		{
			fieldHeader = (Row)getRows().get(0);
		}
		return fieldHeader;
	}
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public void saveRowHeight(String inRowId, String inValue)
	{
		Row row = getRow( inRowId);
		row.setHeight(inValue);
		
		
	}
	public void saveColumnWidth(String inColId, String inValue)
	{
		Col col = getColumn(inColId);
		col.setWidth(inValue);
		
		
	}
	public Col getColumn(String inColId)
	{
		for (Iterator iterator = getColumns().iterator(); iterator.hasNext();)
		{
			Col col = (Col) iterator.next();
			if( col.getId().equals(inColId))
			{
				return col;
			}
		}
		return null;
	}
	public void insertColumn(int inColIndex)
	{
		//TODO: insert column into list
		Element data = getXmlFile().getRoot().element("columns");
		List cells = new ArrayList( data.elements("cell") );
		for (int i = inColIndex; i < cells.size(); i++)
		{
			//remove all the old ones
			Element old = (Element)cells.get(i);
			data.remove(old);
			old.setParent(null);
		}
		//add in new one
		Element cell = data.addElement("cell");
		//cell.addAttribute("id", String.valueOf( incrementCounter() ));
		
		//add in old ones again
		for (int i = inColIndex; i < cells.size(); i++)
		{
			Element old = (Element)cells.get(i);
			data.add(old);
		}
		fieldColumns = null;
		
		relabelColumns();

		//Fix rows
		for (Iterator iterator = getRows().iterator(); iterator.hasNext();)
		{
			Row row = (Row) iterator.next();
			row.insertCol(inColIndex);
		}
		checkColumns();
		clear();
	}
	protected void relabelColumns()
	{
		// TODO Auto-generated method stub
		char label = 'A';
		for (int i = 0; i < getColumns().size(); i++)
		{
			String val = String.valueOf( label );
			Col col = (Col)getColumns().get(i);
			col.setText(val);
			col.setId("col_" + val);
			label = (char)(label + 1);
		}
	}
	protected void checkColumns()
	{
		//make sure we have enough cells for each row
		
		int cells = getHeader().getCells().size();
		int col = getColumnCount();
		while( cells > col)
		{
			//add a spare column
			Element columns = getXmlFile().getRoot().element("columns");
			addColumn(columns);
			
			fieldColumns = null;
			col = getColumnCount();
		}
		for (Iterator iterator = getRows().iterator(); iterator.hasNext();)
		{
			Row row = (Row) iterator.next();
			//only if columns not big enought already
			for (int j = row.getCells().size(); j < col; j++)
			{
				Cell cell = new Cell();
				cell.setData(row.getData().addElement("cell"));
				cell.setId(row.getId() + "_" + j);
				row.getCells().add(cell);
			}
		}
		
	}
	public void insertRow(int inRowIndex)
	{
		Element data = getXmlFile().getRoot();
		List rows = new ArrayList( data.elements("row") );
		for (int i = inRowIndex; i < rows.size(); i++)
		{
			//remove all the old rows
			Element old = (Element)rows.get(i);
			data.remove(old);
			old.setParent(null);
		}
		addRowTo(data);
		
		//add in old ones again
		for (int i = inRowIndex; i < rows.size(); i++)
		{
			Element old = (Element)rows.get(i);
			data.add(old);
		}
		relabelRows();
		clear();
	}
	protected Row addRowTo(Element data)
	{
		Row arow = new Row();
		arow.setData(data.addElement("row"));  //
		//need to relabel all the rows
		int rowid = incrementCounter();
		arow.setId(String.valueOf( rowid));

		return arow;
	}
	protected void relabelRows()
	{
		for (int i = 0; i < getRows().size(); i++)
		{
			Row row = (Row)getRows().get(i);
			row.setLabel(String.valueOf(i));
		}
	}
	
	//make sure you save after calling this
	protected synchronized int  incrementCounter()
	{
		String id = getXmlFile().getRoot().attributeValue("idcounter");
		if( id == null)
		{
			id = "100";
		}
		int c = Integer.parseInt(id);
		c++;
		getXmlFile().getRoot().addAttribute("idcounter", String.valueOf(c));
		return c;
	}
	public void deleteColumn(String inColId)
	{
		Element data = getXmlFile().getRoot().element("columns");
		
		Element col =  data.elementByID(inColId);
		if( col != null)
		{
			List cols = data.elements("cell");
			int index = cols.indexOf(col);
			data.remove(col);
			relabelColumns();
			if ( index > -1)
			{
				//remove all the cells for each row at that index
				for (Iterator iterator = getRows().iterator(); iterator.hasNext();)
				{
					Row row = (Row) iterator.next();
					//only if columns not big enought already
					Element element = (Element)row.getData().elements().get(index);
					row.getData().remove(element);
				}
			}
		}
		clear();
	}
	public void deleteRow(String inRowId)
	{
		Element data = getXmlFile().getRoot();
		//Element row =  (Element)data.selectSingleNode("//cell[@id='" + inRowId +"']");
		Element row =  data.elementByID(inRowId);
		if( row != null)
		{
			data.remove(row);
		}
		fieldRows = null;
	}
	public void importCsv(Page inPage)
	{
		// TODO Auto-generated method stub
		ImportFile imfile = new ImportFile();
		Element backup = getXmlFile().getRoot(); 
		try
		{
			imfile.load(new BufferedReader( inPage.getReader()));
			
			Element root = DocumentHelper.createElement("sheet");
			getXmlFile().setRoot(root);
			
			Element colums = root.addElement("columns");
			Element row = root.addElement("row");
			row.addAttribute("ID",String.valueOf(incrementCounter()));
			for (Iterator iterator = imfile.getHeader().getHeaderNames().iterator(); iterator.hasNext();)
			{
				String name = (String) iterator.next();
				Element cell = row.addElement("cell");
				cell.addAttribute("ID",String.valueOf(incrementCounter()));
				cell.setText(name);
			}
			
			org.openedit.entermedia.util.Row trow = null;
			while( (trow = imfile.getNextRow()) != null )
			{
				row = root.addElement("row");
				row.addAttribute("ID",String.valueOf(incrementCounter()));
				for (int i = 0; i < trow.getData().length; i++)
				{
					String value = trow.getData(i);
					Element cell = row.addElement("cell");
					cell.addAttribute("ID",String.valueOf(incrementCounter()));
					cell.setText(value);
				}				
			}
			clear();
			checkColumns();
			relabelColumns();
		}
		catch( Exception ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			imfile.close();
		}
	}
	public String getLastEditedBy()
	{
		return getXmlFile().getRoot().attributeValue("lastedited");
	}
	public void setLastEditedBy(String inLastEditedBy)
	{
		getXmlFile().getRoot().addAttribute("lastedited", inLastEditedBy);
	}
	public String getLastEdit()
	{
		Date edited = new Date( getXmlFile().getLastModified() );
		return DateFormat.getDateTimeInstance().format(edited);
	}
	public int getWidth()
	{
		int total = 0;
		for (Iterator iterator = getColumns().iterator(); iterator.hasNext();)
		{
			Col col = (Col) iterator.next();
			total = total + col.getWidth();
		}
		return total;
	}
	
	public void addColorRule(ColorRule inColorRule)
	{
		Element element = getXmlFile().getRoot().addElement("colorrule");
		element.addAttribute("color", inColorRule.getColor());
		element.addAttribute("contains", inColorRule.getContains());
		element.addAttribute("row", Boolean.toString(inColorRule.isRow()));
		
		getColorRules().add(inColorRule);
	}

	public void addColorRule(String inColor, String inContains)
	{
		ColorRule rule = new ColorRule();
		rule.setColor(inColor);
		rule.setContains(inContains);
		addColorRule(rule);
	}

	public void removeRule(String inContains)
	{
		Element root = getXmlFile().getRoot();
		for (Iterator iterator = root.elementIterator("colorrule"); iterator.hasNext();)
		{
			Element element = (Element) iterator.next();
			if (element.attributeValue("contains").equals(inContains))
			{
				iterator.remove();
			}
		}
		
		for (Iterator iterator = getColorRules().iterator(); iterator.hasNext();)
		{
			ColorRule rule = (ColorRule) iterator.next();
			if (rule.getContains().equals(inContains))
			{
				iterator.remove();
			}
		}
	}
}
