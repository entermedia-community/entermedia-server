package notifications;

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.search.AssetSearcher

public void init(){
	log.info("------ Running Asset Approved Notification ------");
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	SearcherManager sm = archive.getSearcherManager();
	Searcher statusSearcher = sm.getSearcher(archive.getCatalogId(), "asset/editstatus");
	Data info = (Data) statusSearcher.searchByField("name", "Approved");
	String approvedId = info.getId();
	
	//todo
	//requires rest api to synch between servers
	
//	server = partner server
//	String url = server + "/media/services/rest/searchassets.xml?catalogid=" + targetcatalogid;
//	//url = url + "&field=remotempublishstatus&remotempublishstatus.value=new&operation=exact";
//	PostMethod method = new PostMethod(url);

	//loop over all the destinations we are monitoring
////		Searcher dests = getSearcherManager().getSearcher(inArchive.getCatalogId(),"publishdestination");
////		Collection hits = dests.fieldSearch("remotempublish","true");
////		if( hits.size() == 0 )
////		{
////			log.info("No remote publish destinations defined. Disable Pull Remote Event");
////			return;
////		}
//		StringBuffer ors = new StringBuffer();
//		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
//		{
//			Data dest = (Data) iterator.next();
//			ors.append(dest.getId());
//			if( iterator.hasNext() )
//			{
//				ors.append(" ");
//			}
//		}
//	method.addParameter("field", "editstatus");
//	method.addParameter("editstatus.value", "7");
//	method.addParameter("operation", "not");
//
//	method.addParameter("field", "approvalstatus");
//	method.addParameter("status.value", "complete");
//	method.addParameter("operation", "not");
//
//	
//
//	try
//	{
//		Element root = execute(inArchive.getCatalogId(), method);
//		if( root.elements().size() > 0 )
//		{
//			log.info("polled " + root.elements().size() + " children" );
//		}
//		for (Object row : root.elements("hit"))
//		{
//			Element hit = (Element)row;
//			try
//			{
//				runRemotePublish(inArchive, server, targetcatalogid, hit);
//			}
//			catch (Exception e)
//			{
//				log.error("Could not save publish " , e);
//			}
//		}
//	}
//	catch (Exception e)
	
	log.info("------ Finished running Asset Approved Notification ------");
}

init();