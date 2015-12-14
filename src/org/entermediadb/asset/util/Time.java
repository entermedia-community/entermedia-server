package org.entermediadb.asset.util;

import java.text.NumberFormat;

public class Time
{
	protected String fieldType; //h m s
	protected float fieldValue; //23.0
	protected boolean fieldBefore; //before after
	
	public boolean isBefore()
	{
		return fieldBefore;
	}
	public void setBefore(boolean inDirection)
	{
		fieldBefore = inDirection;
	}
	public String getType()
	{
		return fieldType;
	}
	public void setType(String inType)
	{
		fieldType = inType;
	}
	public float getValue()
	{
		return fieldValue;
	}
	public void setValue(float inValue)
	{
		fieldValue = inValue;
	}
	public String toString()
	{
		NumberFormat format = NumberFormat.getNumberInstance();
		format.setGroupingUsed(true);
		format.setMaximumFractionDigits(2);
		format.setMinimumFractionDigits(0);
		String now =  format.format(getValue())+ " " + getType();
		if( isBefore())
		{
			now = "-" + now;
		}
		return now;
	}
}
