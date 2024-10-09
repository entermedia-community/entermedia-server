package org.entermediadb.modules.publishing;

import java.io.File;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;

public class ContentModule extends BaseMediaModule {

	
	private static final Log log = LogFactory.getLog(ContentModule.class);

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
		String topmodule = inReq.findPathValue("topmodule");
		String entityid = inReq.getRequestParameter("entityid");
		String targetentity= inReq.getRequestParameter("moduleid");
		
		Data entity = getMediaArchive(inReq).getData(topmodule, entityid);

		String lastprompt= inReq.getRequestParameter("lastprompt.value");
		entity.setValue("lastprompt",lastprompt);
		
		getMediaArchive(inReq).saveData(topmodule,entity);
		
		ContentManager manager = getContentManager(inReq);		
		manager.createNewEntityFromAI(topmodule,entityid,targetentity);
		
	}
	public void loadDitaXml(WebPageRequest inReq)
	{
		String moduleid = inReq.findPathValue("module");
		String entityid = inReq.getRequestParameter("entityid");
		Data entity = getMediaArchive(inReq).getData(moduleid, entityid);
		String assetid = inReq.getRequestParameter("assetid");
		String renderformat = inReq.findValue("renderformat");
		Asset asset = getMediaArchive(inReq).getAsset(assetid);
		ContentManager manager = getContentManager(inReq);		
		
		//Make sure file is still here?
		ContentItem item = getMediaArchive(inReq).getOriginalContent(asset);
		
		//Load XML tree
		File file = new File(item.getAbsolutePath());
		Element root = manager.getXmlUtil().getXml(file, "UTF-8");
		
		inReq.putPageValue("rootelement",root);
		
		//Look for inlcudes
		
		//chchapter topicref
		Collection nodes = root.element("chapter").elements("topicref");
		inReq.putPageValue("chapters",nodes);
	}	
	public void loadVisual(WebPageRequest inReq)
	{
		String moduleid = inReq.findPathValue("module");
		String entityid = inReq.getRequestParameter("entityid");
		MediaArchive mediaArchive = getMediaArchive(inReq);
		Data entity = mediaArchive.getData(moduleid, entityid);
		String assetid = inReq.getRequestParameter("assetid");
		String renderformat = inReq.findValue("renderformat");
		Asset asset = mediaArchive.getAsset(assetid);
		ContentManager manager = getContentManager(inReq);		
		
		//Make sure file is still here?
		ContentItem item = mediaArchive.getOriginalContent(asset);
		if( !item.exists())
		{
			asset.setValue("editstatus", "7");
			mediaArchive.saveAsset(asset);
			return;
		}
		else if( "7".equals( asset.get("editstatus") ) )
		{
			asset.setValue("editstatus", "2"); //Undelete
			mediaArchive.saveAsset(asset);
		}
		
		String path = manager.loadVisual(moduleid,entity,renderformat, asset);
		inReq.putPageValue("renderedpath",path);
		String catpath = PathUtilities.extractDirectoryPath(path);
		Category cat= mediaArchive.getCategorySearcher().createCategoryPath(catpath);
		inReq.putPageValue("renderedcategory",cat);
	}
	public void loadXml(WebPageRequest inReq) throws Exception
	{
		String moduleid = inReq.findPathValue("module");
		String entityid = inReq.getRequestParameter("entityid");
		Data entity = getMediaArchive(inReq).getData(moduleid, entityid);
		String assetid = inReq.getRequestParameter("assetid");
		Asset asset = getMediaArchive(inReq).getAsset(assetid);
		ContentManager manager = getContentManager(inReq);		
		manager.loadTree(moduleid,entity,asset);
		//inReq.putPageValue("components",components);
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
	
	public void renderDitaTable(WebPageRequest inReq) throws Exception
	{
		String parentmodule = inReq.getRequestParameter("topmodule");
		String entityid = inReq.getRequestParameter("entityid");
		String targetmodule = inReq.getRequestParameter("submodule");
		//String renderformat = inReq.getRequestParameter("renderformat");

		MediaArchive mediaArchive = getMediaArchive(inReq);

		Data entity = mediaArchive.getCachedData(parentmodule, entityid);
		HitTracker children = mediaArchive.query(targetmodule).exact(parentmodule,entityid).sort("userchapter_number").sort("useritem_number").search();

		ContentManager manager = getContentManager(inReq);		
		manager.renderDita(inReq, parentmodule, entity, targetmodule, children, mediaArchive);

	}

	public void loadDitaViewer(WebPageRequest inReq) throws Exception
	{
		Data entity = (Data)inReq.getPageValue("entity");
		Data asset = (Data)inReq.getPageValue("asset");
		ContentManager manager = getContentManager(inReq);		
		Collection menu = (Collection)manager.findDitaAssets(entity); 
		if( asset == null && !menu.isEmpty())
		{
			asset = (Data)menu.iterator().next();
			inReq.putPageValue("asset",asset);
			String path = manager.loadVisual(entity.get("entitysourcetype"),entity,"html", asset);
			inReq.putPageValue("renderedpath",path);

		}
		inReq.putPageValue("found",menu);
	}	

	
}
