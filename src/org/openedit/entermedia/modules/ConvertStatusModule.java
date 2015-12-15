package org.openedit.entermedia.modules;

import java.util.Iterator;

import org.entermedia.upload.FileUpload;
import org.entermedia.upload.UploadRequest;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.creator.ConvertInstructions;
import org.openedit.entermedia.creator.MediaCreator;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;

public class ConvertStatusModule extends BaseMediaModule
{
	
	protected SearcherManager fieldSearcherManager;
	protected WebEventListener fieldWebEventListener;


	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}



	public void setSearcherManager(SearcherManager searcherManager)
	{
		fieldSearcherManager = searcherManager;
	}
	
	public WebEventListener getWebEventListener()
	{
		return fieldWebEventListener;
	}

	public void setWebEventListener(WebEventListener webEventListener)
	{
		fieldWebEventListener = webEventListener;
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
		if(fields != null){
			presetSearcher.updateData(inReq, fields, settings);
		}
		settings.setProperty("presetdataid", preset.get("guid"));
		settings.setProperty("croplast", "true");
        MediaCreator c = archive.getCreatorManager().getMediaCreatorByOutputFormat("jpg");
        
		ConvertInstructions instructions = c.createInstructions(settings.getProperties(), archive, "jpg", asset.getSourcePath());
		instructions.setForce(true);
		Page outputpage = getPageManager().getPage("/WEB-INF/data/" + archive.getCatalogId() + "/generated/"+ asset.getSourcePath() + "/" + preset.get("outputfile"));
		instructions.setOutputPath(outputpage.getPath());
	 	 
	 	 c.createOutput(archive, instructions);
		
		//archive.fireMediaEvent("conversions/runconversion", inReq.getUser(), asset);//block
		if(outputpage.exists()){
			getPageManager().putPage(outputpage); // this should create a new version
		}
		processConversions(inReq);//non-block
	}
	
	
	
	
	



	public void processConversions(WebPageRequest inReq)
	{
		
		
		WebEvent event = new WebEvent();
		event.setSource(this);
		MediaArchive archive = getMediaArchive(inReq);
		event.setCatalogId(archive.getCatalogId());
		event.setOperation("conversions/runconversions");
		event.setUser(inReq.getUser());
		//log.info(getWebEventListener());
		getWebEventListener().eventFired(event);
	}
	
	
	
	public void uploadConversionDocument(WebPageRequest inReq){
		MediaArchive archive = getMediaArchive(inReq);
		FileUpload command = (FileUpload) archive.getSearcherManager().getModuleManager().getBean("fileUpload");
		UploadRequest properties = command.parseArguments(inReq);
		
		if (properties == null) {
			return;
		}
		if (properties.getFirstItem() == null) {
			return;
			
		}
		String assetid = inReq.getRequestParameter("assetid");
		
		String presetid = inReq.getRequestParameter("presetid");
		Data preset  = getSearcherManager().getData(archive.getCatalogId(), "convertpreset",presetid);
		Asset current = archive.getAsset(assetid);
	
		
		String generated = "/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + current.getSourcePath() + "/" + preset.get("outputfile");
		properties.saveFileAs(properties.getFirstItem(), generated, inReq.getUser());

		boolean newdefault = Boolean.parseBoolean(inReq.getRequestParameter("replaceall"));
		if(newdefault){
			
		}
		archive.fireMediaEvent("asset/saved", inReq.getUser(), current);

		inReq.putPageValue("asset", current);
		
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
		String assetid = inReq.getRequestParameter("assetid");
		Asset current = archive.getAsset(assetid);
		String input = "/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + current.getSourcePath() + "/" + properties.getFirstItem().getName(); //TODO: Should run a conversion here first to ensure this is a large JPG
		properties.saveFileAs(properties.getFirstItem(), input, inReq.getUser());
		Page page = getPageManager().getPage(input);
		Page crop1024jpg = getPageManager().getPage("/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + current.getSourcePath() + "/customthumb.jpg");
		getPageManager().removePage(crop1024jpg);
		Page crop1024 = getPageManager().getPage("/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + current.getSourcePath() + "/customthumb.png");
		getPageManager().removePage(crop1024);
		
		String generated = "/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + current.getSourcePath() + "/customthumb.jpg"; //TODO: Should run a conversion here first to ensure this is a large JPG
		String generatedpng = "/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + current.getSourcePath() + "/customthumb.png"; //TODO: Should run a conversion here first to ensure this is a large JPG

		String s1024 = "/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + current.getSourcePath() + "/image1024x768.jpg"; //TODO: Should run a conversion here first to ensure this is a large JPG

        MediaCreator c = archive.getCreatorManager().getMediaCreatorByOutputFormat("jpg");
		ConvertInstructions instructions = new ConvertInstructions();
		instructions.setForce(true);
		instructions.setInputPath(page.getPath());
		instructions.setOutputPath(generated);
	 	 
	 	// instructions.setOutputExtension("jpg");
	 	 c.createOutput(archive, instructions);
		instructions.setOutputPath(generatedpng);
		 c.createOutput(archive, instructions);
		 instructions.setMaxScaledSize(1024, 768);
		 instructions.setOutputPath(s1024);
		 c.createOutput(archive, instructions);
		
		saveCustomThumb( inReq, archive, current);
		
	}
	public void copyLargeForCustomThumb(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		Page s1024 = getPageManager().getPage("/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + asset.getSourcePath() + "/image1024x768.jpg"); 
		Page crop1024 = getPageManager().getPage("/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + asset.getSourcePath() + "/customthumb.jpg");
		getPageManager().copyPage(s1024, crop1024);
		saveCustomThumb( inReq, archive, asset);
	}
	public void clearCustomThumb(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		Page crop1024jpg = getPageManager().getPage("/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + asset.getSourcePath() + "/customthumb.jpg");
		getPageManager().removePage(crop1024jpg);
		Page crop1024 = getPageManager().getPage("/WEB-INF/data/" + archive.getCatalogId()	+ "/generated/" + asset.getSourcePath() + "/customthumb.png");
		getPageManager().removePage(crop1024);
		saveCustomThumb( inReq, archive, asset);

	}
	protected void saveCustomThumb(WebPageRequest inReq, MediaArchive archive, Asset current)
	{
		archive.removeGeneratedImages(current, false);
		archive.getPresetManager().queueConversions(archive,archive.getSearcher("conversiontask"),current,true);
		//current.setProperty("importstatus", "imported");
		//archive.fireMediaEvent("importing/assetsimported", inReq.getUser());
		//archive.fireMediaEvent("conversions/thumbnailreplaced", inReq.getUser(), current);
		archive.fireMediaEvent("conversions/runconversion", inReq.getUser(), current);//block
		//archive.fireMediaEvent("asset/saved", inReq.getUser(), current);

		inReq.putPageValue("asset", current);
	}
	
}
