package org.openedit.entermedia.cluster;

import java.util.Iterator;

import org.dom4j.Element;
import org.openedit.xml.ElementData;

public class Node extends ElementData
{
	public Node()
	{

	}
	
	public Node(Element inVal)
	{
		super(inVal);
	}
	
	public String get(String inId)
	{
		String val = getElement().attributeValue(inId);
		if( val == null)
		{
			for (Iterator iterator = getElement().elementIterator(); iterator.hasNext();)
			{
				Element ele = (Element)iterator.next();
				if( inId.equals( ele.attributeValue("id") ) )
				{
					val = ele.getTextTrim();
					break;
				}
			}
		}
		return val;
	}
	
}
