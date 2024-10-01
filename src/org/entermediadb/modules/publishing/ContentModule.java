package org.entermediadb.modules.publishing;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;

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
	
	public void loadVisual(WebPageRequest inReq)
	{
		String moduleid = inReq.findPathValue("module");
		String entityid = inReq.getRequestParameter("entityid");
		Data entity = getMediaArchive(inReq).getData(moduleid, entityid);
		String assetid = inReq.getRequestParameter("assetid");
		Asset asset = getMediaArchive(inReq).getAsset(assetid);
		ContentManager manager = getContentManager(inReq);		
		String path = manager.loadVisual(moduleid,entity,asset);
		inReq.putPageValue("renderedpath",path);
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
		String entityid = inReq.getRequestParameter("entityid");
		String parentmodule = inReq.getRequestParameter("topmodule");

		String targetmodule = inReq.getRequestParameter("entitymodule");
		final String externalfieldname = inReq.getRequestParameter("fieldexternalid");
		final String externalfieldvalue = inReq.getRequestParameter("fieldexternalvalue");

		MediaArchive mediaArchive = getMediaArchive(inReq);

		Data entity = mediaArchive.getCachedData(parentmodule, entityid);
		Category cat = mediaArchive.getEntityManager().createDefaultFolder(entity, null);
		
		HitTracker children = mediaArchive.query(targetmodule).exact(externalfieldname,externalfieldvalue).search();
		
		//Render DITAS for each question and a map
		String appid = inReq.findPathValue("applicationid");
		Page ditatemplate = mediaArchive.getPageManager().getPage("/" + appid + "/views/modules/" + parentmodule + "/components/entities/renderdita/templatedita.dita");
		PropertyDetail detail = mediaArchive.getSearcher(targetmodule).getDetail("name");
		WebPageRequest newcontext = inReq.copy(ditatemplate);
		Collection savedtopics = new ArrayList();
		String root = "/WEB-INF/data/" + mediaArchive.getCatalogId() + "/originals/";
		int it = 0;
		for (Iterator iterator = children.iterator(); iterator.hasNext();) 
		{
			Data subentity = (Data) iterator.next();
			newcontext.putPageValue("subdata",subentity);
			String name = (String)mediaArchive.getSearcherManager().getValue(subentity, detail, null);
			name= name.replace('/', '-');
			newcontext.putPageValue("entityname",name);

			StringWriter output = new StringWriter();
			ditatemplate.generate(newcontext, output);

			String ending = "learningassesment/" + name + ".dita";
			String basesourcepath = cat.getCategoryPath() +"/Rendered/" + ending;
			Page outputfile = mediaArchive.getPageManager().getPage(root + basesourcepath);
			//Save content
			mediaArchive.getPageManager().saveContent(outputfile, inReq.getUser(), output.toString(), "Generated DITA");
			savedtopics.add(ending);
			if( it > 10)
			{
				break;
			}
			it++;
		}
		
		Page ditatemplatemap = mediaArchive.getPageManager().getPage("/" + appid + "/views/modules/" + parentmodule + "/components/entities/renderdita/templateditamap.ditamap");

		String exportname = inReq.getRequestParameter("exportname");
		if( exportname == null)
		{
			exportname = entity.getName() + ".ditamap";
			exportname  = exportname.replace('/', '-');
		}
		

		StringWriter output = new StringWriter();
		newcontext = inReq.copy(ditatemplatemap);
		newcontext.putPageValue("exportname", entity.getName());
		newcontext.putPageValue("savedtopics",savedtopics);
		ditatemplatemap.generate(newcontext, output);
		
		
		String basesourcepath = cat.getCategoryPath() +"/Rendered/" + exportname;
		Page outputfile = mediaArchive.getPageManager().getPage(root + basesourcepath);
		//Save content
		mediaArchive.getPageManager().saveContent(outputfile, inReq.getUser(), output.toString(), "Generated DITAMMAP");

		//Now import assets like crazy?
		Page outdirectory = mediaArchive.getPageManager().getPage(root + cat.getCategoryPath() +"/Rendered/");
		Collection assetids = mediaArchive.getAssetImporter().processOn(outdirectory.getPath(), outdirectory.getPath(),true,mediaArchive, null);
		
		//Save to Question Area? Or parent or both
		ContentManager manager = (ContentManager)mediaArchive.getBean("contentManager");
		Asset asset = mediaArchive.getAssetBySourcePath(basesourcepath);
		if( asset != null)
		{
			manager.loadVisual(parentmodule, entity, asset);
		}


	}
	
}
