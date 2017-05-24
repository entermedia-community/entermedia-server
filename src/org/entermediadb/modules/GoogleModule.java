package org.entermediadb.modules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.google.GoogleManager;
import org.openedit.Data;
import org.openedit.WebPageRequest;

public class GoogleModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(GoogleModule.class);

	public GoogleManager getGoogleManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		return (GoogleManager)getModuleManager().getBean(catalogid,"googleManager");
	}


	public void syncAssets(WebPageRequest inReq) throws Exception
	{

		MediaArchive archive = getMediaArchive(inReq);
		Data authinfo = archive.getData("oauthprovider", "google");
		log.info("Running syncAssets");
		getGoogleManager(inReq).syncAssets(authinfo);

	}

}
