package org.entermediadb.asset.util;

public class MathTool
{
	public int toInteger(double inVal)
	{
		long val = Math.round(inVal);
		return (int)val;
	}
}
