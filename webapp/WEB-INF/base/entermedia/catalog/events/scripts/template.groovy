package assets;

import org.openedit.data.*;
import org.entermediadb.asset.*;
import org.openedit.hittracker.*;

public void go()
{
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	HitTracker assets = mediaArchive.query("asset").match("editstatus","7").search();
	for(Asset asset in assets)
	{
		//do something
		
	}
	log.info( "${assets.size()} assets found");
}

go();
