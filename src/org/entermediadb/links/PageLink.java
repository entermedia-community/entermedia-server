/*
 * Created on Sep 25, 2006
 */
package org.entermediadb.links;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openedit.OpenEditException;
import org.openedit.OpenEditRuntimeException;
import org.openedit.page.manage.PageManager;
import org.openedit.util.PathUtilities;

public class PageLink extends Link
{
	protected PageManager fieldPageManager;
	
	public List getChildren() 
	{
		try
		{
			if( fieldChildren == null)
			{
				List children = getPageManager().getChildrenNames(getPath());
				fieldChildren = new ArrayList();
				//String dir = PathUtilities.extractDirectoryPath( getUrl() );
				for (Iterator iterator = children.iterator(); iterator.hasNext();)
				{
					String path = (String) iterator.next();
					PageLink child = new PageLink();
					child.setPath(path);
					
					String id = child.getPath();
					id = PathUtilities.makeId(id);

					child.setId(id);
					child.setPageManager(getPageManager());
					fieldChildren.add(child);
				}
			}
			return fieldChildren;
		}
		catch ( OpenEditException ex)
		{
			throw new OpenEditRuntimeException(ex);
		}
		
	}
	//get the root link that searches for pages below it

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}


	
}
