import org.openedit.Data

import org.openedit.hittracker.HitTracker

//see if the hits we have uses any other data elements such as category or collection

String name = context.getRequestParameter("hitssessionid");
HitTracker hits = (HitTracker) context.getSessionValue(name);
if( hits != null)
{
	String collectionid = hits.getInput("collectionid");
	if( collectionid != null)
	{
		Data coll = mediaarchive.getData("librarycollection",collectionid);
		context.putPageValue("librarycol",coll);
	}
}