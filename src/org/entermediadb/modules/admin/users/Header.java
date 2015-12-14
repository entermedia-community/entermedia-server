/*
 * Created on Aug 15, 2005
 */
package org.entermediadb.modules.admin.users;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openedit.util.FileUtils;

public class Header
{
	protected Map fieldHeaderNames;
	protected BufferedReader fieldReader;
	
	public Map getHeaderNames()
	{
		if (fieldHeaderNames == null)
		{
			fieldHeaderNames = new HashMap();
		}
		return fieldHeaderNames;
	}
	public void setHeaders(String[] inHeaders)
	{
		for (int i = 0; i < inHeaders.length; i++)
		{
			Integer integer = new Integer(i);
			getHeaderNames().put(integer,inHeaders[i]);
		}
	}

	public int getIndex(String inName)
	{
		Map headerNames = getHeaderNames();
		for (Iterator iter = headerNames.keySet().iterator(); iter.hasNext();)
		{
			Integer index = (Integer)iter.next();
			String name = (String) headerNames.get(index);
			if ( name.equalsIgnoreCase(inName))
			{
				return index.intValue();
			}
		}
		return -1;
	}
	public String getColumn(int inIndex)
	{
		String name = (String)getHeaderNames().get(new Integer(inIndex));
		if ( name != null)
		{
			return name;
		}
		return null;
	}
	public int getSize()
	{
		return getHeaderNames().size();
	}
	public void loadColumns( File inFile ) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader( inFile ) );
		setReader(reader);

		String line = getReader().readLine();
		if ( line == null)
		{
			FileUtils.safeClose(getReader());
			return;
		}
		String[] cells = line.split("\t");

		setHeaders(cells);
	}
	
	public Row getNextRow() throws IOException
	{
		String line = getReader().readLine();
		if ( line == null)
		{
			FileUtils.safeClose(getReader());
			return null;
		}
		String[] cells = line.split("\t");
		Row row = new Row();
		row.setHeader(this);
		row.setData( cells );
		return row;
	}
	public BufferedReader getReader()
	{
		return fieldReader;
	}
	public void setReader(BufferedReader inReader)
	{
		fieldReader = inReader;
	}
}
