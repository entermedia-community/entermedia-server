/*
 * Created on Aug 15, 2005
 */
package org.entermediadb.asset.util;


public class Row
{
	protected Header fieldHeader;
	protected String[] fieldData;
	public String[] getData()
	{
		return fieldData;
	}
	public void setData(String[] inData)
	{
		fieldData = inData;
	}
	public String get(String inName)
	{
		int index = getHeader().getIndex(inName);
		if ( index == -1)
		{
			return null;
		}
		String data = getData(index);
		if( data != null)
		{
			return data.trim();
		}
		return null;
	}
	public String getData(int index)
	{
		return getData()[index];
	}
	public Header getHeader()
	{
		return fieldHeader;
	}
	public void setHeader(Header inHeader)
	{
		fieldHeader = inHeader;
	}
	public String[] getRemainder()
	{
		String[] rem = new String[getData().length - getHeader().getSize() ];
		int index = 0;
		for (int i = getHeader().getSize(); i < getData().length; i++)
		{
			rem[index++] = getData(i);
		}
		return rem;
	}
	public void set(String inName, String inValue)
	{
		int index = getHeader().getIndex(inName);
		if ( index == -1)
		{
			return;
		}
		getData()[index] = inValue;

	}
}
