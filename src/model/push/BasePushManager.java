package model.push;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.push.PushManager;
import org.entermediadb.asset.search.AssetSearcher;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.data.ImmutableData;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.page.manage.PageManager;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.PathUtilities;
import org.openedit.util.XmlUtil;

public abstract class BasePushManager  implements PushManager{

	protected SearcherManager fieldSearcherManager;
	protected PageManager fieldPageManager;
	protected XmlUtil xmlUtil = new XmlUtil();
	static final Log log = LogFactory.getLog(BasePushManager.class);

	public BasePushManager() {
		super();
	}

	public UserManager getUserManager(String inCatalogId) {
		return 	(UserManager)getSearcherManager().getModuleManager().getBean(inCatalogId,"userManager");
	}

	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}

	

	public void processPushQueue(MediaArchive archive, User inUser) {
		processPushQueue(archive,null,inUser);
	}

	public void processPushQueue(MediaArchive archive, String inAssetIds, User inUser) {
		
		
		//field=importstatus&importstatus.value=complete&operation=matches&field=pushstatus&pushstatus.value=complete&operation=not&field=pushstatus&pushstatus.value=nogenerated&operation=not&field=pushstatus&
		//pushstatus.value=error&operation=not&field=pushstatus&pushstatus.value=deleted&operation=not
		
		//Searcher hot = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "hotfolder");
		Searcher searcher = archive.getAssetSearcher();
		SearchQuery query = searcher.createSearchQuery();
		if( inAssetIds == null )
		{
			//query.addMatches("category","index");
			query.addMatches("importstatus","complete");
			query.addNot("pushstatus","complete");
			query.addNot("pushstatus","nogenerated");
			query.addNot("pushstatus","error");
			query.addNot("pushstatus","deleted");
			query.addNot("editstatus","7");
		}
		else
		{
			String assetids = inAssetIds.replace(","," ");
			query.addOrsGroup( "id", assetids );
		}
		query.addSortBy("assetmodificationdate");
		HitTracker hits = searcher.search(query);
		hits.setHitsPerPage(1000);
		if( hits.size() == 0 )
		{
			log.info("No new assets to push");
			return;
		}
		log.info("processing " + hits.size() + " assets to push");
		List savequeue = new ArrayList();
		int noasset = 0;
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{			
			Data hit = (Data) iterator.next();
			Asset asset = (Asset) searcher.loadData(hit);
			if( asset != null )
			{
				savequeue.add(asset);
				if( savequeue.size() > 100 )
				{
					pushAssets(archive, savequeue);
					savequeue.clear();
				}
			}
			else
			{
				noasset++;
			}
		}
		log.info("Could not load " + noasset + " assets");
		if( savequeue.size() > 0 )
		{
			pushAssets(archive, savequeue);
			savequeue.clear();
		}
	}

	

	public void resetPushStatus(MediaArchive inArchive, String oldStatus, String inNewStatus) {
		AssetSearcher assetSearcher = inArchive.getAssetSearcher();
		List savequeue = new ArrayList();
		HitTracker hits = assetSearcher.fieldSearch("pushstatus", oldStatus);
		hits.setHitsPerPage(1000);
	
		int size = 0;
		while(true)
		{
			size = hits.size();
			for (Iterator iterator = hits.getPageOfHits().iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				Asset asset = inArchive.getAssetBySourcePath(data.getSourcePath());
				if( asset == null )
				{
					log.error("Missing asset" + data.getSourcePath());
					continue;
				}
				asset.setValue("pushstatus", inNewStatus);
				savequeue.add(asset);
				if( savequeue.size() == 1000 )
				{
					assetSearcher.saveAllData(savequeue, null);
					savequeue.clear();
				}
			}
			assetSearcher.saveAllData(savequeue, null);
			savequeue.clear();
			hits = assetSearcher.fieldSearch("pushstatus", oldStatus);
			hits.setHitsPerPage(1000);
			log.info(hits.size() + " remaining status updates " + oldStatus );
			if( hits.size() == 0 || size > hits.size() )
			{
				break;
			}
		} 
		
		
	}

	public Collection getCompletedAssets(MediaArchive inArchive) {
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("pushstatus", "complete");
		return hits;
	}

	public Collection getPendingAssets(MediaArchive inArchive) {
		SearchQuery query = inArchive.getAssetSearcher().createSearchQuery();
		query.addMatches("importstatus","complete");
		query.addNot("pushstatus","complete");
		query.addNot("pushstatus","nogenerated");
		query.addNot("pushstatus","error");
		query.addNot("pushstatus","deleted");
		query.addNot("editstatus","7");
	
	
		HitTracker hits = inArchive.getAssetSearcher().search(query);
		log.info("Found "+ hits.size() +" Ready to be pushed");
		return hits;
	}

	public Collection getNoGenerated(MediaArchive inArchive) {
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("pushstatus", "nogenerated");
		return hits;
	}

	public Collection getErrorAssets(MediaArchive inArchive) {
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("pushstatus", "error");
		return hits;
	}

	public Collection getImportCompleteAssets(MediaArchive inArchive) {
		SearchQuery query = inArchive.getAssetSearcher().createSearchQuery();
		//query.addMatches("category","index");
		query.addMatches("importstatus","complete");
		query.addNot("editstatus","7");
	
		//Push them and mark them as pushstatus deleted
		HitTracker hits = inArchive.getAssetSearcher().search(query);
		return hits;
	}

	public Collection getImportPendingAssets(MediaArchive inArchive) {
		SearchQuery query = inArchive.getAssetSearcher().createSearchQuery();
		query.addMatches("importstatus","imported");
		query.addNot("editstatus","7");
	
		//Push them and mark them as pushstatus deleted
		HitTracker hits = inArchive.getAssetSearcher().search(query);
		return hits;
	}

	public Collection getImportErrorAssets(MediaArchive inArchive) {
		HitTracker hits = inArchive.getAssetSearcher().fieldSearch("importstatus", "error");
		return hits;
	}

	


	

	@Override
	public void acceptPush(WebPageRequest inReq, MediaArchive archive) {
			FileUpload command = new FileUpload();
			command.setPageManager(archive.getPageManager());
			UploadRequest properties = command.parseArguments(inReq);
	
			String id = inReq.getRequestParameter("id");
			Asset target = archive.getAsset(id);
			
			
			String sourcepath = inReq.getRequestParameter("sourcepath");
			
			if (target == null)
			{
				target = (Asset) archive.getAssetSearcher().createNewData();
				target.setId(id);
				target.setSourcePath(sourcepath);
			}
			
			
			String categorypath = PathUtilities.extractDirectoryPath(sourcepath);
			Category category = archive.getCategorySearcher().createCategoryPath(categorypath);
			archive.getCategorySearcher().saveData(category);
			target.addCategory(category);
			
			String[] fields = inReq.getRequestParameters("field");
			
			//Make sure we ADD libraries not replace them
			String editstatus = inReq.getRequestParameter("editstatus.value");
			String k4processed = inReq.getRequestParameter("k4processed.value");
			log.info("OVERRIDE: " + "override".equals(editstatus));
			
			if("true".equals(k4processed) || "override".equals(editstatus)) 
			{
				
				log.info("OVERRIDE WAS " + editstatus);
				log.info("Fields were:");
				for (int i = 0; i < fields.length; i++) {
					String string = fields[i];
					if("description".equals(string)) {
						fields[i] = "";
					}
					if("id".equals(string)) {
						fields[i] = "";
					}
					String val = inReq.getRequestParameter(string + ".value");
					log.info(string + ":" + val);

				}
				target.setValue("description", null);
				target.setId(id);

				//THIS IS NOT WORKING?
				archive.getAssetSearcher().updateData(inReq, fields, target);
			}
			else
			{
				archive.getAssetSearcher().updateData(inReq, fields, new ImmutableData(target));
			}
			if( editstatus != null )
			{
				target.setValue("editstatus", editstatus);
			}
			String keywords = inReq.getRequestParameter("keywords");
			if( keywords != null )
			{
				String[] keys =  keywords.split("\\|");
				for (int i = 0; i < keys.length; i++)
				{
					target.addKeyword(keys[i]);
				}
			}
			
			List<FileUploadItem> uploadFiles = properties.getUploadItems();
	
	
			String	generatedsaveroot = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + sourcepath;
			String	originalsaveroot = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath;
			
			//String originalsroot = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcepath + "/";
	
			if (uploadFiles != null)
			{
				archive.removeGeneratedImages(target, true);
				Iterator<FileUploadItem> iter = uploadFiles.iterator();
				while (iter.hasNext())
				{
					FileUploadItem fileItem = iter.next();
					String inputName = fileItem.getFieldName();
					if( inputName.startsWith("original") )
					{
	//					if( target.isFolder())
	//					{
	//						properties.saveFileAs(fileItem, originalsaveroot + "/" + target.getMediaName(), inReq.getUser());
	//					}
	//					else
	//					{
							properties.saveFileAs(fileItem, originalsaveroot, inReq.getUser());
	//					}
					}
					else if( fileItem.getName().equals( "fulltext.txt"))
					{
						properties.saveFileAs(fileItem, "/WEB-INF/data/" + archive.getCatalogId() + "/assets/" + sourcepath + "/fulltext.txt", inReq.getUser());
					}
					else
					{
						properties.saveFileAs(fileItem, generatedsaveroot + "/" + fileItem.getName(), inReq.getUser());
					}
				}
			}
			log.info("Received a pushed asset: " + target.getId() +" at " + target.getSourcePath() +  " : "  + "details(k4 / edit status) : " + k4processed + " / " + editstatus );
			archive.getAssetSearcher().saveData(target);
			archive.fireMediaEvent("importing","pushassetimported", inReq.getUser(), target);
	
		}
		/*
		public void pullApprovedAssets(WebPageRequest inReq, MediaArchive inArchive){
			log.info("pulling approved assets from remote server");
			Map<String,Properties> map = getApprovedAssets(inArchive);
			log.info("found the following files, $map");
			if (!map.isEmpty()){
				processApprovedAssets(inArchive,map);
				log.info("finished pull");
			} else{
				log.info("no files approved on remote server, returning");
			}
		}
		
		 * Gets the approved assets (that are not marked for deletion) from remote server
		 * @param inArchive
		 * @return
		protected HashMap<String,Properties> getApprovedAssets(MediaArchive inArchive) {
			log.info("getApprovedAssets starting");
			String server = inArchive.getCatalogSettingValue("push_server_url");
			String remotecatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");
			log.info("push_server_url = $server, push_target_catalogid = $remotecatalogid");
			String [] inFields = {"approvalstatus", "editstatus"};
			String [] inValues = {"approved", "7"};
			String [] inOperations = {"matches", "not"};
	
			String url = server + "/media/services/rest/assetsearch.xml";
			HttpPost method = new HttpPost(url);
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			
			builder.addPart("catalogid", "remotecatalogid");
	
			for(int i=0; i<inFields.length; i++){
				builder.addPart("field", "inFields[i]");
				builder.addPart("operation", inOperations[i]);
				builder.addPart(inFields[i] + ".value", inValues[i]);
			}
			log.info("executing $remotecatalogid, $method");
			method.setEntity(builder.build());
			Element root = execute(remotecatalogid,method);
			method.releaseConnection();
			Element hits = (Element)root.elements().get(0);
			
			int pages = Integer.parseInt(hits.attributeValue("pages"));
			String sessionid = hits.attributeValue("sessionid");
			
			log.info("found $pages, $sessionid, $root");
			Map<String, Properties> map = new HashMap<String, Properties>();
			addHits(hits, map);
			
			url = server + "/media/services/rest/getpage.xml";
			for( int i = 2; i <= pages; i++ )
			{
				method = new HttpPost(url);
				builder = MultipartEntityBuilder.create();
	
				builder.addPart("catalogid", remotecatalogid);
				builder.addPart("hitssessionid", sessionid);
				builder.addPart("page", String.valueOf(i));
				root = execute(remotecatalogid,method);
				method.releaseConnection();
				hits = (Element)root.elements().get(0);
				addHits(hits, map);
			}
			return (HashMap<String,Properties>) map;
		}
		
		protected void addHits(Element inHits, Map<String, Properties> inResults){
			Iterator<?> hits = inHits.elements("hit").iterator();
			while (hits.hasNext()){
				Element e = (Element) hits.next();
				Properties props = new Properties();
				Iterator<Attribute> attributes = e.attributeIterator();
				while(attributes.hasNext()){
					Attribute attr = attributes.next();
					String n = attr.getName();
					String v = attr.getValue();
					if (n.equalsIgnoreCase("id")){
						inResults.put(v, props);
					} else {
						props.put(n,v);
					}
				}
				Iterator<Element> elements = e.elementIterator();
				while(elements.hasNext()){
					Element element = elements.next();
					String n = element.getName();
					String v = element.getText();
					props.put(n,v);
				}
			}
		}
		
		protected void processApprovedAssets(MediaArchive inArchive, Map<String,Properties> inMap){
			String catalogid = inArchive.getCatalogId();
			String server = inArchive.getCatalogSettingValue("push_server_url");
			String remotecatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");
			String exportpath = inArchive.getCatalogSettingValue("push_download_exportpath");
			if (exportpath == null){
				exportpath = "/WEB-INF/data/${catalogid}/originals/";
			} else if (exportpath.startsWith("/")){
				exportpath = "/WEB-INF/data/${catalogid}/originals${exportpath}";
			} else {
				exportpath = "/WEB-INF/data/${catalogid}/originals/${exportpath}";
			}
			if (!exportpath.endsWith("/")){
				exportpath = "${exportpath}/";
			}
			Iterator<String> itr = inMap.keySet().iterator();
			while(itr.hasNext()){
				String key = itr.next();
				Properties prop = inMap.get(key);
				//1. query REST for metadata of particular asset
				Properties metadata = getAssetMetadata(inArchive,key);
				//2. download original to a specific location
				String url = prop.getProperty("original");
				String name = prop.getProperty("name");
				if (url == null || name == null){
					log.info("unable to process $key, name ($name) or url ($url) are null, skipping");
					continue;
				}
				Page page = getDownloadedAsset(inArchive,url,name,exportpath);
				if (!page.exists()){
					log.info("unable to download asset $name, skipping");
					continue;
				}
				//3. update sourcepath
				page = moveDownloadedAsset(inArchive,page,metadata);
				//4. copy metadata to new asset
				Asset asset = null;
				if ( (asset = createAsset(inArchive,page,metadata)) == null){
					log.info("unable to create asset skipping changing asset status to deleted");
					continue;
				}
				//5. query REST to set delete status of asset
				updateAssetEditStatus(inArchive,key);
				//6. fire event
				inArchive.fireMediaEvent("asset/finalizepull",null,asset);
			}
		}
		
		protected Page getDownloadedAsset(MediaArchive inArchive, String inUrl, String inName, String inExportPath){
			String server = inArchive.getCatalogSettingValue("push_server_url");
			String incomingPath = "${inExportPath}${inName}";
			Page page = inArchive.getPageManager().getPage(incomingPath);
			File fileOut = new File(page.getContentItem().getAbsolutePath());
			getDownloader().download(server+inUrl,fileOut);
			return page;
		}
		
		protected Properties getAssetMetadata(MediaArchive inArchive, String inAssetId){
			log.info("get asset metadata for $inAssetId");
			String server = inArchive.getCatalogSettingValue("push_server_url");
			String remotecatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");
			String url = server + "/media/services/rest/assetdetails.xml";
			HttpPost method = new HttpPost(url);
			builder.addPart(new BasicNameValuePair(("catalogid", remotecatalogid);
			method.add(new BasicNameValuePair(("id", inAssetId);
			Element root = execute(remotecatalogid,method);
			method.releaseConnection();
			Properties props = new Properties();
			Iterator<Element> itr = root.elementIterator();
			while(itr.hasNext()){
				Element e = itr.next();
				if (e.getName()==null || !e.getName().equals("property") || e.attribute("id")==null || e.attribute("id").getValue().isEmpty()){
	//				log.info("skipping ${inAssetId}: ${e}");
					continue;
				}
				String id = e.attribute("id").getValue();
				String valueid = e.attribute("valueid")!=null ? e.attribute("valueid").getValue() : null;
				String text = e.getText();
				if (valueid!=null && !valueid.isEmpty()){
					props.put(id,valueid);
				} else {
					props.put(id,text);
				}
			}
			return props;
		}
		
		protected Asset createAsset(MediaArchive inArchive, Page inPage, Properties inMetadata){
			AssetImporter importer = (AssetImporter) inArchive.getModuleManager().getBean("assetImporter");
			String catalogid = inArchive.getCatalogId();
			String exportpath = "/WEB-INF/data/" + catalogid + "/originals/";
			String path = inPage.getPath();
			int index;
			if ( (index = path.toLowerCase().indexOf(exportpath.toLowerCase())) !=-1 ){
				path = path.substring(index + exportpath.length());
			}
			Asset asset = (Asset) importer.createAssetFromExistingFile(inArchive,null,false,path);
			if (asset == null){
				log.info("unable to create asset, aborting");
				return null;
			}
			log.info("created $asset: ${asset.getId()}");
			Enumeration<?> keys = inMetadata.keys();
			while (keys.hasMoreElements()){
				String key = keys.nextElement().toString();
				String value = inMetadata.getProperty(key);
				asset.setValue(key, value);
			}
			importer.saveAsset(inArchive, null, asset); //TODO: check if zip functionality works
			return asset;
		}
		
		protected Page moveDownloadedAsset(MediaArchive inArchive, Page inPage, Properties inMetadata){
			PropertyDetails props = inArchive.getAssetSearcher().getPropertyDetails();
			StringBuilder buf = new StringBuilder();
			//parser pattern specified in download sourcepath; look for keys in metadata and match fields in asset property definition
			//to determin datatype; otherwise just use exact string provided
			String pattern = inArchive.getCatalogSettingValue("push_download_sourcepath");
			if (pattern!=null && !pattern.isEmpty()){
	            List<String> tokens = findKeys(pattern,"//");
	            for (Iterator iterator = tokens.iterator(); iterator.hasNext();) {
					String token = (String) iterator.next();
	                if (token.startsWith("\\$"))
	                {	//metadata field
						//get field, parameter and value from metadata map
	                    String field = token.substring(1);
	                    String param = null;
	                    int start = -1;
	                    int end = -1;
	                    if ( (start = field.indexOf("{"))!=-1 && (end = field.indexOf("}"))!=-1 && start < end){
	                        param = field.substring(start+1,end).trim();//eg, YYYY, MM for dates
	                        field = field.substring(0,start).trim();//eg, $owner, $assetcreationdate
	                    }
						String value = inMetadata.getProperty(field,"").trim();
						//check if it's a date
						boolean isDate = false;
						if (props.contains(field)){
							PropertyDetail prop = props.getDetail(field);
							if (prop.isDate()){
								isDate = true;
							} else if (param!=null){//check if it's formatted as a date because of provided param
								//if this succeeds then we know there's a difference in configs between client and server
								isDate = DateStorageUtil.getStorageUtil().parseFromStorage(value) != null;
							}
						}
						if (value!=null && !value.isEmpty()){
							if (isDate){
								String cleaned = DateStorageUtil.getStorageUtil().checkFormat(value);
								Date date = DateStorageUtil.getStorageUtil().parseFromStorage(value);
								if (date == null){
									date = new Date();
									log.info("unable to parse Date value from remote server: field = $field, value = $value, defaulting to NOW");
								}
								String formatted = null;
								try{
									SimpleDateFormat format = new SimpleDateFormat(param.trim());
									formatted = format.format(date);
								}catch (Exception e){
									log.info("exception caught parsing $date using format \"${param.trim()}\", ${e.getMessage()}, defaulting to $value");
								}
								if (formatted!=null && !formatted.isEmpty()){
									buf.append(formatted).append("/");
								} else {
									buf.append(value).append("/");
								}
							} else {
								buf.append(value).append("/");
							}
						} else {
							log.info("skipping $field, unable to find in metadata obtained from server");
						}
	                } else {
						buf.append(token.trim()).append("/");
	                }
	            }
				if (!buf.toString().isEmpty()){
					buf.append("${inPage.getName()}");
				}
	        }
			if (buf.toString().isEmpty()){
				String user = inMetadata.getProperty("owner","admin").trim();//make the default "admin" if owner has not been specified
				Calendar cal = Calendar.getInstance();
				String month = String.valueOf(cal.get(Calendar.MONTH)+1);
				if (month.length() == 1)
				{
					month = "0${month}";
				}
				String day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
				if (day.length() == 1)
				{
					day = "0${day}";
				}
				String year = String.valueOf(cal.get(Calendar.YEAR));
				buf.append("users/${user}/${year}/${month}/${day}/${inPage.getName()}");
			}
			String generatedpath = buf.toString();
			log.info("moving ${inPage.getName()} to generated path \"$generatedpath\"");
			String destinationpath = "/WEB-INF/data/" + inArchive.getCatalogId() + "/originals/$generatedpath";
			Page destinationpage = inArchive.getPageManager().getPage(destinationpath);
			inArchive.getPageManager().movePage(inPage,destinationpage);
			return destinationpage;
		}
		
		protected ArrayList<String> findKeys(String Subject, String Delimiters)
		{
			StringTokenizer tok = new StringTokenizer(Subject, Delimiters);
			ArrayList<String> list = new ArrayList<String>(Subject.length());
			while(tok.hasMoreTokens()){
				list.add(tok.nextToken());
			}
			return list;
		}
		
		protected void updateAssetEditStatus(MediaArchive inArchive, String inAssetId){
			String server = inArchive.getCatalogSettingValue("push_server_url");
			String remotecatalogid = inArchive.getCatalogSettingValue("push_target_catalogid");
			String url = server + "/media/services/rest/saveassetdetails.xml";
			HttpPost method = new HttpPost(url);
			method.add(new BasicNameValuePair(("catalogid", remotecatalogid);
			method.add(new BasicNameValuePair(("id", inAssetId);
			method.add(new BasicNameValuePair(("field", "editstatus");
			method.add(new BasicNameValuePair(("editstatus.value", "7");
			Element root = execute(remotecatalogid,method);
			method.releaseConnection();
			String out = root.attributeValue("stat");
			if (!"ok".equalsIgnoreCase(out)){
				log.info("warning, could not update $inAssetId editstatus!!!");
			}
		}
		*/
	public void 	saveAssetStatus(Searcher searcher, List savequeue, Asset target, String inNewStatus, User inUser)
	{
		String oldstatus = target.get("pushstatus");
		log.info("Old Status was : " + oldstatus);
		log.info("New Status was : " + inNewStatus);
		
		if( oldstatus == null || !oldstatus.equals(inNewStatus))
		{
			target.setValue("pushstatus", inNewStatus);
			savequeue.add(target);
			if( savequeue.size() == 50)
			{
				searcher.saveAllData(savequeue, inUser);
				savequeue.clear();
			}
		}
	}
}