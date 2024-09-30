package org.entermediadb.modules.publishing;

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
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.View;
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;
import org.openedit.util.XmlUtil;

public class ContentManager implements CatalogEnabled {

	private static final Log log = LogFactory.getLog(ContentManager.class);
	protected XmlUtil fieldXmlUtil;
	
	public XmlUtil getXmlUtil()
	{
		return fieldXmlUtil;
	}


	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
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
		
		Map tosendparams = new HashMap();

		String createtype= entity.get("ai-lastcreationtype");
		
		Data targetmodule = getMediaArchive().getCachedData("module",inTargetentity);
		String extra = entity.get("ai-extrainstructions");
		
		if( extra == null)
		{
			extra = "Create a new " + targetmodule.getName();
		}
		tosendparams.put("inputdata", extra);
	
		
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
				//get fields
				String viewpath = inModuleid + "/" + data.getId();
				View view = (View)getMediaArchive().getPropertyDetailsArchive().getView(inModuleid, viewpath,null);
				for (Iterator iterator2 = view.iterator(); iterator2.hasNext();) {
					PropertyDetail detail = (PropertyDetail) iterator2.next();
					Object value = entity.getValue(detail.getId());
					//TODO: use SeacherMaager.getValue
					entitymetadata.put(detail.getId(),value);
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
		tosendparams.put("metadata", entitymetadata);
        
        //=Fish%20Recipie&entermedia.key=adminmd5420b06b0ea0d5066b0bb413837460f409108a0be38tstampeMxOa62cNXmuVomBh0oFNw==
        
		String url = "https://oneliveweb.com/oneliveweb/ditachat";
		CloseableHttpResponse resp = null;
		resp = getSharedConnection().sharedMimePost(url + "/llm/api/ditapayload.json",tosendparams);
		if (resp.getStatusLine().getStatusCode() != 200)
		{
			//error
			log.info("Remote Error: " + resp.getStatusLine().toString() ) ;
			getSharedConnection().release(resp);
			return;
		}
		JSONObject json = getSharedConnection().parseJson(resp);
	
		//Pare DITA xml stuff
		Data child = getMediaArchive().getSearcher(inTargetentity).createNewData();
		Map<String, Object> returned = (Map)json.get("metadata");
		
		for (Map.Entry<String, Object> entry : returned.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();
			child.setValue(key,val);
		}
		
		child.setValue("entity_date",new Date());
		child.setValue(inModuleid,inEntityid); //Lookup
		
		String xml = (String)json.get("xml");
		Element root = getXmlUtil().getXml(xml, "UTF-8");
		String name = root.attributeValue("id");
		child.setName(name);
		String id = PathUtilities.makeId(name);
		String path = child.getSourcePath() + "/" + id + ".xml";
		
		ContentItem item = getMediaArchive().getContent(path);
		
		//Version control?
		if( item.exists())
		{
			item.setMessage("replace");
			getMediaArchive().getPageManager().getRepository().saveVersion(item); //About to replace it
		}
		getXmlUtil().saveXml(root, item.getOutputStream(), "UTF-8");
		
		//Make asset
		Asset asset = getMediaArchive().getAssetImporter().createAsset(getMediaArchive(), path);
		getMediaArchive().getAssetImporter().getAssetUtilities().populateAsset(asset, item, getMediaArchive(), path, null);
		//send Thumbnail?
		
		//Needed?
		Category folder = getMediaArchive().getEntityManager().createDefaultFolder(entity, null);
		asset.addCategory(folder);
		getMediaArchive().saveData("asset",asset);
		
		child.setValue("primarymedia",asset.getId());
		getMediaArchive().saveData(inTargetentity,child);
			
	}
}
