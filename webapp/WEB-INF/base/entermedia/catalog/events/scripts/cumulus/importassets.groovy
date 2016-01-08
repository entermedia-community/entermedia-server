package cumulus

import org.entermediadb.asset.ConvertStatus
import org.entermediadb.asset.MediaArchive

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	boolean forced = Boolean.parseBoolean(context.findValue("forced"));
	ConvertStatus errorlog = archive.convertCatalog(context.getUser(), forced);
	if (context != null)
	{
		context.removeSessionValue("store");
		context.putPageValue("exception-report", errorlog.getLog());
	}
	context.putPageValue("logs", errorlog.getLog());
	//log.info("checked " + count + " records. updated " + made + " images" );
	
}


init();
