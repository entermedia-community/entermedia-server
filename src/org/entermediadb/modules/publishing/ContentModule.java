package org.entermediadb.modules.publishing;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.llm.LLMManager;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.data.ViewItem;
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

	public void createNewEntityFromAI(WebPageRequest inReq) throws Exception
	{
		//Add as child
		Data entitypartentview = (Data) inReq.getPageValue("entitymoduleviewdata");		
		Data entity = (Data) inReq.getPageValue("entity");
		Data entitymodule = (Data) inReq.getPageValue("entitymodule");

		String submodsearchtype = entitypartentview.get("rendertable");		

		String lastprompt= inReq.getRequestParameter("lastprompt.value");
		entity.setValue("lastprompt",lastprompt);
		
		MediaArchive archive = getMediaArchive(inReq);
		archive.saveData(entitymodule.getId(),entity);
		
		ContentManager manager = getContentManager(inReq);	
		String model = inReq.findValue("llmmodel.value");
		Data modelinfo = archive.getData("llmmodel", model);
				
		String type = modelinfo != null ?  modelinfo.get("llmtype") : null;
		
		if(type == null) {
			type = "gptManager";			
		} else {
			type = type + "Manager";
		}
		LLMManager llm = (LLMManager) archive.getBean(type);

		Data newdata = manager.createFromLLM(inReq,llm, model,entitymodule.getId(),entity.getId(),submodsearchtype);
		boolean createassets = Boolean.parseBoolean(inReq.findValue("createassets"));
		Searcher targetsearcher = archive.getSearcher(submodsearchtype);

		if(createassets) {
			

			Collection <PropertyDetail> details = targetsearcher.getDetailsForView(targetsearcher.getSearchType() + "addnew");
			
			for (Iterator iterator = details.iterator(); iterator.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator.next();
			    if (detail.isList() && "asset".equals(detail.getListId())) {
			        inReq.putPageValue("detail", detail);
			        inReq.putPageValue("newdata", newdata);

			        String template = llm.loadInputFromTemplate(inReq, archive.getCatalogHome() + "/gpt/templates/createentityassets.html");
			        Category rootcat = archive.getEntityManager().loadDefaultFolder(entitymodule, entity, inReq.getUser());
			        String sourcepathroot = rootcat.getCategoryPath();
			        Asset asset = manager.createAssetFromLLM(inReq, sourcepathroot, template);
			        asset.addCategory(rootcat);
			        archive.saveAsset(asset);
			        log.info("Saving asset as " + detail.getName() + ": " + detail.getId());
			        newdata.setValue(detail.getId(), asset.getId());

			        // Break out of the loop for now...
			    }
			}
			
			
			targetsearcher.saveData(newdata);

		}
	}
	
	public void createNewAssetsWithAi(WebPageRequest inReq) throws Exception
	{
		//Add as child
		Data entitypartentview = (Data) inReq.getPageValue("entitymoduleviewdata");		
		Data entity = (Data) inReq.getPageValue("entity");
		Data entitymodule = (Data) inReq.getPageValue("entitymodule");
	
		String lastprompt= inReq.getRequestParameter("createassetprompt.value");
		entity.setValue("createassetprompt",lastprompt);
		getMediaArchive(inReq).saveData(entitymodule.getId(),entity);
		ContentManager manager = getContentManager(inReq);		
		String type = inReq.findValue("llmtype.value");
		if(type == null) {
			type = "gptManager";			
		} else {
			type = type + "Manager";
		}
		LLMManager llm = (LLMManager) getMediaArchive(inReq).getBean(type);
		String edithome = inReq.findPathValue("edithome");
		String template = llm.loadInputFromTemplate(inReq,edithome+ "/aitools/createnewasset.html");
		manager.createAssetFromLLM(inReq,entitymodule.getId(),entity.getId(),template);
		
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
		
		String path = manager.loadVisual(entity,renderformat, asset);
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
		String entitymoduleid = inReq.getRequestParameter("entitymoduleid");
		String entityid = inReq.getRequestParameter("entityid");
		String targetmodule = inReq.findPathValue("submoduleid");
		
		MediaArchive mediaArchive = getMediaArchive(inReq);

		Data entity = mediaArchive.getCachedData(entitymoduleid, entityid);
		ContentManager manager = getContentManager(inReq);		
		manager.renderDita(inReq, entitymoduleid, entity, targetmodule);

	}

	public void loadDitaViewer(WebPageRequest inReq) throws Exception
	{
		Data entity = (Data)inReq.getPageValue("entity");
		Data asset = (Data)inReq.getPageValue("asset");
		ContentManager manager = getContentManager(inReq);		
		Collection menu = (Collection)manager.findDitaAssets(entity); 
		if( menu == null)
		{
			log.error("No menu");
			return;
		}
		if( asset == null && !menu.isEmpty())
		{
			asset = (Data)menu.iterator().next();
			inReq.putPageValue("asset",asset);
			String path = manager.loadVisual(entity,"html", asset);
			inReq.putPageValue("renderedpath",path);

		}
		inReq.putPageValue("found",menu);
	}	

	
}
