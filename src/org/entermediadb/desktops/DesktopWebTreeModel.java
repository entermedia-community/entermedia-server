package org.entermediadb.desktops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.BaseCategory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.links.CategoryWebTreeModel;
import org.openedit.CatalogEnabled;
import org.openedit.OpenEditException;
import org.openedit.util.PathUtilities;

public class DesktopWebTreeModel  extends CategoryWebTreeModel implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(DesktopWebTreeModel.class);
	
	protected Category fieldRoot;
	
	public Object getRoot()
	{
		if (fieldRoot == null)
		{
			fieldRoot = new BaseCategory("index","My Computer");
			fieldRoot.setProperty("categorypath", "/");
		}

		return fieldRoot;
	}
	
	protected String getUserId()
	{
		return getUserProfile().getUserId();
	}
	
	protected List<Category> getCategoriesForPath(Category inCat)
	{
			String inPath = inCat.getCategoryPath();
			if( !inPath.startsWith("/"))
			{
				inPath = ((BaseCategory)getRoot()).getCategoryPath() + inPath;
			}
			String id = getUserId() + "_" + inPath;
			log.info("Loading " + inPath);

			List<Category> subfolders = (List)getMediaArchive().getCacheManager().get("desktoptree",id);
			if( subfolders == null)
			{
				//GOTO The desktop API and get files
				Desktop desktop = getMediaArchive().getProjectManager().getDesktopManager().getDesktop(getUserId());
				if( desktop == null)
				{
					//Let them know,desktop not available
					log.error("No desktop");
					return null;
				}
				
				Map filesandfolders = null;
				if( inCat == getRoot() )
				{
					filesandfolders = desktop.getTopLevelFolders(getMediaArchive());
				}
				else
				{
					String abspath = inCat.get("abspath");
					if( abspath == null)
					{
						throw new OpenEditException("abs path is empty ");
					}
					filesandfolders = desktop.getLocalFiles(getMediaArchive(), abspath);
				}
				if( filesandfolders == null)
				{
					//Let them know desktop not available
					log.error("No data found for " + inPath);
					return Collections.EMPTY_LIST;
				}
				Collection 	folders = (Collection)filesandfolders.get("childfolders");
				for (Iterator iterator = folders.iterator(); iterator.hasNext();)
				{
					Map details = (Map) iterator.next();
					String name = (String)details.get("foldername");
					String catid = PathUtilities.extractId(name);
					BaseCategory newchild = new BaseCategory(catid,name);
					String abspath = (String)details.get("abspath");
					newchild.setProperty("abspath", abspath);
					inCat.addChild(newchild);
				}
				subfolders = inCat.getChildren();
				getMediaArchive().getCacheManager().put("desktoptree",id,subfolders);
			}
			return subfolders;
	}

	
	public Object findNodeById(Object inRoot, String inId)
	{
		String test = getId(inRoot);
		if (test.equals(inId))
		{
			return inRoot;
		}
		//check one level deep
		for (Iterator iterator = getChildren(inRoot).iterator(); iterator.hasNext();)
		{
			Object child = iterator.next();
			String id = getId(child);
			if (id.equals(inId))
			{
				return child;
			}
		}
		return null;
	}

	
	public List listChildren(Object inParent)
	{
		if (inParent == null)
		{
			return Collections.EMPTY_LIST;
		}
		List ok = new ArrayList();
	
		BaseCategory parent = (BaseCategory) inParent;
		if( !parent.hasLoadedChildren() )
		{
			List children = getCategoriesForPath(parent);
			parent.setChildren(children);
		}
		for (Iterator iter = parent.getChildren().iterator(); iter.hasNext();)
		{
			// If this is slow then we might consider only checking the top
			// cache the results in a cache map
			Category cat = (Category) iter.next();
			if (okToAdd(cat))
			{
				ok.add(cat);
			}
		}
		return ok;
	}


}
