package org.openedit.entermedia.modules;

import java.util.Iterator;

import org.entermedia.upload.FileUpload;
import org.entermedia.upload.UploadRequest;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
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
		//Searcher presetSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "conversions/convertpresets");
		
		//Data preset = (Data) presetSearcher.searchById(presetId);
		
		
		
		Searcher taskSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "conversiontask");
		SearchQuery q = taskSearcher.createSearchQuery();
		q.addMatches("assetid", asset.getId());
		q.addMatches("presetid", presetId);
		
		HitTracker t = taskSearcher.search(q);
		if(t.size() > 0){
			for (Iterator iterator = t.iterator(); iterator.hasNext();) {
				Data task = (Data) iterator.next();
				
				taskSearcher.delete(task, null);
			}
		}
		Data newTask = taskSearcher.createNewData();
		
		
		
		newTask.setSourcePath(sourcePath);
		newTask.setProperty("status", "new");
		newTask.setProperty("presetid", presetId);
		newTask.setProperty("assetid", asset.getId());
	
		String []fields = inReq.getRequestParameters("field");
		if(fields != null){
			taskSearcher.updateData(inReq, fields, newTask);
		}
		
		taskSearcher.saveData(newTask, inReq.getUser());
		archive.fireMediaEvent("conversions/runconversions", inReq.getUser(), asset);//block
		Data preset = archive.getData("convertpreset", presetId);
		
		Page outputpage = getPageManager().getPage("/WEB-INF/data/" + archive.getCatalogId() + "generated"+ asset.getSourcePath() + "/" + preset.get("outputfile"));
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
			

		inReq.putPageValue("asset", current);
		
	}

}
