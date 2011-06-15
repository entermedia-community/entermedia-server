package publishing;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.Publisher
import org.openedit.event.*

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery



public void init()
{

	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos
	
	Searcher queuesearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "publishqueue");

		
	SearchQuery query = queuesearcher.createSearchQuery();
	WebEvent webevent = context.getPageValue("webevent");
	Asset asset = null;
	if( webevent != null)
	{
		String sourcepath = webevent.getSourcePath();
		 asset = mediaArchive.getAssetBySourcePath(sourcepath);
		if( asset != null)
		{
			query.addExact("assetid",asset.getId());
		}
	}
	String assetid = context.getRequestParameter("assetid");
	if(assetid != null){
		query.addExact("assetid", assetid);
	}
	query.addOrsGroup("status","pending retry");
	HitTracker tracker = queuesearcher.search(query);
	log.info("publishing " + tracker.size() + " assets");
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
			String presetid = publishrequest.get("presetid");

			if(asset == null)
			{
				 assetid = result.get("assetid");
				asset = mediaArchive.getAsset(assetid);
			}
			
			String publishdestination = publishrequest.get("publishdestination");
			Data preset = mediaArchive.getSearcherManager().getData(mediaArchive.getCatalogId(), "convertpreset", presetid);
			
			if( publishdestination != null)
			{
				Data destination = mediaArchive.getSearcherManager().getData(mediaArchive.getCatalogId(), "publishdestination",publishdestination);
				try
				{
					Publisher publisher = getPublisher(mediaArchive, destination.get("publishtype"));
					publisher.publish(mediaArchive,asset,publishrequest, destination,preset);
					//log.info("Published " +  asset + " to " + destination);
					//firePublishEvent(result.getId());

				}
				catch( Exception ex)
				{
					log.error("Problem publishing ${asset} to ${publishdestination} ${ex}");
					
					String counted =  publishrequest.get("errorcount");
					if( counted == null)
					{
						counted = "0";
					}
					int num = Integer.parseInt( counted );
					num++;
					if( num > 5)
					{
						publishrequest.setProperty('status', 'error');
					}
					else
					{
						publishrequest.setProperty('status', 'retry');
					}
					publishrequest.setProperty('errorcount', String.valueOf(num));
					publishrequest.setProperty("errordetails", "${destination} publish failed ${ex}");
					queuesearcher.saveData(publishrequest, context.getUser());
					continue;
				}
			}
			else
			{
				log.info("publish destination is null")
			}
			publishrequest.setProperty('status', 'complete');
			publishrequest.setProperty("errordetails", " ");
			queuesearcher.saveData(publishrequest, context.getUser());
			firePublishEvent(publishrequest.getId());
		}
	}
}

protected firePublishEvent(String inOrderItemId)
{
	WebEvent event = new WebEvent();
	event.setSearchType("publishqueue");
	event.setProperty("publishqueueid", inOrderItemId);
	event.setOperation("publishing/publishfinished");
	event.setUser(context.getUser());
	event.setCatalogId(mediaarchive.getCatalogId());
	mediaarchive.getMediaEventHandler().eventFired(event);

}

protected Publisher getPublisher(MediaArchive inArchive, String inType)
{
	GroovyClassLoader loader = engine.getGroovyClassLoader();
	Class groovyClass = loader.loadClass("publishing.publishers.${inType}publisher");
	Publisher publisher = (Publisher) groovyClass.newInstance();
	return publisher;
}

init();