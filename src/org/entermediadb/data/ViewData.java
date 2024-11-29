package org.entermediadb.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;

public class ViewData extends BaseData implements CatalogEnabled
{
	protected SearcherManager fieldSearchManager;
	protected Collection<ViewData> fieldChildren;

	protected String fieldCatalogId;
	

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public SearcherManager getSearchManager()
	{
		return fieldSearchManager;
	}

	public void setSearchManager(SearcherManager inSearchManager)
	{
		fieldSearchManager = inSearchManager;
	}
	
	public Collection<ViewData> getChildren()
	{
		if (fieldChildren == null)
		{
			Searcher viewsearcher = getSearchManager().getSearcher(getCatalogId(), "view");
			Collection hits = getSearchManager().query(getCatalogId(), "view").exact("parentid", getId()).search();
			fieldChildren = new ArrayList<ViewData>(hits.size());
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				Data view = (Data) iterator.next();
				ViewData data = (ViewData)viewsearcher.loadData(view);
				fieldChildren.add(data);
			}
		}
		return fieldChildren;
	}
}
