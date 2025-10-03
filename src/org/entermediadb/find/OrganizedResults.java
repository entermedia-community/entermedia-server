package org.entermediadb.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.FilterNode;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;

public class OrganizedResults
{
	
	protected int fieldSizeOfResults;
	
	
	public int getSizeOfResults()
	{
		return fieldSizeOfResults;
	}

	public void setSizeOfResults(int inSizeOfResults)
	{
		fieldSizeOfResults = inSizeOfResults;
	}

	protected MediaArchive fieldMediaArchive;
	
	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	public HitTracker getEntityResults()
	{
		return fieldEntityResults;
	}

	public void setEntityResults(HitTracker inEntityResults)
	{
		fieldEntityResults = inEntityResults;
	}

	public HitTracker getAssetResults()
	{
		return fieldAssetResults;
	}

	public void setAssetResults(HitTracker inAssetResults)
	{
		fieldAssetResults = inAssetResults;
	}

	public List<Data> getModules()
	{
		if( fieldModules == null)
		{
			fieldModules = new ArrayList();
			if (getEntityResults() != null)
			{
				FilterNode nodes = (FilterNode)getEntityResults().getActiveFilterValues().get("entitysourcetype");
				for (Iterator iterator = nodes.getChildren().iterator(); iterator.hasNext();)
				{
					FilterNode onetype = (FilterNode) iterator.next();
					String searchtype = onetype.getId();
					Data module = getMediaArchive().getCachedData("module", searchtype);
					fieldModules.add(module);
				}
			}
			if(getAssetResults() != null)
			{
				Data module = getMediaArchive().getCachedData("module", "asset");
				fieldModules.add(module);
			}
			sortModules(fieldModules);
		}
		return fieldModules;
	}

	public void setModules(List<Data> inModules)
	{
		fieldModules = inModules;
	}

	protected HitTracker fieldEntityResults;
	protected HitTracker fieldAssetResults;
	protected List<Data> fieldModules;
	
	public int getCount(Data inModule)
	{
		return getCount(inModule.getId());
	}
	public int getCount(String inModuleId)
	{
		if( inModuleId.equals("asset") )
		{
			return getAssetResults().size();
		}
		FilterNode node = (FilterNode)getEntityResults().getActiveFilterValues().get("entitysourcetype");
		if( node == null)
		{
			return 0;
		}
		int count = node.getCount(inModuleId);
		return count;
	}
	
	public void reloadIfNeeded()
	{
		//search in searcher
		HitTracker tracker = getEntityResults().getSearcher().search(getEntityResults().getSearchQuery());
		setEntityResults(tracker);

		if( getAssetResults() != null)
		{
			HitTracker hits = getAssetResults().getSearcher().search(getAssetResults().getSearchQuery());
			setAssetResults(tracker);
		}
	}
	
	protected Map<String,HitTracker> fieldByModule;
	
	public Map<String,HitTracker> getByType() 
	{
		if( fieldByModule == null)
		{
			fieldByModule = new HashMap();

			for (Iterator iterator = getModules().iterator(); iterator.hasNext();)
			{
				Data module = (Data) iterator.next();
				String searchtype = module.getId();
				
				HitTracker newvalues = null;
				if (searchtype.equals("asset"))
				{
					newvalues = getAssetResults();
				}
				else
				{
					//search for one page of the actual results... Seems crazy but only way to see right results?
					Searcher searcher = getMediaArchive().getSearcher(searchtype);
					SearchQuery query = searcher.createSearchQuery();
	
					Collection queryTerms = getEntityResults().getSearchQuery().getTerms();
					query.copyTerms(queryTerms);
					
					query.setResultType(searchtype);
					
	//				newvalues.setActiveFilterValues(  getEntityResults().getActiveFilterValues() );
	//				newvalues.setHitsPerPage(getSizeOfResults());
	//				newvalues.setSearcher(searcher);
	//				newvalues.setSearchQuery(query);
	
					query.setHitsName("organized" + searchtype);
					newvalues = searcher.search(query);
					//Set filters?
					//set name and session session
				}	
				fieldByModule.put(searchtype,newvalues);
	//				newvalues.setHitsName(searchtype +"idhits");
	//				newvalues.setSessionId(searchtype + "idhits"+ archive.getCatalogId());
					//inReq.putSessionValue(newvalues.getSessionId(), newvalues);
				
			}
//			Collection values = getEntityResults().getSearchQuery().getValues("searchtypes");
//			
//			if(getAssetResults() != null && values != null && values.contains("asset"))
//			{
//				Collection onepage = getAssetResults().getPageOfHits();
//				fieldByModule.put( "asset",onepage);
//			}
		}
		return fieldByModule;
	}
	public HitTracker getByType(String inModuleId, WebPageRequest inReq)
	{
		HitTracker existing = getByType(inModuleId);
		inReq.putSessionValue(existing.getSessionId(), existing);
		Searcher searcher = existing.getSearcher();
		HitTracker updated = searcher.cachedSearch(inReq, existing.getSearchQuery()); //Might not do anything
		return updated;
	}
	public HitTracker getByType(String inModuleId)
	{
		HitTracker hits = getByType().get(inModuleId);
		return hits;
	}

	protected void sortModules(List<Data> foundmodules)
	{
		if (!foundmodules.isEmpty()) {
			Collections.sort(foundmodules,  new Comparator<Data>() 
			{ 
			    // Used for sorting in ascending order of 
			    // roll number 
			    public int compare(Data a, Data b) 
			    { 
			    	int a1 = Integer.parseInt(a.get("ordering"));
			    	int b1 = Integer.parseInt(b.get("ordering"));
			    	if( a1 == b1)
			    	{
			    		return 0;
			    	}
			        if ( a1 > b1 ) {
			        	return 1;
			        }
			        return -1;
			    } 
			    
			});
		}
		//log.info("Complete sort" + foundmodules);
	}
	
	public boolean isEmpty()
	{
		boolean isemty = true;
		if(getEntityResults() != null && !getEntityResults().isEmpty() )
		{
			isemty = false;
		}	
		if( getAssetResults() != null && !getAssetResults().isEmpty()) 
		{
			isemty = false;
		}
		return isemty;
	}
	
	public int size()
	{
		int size = 0;
		if(getEntityResults() != null && !getEntityResults().isEmpty() )
		{
			size = getEntityResults().size();
		}	
		if( getAssetResults() != null && !getAssetResults().isEmpty()) 
		{
			size = size + getAssetResults().size();
		}
		return size;
	}

}
