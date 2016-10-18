/*
 * Created on Jun 30, 2006
 */
package org.entermediadb.data;

import java.util.Iterator;

import org.dom4j.Element;
import org.openedit.CatalogEnabled;
import org.openedit.event.WebEventListener;

public class BaseDataArchive implements CatalogEnabled
{
		protected String fieldCatalogId;
		protected WebEventListener fieldWebEventListener;

		public String getCatalogId()
		{
			return fieldCatalogId;
		}

		public void setCatalogId(String inCatalogId)
		{
			fieldCatalogId = inCatalogId;
		}
		public WebEventListener getWebEventListener()
		{
			return fieldWebEventListener;
		}
		public void setWebEventListener( WebEventListener inWebEventListener)
		{
			fieldWebEventListener = inWebEventListener;
		}
		protected void deleteElements(Element elm, String inName)
		{
			//Is there a faster way to do this? 			elm.setContent(arg0);
			for (Iterator iter = elm.elements(inName).iterator(); iter.hasNext();)
			{
				Element element = (Element) iter.next();
				elm.remove(element);
			}
		}

}
