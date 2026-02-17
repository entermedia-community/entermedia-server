package org.entermediadb.asset.mediadb;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.assistant.AssistantManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.scanner.AssetImporter;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.asset.util.JsonUtil;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities;


public class JsonDataModule extends BaseJsonModule 
{
	private static final Log log = LogFactory.getLog(JsonDataModule.class);

	protected JsonUtil fieldJsonUtil;
	
	
	public JsonUtil getJsonUtil()
	{
		if (fieldJsonUtil == null)
		{
			fieldJsonUtil = (JsonUtil)getModuleManager().getBean("jsonUtil");
		}
		return fieldJsonUtil;
	}


	public void setJsonUtil(JsonUtil inJsonUtil)
	{
		fieldJsonUtil = inJsonUtil;
	}


	public void handleSearch(WebPageRequest inReq)
	{
		//Could probably handle this generically, but I think they want tags, keywords etc.

		SearcherManager sm = (SearcherManager)inReq.getPageValue("searcherManager");
		
		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);

		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		HitTracker hits = null;
		if( inReq.getJsonRequest() == null)  //Todo: Catch invalid JSON
		{
			hits = searcher.getAllHits(inReq);
			//Add sort
		}
		else
		{
			hits = getJsonUtil().searchByJson(searcher ,inReq);
		}
		inReq.putPageValue("hits", hits);
		inReq.putPageValue("searcher", searcher);
		
		String search = (String)inReq.getPageValue("searchquery");
		log.info(search);
	}
	

	public void handleAiFunction(WebPageRequest inReq)
	{
		//Could probably handle this generically, but I think they want tags, keywords etc.
		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);

		Map request = inReq.getJsonRequest();
		
		String channelid = (String)request.get("channel");
		
		Data channel = archive.getCachedData("channel", channelid);
		if(channel == null)
		{
			channel = archive.getSearcher("channel").createNewData();
			channel.setId(channelid);
			channel.setValue("date",new Date());
			channel.setValue("refreshdate",new Date());
			String siteid = PathUtilities.extractDirectoryPath(catalogid);
			channel.setValue("chatapplicationid",siteid + "/find");
			archive.saveData("channel",channel);
		}
		
		AssistantManager assistantManager = (AssistantManager) getMediaArchive(catalogid).getBean("assistantManager");
		
		AgentContext context = assistantManager.loadContext(channelid);
		
		String entitymoduleid = (String)request.get("entitymoduleid");
		context.setValue("entitymoduleid",entitymoduleid);

		String entityid = (String)request.get("entityid");
		context.setValue("entityid",entityid);

		String pagename = inReq.getContentPage().getPageName();
		context.setFunctionName(pagename);
		
		Data aifunctionstart = archive.getCachedData("aifunction", pagename);
		inReq.putPageValue("aifunctionstart",aifunctionstart);
		inReq.putPageValue("aifunctionsearcher", archive.getSearcher("aifunction") );
		
		
		MultiValued usermessage = (MultiValued)archive.getSearcher("chatterbox").createNewData();
		usermessage.setValue("user", "agent");
		usermessage.setValue("channel",channelid);
		usermessage.setValue("date", new Date());
		String message = (String)request.get("message");
		usermessage.setValue("message", message);
		archive.saveData("chatterbox",usermessage);
		MultiValued agentmessage = assistantManager.newAgentMessage(usermessage,context);
		
		assistantManager.execCurrentFunctionFromChat(usermessage,agentmessage,context);

		context.addContext("agentmessage",agentmessage);
		context.addContext("usermessage",usermessage);
		
		context.setUserProfile(archive.getUserProfile("admin") );

		inReq.putPageValue("agentmessage",agentmessage);
		inReq.putPageValue("usermessage",usermessage);
		inReq.putPageValue("agentcontext",context);
		inReq.putPageValue("contextmap",context.getContext());
		inReq.putPageValue("chatterboxsearcher", archive.getSearcher("chatterbox") );
		inReq.putPageValue("agentcontextsearcher", archive.getSearcher("agentcontext" ) );
		
		String function = context.getNextFunctionName();
		if( function == null)
		{
			function = context.getFunctionName();
		}
		Data aifunction = archive.getCachedData("aifunction", function);
		inReq.putPageValue("aifunction",aifunction);
		

		
	}

	public Data createData(WebPageRequest inReq)
	{
		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		Data newdata = searcher.createNewData();
		
		checkAssetUploads(inReq, archive, searcher, newdata);

		String id = inReq.getRequestParameter("id");
		String sourcepath = inReq.getRequestParameter("sourcepath");

		JSONObject request = (JSONObject)inReq.getJsonRequest();
		if( request  == null)
		{
			request = new JSONObject();
			String[] fields = inReq.getRequestParameters("field");
			searcher.updateData(inReq, fields, newdata);
		}
		else
		{
			if(id == null) 
			{
				id = (String)inReq.getPageValue("id");
			}
			if (id != null && id.isEmpty()) 
			{
				id = null;
			}
			populateJsonData(request,searcher,newdata);
		}
		newdata.setId(id);
		newdata.setProperty("sourcepath", sourcepath);

		searcher.saveData(newdata, inReq.getUser());
		archive.fireDataEvent(inReq.getUser(),searcher.getSearchType(), "saved", newdata);

		inReq.putPageValue("searcher", searcher);
		inReq.putPageValue("data", newdata);
		
		return newdata;

	}
	
	public Data searchOrCreateData(WebPageRequest inReq)
	{

		String catalogid = findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);

		String searchtype = resolveSearchType(inReq);
		
		Searcher searcher = archive.getSearcher(searchtype);
		inReq.putPageValue("searcher", searcher);
		
		String name = inReq.getRequestParameter("name");
		if(name == null)
		{
			name = inReq.getRequestParameter("name.value");
		}

		Data data = (Data) searcher.searchByField("name", name);

		if(data == null)
		{
			data = createData(inReq);
		}
		
		inReq.putPageValue("data", data);
		

		return data;

	}
	


	protected void checkAssetUploads(WebPageRequest inReq, MediaArchive archive, Searcher searcher, Data newdata)
	{
		if (inReq.getRequest() != null && inReq.getRequest().getContentType() != null && inReq.getRequest().getContentType().toLowerCase().contains("multipart/form-data") ) 
		{
			FileUpload command = (FileUpload) archive.getBean("fileUpload");
			UploadRequest properties = command.parseArguments(inReq);

			for (Iterator iterator = searcher.getProperties().iterator(); iterator.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator.next();
				if( "asset".equals(detail.getListId()) )
				{
					//Save all the assets first
					Asset asset = createAssetForFileField(inReq,properties,detail);
					if( asset != null)
					{
						newdata.setValue(detail.getId(),asset.getId());
					}
				}
			}
		}
	}
	
	public Data loadData(WebPageRequest inReq)
	{

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);

		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		inReq.putPageValue("searcher", searcher);
		
		String id = getId(inReq);

		Data data = (Data) searcher.searchById(id);

		if(data == null)
		{
			//throw new OpenEditException("Asset was not found!");
			return null;
		}
		
		inReq.putPageValue("data", data);
		

		return data;

	}
	
	
	public void deleteData(WebPageRequest inReq)
	{

		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);

		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		String id = getId(inReq);

		Data data = (Data) searcher.searchById(id);

		if(data == null)
		{
			//throw new OpenEditException("Asset was not found!");
//			if( inReq.getResponse() != null)
//			{
//				inReq.getResponse().setStatus(404);
//			}
			inReq.setCancelActions(true);
			return;
		}
		searcher.delete(data, inReq.getUser());
		inReq.putPageValue("data", data);
		//inReq.putPageValue("searcher", searcher);

		

	}
	
	public String resolveSearchType(WebPageRequest inReq)
	{
		String	searchtype = inReq.getContentProperty("searchtype");
		if(searchtype == null)
		{
			String root  = inReq.getContentProperty("searchtyperoot");
			if( root != null)
			{
				String url = inReq.getPath();
				if(!url.endsWith("/"))
				{
					url = url + "/";
				}
				String ending = url.substring(root.length(), url.length());
				searchtype = ending.substring(1, ending.indexOf("/",1));
				searchtype = PathUtilities.extractPageName(searchtype);
			}
		}
		if(searchtype == null)
		{
			searchtype = inReq.findPathValue("searchtype");
		}
		return searchtype;
	}
	
	public void updateData(WebPageRequest inReq)
	{
		String catalogid =  findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
		String searchtype = resolveSearchType(inReq);
		Searcher searcher = archive.getSearcher(searchtype);
		Data newdata = loadData(inReq);
		if(newdata == null)
		{
			newdata = searcher.createNewData();
		}
		checkAssetUploads(inReq, archive, searcher, newdata);
		Map request = inReq.getJsonRequest();
		populateJsonData(request,searcher,newdata);
		searcher.saveData(newdata, inReq.getUser());
		archive.fireDataEvent(inReq.getUser(),searcher.getSearchType(), "saved", newdata);
		inReq.putPageValue("searcher", searcher);
		inReq.putPageValue("data", newdata);
	}
	
	public  void getUUID(WebPageRequest inReq) {
		Map request = inReq.getJsonRequest();
		String id = null;
		if (request != null) {
			id = (String)request.get("id");
		}
		if(id == null) {
			 id = UUID.randomUUID().toString();
		}
		inReq.putPageValue("id", id);
	}
	
	
	public Asset createAssetForFileField(WebPageRequest inReq, UploadRequest properties,PropertyDetail inDetails ) 
	{
		SearcherManager sm = (SearcherManager) inReq.getPageValue("searcherManager");

		String catalogid = findCatalogId(inReq);
		MediaArchive archive = getMediaArchive(inReq, catalogid);
	
		FileUploadItem item = properties.getUploadItemByName(inDetails.getId());
		if( item == null)
		{
			return null;
		}
		AssetImporter importer = archive.getAssetImporter();
		
		HashMap vals = new HashMap();
		vals.putAll(inReq.getParameterMap());  //Includes json

		if( inDetails.get("sourcepath") == null)
		{
			throw new OpenEditException("sourcepath must be set on " + inDetails);
		}

		//String sourcepath = importer.getAssetUtilities().createSourcePathFromMask(archive,inReq.getUser(), item.getName(), inDetails.get("sourcepath"), vals);
		String sourcepath = archive.replaceFromMask(inDetails.get("sourcepath"), null, "asset", vals, null);
		
		Asset asset = null;
		String 	id = (String) vals.get("id");
		if(id != null)
		{
			asset = archive.getAsset(id);
			if(asset != null )
			{
				sourcepath = asset.getSourcePath();
			}
		}
		
		String path = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath ;
		boolean foldrbased = false;
		if( path.endsWith("/"))
		{
			path = path + "/" + item.getName();
			foldrbased = true;
		}
		path = path.replace("//", "/");
		properties.saveFileAs(item, path, inReq.getUser());
		Page newfile = archive.getPageManager().getPage(path);
		// THis will NOT append the filename to the source path
		asset = importer.createAssetFromPage(archive, foldrbased, inReq.getUser(), newfile, id);

		importer.saveAsset(archive, inReq.getUser(), asset);

		return asset;
	}


	protected void extractVals(HashMap vals, MediaArchive archive, FileUploadItem item)
	{
		String fileName = item.getName();
		if (fileName != null) {
			vals.put("filename", fileName);
			String ext = PathUtilities.extractPageType(fileName);
			String render = archive.getMediaRenderType(ext);
			vals.put("extension", ext);
			vals.put("rendertype", render);
		}
		String guid = UUID.randomUUID().toString();
		String sguid = guid.substring(0, Math.min(guid.length(), 13));
		vals.put("guid", sguid);
		vals.put("splitguid", sguid.substring(0, 2) + "/" + sguid.substring(3).replace("-", ""));

		String df = DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyyMM");// new
																							// SimpleDateFormat("yyyyMM");
		vals.put("formatteddate", df);

		df = DateStorageUtil.getStorageUtil().formatDateObj(new Date(), "yyyy/MM");
		vals.put("formattedmonth", df);

		String importpath = (String)vals.get("importpath");
		if( importpath != null)
		{
			String filename = PathUtilities.extractFileName(importpath);
			vals.put("filename", filename);
		}
	}
	
	
}