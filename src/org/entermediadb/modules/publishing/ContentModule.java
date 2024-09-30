package org.entermediadb.modules.publishing;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public class ContentModule extends BaseMediaModule {

	
	public void createHtmlView(WebPageRequest inReq)
	{
		//Check the type? Run the conversion
		String moduleid = inReq.findPathValue("module");
		String entityid = inReq.getRequestParameter("entityid");
		
		MediaArchive archive = getMediaArchive(inReq);
		Data entity = archive.getData(moduleid,entityid);
		//Exec
//		Exec exec = (Exec)getModuleManager().getBean("exec");
//		ExecResult res = exec.runExec("restartdocker", null, true);
//		log.info( "Restarting site: " + res.getStandardOut() );
//		inReq.putPageValue("result", res);

		//PDF?
	}

	public void createNewEntityFromAI(WebPageRequest inReq)
	{
		//Add as child
		String moduleid = inReq.findPathValue("module");
		String entityid = inReq.getRequestParameter("entityid");
		String targetentity= inReq.getRequestParameter("targetentity");
		
		Data entity = getMediaArchive(inReq).getData(moduleid, entityid);

		String extrainstructions= inReq.getRequestParameter("ai-extrainstructions");
		String lastcreationtype= inReq.getRequestParameter("ai-lastcreationtype");
		entity.setValue("ai-extrainstructions",extrainstructions);
		entity.setValue("ai-lastcreationtype",lastcreationtype);
		getMediaArchive(inReq).saveData(moduleid,entity);
		
		ContentManager manager = getContentManager(inReq);		
		manager.createNewEntityFromAI(moduleid,entityid,targetentity);
		
	}
	
	protected ContentManager getContentManager(WebPageRequest inReq)
	{
		MediaArchive archive  = getMediaArchive(inReq);
		ContentManager manager = (ContentManager)archive.getBean("contentManager");
		if( manager.getMediaArchive() == null)
		{
			manager.setMediaArchive(archive);
		}
		return manager;
	}
	
}
