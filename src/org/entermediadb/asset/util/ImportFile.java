/*
 * Created on Aug 15, 2005
 */
package org.entermediadb.asset.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.openedit.OpenEditException;

public class ImportFile
{
	protected List fieldRows;
	protected Header fieldHeader;
	protected Parser fieldParser;
	
	public ImportFile()
	{
	}
	
	public ImportFile(char delim) 
	{
		if( delim == '\t')
		{
			fieldParser = new TabParser();
		}
		else
		{
			fieldParser = new CSVReader();
		}
	}
	public void load(File inFile) throws Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(inFile));
		load( reader);
	}
	public void read(Reader inFile) throws Exception
	{
		BufferedReader reader = new BufferedReader(inFile);
		load( reader);
	}
	
	public void load(BufferedReader reader) throws Exception
	{
		//read in tabs or whatever into header object.
		//foreach row add a row object
		getParser().setBufferedReader(reader);
		
		String[] cells = getParser().readNext();
		setHeader(new Header());
		List valid = new ArrayList();
		for (int i = 0; i < cells.length; i++)
		{
			String cell = cells[i];
			if( cell == null || cell.trim().isEmpty() )
			{
				throw new OpenEditException("Empty header is not allowed");
			}
		}
		
		getHeader().setHeaders(cells);

		//		while( line != null)
//		{
//			if( line.startsWith("%Fieldnames"))
//			{
//				String row = reader.readLine(); //header
//				List cells = getParser().parse(row);
//				setHeader(new Header());
//				getHeader().setHeaders((String[]) cells.toArray(new String[cells.size()]));
//			}
//			else if( line.startsWith("%Data"))
//			{
//				break;
//			}
//			line = reader.readLine(); 
//		}
	}
	public List getAllRows() throws IOException
	{
		List all = new ArrayList();
		Row row = getNextRow();
        while (row != null)
        {
        	all.add(row);
        	row = getNextRow();
        }
        return all;
	}
	
	public Row getNextRow() throws IOException
	{
		String[] cells = getParser().readNext();
		if ( cells == null || cells.length == 0)
		{
			close();
			return null;
		}
		//line = line.replace('\u001e',','); //get rid of junk chars
		/*
		line = line.replace('\u001e',' '); //get rid of junk chars
		line = line.replace('\u0005',' '); //get rid of junk chars
		line = line.replace('\u0010',' '); //get rid of junk chars
		line = line.replace('\u001f',' '); //get rid of junk chars
		line = line.replace('\u000f',' '); //get rid of junk chars
		*/
		Row row = new Row();
		row.setHeader(getHeader());
		//row.setData((String[])cells.toArray(new String[cells.size()]));
		row.setData( cells );
		return row;
	}

	public Header getHeader()
	{
		return fieldHeader;
	}

	public void setHeader(Header inHeader)
	{
		fieldHeader = inHeader;
	}
	public Parser getParser()
	{
		if ( fieldParser == null)
		{
			fieldParser = new TabParser('\t');
			
		}
		return fieldParser;
	}
	public void setParser(Parser inParser)
	{
		fieldParser = inParser;
	}
	public void close()
	{
		getParser().close();
	}
	
}
