package publishing;

import java.awt.JobAttributes.DestinationType

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.publishing.PublishResult
import org.entermediadb.asset.publishing.Publisher
import org.entermediadb.asset.xmp.XmpWriter
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.event.WebEvent
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery
import org.openedit.locks.Lock
import org.openedit.page.Page


public void init() {

	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos

	Searcher queuesearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "publishqueue");


	SearchQuery query = queuesearcher.createSearchQuery();
	WebEvent webevent = context.getPageValue("webevent");
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
	String assetid = context.getRequestParameter("assetid");
	if(assetid != null){
		query.addExact("assetid", assetid);
	}
	query.addOrsGroup("status","new pending retry");
	//query.addNot("remotepublish","true");
	
	HitTracker tracker = queuesearcher.search(query);
	log.info("publishing " + tracker.size() + " assets" + queuesearcher.getCatalogId());
	if( tracker.size() > 0)
	{
		for( Data result:tracker)
		{
			Data publishrequest = queuesearcher.searchById(result.getId());
			if( publishrequest == null)
			{
				log.error("Publish queue index out of date");
				continue;
			}
			
			assetid = result.get("assetid");
			asset = mediaArchive.getAsset(assetid);
//			}
//			String resultassetid = result.get("assetid");
//			if(asset != null && !asset.getId().equals(resultassetid))
//			{
//				asset = mediaArchive.getAsset(resultassetid );
//			}

			String presetid = publishrequest.get("presetid");
			Data preset = mediaArchive.getSearcherManager().getData(mediaArchive.getCatalogId(), "convertpreset", presetid);
					
			String publishdestination = publishrequest.get("publishdestination");
			Data destination = mediaArchive.getSearcherManager().getData(mediaArchive.getCatalogId(), "publishdestination",publishdestination);
			if( destination == null)
			{
				publishrequest.setProperty('status', 'error');
				publishrequest.setProperty("errordetails", "Publish destination is invalid " + publishdestination);
				queuesearcher.saveData(publishrequest, context.getUser());
				log.error("Publish destination is invalid " + publishdestination);
				
				continue;
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
				
				if(Boolean.parseBoolean(destination.get("includemetadata"))){
				
					Page inputpage = publisher.findInputPage(mediaArchive, asset, preset);
					XmpWriter writer = (XmpWriter) mediaArchive.getModuleManager().getBean("xmpWriter");
					if(inputpage.exists()){
						writer.saveMetadata(mediaArchive, inputpage.getContentItem(), asset);
						
					}
						
				}
				
				
				
					
					presult = publisher.publish(mediaArchive,asset,publishrequest, destination,preset);
					//Thread.sleep(3000);
				
				
				if (presult == null)
				{
					log.info("result from publisher is null, continuing");
					continue;
				}
				if( presult.isError() )
				{
					publishrequest.setProperty('status', 'error');
					publishrequest.setProperty("errordetails", presult.getErrorMessage());
					queuesearcher.saveData(publishrequest, context.getUser());
					firePublishEvent(publishrequest.getId());
					continue;
				}
				if( presult.isComplete() )
				{
					log.info("Published " +  asset + " to " + destination);
					publishrequest.setProperty('status', 'complete');
					publishrequest.setProperty("errordetails", " ");
					queuesearcher.saveData(publishrequest, context.getUser());
					firePublishEvent(publishrequest.getId());
				}
				else if( presult.isPending() )
				{
					publishrequest.setProperty('status', 'pending');
					publishrequest.setProperty("errordetails", " ");
					queuesearcher.saveData(publishrequest, context.getUser());
				}
				//check for remotempublishstatus?
			}
			
			
			
			catch( Throwable ex)
			{
				log.error("Problem publishing ${asset} to ${publishdestination}", ex);
				publishrequest.setProperty('status', 'error');
				if(ex.getCause() != null)
				{
					ex = ex.getCause();
				}
				publishrequest.setProperty("errordetails", "${destination} publish failed ${ex}");
				queuesearcher.saveData(publishrequest, context.getUser());
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


protected firePublishEvent(String inOrderItemId)
{
	WebEvent event = new WebEvent();
	event.setSearchType("publishqueue");
	event.setProperty("publishqueueid", inOrderItemId);
	event.setOperation("publishing/publishcomplete");
	event.setUser(context.getUser());
	event.setCatalogId(mediaarchive.getCatalogId());
	mediaarchive.getEventManager().fireEvent(event);

}

protected Publisher getPublisher(MediaArchive inArchive, String inType)
{
//	GroovyClassLoader loader = engine.getGroovyClassLoader();
//	Class groovyClass = loader.loadClass("publishing.publishers.${inType}publisher");
//	Publisher publisher = (Publisher) groovyClass.newInstance();
//	return publisher;
	return moduleManager.getBean("${inType}publisher");
}

init();