package org.entermediadb.projects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.links.CategoryWebTreeModel;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.data.QueryBuilder;

public class LibraryWebTreeModel extends CategoryWebTreeModel implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(LibraryWebTreeModel.class);
	
	//Map collectioncache = new HashMap();
	
	protected Collection<Category> getCategoriesForLibrary(String inLibraryId)
	{
			String id = getUserProfile().getUserId() + "_" + inLibraryId;

			Collection<Category> categoryojb = (Collection)getMediaArchive().getCacheManager().get("librarycollectionstree",id);
			if( categoryojb == null)
			{
				Set allowedcats = new HashSet(getUserProfile().getViewCategories());
 
				//@deprecate this code
				for (Iterator iterator = getMediaArchive().listPublicCategories().iterator(); iterator.hasNext();)
				{
					Category publiccat = (Category) iterator.next();
					allowedcats.add(publiccat);
				}
				if( allowedcats.isEmpty() )
				{
					allowedcats.add("NONE");
				}
				
				QueryBuilder builder = getMediaArchive().query("librarycollection")
						.exact("library",inLibraryId)
						.orgroup("parentcategories",allowedcats)
						//.notgroup("parentcategories", catshidden)
						.notgroup("collectiontype", Arrays.asList("0","2","3"));
				Collection results = builder.search();

				for (Iterator iterator = results.iterator(); iterator.hasNext();)
				{
					Data col = (Data) iterator.next();
					String catid = col.get("rootcategory");
					if( catid != null)
					{
						Category cat = getMediaArchive().getCategory(catid);
						if( cat != null)
						{
							categoryojb.add(cat);
						}
					}
				}
				getMediaArchive().getCacheManager().put("librarycollectionstree",id,categoryojb);
			}
			return categoryojb;
	}

	
	public List listChildren(Object inParent)
	{
		if (inParent == null)
		{
			return Collections.EMPTY_LIST;
		}
		List ok = new ArrayList();
		
		if( inParent instanceof String)
		{
			//Might be a libraryid?
			Collection cats = getCategoriesForLibrary((String)inParent);
			for (Iterator iter = cats.iterator(); iter.hasNext();)
			{
				Category cat = (Category) iter.next();
				if (okToAdd(cat))
				{
					ok.add(cat);
				}
			}
		}
		else
		{
			Category parent = (Category) inParent;
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
		}
		return ok;
	}


}
