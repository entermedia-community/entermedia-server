package org.entermediadb.data;

import org.openedit.Data;
import org.openedit.data.BaseData;

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
	
	@Override
	public void setValue(String inKey, Object inValue)
	{
		if( inKey.equals("assetmodificationdate"))
		{
			getTarget().setValue(inKey, inValue);
			return;
		}
		Object existing = getTarget().getValue(inKey);
		if( existing == null || "".equals(existing))
		{
			getTarget().setValue(inKey, inValue);
		}
	}
			
}
