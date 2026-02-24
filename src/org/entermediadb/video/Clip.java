package org.entermediadb.video;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openedit.data.ValuesMap;
import org.openedit.util.MathUtils;


public class Clip implements Comparable
{
	protected ValuesMap fieldData;
	
	public ValuesMap getData()
	{
		return fieldData;
	}
	public void setData(Map inData)
	{
		fieldData = new ValuesMap(inData);
	}
	public long getStart()
	{
		Long d = getData().getLong("timecodestart");
		if( d == null)
		{
			return 0L;
		}
		return d;
	}
	public String getSpeaker()
	{
		String speaker = (String)getData().get("speaker");
		return speaker;
	}
	public String getLabel()
	{
		String cliplabel = (String)getData().get("cliplabel");
		return cliplabel;
	}
	public long getLength()
	{
		Long d = getData().getLong("timecodelength");
		if( d == null)
		{
			return 0L;
		}
		return d;
	}
	
	public String getStartSecondsAndHours()
	{
		return MathUtils.toDuration(getStart());
	}
	
	public Object getValue(String inKey)
	{
		return getData().get(inKey);
	}
	
	public Map getOtherDetails()
	{
		Map other = new HashMap();
		for (Iterator iterator = getData().keySet().iterator(); iterator.hasNext();)
		{
			String id = (String) iterator.next();
			if( id.equals("cliplabel") || id.equals("timecodestart") ||id.equals("timecodelength") )
			{
				continue;
			}
			other.put(id,getData().get(id));
		}
		
		return other;
	}
	@Override
	public int compareTo(Object inO)
	{
		Clip clip = (Clip)inO;
		double d1 = getStart();
		double d2 = clip.getStart();
		if(d1 < d2){
			return -1;
		}
		if(d2 < d1){
			return 1;
		}
		return 0;
	}
}
