package org.entermediadb.asset.convert;

import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.DataHitTracker;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;

public class ConversionUtil {
	
	private static final Log log = LogFactory.getLog(ConversionUtil.class);
	
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
	
	protected HitTracker getParameterData(String inCatalogId, String inPresetId) {
		SearcherManager sm = getSearcherManager();
		Searcher cpsearcher = sm.getSearcher(inCatalogId, "convertpreset");
		Searcher pdsearcher = sm.getSearcher(inCatalogId, "presetdata");
		Searcher ppsearcher = sm.getSearcher(inCatalogId, "presetparameter");
		Data cpdata = (Data) cpsearcher.searchById(inPresetId);
		String guid = cpdata.get("guid");
		Data pddata = (Data) pdsearcher.searchById(guid);
		if( pddata == null)
		{
			return null;
		}
		String pdata = pddata.getId();
		SearchQuery sq = ppsearcher.createSearchQuery().append("parameterdata", pdata);
		HitTracker hits = ppsearcher.search(sq);
		return hits;
	}
	
	public boolean canCrop(String inCatalogId, String inPresetId, String inAssetId) throws Exception{
		//log.info("canCrop "+inCatalogId+", "+inPresetId+", "+inAssetId);
		boolean canCrop = false;
		Dimension cropDimension = getConvertPresetDimension(inCatalogId,inPresetId);
		//log.debug("Crop Dimension: "+cropDimension);
		if (cropDimension!=null && cropDimension.getHeight()!=0 && cropDimension.getWidth()!=0){
//			Dimension inputDimension = getConvertPresetDimension(inCatalogId,"cropinput");//this needs to be in convertpreset table!
//			log.debug("Preset Input Dimension: "+inputDimension);
//			if (inputDimension!=null && inputDimension.getHeight()!=0 && inputDimension.getWidth()!=0){
//				Asset asset = (Asset) getSearcherManager().getData(inCatalogId, "asset", inAssetId);
//				double assetwidth = asset.get("width") != null ? (double) Integer.parseInt(asset.get("width")) : 0d;
//				double assetheight = asset.get("height") != null ? (double) Integer.parseInt(asset.get("height")) : 0d;
//				log.debug("Asset dimension: "+assetwidth+", "+assetheight);
//				if (assetwidth != 0 && assetheight != 0){
//					//input w x h
//					double inputwidth = inputDimension.getWidth();
//					double inputheight = inputDimension.getHeight();
//					log.debug("input dimension: "+inputwidth+" x "+inputheight);
//					//crop w x h
//					double cropwidth = cropDimension.getWidth();
//					double cropheight = cropDimension.getHeight();
//					log.debug("crop dimension: "+cropwidth+" x "+cropheight);
//					//calculate cropinput for particular asset; does best fit
//					double bestfitwidth = 0;
//					double bestfitheight = 0;
//					double factorwidth = assetwidth / inputwidth;
//					double factorheight = assetheight / inputheight;
//					log.debug("factor dimension: "+factorwidth+" x "+factorheight);
//					if (factorwidth > factorheight){
//						bestfitwidth = assetwidth / factorwidth;
//						bestfitheight = assetheight / factorwidth;
//					} else {
//						bestfitwidth = assetwidth / factorheight;
//						bestfitheight = assetheight / factorheight;
//					}
//					log.debug("best-fit dimension: "+bestfitwidth+" x "+bestfitheight);
//					//now have calculated cropinput dimension and crop dimension
//					canCrop = (cropwidth <= bestfitwidth && cropheight <= bestfitheight);
//				}
//			}
			
			//use asset dimension instead of standardized input dimension
			Asset asset = (Asset) getSearcherManager().getData(inCatalogId, "asset", inAssetId);
			//error check dimensions
			double assetwidth = 0.0d;
			try{
				String num = asset.get("width");
				if (num!=null) num = num.trim();
				assetwidth = (double) Integer.parseInt(num);
			}catch (Exception e){
				log.warn("Exception caught parsing asset width, assetid="+asset.getId()+", width="+asset.get("width")+", defaulting value to 0");
			}
			double assetheight = 0.0d; 
			try{
				String num = asset.get("height");
				if (num!=null) num = num.trim();
				assetheight = (double) Integer.parseInt(num);
			}catch (Exception e){
				log.warn("Exception caught parsing asset height, assetid="+asset.getId()+", height="+asset.get("height")+", defaulting value to 0");
			}
			double cropwidth = cropDimension.getWidth();
			double cropheight = cropDimension.getHeight();
			canCrop = (cropwidth <= assetwidth && cropheight <= assetheight);
		}
		//log.info("Can image be cropped? "+canCrop);
		return canCrop;
	}
	
	public Dimension getConvertPresetDimension(String inCatalogId, String inPresetId) {
		Dimension dimension = new Dimension();
		double width = 0d;
		double height = 0d;
		HitTracker hits = getParameterData(inCatalogId,inPresetId);
		if( hits == null)
		{
			return null;
		}
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
		if( dimension == null)
		{
			return null;
		}
		return getConvertPresetAspectRatio(dimension);
	}
	
	public String getConvertPresetAspectRatio(Dimension inDimension){
		if( inDimension == null)
		{
			return null;
		}
		double height = inDimension.getHeight();
		double width = inDimension.getWidth();
		if (height > 0 && width > 0){
			DecimalFormat format = new DecimalFormat(DECIMAL_FORMAT);
			return format.format(width / height) + ":1";
		}
		return null;
	}
	
	public String getConvertPresetParameter(String inCatalogId, String inPresetId, String inParameter) throws Exception{
		HitTracker hits = getParameterData(inCatalogId,inPresetId);
		if( hits == null)
		{
			return null;
		}
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
		if(hits.size() > 0){
			Data hit = hits.get(0);
			Data data2 = (Data) cssearcher.searchById(hit.get("status"));
			status = data2.getName();
			return status;
		}
		Asset asset = (Asset) sm.getData(inCatalogId, "asset", inAssetId);
		if(doesExist(inCatalogId, inAssetId, asset.getSourcePath(), inPresetId)){
 			Data data2 = (Data) cssearcher.searchById("complete");
			if(data2 != null){
			return  data2.getName();
			} return "Complete";

		}
		return status;
	}
	
	public boolean isConvertPresetReady(String inCatalogId, String inAssetId, String sourcepath, String inPresetId) throws Exception{
		boolean isOk = false;
		
		if( "0".equals( inPresetId ) )
		{
			return true;
		}
		
		SearcherManager sm = getSearcherManager();
		if(doesConvertedFileExist(inCatalogId, sourcepath, inPresetId)){
			return true;
		}
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

	public boolean doesConvertedFileExist(String inCatalogId,
			String sourcepath, String inPresetId) {
		PageManager pm = (PageManager) getSearcherManager().getModuleManager().getBean("pageManager");
		Data preset = getSearcherManager().getData(inCatalogId, "convertpreset", inPresetId);
		String outputfile = preset.get("generatedoutputfile");
		Page outputpage = pm.getPage("/WEB-INF/data/" + inCatalogId + "/generated/" + sourcepath + "/" + outputfile);
		return outputpage.exists();
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
	
	public HitTracker getActivePresetList(String inCatalogId, String mediatype)  {
		SearcherManager sm = getSearcherManager();
		Collection both = new ArrayList();
		both.add("all");
		if(mediatype != null) {
		both.add(mediatype);
		}
		HitTracker all = sm.getSearcher(inCatalogId, "convertpreset").query().match("display", "true").orgroup("inputtype", both).sort("ordering").search();
		//HitTracker all = sm.getSearcher(inCatalogId, "convertpreset").query().match("display", "true").sort("ordering").search();
		return all;
	}
	
	public HitTracker getFatwireConvertPresetList(String inCatalogId, String inAssetId) {
		SearcherManager sm = getSearcherManager();
		Searcher cpsearcher = sm.getSearcher(inCatalogId, "convertpreset");
		SearchQuery query = cpsearcher.createSearchQuery().append("publishtofatwire","true");
		query.addSortBy("ordering");
		HitTracker hits = cpsearcher.search(query);
		return hits;
	}
	
	public HitTracker getFatwirePublishQueueList(String inCatalogId, String inAssetId, String inPresetId) {
		SearcherManager sm = getSearcherManager();
		Searcher pdsearcher = sm.getSearcher(inCatalogId, "publishdestination");
		Data data = (Data) pdsearcher.searchByField("name", "Fatwire");
		if (data!=null){//np check
			Searcher pqsearcher = sm.getSearcher(inCatalogId, "publishqueue");
			SearchQuery query = pqsearcher.createSearchQuery().append("presetid", inPresetId).append("assetid",inAssetId).append("publishdestination",data.getId());
			query.addSortBy("id");
			HitTracker hits = pqsearcher.search(query);
			return hits;
		}
		return null;
	}

	public boolean doesExist(String inCatalogId, String inAssetId, String assetSourcePath, String inPresetId)
	{
		if( "0".equals( inPresetId ))
		{
			return true;
		}
		SearcherManager sm = getSearcherManager();
		Searcher ctsearcher = sm.getSearcher(inCatalogId, "conversiontask");
		SearchQuery q = ctsearcher.createSearchQuery();
		q.addExact("presetid", inPresetId);
		q.addExact("assetid", inAssetId);
		HitTracker hits = ctsearcher.search(q);
		if(hits.size() > 0){
			return true;
			
		}
		return doesConvertedFileExist(inCatalogId, assetSourcePath, inPresetId);
	}
	

	public boolean isEnforceAspectRatio(MediaArchive inArchive, String inPresetId) 
	{
		Data preset = inArchive.getData("convertpreset", inPresetId);
		String enforce = preset.get("enforceaspectratio");
		return Boolean.parseBoolean(enforce);
	}

	
}
