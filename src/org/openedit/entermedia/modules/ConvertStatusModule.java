package org.openedit.entermedia.modules;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;

import com.openedit.WebPageRequest;

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
		String presetId = inReq.getRequestParameter("preset");
		
		MediaArchive archive = getMediaArchive(inReq);
		
		//Searcher presetSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "conversions/convertpresets");
		
		//Data preset = (Data) presetSearcher.searchById(presetId);
		
		Searcher taskSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "conversiontask");
		Data newTask = taskSearcher.createNewData();
		newTask.setSourcePath(sourcePath);
		newTask.setProperty("status", "new");
		newTask.setProperty("presetid", presetId);
		
	
		String []fields = inReq.getRequestParameters("field");
		if(fields != null){
			taskSearcher.updateData(inReq, fields, newTask);
		}
		Asset asset = archive.getAssetBySourcePath(sourcePath);
		taskSearcher.saveData(newTask, inReq.getUser());
		archive.fireMediaEvent("conversions/runconversion", inReq.getUser(), asset);
		//processConversions(inReq);
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

}
