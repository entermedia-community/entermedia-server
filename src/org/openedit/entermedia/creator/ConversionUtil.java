package org.openedit.entermedia.creator;

import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.Iterator;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;

import com.openedit.hittracker.DataHitTracker;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;

public class ConversionUtil {
	
	public static final String DECIMAL_FORMAT = "#.00";
	public static final String NOTHING_FOUND = "Not Converted";
	
	protected SearcherManager fieldSearcherManager;

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	
	protected HitTracker getParameterData(String inCatalogId, String inPresetId) throws Exception{
		SearcherManager sm = getSearcherManager();
		Searcher cpsearcher = sm.getSearcher(inCatalogId, "convertpreset");
		Searcher pdsearcher = sm.getSearcher(inCatalogId, "presetdata");
		Searcher ppsearcher = sm.getSearcher(inCatalogId, "presetparameter");
		Data cpdata = (Data) cpsearcher.searchById(inPresetId);
		String guid = cpdata.get("guid");
		Data pddata = (Data) pdsearcher.searchById(guid);
		String pdata = pddata.getId();
		SearchQuery sq = ppsearcher.createSearchQuery().append("parameterdata", pdata);
		HitTracker hits = ppsearcher.search(sq);
		return hits;
	}
	
	public Dimension getConvertPresetDimension(String inCatalogId, String inPresetId) throws Exception {
		Dimension dimension = new Dimension();
		double width = 1.0d;
		double height = 1.0d;
		HitTracker hits = getParameterData(inCatalogId,inPresetId);
		Iterator<?> itr = hits.iterator();
		while (itr.hasNext()){
			Data data = (Data) itr.next();
			if ("prefwidth".equals(data.get("name")) && data.get("value")!=null)
			{
				try
				{
					width = Double.parseDouble(data.get("value"));
				}catch (Exception e){}
			}
			else if ("prefheight".equals(data.get("name")) && data.get("value")!=null)
			{
				try
				{
					height = Double.parseDouble(data.get("value"));
				}catch (Exception e){}
			}
		}
		dimension.setSize(width,height);
		return dimension;
	}
	
	public String getConvertPresetAspectRatio(String inCatalogId, String inPresetId) throws Exception {
		Dimension dimension = getConvertPresetDimension(inCatalogId, inPresetId);
		return getConvertPresetAspectRatio(dimension);
	}
	
	public String getConvertPresetAspectRatio(Dimension inDimension){
		double height = inDimension.getHeight();
		double width = inDimension.getWidth();
		DecimalFormat format = new DecimalFormat(DECIMAL_FORMAT);
		String ratio = format.format(width / height) + ":1";
		return ratio;
	}
	
	public String getConvertPresetParameter(String inCatalogId, String inPresetId, String inParameter) throws Exception{
		HitTracker hits = getParameterData(inCatalogId,inPresetId);
		Iterator<?> itr = hits.iterator();
		while (itr.hasNext()){
			Data data = (Data) itr.next();
			if (inParameter.equals(data.get("name")) && data.get("value")!=null)
			{
				return data.get("value");
			}
		}
		return null;
	}
	
	public String getConversionStatus(String inCatalogId, String inAssetId, String inPresetId) throws Exception {
		String status = NOTHING_FOUND;
		SearcherManager sm = getSearcherManager();
		Searcher ctsearcher = sm.getSearcher(inCatalogId, "conversiontask");
		Searcher cssearcher = sm.getSearcher(inCatalogId, "convertstatus");
		SearchQuery sq = ctsearcher.createSearchQuery().append("presetid", inPresetId).append("assetid",inAssetId);
		sq.addSortBy("id");
		HitTracker hits = ctsearcher.search(sq);
		Iterator<?> itr = hits.iterator();
		while (itr.hasNext()){
			Data data = (Data) itr.next();
			String str = data.get("status");// get the LAST status if more than one
			Data data2 = (Data) cssearcher.searchById(str);
			status = data2.getName();
		}
		return status;
	}
	
	public boolean isConvertPresetReady(String inCatalogId, String inAssetId, String inPresetId) throws Exception{
		boolean isOk = false;
		SearcherManager sm = getSearcherManager();
		Searcher ctsearcher = sm.getSearcher(inCatalogId, "conversiontask");
		SearchQuery sq = ctsearcher.createSearchQuery().append("presetid", inPresetId).append("assetid",inAssetId);
		sq.addSortBy("id");
		HitTracker hits = ctsearcher.search(sq);
		Iterator<?> itr = hits.iterator();
		while (itr.hasNext()){
			Data data = (Data) itr.next();
			String str = data.get("status");
			isOk =  ("complete".equals(str));
		}
		return isOk;
	}
	
	public HitTracker getUnprocessedFatwireConvertPresetList(String inCatalogId, String inAssetId, String inOmitPresetId) throws Exception {
		HitTracker hits = new DataHitTracker();
		SearcherManager sm = getSearcherManager();
		Searcher ctsearcher = sm.getSearcher(inCatalogId, "conversiontask");
		HitTracker all = getFatwireConvertPresetList(inCatalogId,inAssetId);
		Iterator<?> itr = all.iterator();
		while (itr.hasNext()){
			Data data = (Data) itr.next();
			String presetId = data.getId();
			if (presetId.equals(inOmitPresetId)){
				continue;
			}
			SearchQuery query = ctsearcher.createSearchQuery().append("presetid", presetId).append("assetid",inAssetId);
			HitTracker hits2 = ctsearcher.search(query);
			if (hits2.size() == 0){//nothing on conversion list so include in hits
				hits.add(data);
			}
		}
		return hits;
	}
	
	public HitTracker getProcessedFatwireConvertPresetList(String inCatalogId, String inAssetId, String inIncludePresetId) throws Exception {
		HitTracker hits = new DataHitTracker();
		SearcherManager sm = getSearcherManager();
		Searcher ctsearcher = sm.getSearcher(inCatalogId, "conversiontask");
		HitTracker all = getFatwireConvertPresetList(inCatalogId,inAssetId);
		Iterator<?> itr = all.iterator();
		while (itr.hasNext()){
			Data data = (Data) itr.next();
			String presetId = data.getId();
			if (presetId.equals(inIncludePresetId)){
				hits.add(data);
				continue;
			}
			SearchQuery query = ctsearcher.createSearchQuery().append("presetid", presetId).append("assetid",inAssetId);
			HitTracker hits2 = ctsearcher.search(query);
			if (hits2.size() != 0){//at least one found in conversion list so include in hits
				hits.add(data);
			}
		}
		return hits;
	}
	
	public HitTracker getFatwireConvertPresetList(String inCatalogId, String inAssetId) throws Exception {
		SearcherManager sm = getSearcherManager();
		Searcher cpsearcher = sm.getSearcher(inCatalogId, "convertpreset");
		SearchQuery query = cpsearcher.createSearchQuery().append("publishtofatwire","true");
		query.addSortBy("ordering");
		HitTracker hits = cpsearcher.search(query);
		return hits;
	}
	
	public HitTracker getFatwirePublishQueueList(String inCatalogId, String inAssetId, String inPresetId) throws Exception{
		SearcherManager sm = getSearcherManager();
		Searcher pdsearcher = sm.getSearcher(inCatalogId, "publishdestination");
		Data data = (Data) pdsearcher.searchByField("name", "Fatwire");
		Searcher pqsearcher = sm.getSearcher(inCatalogId, "publishqueue");
		SearchQuery query = pqsearcher.createSearchQuery().append("presetid", inPresetId).append("assetid",inAssetId).append("publishdestination",data.getId());
		query.addSortBy("id");
		HitTracker hits = pqsearcher.search(query);
		return hits;
	}

}
