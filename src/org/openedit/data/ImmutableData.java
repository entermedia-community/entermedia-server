package org.openedit.data;

import org.openedit.Data;

public class ImmutableData extends BaseData
{
	protected Data fieldTarget;

	public ImmutableData(Data inTarget)
	{
		setTarget(inTarget);
	}
	public Data getTarget()
	{
		return fieldTarget;
	}

	public void setTarget(Data inTarget)
	{
		fieldTarget = inTarget;
	}
	
	public void setProperty(String inKey, String inValue)
	{
		if( inKey.equals("assetmodificationdate"))
		{
			getTarget().setProperty(inKey, inValue);
			return;
		}
		String existing = getTarget().get(inKey);
		if( existing == null)
		{
			getTarget().setProperty(inKey, inValue);
		}
	}
			
}
