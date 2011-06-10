/*
 * Created on Jun 30, 2006
 */
package org.openedit.data;

import org.openedit.event.WebEventListener;

public class BaseArchive
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
}
