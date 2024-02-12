package org.entermediadb.asset.publishing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.xmp.XmpWriter;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.event.WebEvent;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.locks.Lock;
import org.openedit.page.Page;


public class PublishManager implements CatalogEnabled {

	private static final Log log = LogFactory.getLog(PublishManager.class);

	protected String fieldCatalogId;
	
	protected ModuleManager fieldModuleManager;
	
	protected ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public String getCatalogId() {
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId) {
		fieldCatalogId = inCatalogId;
	}

	protected MediaArchive getMediaArchive()
	{
		MediaArchive archive = (MediaArchive)getModuleManager().getBean(getCatalogId(), "mediaArchive");
		return archive;
	}
	
	protected Page findInputPage(MediaArchive mediaArchive, Asset asset, Data inPreset) {
		String transcodeid = inPreset.get("transcoderid");
		if( "original".equals( transcodeid ) )
		{
			return mediaArchive.getOriginalDocument(asset);
		}
		String input= "/WEB-INF/data/" + mediaArchive.getCatalogId() + "/generated/" + asset.getSourcePath() + "/" + inPreset.get("generatedoutputfile");
		Page inputpage= mediaArchive.getPageManager().getPage(input);
		return inputpage;
	}

	
	public void checkQueue(WebPageRequest inRequest) {

		MediaArchive  mediaArchive = getMediaArchive();

		Searcher queuesearcher = getMediaArchive().getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "publishqueue");

		SearchQuery query = queuesearcher.createSearchQuery();
		WebEvent webevent = (WebEvent)inRequest.getPageValue("webevent");
		Asset asset = null;
		if( webevent != null)
		{
			String sourcepath = webevent.getSourcePath();
			if( sourcepath != null )
			{
				asset = mediaArchive.getAssetBySourcePath(sourcepath);
				if( asset != null)
				{
					query.addExact("assetid",asset.getId());
				}
			}
		}
		String assetid = inRequest.getRequestParameter("assetid");
		if(assetid != null){
			query.addExact("assetid", assetid);
		}
		query.addOrsGroup("status","new pending retry");
		//query.addNot("remotepublish","true");
		
		HitTracker<Data> publishtasks = queuesearcher.search(query);
		log.info("publishing " + publishtasks.size() + " assets" + queuesearcher.getCatalogId());
		if( publishtasks.size() > 0)
		{
			for (Iterator iterator = publishtasks.iterator(); iterator.hasNext();)
			{
				Data result = (Data) iterator.next();
				Data publishrequest = (Data)queuesearcher.searchById(result.getId());
				if( publishrequest == null)
				{
					log.error("Publish queue index out of date");
					continue;
				}
				
				assetid = result.get("assetid");
				asset = getMediaArchive().getAsset(assetid);
//				}
//				String resultassetid = result.get("assetid");
//				if(asset != null && !asset.getId().equals(resultassetid))
//				{
//					asset = mediaArchive.getAsset(resultassetid );
//				}

				String presetid = publishrequest.get("presetid");
				Data preset = getMediaArchive().getSearcherManager().getData(mediaArchive.getCatalogId(), "convertpreset", presetid);
						
				String publishdestination = publishrequest.get("publishdestination");
				Data destination = getMediaArchive().getSearcherManager().getData(mediaArchive.getCatalogId(), "publishdestination",publishdestination);
				if( destination == null)
				{
					publishrequest.setProperty("status", "error");
					publishrequest.setProperty("errordetails", "Publish destination is invalid " + publishdestination);
					queuesearcher.saveData(publishrequest);
					log.error("Publish destination is invalid " + publishdestination);
					continue;
				}
				
				Collection excludes = destination.getValues("excludeassettype");
				if( excludes != null)
				{
					String type = asset.get("assettype");
					if( type == null)
					{
						type = "none";
					}
					if( excludes.contains(type))
					{
						publishrequest.setProperty("status", "error");
						
						Data assetttype = getMediaArchive().getData("assettype",type);
						publishrequest.setProperty("errordetails", "667: Asset Type excluded from publishing");
						queuesearcher.saveData(publishrequest);
						log.error("Publish destination asset type excluded " + publishdestination);
						continue;
					}
				}
				
				Lock lock = null;
				try
				{
					Publisher publisher = getPublisher(mediaArchive, destination.get("publishtype"));
					lock = mediaArchive.getLockManager().lockIfPossible("assetpublish/" + asset.getSourcePath(), "admin");
					
					if( lock == null)
					{
						log.info("asset already being published ${asset}");
						continue;
					}
					//log.info("Lock Version (${asset}): Version:  " + lock.get(".version") + "Thread: " + Thread.currentThread().getId()  + "Lock ID" + lock.getId());
					PublishResult presult = null;
					
					//	log.info("Publishing  Version (${asset}): Version:  " + lock.get(".version") + "Thread: " + Thread.currentThread().getId()  + "Lock ID" + lock.getId());
					
					if(Boolean.parseBoolean(destination.get("includemetadata")))
					{

						Page inputpage = findInputPage(mediaArchive, asset, preset);
						if( !inputpage.exists() )
						{
							//Make sure we have a preset conversion task
						}

						XmpWriter writer = (XmpWriter) mediaArchive.getModuleManager().getBean("xmpWriter");
						if(inputpage.exists()){
							writer.saveMetadata(mediaArchive, inputpage.getContentItem(), asset, new HashMap());
							
						}
							
					}
						
					//MAIN PUBLISH EVENT
					presult = publisher.publish(mediaArchive,asset,publishrequest, destination,preset);
					
					
					if (presult == null)
					{
						log.info("result from publisher is null, continuing");
						continue;
					}
					if( presult.isError() )
					{
						publishrequest.setProperty("status", "error");
						publishrequest.setProperty("errordetails", presult.getErrorMessage());
						queuesearcher.saveData(publishrequest);
						firePublishEvent(publishrequest.getId());
						continue;
					}
					if( presult.isComplete() )
					{
						log.info("Published " +  asset + " to " + destination);
						publishrequest.setProperty("status", "complete");
						publishrequest.setProperty("errordetails", " ");
						queuesearcher.saveData(publishrequest);
						firePublishEvent(publishrequest.getId());
					}
					else if( presult.isPending() )
					{
						publishrequest.setProperty("status", "pending");
						publishrequest.setProperty("errordetails", " ");
						queuesearcher.saveData(publishrequest);
					}
					//check for remotempublishstatus?
				}
				
				
				
				catch( Throwable ex)
				{
					log.error("Problem publishing ${asset} to ${publishdestination}", ex);
					publishrequest.setProperty("status", "error");
					if(ex.getCause() != null)
					{
						ex = ex.getCause();
					}
					publishrequest.setProperty("errordetails", "${destination} publish failed ${ex}");
					queuesearcher.saveData(publishrequest);
				}
				
				
				finally
				{
					if(lock != null){
					//	log.info("Release Lock Version (${asset}): Version: " + lock.get(".version") + " Thread: " + Thread.currentThread().getId() + "Lock ID" + lock.getId());
						mediaArchive.releaseLock(lock);
					}
				}
				
				
				asset = null; //This is kind of crappy code.
			
			}
		}
	}


	protected void firePublishEvent(String inOrderItemId)
	{
		WebEvent event = new WebEvent();
		event.setCatalogId(getMediaArchive().getCatalogId());
		event.setSearchType("publishqueue");
		event.setProperty("publishqueueid", inOrderItemId);
		event.setOperation("publishing/publishcomplete");
		getMediaArchive().getEventManager().fireEvent(event);
		
		getMediaArchive().fireSharedMediaEvent("ordering/checkorderstatus");

	}

	protected Publisher getPublisher(MediaArchive inArchive, String inType)
	{
//		GroovyClassLoader loader = engine.getGroovyClassLoader();
//		Class groovyClass = loader.loadClass("publishing.publishers.${inType}publisher");
//		Publisher publisher = (Publisher) groovyClass.newInstance();
//		return publisher;
		return (Publisher)getModuleManager().getBean(inType + "publisher");
	}

	
}
