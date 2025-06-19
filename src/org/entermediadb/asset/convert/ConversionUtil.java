package org.entermediadb.asset.convert;

import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.DataHitTracker;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.repository.ContentItem;
import org.openedit.util.MathUtils;

public class ConversionUtil {
	
	private static final Log log = LogFactory.getLog(ConversionUtil.class);
	
	public static final String DECIMAL_FORMAT = "##.##";
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
	
	public boolean canCrop(MediaArchive inArchive, Data inPreset, Asset inAsset) throws Exception {
		//log.info("canCrop "+inCatalogId+", "+inPresetId+", "+inAssetId);
		boolean canCrop = false;
		String inCatalogId = inArchive.getCatalogId();
		Dimension cropDimension = getConvertPresetDimension(inArchive.getCatalogId(), inPreset.getId());
	
		if (!inPreset.get("transcoderid").equals("imagemagick")) {
			return false;
		}
		//log.debug("Crop Dimension: "+cropDimension);
		if (cropDimension!=null && cropDimension.getHeight()!=0 && cropDimension.getWidth()!=0){
//			Dimension inputDimension = getConvertPresetDimension(inArchive,"cropinput");//this needs to be in convertpreset table!
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
			//error check dimensions
			double assetwidth = 0.0d;
			try{
				String num = inAsset.get("width");
				if (num!=null) num = num.trim();
				assetwidth = (double) Integer.parseInt(num);
				if (assetwidth<=0) {
					return false;
				}
			}catch (Exception e){
				log.warn("Exception caught parsing asset width, assetid="+inAsset.getId()+", width="+inAsset.get("width")+", defaulting value to 0");
			}
			double assetheight = 0.0d; 
			try{
				String num = inAsset.get("height");
				if (num!=null) num = num.trim();
				assetheight = (double) Integer.parseInt(num);
				if (assetheight<=0) {
					return false;
				}
			}catch (Exception e){
				log.warn("Exception caught parsing asset height, assetid="+inAsset.getId()+", height="+inAsset.get("height")+", defaulting value to 0");
			}
			
			double cropwidth = cropDimension.getWidth();
			double cropheight = cropDimension.getHeight();
			String cancropsmallerimages = inArchive.getCatalogSettingValue("cropsmallerimages");
			if (Boolean.parseBoolean(cancropsmallerimages)) {
				return true; //Always allow to crop
			}

			canCrop = (cropwidth <= assetwidth && cropheight <= assetheight);
		}
		//log.info("Can image be cropped? "+canCrop);
		return canCrop;
	}
	
	public Dimension getConvertPresetDimension(String inCatalogId, String inPresetId) {
		Dimension dimension = new Dimension();
		double width = 0d;
		double height = 0d;
		HitTracker hits = getParameterData(inCatalogId, inPresetId);
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
	public Data getConversionTask(MediaArchive inArchive, String inAssetId, String inPresetId) throws Exception 
	{
		Searcher ctsearcher = inArchive.getSearcher("conversiontask");
		Data task = ctsearcher.query().exact("presetid", inPresetId).exact("assetid",inAssetId).searchOne();
		return task;	
	}

	public ContentItem outPutForPreset(MediaArchive inArchive, Asset inAsset, Data preset)
	{
		ContentItem item = inArchive.getPresetManager().outPutForPreset(inArchive,inAsset,preset);
		return item;
	}
	public ContentItem outPutForPreset(MediaArchive inArchive, Asset inAsset, String exportName)
	{
		//Check output file for existance
		String fileName = inArchive.generatedOutputName(inAsset, exportName);
		String generatedfilename = "/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/" + fileName;
		ContentItem output = inArchive.getContent(generatedfilename);
		return output;
	}

	public boolean doesConvertedFileExist(MediaArchive inArchive, Asset inAsset, Data preset)
	{
		long size = inArchive.getPresetManager().getLengthOfOutForPreset(inArchive,inAsset,preset);
		return size > 0;
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
	
	public HitTracker getActivePresetList(MediaArchive inArchive, Asset inAsset)
	{
		String rendertype = inArchive.getMediaRenderType(inAsset.get("fileformat"));
		Collection both = new ArrayList();
		both.add("all");
		if(rendertype != null) {
		both.add(rendertype);
		}
		HitTracker all = inArchive.query("convertpreset").exact("display", "true").orgroup("inputtype", both).sort("ordering").search();
		//HitTracker all = sm.getSearcher(inCatalogId, "convertpreset").query().match("display", "true").sort("ordering").search();
		return all;
	}
	
	public Collection getCroppablePresetList(MediaArchive inArchive, Asset inAsset)
	{
		String rendertype = inArchive.getMediaRenderType(inAsset.get("fileformat"));
		
		Collection inputtype = new ArrayList();
		inputtype.add("all");
		
		if(rendertype != null) {
			inputtype.add(rendertype);
		}
		
		HitTracker all = inArchive.query("convertpreset").exact("display", "true").orgroup("inputtype", inputtype).sort("ordering").search();
		
		Collection croppableList = new ArrayList();
		
		for (Iterator iterator = all.iterator(); iterator.hasNext();) {
			Data preset = (Data) iterator.next();
			String presetid = preset.get("guid");
			if (presetid == null)
			{
				log.error("Convert Preset missing guid: " + preset.getId());
				continue;
			}
			Data croppable = (Data) inArchive.query("presetparameter").exact("parameterdata", presetid).exact("name", "crop").searchOne();
			
			if(croppable != null)
			{
				croppableList.add(preset);
			}
			// doesPresetExist(MediaArchive inArchive, Asset inAsset, String outputname???)
		}
		
		return croppableList;
	}
	
	public Collection getOnImportPresetList(MediaArchive inArchive, Asset inAsset)  
	{
		Collection all = inArchive.getPresetManager().getOnImportPresets(inArchive,inAsset);
		return all;
	}
	
	public boolean isConverting(MediaArchive inArchive,Asset inAsset)
	{
		Data found = (Data)inArchive.query("conversiontask").exact("assetid", inAsset.getId()).not("status","complete").searchOne();
		if( found == null)
		{
			return false;
		}
		return true;
		
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


	public boolean isEnforceAspectRatio(MediaArchive inArchive, String inPresetId) 
	{
		Data preset = inArchive.getData("convertpreset", inPresetId);
		String enforce = preset.get("enforceaspectratio");
		return Boolean.parseBoolean(enforce);
	}

	public boolean doesPresetExist(MediaArchive inArchive, Asset inAsset, String outputname)
	{
		if( inAsset == null)
		{
			return false;
		}
		String generatedfilename = "/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/" + outputname;
		ContentItem output = inArchive.getContent(generatedfilename);
		return output.getLength() > 0;
	}

	public List<Integer> loadCropBox(MediaArchive inArchive, Asset inAsset, Data preset)
	{
		Searcher assetcrops = inArchive.getSearcher("assetcrop");
		MultiValued assetcrop = (MultiValued)assetcrops.query().exact("presetid", preset.getId()).exact("assetid", inAsset.getId()).searchOne();
	
		List<Integer> box = new ArrayList();

		int cropx = 0;
		int cropy = 0;
		int cropwidth = 0;
		int cropheight = 0;

		if( assetcrop == null)
		{
			//Take a guess
			cropwidth = inAsset.getInt("width");
			
			String ext = preset.get("outputextension");
			ConversionManager manager = inArchive.getTranscodeTools().getManagerByFileFormat(ext);
			ConvertInstructions instructions = manager.createInstructions(inAsset, preset);
			
			int aspectwidth = instructions.intValue("prefwidth",-1);
			int aspectheight= instructions.intValue("prefheight",-1);
			double aspect = MathUtils.divide(aspectwidth , aspectheight );
			float ch = (float)  MathUtils.divide(cropwidth , aspect);
			cropheight = Math.round(ch);
			
			int height = inAsset.getInt("height");
			float halfh = (float)MathUtils.divide( height, 2 );
			float halfcrop = (float)MathUtils.divide( cropheight,2 );
			cropy = 0;
			cropy =  Math.round(halfh - halfcrop);
		}
		else
		{
			cropx = assetcrop.getInt("x1");
			cropy = assetcrop.getInt("y1");
			cropwidth = assetcrop.getInt("cropwidth");
			cropheight = assetcrop.getInt("cropheight");
		}
		
		box.add(cropx);
		box.add(cropy);
		box.add(cropwidth);
		box.add(cropheight);
		
		return box;
	}
	
}
