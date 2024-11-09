package org.entermediadb.asset.modules;

import java.util.Collection;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.video.Block;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.event.EventManager;
import org.openedit.event.WebEvent;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;

public class ConvertStatusModule extends BaseMediaModule
{
	
	protected SearcherManager fieldSearcherManager;
	protected EventManager fieldEventManager;
	
	private static final Log log = LogFactory.getLog(ConvertStatusModule.class);

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}



	public void setSearcherManager(SearcherManager searcherManager)
	{
		fieldSearcherManager = searcherManager;
	}
	
	public EventManager getEventManager()
	{
		return fieldEventManager;
	}

	public void setEventManager(EventManager EventManager)
	{
		fieldEventManager = EventManager;
	}

	//this should kick off the groovy event by firing a path event?
	public void addConvertRequest(WebPageRequest inReq)
	{
		//sourcepath=" + asset.getSourcePath() + "preset=" + preset.getId());
		String sourcePath = inReq.getRequestParameter("sourcepath");
		if( sourcePath == null)
		{
			return;
		}
		String presetId = inReq.getRequestParameter("preset");
		
		if(presetId == null){
			presetId = inReq.getRequestParameter("presetid.value");
		}
		MediaArchive archive = getMediaArchive(inReq);

		Asset asset = archive.getAssetBySourcePath(sourcePath);
		if(presetId == null){
			return;
		}
		if(asset == null){
			return;
		}
		Searcher presetSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "convertpreset");
		
		Data preset = (Data) presetSearcher.searchById(presetId);
		
	 	BaseData settings = new BaseData();
			
		String []fields = inReq.getRequestParameters("field");
		
		for (int i = 0; i < fields.length; i++)
		{
			String field = fields[i];
			String val = inReq.getRequestParameter(field + ".value");
			if(field != null && val != null){
				settings.setValue(field, val);
			}		
		}
		
		
		settings.setProperty("presetdataid", preset.get("guid"));
		settings.setProperty("croplast", "true");
		settings.setProperty("force", "true");
		settings.setProperty("gravity", "NorthWest");
        //archive.getTranscodeTools().createOutputIfNeeded(settings, sourcePath, "jpg");
		ConversionManager manager = archive.getTranscodeTools().getManagerByFileFormat(asset.getFileFormat());
        
		ConvertInstructions instructions = manager.createInstructions(asset,preset,settings.getProperties() );
        
		instructions.setForce(true);
		
		ContentItem outputpage = archive.getContent("/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + asset.getSourcePath() + "/" + preset.get("generatedoutputfile"));
		
		
//		//TODO: Re-enamble version control
//		if(outputpage.exists()){
//			getPageManager().putPage(outputpage); // this should create a new version
//		}archive
			
		
		instructions.setOutputFile(outputpage);
		//always use the 1024 - otherwise larger crops are incorrect
		
		//TODO: should do some scaling math based on the input file it selects and the numbers we got so there is no loss in quality
		
		
		Double originalheight = asset.getDouble("height");
		Double originalwidth = asset.getDouble("width");
		
		String hasheight = instructions.get("cropheight");
		//if(hasheight != null && (instructions.getMaxScaledSize().getHeight() > 768 || instructions.getMaxScaledSize().getWidth() > 1024)) {
		if(hasheight != null && originalheight != null && originalwidth != null)
		{
			//input will be the original
			boolean wide = true;
			instructions.setInputFile(archive.getOriginalContent(asset));
			if(originalheight > originalwidth) {
				wide = false;				
			}
			
			//{cropheight=165, assetid=AWEEgnrnvcTz0GAGVvnK, presetdataid=test, croplast=true, y1=101, x1=269, force=true, cropwidth=220, crop=true, outputextension=jpg, cachefilename=image.jpg}
			Double cropheight = Double.parseDouble(hasheight);
			Double cropwidth = Double.parseDouble(instructions.get("cropwidth"));
			Double x1 = Double.parseDouble(instructions.get("x1"));
			Double y1 = Double.parseDouble(instructions.get("y1"));

			Double scalefactor = 1d;
			
			Double croppreviewwidth = 1024d;
			if (instructions.get("cropprevieww") != null) {
				croppreviewwidth = Double.parseDouble(instructions.get("cropprevieww"));
			}
			Double croppreviewheight = 768d;
			if (instructions.get("croppreviewh") != null) {
				Double.parseDouble(instructions.get("croppreviewh"));
			}
			//Scale down to deal with big images
			if(originalwidth > croppreviewwidth && wide) 
			{
				//scalefactor = 1024d/originalwidth;
				scalefactor = croppreviewwidth/originalwidth;
			}
			else if( originalheight > croppreviewheight)
			{
				scalefactor = croppreviewheight/originalheight;
			}

			cropheight = cropheight/scalefactor;
			cropwidth = cropwidth/scalefactor;
			x1 = x1/scalefactor;
			y1 = y1/scalefactor;
			
			instructions.setProperty("cropheight", Integer.toString(cropheight.intValue()));
			instructions.setProperty("cropwidth", Integer.toString(cropwidth.intValue()));
			instructions.setProperty("x1", Integer.toString(x1.intValue()));
			instructions.setProperty("y1", Integer.toString(y1.intValue()));
			instructions.setOutputFile(outputpage);
			
		}
		
		
		
		
		if("image1024x768.jpg".equals(preset.get("generatedoutputfile"))){
			Page s1024 = getPageManager().getPage("/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + asset.getPath() + "/image1024x768.jpg");
			instructions.setInputFile(s1024.getContentItem());//So it doesn't go back to the original when cropping 
		}
	
	
		manager.createOutput(instructions); //This will go back to the original if needed
	
		Searcher tasks = archive.getSearcher("conversiontask");
		Data task = tasks.query().exact("presetid", preset.getId()).exact("assetid", asset.getId()).searchOne();

		if( task == null)
		{
			task = tasks.createNewData();
			task.setProperty("presetid", preset.getId());
			task.setProperty("assetid", asset.getId());
		}
		task.setValue("submitteddate", new Date());
		task.setValue("completed", new Date());
		task.setValue("status", "complete");
		tasks.saveData(task);
		
		archive.fireMediaEvent("usercrop",inReq.getUser(),asset );
		
		processConversions(inReq);//non-block
		
		archive.saveAsset(asset); //Updates the lastmoddate for push
	}
	
	public void processConversions(WebPageRequest inReq)
	{
		
		WebEvent event = new WebEvent();
		event.setSource(this);
		MediaArchive archive = getMediaArchive(inReq);
		event.setCatalogId(archive.getCatalogId());
		event.setOperation("runconversions");
		event.setUser(inReq.getUser());
		//log.info(getEventManager());
		getEventManager().fireEvent(event);
	}
	
	
	
	public void uploadConversionDocument(WebPageRequest inReq){
		MediaArchive archive = getMediaArchive(inReq);
		FileUpload command = (FileUpload) archive.getSearcherManager().getModuleManager().getBean("fileUpload");
		UploadRequest properties = command.parseArguments(inReq);
		
		if (properties == null) {
			return;
		}
		if (properties.getFirstItem() == null) 
		{
			log.info("No upload found");
			return;
			
		}
		String assetid = inReq.getRequestParameter("assetid");
		Asset current = archive.getAsset(assetid);

		String all = inReq.getRequestParameter("replaceall");

		boolean createall = false;
		String generated = "";
		if( "true".equals(all))
		{
			//String fileFormat = current.getFileFormat();

			generated = "/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + current.getSourcePath() + "/image3000x3000.jpg";
			ContentItem saved = properties.saveFileAs(properties.getFirstItem(), generated, inReq.getUser());
			
//			String copytogenerated = "/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + current.getSourcePath() + "/image3000x3000.jpg";
//			archive.getPageManager().getRepository().copy(saved,archive.getContent(copytogenerated));
			createall = true;
		}
		else
		{
			String presetid = inReq.getRequestParameter("presetid");
			Data preset = null;
			preset  = getSearcherManager().getData(archive.getCatalogId(), "convertpreset",presetid);
			if(presetid.equals("0")) 
			{
				generated  =  archive.getOriginalContent(current).getPath();
			}
			else {
				generated = "/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + current.getSourcePath() + "/" + preset.get("generatedoutputfile");
			}
			properties.saveFileAs(properties.getFirstItem(), generated, inReq.getUser());
		}
		
		log.info("Asset: " + assetid + " Replaced " + generated);
		inReq.putPageValue("asset", current);
		archive.saveAsset(current);
		archive.fireMediaEvent("saved", inReq.getUser(), current);

		if( createall)
		{
			rerunSmallerThumbnails(inReq);
		}
		
		
	}

	public void uploadSaveAsDocument(WebPageRequest inReq){
		MediaArchive archive = getMediaArchive(inReq);
		FileUpload command = (FileUpload) archive.getSearcherManager().getModuleManager().getBean("fileUpload");
		UploadRequest properties = command.parseArguments(inReq);
		
		if (properties == null) {
			return;
		}
		if (properties.getFirstItem() == null) 
		{
			log.info("No upload found");
			return;
		}
		
		String newfilename = inReq.getRequestParameter("newfilename");
		String newfiletype = inReq.getRequestParameter("newfiletype");
		
		String assetid = inReq.getRequestParameter("assetid");
		Asset current = archive.getAsset(assetid);

		String base = current.getSourcePath();
		base = PathUtilities.extractDirectoryPath(base);
		
		String outname =  newfilename + "." + newfiletype;
		String sourcepath =  base + "/" + outname;
		//TODO: Check for formats
		
		//Save to temp place to change format
		String tmpplace = "/WEB-INF/trash/" + archive.getCatalogId()	+ "/originals/" + sourcepath;
		ContentItem tosave = archive.getPageManager().getRepository().getStub(tmpplace);
		
		ContentItem saved = properties.saveFileAs(tosave, properties.getFirstItem(), inReq.getUser());
		
		//Convert
		String originalapath = "/WEB-INF/data/" + archive.getCatalogId()	+ "/originals/" + sourcepath;
		
		ContentItem finalpath = archive.getPageManager().getRepository().getStub(originalapath); 
		ConvertInstructions instructions =  archive.createInstructions(current, saved);

		if( finalpath.exists() )
		{
			ContentItem preview = archive.getPresetManager().outPutForGenerated(archive, current, "image3000x3000");
			finalpath.setPreviewImage(preview.getPath());
			finalpath.setMessage("Image Editor Saved");
			finalpath.setAuthor(inReq.getUserName());
			getPageManager().getRepository().saveVersion(finalpath);
		}
		archive.convertFile(instructions, finalpath);

		Collection assetids = archive.getAssetImporter().processOn(originalapath, originalapath,true,archive, null);
		Asset newasset = archive.getAssetBySourcePath(sourcepath);
		if( newasset != null)
		{
			newasset.setValue("parentid",assetid);
			archive.saveAsset(newasset);
			//archive.fireMediaEvent("saved", inReq.getUser(), current);
		}		
		inReq.putPageValue("asset", current);
		
	}

	
	
	public void replaceOriginal(WebPageRequest inReq){
		MediaArchive archive = getMediaArchive(inReq);
		FileUpload command = (FileUpload) archive.getSearcherManager().getModuleManager().getBean("fileUpload");
		UploadRequest properties = command.parseArguments(inReq);
		
		if (properties == null) {
			return;
		}
		if (properties.getFirstItem() == null) {
			return;
			
		}
		Asset current = getAsset(inReq);
		
		//TODO: Make version of old file before replacing it
		//Must be the same type
		ContentItem tosave = archive.getOriginalContent(current);
		if( tosave.exists() )
		{
			ContentItem preview = archive.getPresetManager().outPutForGenerated(archive, current, "image3000x3000");
			tosave.setPreviewImage(preview.getPath());
			tosave.setMessage("Image Editor Saved");
			tosave.setAuthor(inReq.getUserName());
			getPageManager().getRepository().saveVersion(tosave); //Makes a version
		}

		String input = "/WEB-INF/data/" + archive.getCatalogId()	+ "/originals/" + current.getSourcePath(); 
		properties.saveFileAs(tosave, properties.getFirstItem(), inReq.getUser()); //Does not make a version
		//Read New Metadata
		ContentItem original = archive.getOriginalContent(current);
		archive.getAssetImporter().getAssetUtilities().getMetaDataReader().populateAsset(archive, original, current );
		archive.saveAsset(current);
		 archive.removeGeneratedImages(current, true);
		 reloadThumbnails( inReq, archive, current);
		 log.info("Original replaced: " + current.getId() + " Sourcepath: " + input);
		
	}
	
	public void restoreVersion(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String version = inReq.getRequestParameter("version");
	
		Asset current = getAsset(inReq);
		ContentItem original = archive.getOriginalContent(current);
		original.setAuthor(inReq.getUserName());
		original.setMessage("version " + version + " restored");
		archive.getPageManager().getRepository().restoreVersion(original, version);
		
		archive.getAssetImporter().getAssetUtilities().getMetaDataReader().populateAsset(archive, original, current );
		archive.saveAsset(current);
		 archive.removeGeneratedImages(current, true);
		 reloadThumbnails( inReq, archive, current);
		 log.info("Original restored: " + current.getId());
	}
	
	

	public void handleCustomThumb(WebPageRequest inReq){
		MediaArchive archive = getMediaArchive(inReq);
		FileUpload command = (FileUpload) archive.getSearcherManager().getModuleManager().getBean("fileUpload");
		UploadRequest properties = command.parseArguments(inReq);
		
		if (properties == null) {
			return;
		}
		if (properties.getFirstItem() == null) {
			return;
			
		}
		Asset current = getAsset(inReq);
		String input = "/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + current.getSourcePath() + "/" + properties.getFirstItem().getName(); //TODO: Should run a conversion here first to ensure this is a large JPG
		properties.saveFileAs(properties.getFirstItem(), input, inReq.getUser());
		
		//String s1024 = "/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + current.getSourcePath() + "/image1024x768.jpg"; //TODO: Should run a conversion here first to ensure this is a large JPG
		
		//archive.getPresetManager().getPresetByOutputName(archive, "image", "image1024x768");
		
        ConversionManager c = archive.getTranscodeTools().getManagerByRenderType("image");
		ConvertInstructions instructions = c.createInstructions(current,"image1024x768.jpg");
		instructions.setForce(true);
		instructions.setInputFile(archive.getContent( input ) );
	 	c.createOutput(instructions);

	 	String png1024 = "/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + current.getSourcePath() + "/image3000x3000.png"; //TODO: Should run a conversion here first to ensure this is a large JPG
		instructions.setOutputFile(archive.getContent( png1024) );
	 	c.createOutput(instructions);
		
		 archive.removeGeneratedImages(current, false);
		 reloadThumbnails( inReq, archive, current);
		
	}
	public void rerunSmallerThumbnails(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		if (asset != null) 
		{
			archive.removeGeneratedImages(asset, false);
			reloadThumbnails( inReq, archive, asset);
		}
	}
	public void rerunAllThumbnails(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		archive.removeGeneratedImages(asset, true);
		reloadThumbnails( inReq, archive, asset);

	}
	protected void reloadThumbnails(WebPageRequest inReq, MediaArchive archive, Asset inAsset)
	{
		//inReq.putPageValue("asset", inAsset);
		HitTracker conversions = archive.query("conversiontask").exact("assetid", inAsset.getId()).search(); //This is slow, we should load up a bunch at once
		archive.getSearcher("conversiontask").deleteAll(conversions, null);
		archive.getPresetManager().queueConversions(archive, archive.getSearcher("conversiontask"), inAsset, true);
		
		//archive.getPresetManager().queueConversions(archive,archive.getSearcher("conversiontask"),current,true);
		//current.setProperty("importstatus", "imported");
		//archive.fireMediaEvent("importing/assetsimported", inReq.getUser());
		//archive.fireMediaEvent("conversions/thumbnailreplaced", inReq.getUser(), current);
		
		//Good idea?
		//archive.fireMediaEvent("conversions","runconversion", inReq.getUser(), inAsset);//block?
		//archive.fireMediaEvent("asset/saved", inReq.getUser(), current);
		archive.fireSharedMediaEvent("conversions/runconversions");
		//archive.saveAsset(current);
	}
	public void reloadIndex(WebPageRequest inReq)
	{
		Asset asset = getAsset(inReq);
		String path = inReq.getContentPage().getDirectory();
		inReq.redirect(path + "/index.html?assetid=" + asset.getId());
	}

}
