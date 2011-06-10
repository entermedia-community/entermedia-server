package org.openedit.entermedia.xmldb;

import java.util.Iterator;

import org.dom4j.Element;
import org.openedit.data.BaseArchive;

public class BaseXmlArchive extends BaseArchive
{
	/**
	 * @param elm
	 */
	protected void deleteElements(Element elm, String inName)
	{
		// remove old pricing
		for (Iterator iter = elm.elements(inName).iterator(); iter.hasNext();)
		{
			Element element = (Element) iter.next();
			elm.remove(element);
		}
	}

}
