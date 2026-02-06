package org.entermediadb.elasticsearch.searchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.data.ViewData;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.hittracker.Term;
import org.openedit.users.User;

public class ElasticViewSearcher extends ElasticListSearcher 
{
	@Override
	public Object searchById(String inId)
	{
		if(inId  == null) {
			return null;
		}
		//Make sure we call getCachedData for views
		ViewData data = (ViewData)super.searchById(inId);

		if( data != null)
		{
			if( data.getBoolean("deleted") )
			{
				return null;
			}
		}
		
		if( data == null)   //TODO: Cache lookup?
		{
			HitTracker hits = getSearcherManager().query(getCatalogId(),"viewtemplate").all().cachedSearch();
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				String id = hit.getId().replace("default", "");
				if( inId.endsWith(id) )
				{
					String moduleid = inId.substring(0,inId.length() - id.length());
					ViewData viewdata = loadViewData(moduleid,hit);
					return viewdata;
				}
			}
		}
		return data;
	}
	
	@Override
	public boolean hasChanged(HitTracker inTracker)
	{
		boolean changed = super.hasChanged(inTracker);
		return changed;
	}
	
	public void saveData(Data inData, User inUser)
	{
		super.saveData(inData,inUser);
	}

	@Override
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
		super.saveAllData(inAll, inUser);
	}

	@Override
	public void delete(Data inData, User inUser)
	{
		//Mark as deleted to hide the parents from showing up?
		inData.setValue("deleted",true);
		saveData(inData);
		//super.delete(inData, inUser);
	}
	
	@Override
	public HitTracker search(SearchQuery inSearch) throws OpenEditException
	{
		HitTracker actualviews = super.search(inSearch);

		Term moduleid = inSearch.getTermByDetailId("moduleid");
		Term systemdefined = inSearch.getTermByDetailId("systemdefined");
		
		if (moduleid == null)
		{
			return actualviews;
		}
		
		HitTracker combinedviews = actualviews;
		//Basic searches we can mege with template
		if( moduleid  != null && systemdefined != null)
		{
			//Now go deal with the standard list
			Searcher searcher = getSearcherManager().getSearcher(getCatalogId(),"viewtemplate");
			
			String	systemdefinedval = systemdefined.getValue();
			if( systemdefinedval == null)
			{
				systemdefinedval = "false";
			}
			SearchQuery q = searcher.query().exact("systemdefined",systemdefinedval).getQuery();
			
			if (inSearch.getTermByDetailId("rendertype") != null)
			{
				Term rendertype = inSearch.getTermByDetailId("rendertype").copy();
				rendertype.setDetail(searcher.getDetail("rendertype"));
				q.addTerm(rendertype);
			}
			
			Collection templateresults = searcher.search(q); 
		
			combinedviews = mergeResults( actualviews, moduleid.getValue(),templateresults);
		}		
		
		//Remove deleted
		List<ViewData> finallist = new ArrayList<ViewData>();
		for (Iterator iterator = combinedviews.iterator(); iterator.hasNext();)
		{
			Data d = (Data)iterator.next();
			//Make sure these aren't SearchHitData
			ViewData data = (ViewData) loadData(d);
			if( !data.getBoolean("deleted"))
			{
//				if( "asset".equals(moduleid.getValue() ) )
//				{
//					//Dont add the dataone
//					if( data.getId().equals("assetgeneral") ) //legacy id 
//					{
//						continue;
//					}
//				}
				finallist.add(data);
			}
		}
		
		Collections.sort(finallist,new Comparator<ViewData>(){
			@Override
			public int compare(ViewData inO1, ViewData inO2)
			{
				long i1 = inO1.getLong("ordering");
				long i2 = inO2.getLong("ordering");
				if( i1 == i2 )
				{
					return 0;
				}
				if( i1 < i2)
				{
					return -1;
				}
				else
				{
					return 1;
				}
			}
		});
		ListHitTracker combined = new ListHitTracker(finallist);
		combined.setIndexId(getIndexId());
		return combined;
	}
	
	//TODO Save deleted with special flag

	protected HitTracker mergeResults(HitTracker actualviews,String inModuleId, Collection baseresults)
	{
		ListHitTracker combinedviews = new ListHitTracker(); 
		
		for (Iterator iterator = actualviews.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			ViewData existing = (ViewData)loadData(hit);
			combinedviews.add(existing);
		}
		//Fix all the IDS and parents and module
		for (Iterator iterator = baseresults.iterator(); iterator.hasNext();)
		{
			Data template = (Data) iterator.next();
			ViewData extra = loadViewData(inModuleId,template);
			ViewData goodone = (ViewData)combinedviews.findData("id", extra.getId());
			if( goodone != null )
			{
				mergeDataInto(extra,goodone);
			}
			else
			{
				combinedviews.add(extra); //From the template area
			}
		}
		
		//Resort
		return combinedviews;
	}
	
	private void mergeDataInto(ViewData inExtra, ViewData inGoodone)
	{
		if( inGoodone.getValue("ordering") == null)
		{
			inGoodone.setValue("ordering",inExtra.getValue("ordering") );
		}
		if( inGoodone.getValue("rendertype") == null)
		{
			inGoodone.setValue("rendertype",inExtra.getValue("rendertype") );
		}
		if( inGoodone.getValue("parentview") == null)
		{
			inGoodone.setValue("parentview",inExtra.getValue("parentview") );
		}
		if( inGoodone.getValue("rendertable") == null)
		{
			inGoodone.setValue("rendertable",inExtra.getValue("rendertable") );
		}
	}


	protected ViewData loadViewData(String inModuleId, Data inTemplateView)
	{
		//	<property id="defaultentityshare" moduleid="default" systemdefined="true"  rendertype="entityshare" parentview="defaultentityexports" >Share</property>
		String id = inTemplateView.getId();
		id = id.replace("default",toId(inModuleId));
		inTemplateView.setId(id);

		inTemplateView.setValue("moduleid",inModuleId);

		String parentview = inTemplateView.get("parentview");
		if( parentview != null)
		{
			parentview = parentview.replace("default",toId(inModuleId));
			inTemplateView.setValue("parentview",parentview);
		}
		
		ViewData viewdata = (ViewData)loadData(inTemplateView);
		viewdata.setCatalogId(getCatalogId());
		return viewdata;
	}
	
	@Override
	public Data createNewData()
	{
		ViewData view = new ViewData();
		view.setSearchManager(getSearcherManager());
		view.setCatalogId(getCatalogId());
		return view;
	}
	
	public Data loadData(Data inHit)
	{
		if (inHit == null)
		{
			return null;
		}
		if (inHit instanceof ViewData)
		{
			return inHit;
		}
		else
		{
			ViewData data = (ViewData) createNewData();
			ValuesMap fields = inHit.getProperties();
			fields = checkTypes(fields);
			data.setProperties(fields);
			data.setId(inHit.getId());
			return data;
		}
	}

	
	@Override
	public void reindexInternal() throws OpenEditException {
		// TODO Auto-generated method stub
		super.reindexInternal();
	}
	
}
