package org.entermediadb.modules.publishing;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.dom4j.Element;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.importer.DitaImporter;
import org.entermediadb.asset.util.JsonUtil;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.data.PropertyDetail;
import org.openedit.data.View;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;
import org.openedit.util.PathUtilities;
import org.openedit.util.XmlUtil;

public class ContentManager implements CatalogEnabled {

	private static final Log log = LogFactory.getLog(ContentManager.class);
	protected XmlUtil fieldXmlUtil;
	protected ModuleManager fieldModuleManager;
	
	
	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}


	public void setModuleManager(ModuleManager inModuleManager) {
		fieldModuleManager = inModuleManager;
	}


	public XmlUtil getXmlUtil()
	{
		return fieldXmlUtil;
	}


	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}

	protected Exec fieldExec;
	


	public Exec getExec() {
		return fieldExec;
	}


	public void setExec(Exec inExec) {
		fieldExec = inExec;
	}

	protected HttpSharedConnection fieldHttpSharedConnection;
	protected MediaArchive fieldMediaArchive;
	protected String fieldCatalogId;
	protected String fieldsavedapikey = "null";
	public String getCatalogId() {
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId) {
		fieldCatalogId = inCatalogId;
	}

	public MediaArchive getMediaArchive() {
		
		if( fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(),"mediaArchive");
		}
			
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive) {
		fieldMediaArchive = inMediaArchive;
	}

	public HttpSharedConnection getSharedConnection()
	{
		String api = getMediaArchive().getCatalogSettingValue("apikeyoneliveweb");
		
		if (fieldHttpSharedConnection == null || !fieldsavedapikey.equals(api))
		{
			HttpSharedConnection connection = new HttpSharedConnection();
			connection.addSharedHeader("X-tokentype", "entermedia");
			connection.addSharedHeader("X-token", api);
			fieldHttpSharedConnection = connection;
		}

		return fieldHttpSharedConnection;
	}


	public void createNewEntityFromAI(String inModuleid, String inEntityid, String inTargetentity) {

		Data entity = getMediaArchive().getData(inModuleid, inEntityid);
		
		//https://oneliveweb.com/oneliveweb/ditachat/llm/api/ditapayload.json?inputdata=Fish%20Recipie&entermedia.key=adminmd5420b06b0ea0d5066b0bb413837460f409108a0be38tstampeMxOa62cNXmuVomBh0oFNw==
		
		Map inputdata = new HashMap();

		Data targetmodule = getMediaArchive().getCachedData("module",inTargetentity);
		String extra = entity.get("lastprompt");
		
		if( extra == null)
		{
			extra = "Create a new " + targetmodule.getName();
		}
		inputdata.put("directions", extra);
	
		
		//Loop over all the tabs on the UI

		Collection existingdata = new ArrayList();
		Map entitymetadata = new HashMap();
		
		Collection views = getMediaArchive().query("view").exact("moduleid", inModuleid).exact("systemdefined",false).search();
		for (Iterator iterator = views.iterator(); iterator.hasNext();) {
			Data data = (Data) iterator.next();
			//See if its data or lookup
			String render = data.get("rendertype");
			if( render == null)
			{
				JsonUtil util = new JsonUtil();
				//get fields
				String viewpath = inModuleid + "/" + data.getId();
				View view = (View)getMediaArchive().getPropertyDetailsArchive().getView(inModuleid, viewpath,null);
				for (Iterator iterator2 = view.iterator(); iterator2.hasNext();) {
					PropertyDetail detail = (PropertyDetail) iterator2.next();
					Object value = entity.getValue(detail.getId());
					if( value != null)
					{
						//TODO: use SeacherMaager.getValue
						if( value instanceof Date)
						{
							value = util.formatDateObj(value);
						}
						entitymetadata.put(detail.getId(),value);
					}
				}
			}
			else if(render.equals("table") )
			{
				//get children such as recipies
				
			}
			else if(render.equals("asset") )
			{
				//get a list of files as URLs
			}
		}
		inputdata.put("metadata", entitymetadata);
        
        //=Fish%20Recipie&
		//entermedia.key=adminmd5420b06b0ea0d5066b0bb413837460f409108a0be38tstampeMxOa62cNXmuVomBh0oFNw==
        
		String url = "https://oneliveweb.com/oneliveweb/ditachat";
		CloseableHttpResponse resp = null;
		
		JSONObject obj = new JSONObject();
		obj.put("inputdata",inputdata);
		log.info("Sending: \n" + obj.toJSONString() );
		resp = getSharedConnection().sharedPostWithJson(url + "/llm/api/ditapayload.json?entermedia.key=adminmd5420b06b0ea0d5066b0bb413837460f409108a0be38tstampeMxOa62cNXmuVomBh0oFNw==",obj);
		if (resp.getStatusLine().getStatusCode() != 200)
		{
			//error
			log.info("Remote Error: " + resp.getStatusLine().toString() ) ;
			getSharedConnection().release(resp);
			return;
		}
		JSONObject json = getSharedConnection().parseJson(resp);
		log.info("Received: \n" + json.toJSONString() );
		//Pare DITA xml stuff
		Data child = getMediaArchive().getSearcher(inTargetentity).createNewData();
		Map<String, Object> returned = (Map)json.get("metadata");
		
		for (Map.Entry<String, Object> entry : returned.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();
			child.setValue(key,val);
		}
		String xml = (String)json.get("xml");
		child.setValue("ditatopic", xml);
		
		child.setValue("entity_date",new Date());
		child.setValue(inModuleid,inEntityid); //Lookup
		
		Element root = getXmlUtil().getXml(xml, "UTF-8");
		String name = root.attributeValue("id");
		child.setName(name);
		String id = PathUtilities.makeId(name);
		
		
		Category folder = getMediaArchive().getEntityManager().createDefaultFolder(entity, null);
		
		
		String basesourcepath = folder.getCategoryPath() + "/AI/" + id + ".dita";
		String rootpath = "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/originals/";
		String path =  rootpath+ basesourcepath;
	
				
		ContentItem item = getMediaArchive().getContent(path);
		
		//Version control?
		if( item.exists())
		{
			item.setMessage("replace");
			getMediaArchive().getPageManager().getRepository().saveVersion(item); //About to replace it
		}
		getXmlUtil().saveXml(root, item.getOutputStream(), "UTF-8");
		
		Page outdirectory = getMediaArchive().getPageManager().getPage(rootpath + folder.getCategoryPath() +"/AI/");
		Collection assetids = getMediaArchive().getAssetImporter().processOn(outdirectory.getPath(), outdirectory.getPath(),true,getMediaArchive(), null);
		
		//Save to Question Area? Or parent or both
		Asset asset = getMediaArchive().getAssetBySourcePath(basesourcepath);
		if( asset != null)
		{
			asset.addCategory(folder);
			getMediaArchive().saveData("asset",asset);
			child.setValue("primarymedia",asset.getId());
		}

		//send Thumbnail?
		//Needed?
		getMediaArchive().saveData(inTargetentity,child);
			
	}
	
	public String loadVisual(String inModuleId, Data inEntity, Asset inDita)
	{
		ContentItem item = getMediaArchive().getOriginalContent(inDita);
		 //bin/dita -i ../90130_SPC_C-EFM/DITA-OUTPUT/90130_SPC_C-EFM/90130_SPC_C-EFM.ditamap -o out -f html 
		//output a folder of HTML and read it in
		Collection<String> args = new ArrayList();
		args.add("-i");
		args.add(item.getAbsolutePath());
		
		Category cat = getMediaArchive().getEntityManager().createDefaultFolder(inEntity, null);
		
		String stub = PathUtilities.extractPageName(inDita.getName());
		String root = "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/originals/";
		String basesourcepath = cat.getCategoryPath() +"/Rendered/" + stub;
		Page outdirectory = getMediaArchive().getPageManager().getPage(root + basesourcepath);

		String outputpath = findOutputSourcePath(item, basesourcepath);
		Page finalpage = getMediaArchive().getPageManager().getPage(root + outputpath); //mkdir
		
		if(finalpage.exists()) //Reload?
		{
			return outputpath;
		}
		//getMediaArchive().getPageManager().putPage(finalpage); //mkdir

		args.add("-o");
		args.add(outdirectory.getContentItem().getAbsolutePath());
		
		args.add("-f");
		args.add("html5");
		
		getExec().runExec("dita", args);
		
		//Now import assets like crazy?
		Collection assetids = getMediaArchive().getAssetImporter().processOn(outdirectory.getPath(), outdirectory.getPath(),true,getMediaArchive(), null);
		
		//Load all the HTML?
		return outputpath;
	}


	protected String findOutputSourcePath(ContentItem item, String basesourcepath ) 
	{
		File xml = new File(item.getAbsolutePath());
		Element input = getXmlUtil().getXml(xml, "UTF-8");
		String finalpage = null;
		if( "bookmap".equals(input.getName()) )
		{
			finalpage = basesourcepath + "/index.html";	
		}
		else if( "learningAssessment".equals(input.getName()) )
		{
			finalpage = basesourcepath + "/learningassesment/" + PathUtilities.extractPageName( item.getName() ) + ".html";
		}
		return finalpage;
	}
	
	public Collection findDitaAssets(Data inEntity)
	{
		Category cat = getMediaArchive().getEntityManager().createDefaultFolder(inEntity, null);
		Collection assets = getMediaArchive().query("asset").exact("category", cat).orgroup("fileformat", "ditamap").search();
		return assets;
	}
	
	public void loadTree(String inModuleId, Data inEntity, Asset inDita) throws Exception
	{
		
		//See if we have data already. If not check on version?
		
		ContentItem item = getMediaArchive().getOriginalContent(inDita);
		DitaImporter oniximporter = new DitaImporter();
		oniximporter.setMediaArchive(getMediaArchive());
		oniximporter.setMakeId(false);
		
		Data module = getMediaArchive().getCachedData("module", inModuleId);
		oniximporter.setModule(module);
		oniximporter.setAsset(inDita);
		oniximporter.setEntity(inEntity);
		oniximporter.importData();
		
		//Search using jquery
	}
}
