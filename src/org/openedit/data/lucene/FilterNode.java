package org.openedit.data.lucene;

import java.util.ArrayList;
import java.util.List;

import org.openedit.data.BaseData;

public class FilterNode extends BaseData
{
	
	protected List fieldChildren;
	protected boolean isSelected;
	
	public List getChildren()
	{
		if (fieldChildren == null)
		{
			fieldChildren = new ArrayList();
			
		}

		return fieldChildren;
	}
	public void setChildren(List inChildren)
	{
		fieldChildren = inChildren;
	}
	public boolean isSelected()
	{
		return isSelected;
	}
	public void setSelected(boolean inIsSelected)
	{
		isSelected = inIsSelected;
	}
	public void addChild(FilterNode inFilterNode)
	{
		getChildren().add(inFilterNode);
		
	}
	
	
	
}
