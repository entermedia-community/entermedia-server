package k4;

import java.text.SimpleDateFormat

import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.dom4j.xpath.DefaultXPath
import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.entermediadb.scripts.ScriptLogger
import org.openedit.Data
import org.openedit.MultiValued
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery
import org.openedit.page.Page
import org.openedit.repository.ContentItem
import org.openedit.users.User
import org.openedit.util.DateStorageUtil
import org.openedit.util.PathProcessor


public void init(){
	log.info("<h2>Starting Import K4</h2>");
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	//get export directory from catalog settings
	//i.e. originals/<k4 export path>/<list of folders/subfolders containing k4 xml files>
	Searcher searcher = archive.getSearcher("k4setting");
	Data setting = searcher.searchById("k4_export_directory");
	String k4exportdir = "K4/";
	String k4mode = "false";
	String includedFiles = "";
	if (setting != null){
		k4exportdir = setting.get("value");
		if (k4exportdir == null)
		{
			k4exportdir = "K4/";
		}
	}
	if (!k4exportdir.endsWith("/"))
	{
		k4exportdir = "${k4exportdir}/";
	}
	//k4 test mode setting
	setting = searcher.searchById("k4_test_mode");
	if (setting){
		k4mode = setting.get("value");
	}
	boolean testmode = Boolean.parseBoolean(k4mode);
	//originals directory
	String originals = "/WEB-INF/data/${archive.catalogId}/originals/${k4exportdir}";
	//included, excluded files
	List<String> include = new ArrayList<String>();
	setting = searcher.searchById("k4_xml_include");
	if (setting){
		includedFiles = setting.get("value");
		if (includedFiles!=null){
			String[] vals = includedFiles.split("\\s*\\|\\s*");
			for(String val:vals){
				include.add(val.trim());
			}
		}
	}
	
	setting = searcher.searchById("k4_debug_level");
	String debuglevel = setting ? setting.get("value") : null;
	if (debuglevel){
		if (debuglevel != "low" && debuglevel != "medium"  && debuglevel != "high"){
			debuglevel = "low";
		}
	} else {
		debuglevel = "low";
	}
	
	String datetime = context.getDateTime(new Date());
	String sessionName = "Import K4: $datetime";
	
	XMLPathProcessor processor = new XMLPathProcessor();
	processor.setIncludeXMLFiles(include);
	processor.setSessionName(sessionName);
	processor.setTestMode(testmode);
	processor.setDebugLevel(debuglevel);
	processor.setLogger(log);
	processor.setExportDir(k4exportdir);
	processor.setArchive(archive);
	processor.setRecursive(true);
	processor.setRootPath(originals);
	processor.setPageManager(archive.getPageManager());
	processor.setIncludeMatches("*.xml");
	
	processor.preProcess();
	processor.process();
	processor.postProcess();
}

public class XMLPathProcessor extends PathProcessor {
	MediaArchive fieldArchive;
	String fieldExportDir;
	ScriptLogger log;
	Map<String,List<ContentItem>> pagemap;
	HitTracker k4hierarchyhits;
	boolean testMode = false;
	List<LogEntry> logentries;
	List<String> includedXMLFiles;
	String fieldSessionName;
	String fieldDebugLevel;
	Page fieldCurrentXMLFile;
	
	Map<String,Object> fieldPublication;
	Map<String,Object> fieldIssue;
	Map<String,Object> fieldSection;
	Map<String,Object> fieldAttachment;
	List<Map<String,Object>> fieldAttachmentSiblings;
	
	Map<String,Map<String,List<String>>> inddFieldsByType;
	
	public void setPublication(Map<String,Object> inMap){
		fieldPublication = inMap;
	}
	
	public void setIssue(Map<String,Object> inMap){
		fieldIssue = inMap;
	}
	
	public void setSection(Map<String,Object> inMap){
		fieldSection = inMap;
	}
	
	public void setAttachment(Map<String,Object> inMap){
		fieldAttachment = inMap;
	}
	
	public Map<String,Object> getPublication(){
		return fieldPublication;
	}
	
	public Map<String,Object> getIssue(){
		return fieldIssue;
	}
	
	public Map<String,Object> getSection(){
		return fieldSection;
	}
	
	public Map<String,Object> getAttachment(){
		return fieldAttachment;
	}
	
	public void setAttachmentSiblings(List<Map<String,Object>> inList){
		fieldAttachmentSiblings = inList;
	}
	
	public List<Map<String,Object>> getAttachmentSiblings(){
		return fieldAttachmentSiblings;
	}
	
	public void addINDDField(String field, String value){
		addINDDField(field,value,"_default_");
	}
	
	public void addINDDField(String field, String value, String type){
		if (!type || type == "null") type = "_default_";
		Map<String,List<String>> map;
		if (getINDDFields().containsKey(type)){
			map = getINDDFields().get(type);
		} else {
			map = new HashMap<String,ArrayList<String>>();
			getINDDFields().put(type, map);
		}
		List<String> list;
		if (map.containsKey(field)){
			list = map.get(field);
		} else {
			list = new ArrayList<String>();
			map.put(field, list);
		}
		if (list.contains(value.trim()) == false){
			list.add(value.trim());
		}
	}
	
	public Map<String,Map<String,List<String>>> getINDDFields(){
		if (inddFieldsByType == null){
			inddFieldsByType = new HashMap<String,HashMap<String,ArrayList<String>>>();
		}
		return inddFieldsByType;
	}
	
	public void resetINDDFields(){
		Iterator itr = getINDDFields().keySet().iterator();
		while(itr.hasNext()){
			Map<String,List<String>> map = getINDDFields().get(itr.next());
			map.clear();
		}
		getINDDFields().clear();
	}
	
	public void setIncludeXMLFiles(List<String> included){
		includedXMLFiles = included;
	}
	
	public List<String> getIncludeXMLFiles(){
		if (includedXMLFiles == null){
			includedXMLFiles = new ArrayList<String>();
		}
		return includedXMLFiles;
	}
	
	public void setCurrentXMLFile(Page inPage)
	{
		fieldCurrentXMLFile = inPage;
	}
	
	public Page getCurrentXMLFile()
	{
		return fieldCurrentXMLFile;
	}
	
	public void setSessionName(String inSessionName)
	{
		fieldSessionName = inSessionName;
	}
	
	public String getSessionName()
	{
		return fieldSessionName;
	}
	
	public void setDebugLevel(String debuglevel){
		fieldDebugLevel = debuglevel;
	}
	
	public boolean canLog(String level){
		if (fieldDebugLevel){
			if (level == "low"){
				return (fieldDebugLevel == "low" || fieldDebugLevel == "medium" || fieldDebugLevel == "high");
			}
			if (level == "medium"){
				return (fieldDebugLevel == "medium" || fieldDebugLevel == "high");
			}
			if (level == "high"){
				return (fieldDebugLevel == "high");
			}
		}
		return false;
	}
	
	public List<LogEntry> getLogEntries()
	{
		if (logentries == null)
		{
			logentries = new ArrayList<LogEntry>();
		}
		return logentries;
	}
	
	public void setTestMode(boolean inTestMode){
		testMode = inTestMode;
	}
	
	public boolean isTestMode(){
		return testMode;
	}
	
	public void setArchive(MediaArchive archive){
		fieldArchive = archive;
	}
	
	public void setLogger(ScriptLogger inlog){
		log = inlog;
	}
	
	public ScriptLogger getLogger(){
		return log;
	}
	
	public void setExportDir(String inDir){
		fieldExportDir = inDir;
	}
	
	public boolean acceptFile(ContentItem inItem){
		boolean accepted = super.acceptFile(inItem);
		String path = inItem.getPath();
		Page page = fieldArchive.getPageManager().getPage(path);
		String pagename = page.getName();
		List<String> included = getIncludeXMLFiles();
		if (included.isEmpty()){
			return accepted;
		}
		accepted = included.contains(pagename);
		return accepted;
	}
	
	public void preProcess(){
		if (canLog("medium")) {
			log.info("<h3>Preprocessing XMLPathProcessor</h3>");
		}
		pagemap = new HashMap<String,List<ContentItem>>();
		Searcher searcher = fieldArchive.getSearcher("k4hierarchy");
		SearchQuery query = searcher.createSearchQuery();
		query.addMatches("id","*");
		query.addSortBy("order");
		k4hierarchyhits = searcher.search(query);
	}
	
	public void postProcess(){
		if (canLog("medium")) {
			log.info("<h3>Post Processing XMLPathProcessor</h3>");
		}
		processPageMap();
		if (canLog("low")) {
			log.info("<h3>End Import K4</h3>");
		}
	}
	
	public void updateXMLAsset(Page page){
		if(isTestMode()){
			return;
		}
		String originals = "/WEB-INF/data/${fieldArchive.catalogId}/originals/";
		String path = page.getPath();
		int index = path.indexOf(originals) + originals.length();
		String sourcepath = path.substring(index);
		Asset asset = fieldArchive.getAssetBySourcePath(sourcepath);
		if (asset){
			asset.setProperty("k4processed","true");
			fieldArchive.getAssetSearcher().saveData(asset,null);
		}
	}
	
	public boolean hasBeenProcessed(Page inPage){
		String originals = "/WEB-INF/data/${fieldArchive.catalogId}/originals/";
		String path = inPage.getPath();
		int index = path.indexOf(originals) + originals.length();
		String sourcepath = path.substring(index);
		Asset asset = fieldArchive.getAssetBySourcePath(sourcepath);
		if (asset){
			return Boolean.parseBoolean(asset.get("k4processed"));
		}
		if (canLog("low")) {
			getLogger().info("<span style='color:red'>Unable to find asset for ${inPage.getPath()}</span>");
		}
		return false;
	}
	
	public void processFile(ContentItem inContent, User inUser){
		String path = inContent.getPath();
		Page page = fieldArchive.getPageManager().getPage(path);
		if (canLog("low")) {
			log.info("<h3>Processing ${page.getName()}</h3>");
		}
		if (!isTestMode() && hasBeenProcessed(page)){
			if (canLog("medium")) {
				getLogger().info("<span style='color:red'>${page.getName()} has already been processed, skipping</span>");
			}
			return;
		}
		boolean added = false;
		k4hierarchyhits.each{
			if (added) return;
			if ("${it.operation}" == "startswith"){
				if (page.getName().startsWith("${it.search}")){
					if (canLog("medium")) {
						getLogger().info("<span style='color:green'>${page.getName()}: $it.operation \"$it.search\"</span>");
					}
					List<ContentItem> list = pagemap.get("${it.order}");
					if (list == null){
						list = new ArrayList<ContentItem>();
						pagemap.put("${it.order}", list);
					}
					list.add(inContent);
					added = true;
				}
			} else if ("${it.operation}" == "endswith"){
				if (page.getName().endsWith("${it.search}")){
					if (canLog("medium")) {
						getLogger().info("<span style='color:yellow'>${page.getName()}: $it.operation \"$it.search\"</span>");
					}
					List<ContentItem> list = pagemap.get("${it.order}");
					if (list == null){
						list = new ArrayList<ContentItem>();
						pagemap.put("${it.order}", list);
					}
					list.add(inContent);
					added = true;
				}
			} else if ("${it.operation}" == "contains"){
				if ("${it.search}" == "*"){
					if (canLog("medium")) {
						getLogger().info("<span style='color:blue'>${page.getName()}: $it.operation \"$it.search\"</span>");
					}
					List<ContentItem> list = pagemap.get("${it.order}");
					if (list == null){
						list = new ArrayList<ContentItem>();
						pagemap.put("${it.order}", list);
					}
					list.add(inContent);
					added = true;
				} else {
					if (page.getName().contains("$it.search")){
						if (canLog("medium")) {
							getLogger().info("<span style='color:orange'>${page.getName()}: $it.operation \"$it.search\"</span>");
						}
						List<ContentItem> list = pagemap.get("${it.order}");
						if (list == null){
							list = new ArrayList<ContentItem>();
							pagemap.put("${it.order}", list);
						}
						list.add(inContent);
						added = true;
					}
				}
			}
		}
		if (!added){
			if (canLog("low")) {
				getLogger().info("<span style='color:red'>BUG! ${page.getName()} was not added to hashamp</span>");
			}
		}
	}
	
	public void processPageMap(){
		if (pagemap.isEmpty()){
			if (canLog("low")) {
				log.info("Unable to find any K4 XML files, finished processing");
			}
			return;
		}
		SortedSet<String> sortedkeys = new TreeSet<String>(pagemap.keySet());
		Iterator keyitr = sortedkeys.iterator();
		while(keyitr.hasNext()){
			List<ContentItem> list  = pagemap.get(keyitr.next());
			Iterator<ContentItem> itr2 = list.iterator();
			while(itr2.hasNext()){
				ContentItem item = itr2.next();
				Page page = fieldArchive.getPageManager().getPage(item.getPath());
				setCurrentXMLFile(page);
				processContentItem(item);
				updateXMLAsset(page);
			}
		}
		saveLogEntries();
	}
	
	public void processContentItem(ContentItem inContent){
		String path = inContent.getPath();
		Page page = fieldArchive.getPageManager().getPage(path);
		//go up two levels
		Page parentPage = fieldArchive.getPageManager().getPage(page.getParentPath());
		Page secondLevelParentPage = fieldArchive.getPageManager().getPage(parentPage.getParentPath());
		String packageName = secondLevelParentPage.getName();
		String packagePath = secondLevelParentPage.getPath();
		String rootPath = "/WEB-INF/data/${fieldArchive.catalogId}/originals/";
		log.info("${packageName} ${packagePath} ${rootPath}");
		String searchPath = packagePath.substring(rootPath.length())+"/";
		
		def parser = fieldArchive.getModuleManager().getBean("newk4parser");
		try{
			parser.parse(inContent.getInputStream());
		}
		catch (Exception e){
			throw new OpenEditException(e);
			
		}
			
		//keep track of id and name of each entity to build logical paths
		ArrayList<HashMap<String,Object>> pubs = parser.getPublications();
		ArrayList<HashMap<String,Object>> iss = parser.getIssues();
		ArrayList<HashMap<String,Object>> secs = parser.getSections();
		ArrayList<HashMap<String,Object>> atts = parser.getAttachments();
		
		if (pubs.isEmpty()){
			if (canLog("low")) {
				getLogger().info("<span style='color:red'>Unable to parse ${page.getPath()}, skipping</span>");
			}
			return;
		}
		
		//reference indd asset
		Asset inddasset;
		Map<String,Object> inddpublication;
		Map<String,Object> inddissue;
		Map<String,Object> inddattachments;
		resetINDDFields();
		
		List<Map<String,String>> metadata = new ArrayList<HashMap<String,String>>();
		for (HashMap<String,Object> map:atts){
			ArrayList<HashMap<String,Object>> maps = getHierarchy(parser.PARENT,parser.DEFAULT_ID_LABEL,pubs,iss,map);
			if (maps.size()!=2){
				continue;
			}
			//create document
			if (map.containsKey("_xml")){
				Element element = (Element) map.get("_xml");
				Document document = convertToDocument(element);
				map.put("_xml",document.getRootElement());
			}
			//reference current publication, issue, attachment
			setPublication(maps.get(0));
			setIssue(maps.get(1));
			setAttachment(map);
			//build list of sibling attachments
			buildAttachmentSiblings(atts);
			
//			prettyPrint(map);
			
			//if map does not contain any file reference
			//then cannot add metadata to any asset in particular
			//process the metadata and add to a list
			if (!map.containsKey("fileType") && !map.containsKey("filetype")){
				Map<String,String> m = getMetaData();
				metadata.add(m);
				continue;
			}
			//references a file so can hopefully find assets in the dam
			String _type = map.get("fileType");
			if (!_type) _type = map.get("filetype");
			String _name = map.get("name");
			if (map.containsKey("copytargetname")) _name = map.get("copytargetname");
			else if (map.containsKey("filename")) _name = map.get("filename");
			String version = "";
			if (map.containsKey("fileVersion") && !map.get("fileVersion").isEmpty()) version = map.get("fileVersion");
			String versionFiletype = "${version}.${_type}".toLowerCase();
			String fileType = ".${_type}".toLowerCase();
			//create search query
			AssetSearcher assetSearcher = fieldArchive.getAssetSearcher();
			SearchQuery assetquery = assetSearcher.createSearchQuery();
			assetquery.addStartsWith("name",_name);
			assetquery.addSortBy("sourcepath");
			HitTracker assetHits = assetSearcher.search(assetquery);
			assetHits.each{
				String sourcepath = it.getSourcePath();
				if (!sourcepath.startsWith(searchPath)){
					return;
				}
				String lowercaseName = it.getName().toLowerCase();
				if (lowercaseName.endsWith(versionFiletype) || lowercaseName.endsWith(fileType)){
					Asset asset = fieldArchive.getAssetBySourcePath(sourcepath);
					if (asset){
						if (lowercaseName.endsWith(".indd")){
							inddasset = asset;
							inddpublication = getPublication();
							inddissue = getIssue();
							inddattachments = getAttachment();
						} else {
							populateMetadata(asset);
						}
					}
				}
			}
		}
		//update indd file
		if (inddasset){
			setPublication(inddpublication);
			setIssue(inddissue);
			setAttachment(inddattachments);
			buildAttachmentSiblings(atts);
			populateINDDMetadata(inddasset,packageName,searchPath);
		}
	}
	
	public void populateINDDMetadata(Asset asset, String packagename, String searchpath){
		if (canLog("low")) {
			getLogger().info("<h4>${asset.getName()}</h4>");
		}
		Map<String,String> metadata = getMetaData(asset.get("assettype"));//all fields on asset itself
		Map<String,Map<String,List<String>>> fields = getINDDFields();//all fields that need to be concatenated to indd, including a subset from asset itself
		StringBuilder buf = new StringBuilder();
		Map<String,String> assetentries = new HashMap<String,String>();
		//assettype used to omit fields
		String assettype = asset.getProperty("assettype");
		if (!assettype){
			assettype = "none";
		}
		//add all entries to a single hashmap, disregarding omissions about indd assettype
		//omit photo metadata if TOC
		boolean isTOC = asset.getName().toUpperCase().contains("_TOC");
		Searcher searcher = fieldArchive.getSearcher("k4toasset");
		HitTracker hits = searcher.getAllHits();
		hits.each{
			String key = it.metadata;
			if (key){
				MultiValued data = searcher.searchByField("metadata", key);
				List omits = data.getValues("omit");
				if (omits && omits.contains(assettype)){
					if (canLog("high")) {
						getLogger().info("<span style='color:red'>omitting $key, type=$assettype, omission list=$omits</span>");
					}
					return;
				}
				if (isTOC && Boolean.parseBoolean(it.omitfromtoc)){
					return;
				}
				String value = metadata.get(key);
				List<String> values = new ArrayList<String>();
				
				if (value) values.add(value);
				if (Boolean.parseBoolean(it.addtoindd)){
					Iterator<String> itr = fields.keySet().iterator();
					while(itr.hasNext()){
						String type = itr.next();
						Map<String,List<String>> typemap = fields.get(type);
						List<String> list = typemap.get(key);
						if (list){
							for(String entry:list){
								if (!values.contains(entry)){
									values.add(entry);
								}
							}
						}
					}
				}
				if (values.isEmpty()){
					return;
				}
				StringBuilder sb = new StringBuilder();
				Iterator<String> itr = values.iterator();
				while(itr.hasNext()){
					sb.append(itr.next().trim());
					if (itr.hasNext()) sb.append(" | ");
				}
				value = sb.toString();
				buf.append("<li><strong>$key</strong>Â <span style='color:green'>$value</span></li>");
				assetentries.put(key,value);
				if (key == "keywords"){
					List keywords = asset.getKeywords();
					boolean isadded = false;
					StringTokenizer tokenizer = new StringTokenizer(value,"|");
					while (tokenizer.hasMoreTokens()){
						String tok = tokenizer.nextToken().trim();
						if (tok){
							if (keywords.contains(tok) == false){
								keywords.add(tok);
								isadded = true;
							}
						}
					}
					if (isadded) {
						asset.setKeywords(keywords);
					}
				} else {
					asset.setProperty(key,value);
				}
			}
		}
		if (canLog("medium")) {
			getLogger().info("<ul>${buf.toString()}</ul>");
		}
		addLogEntry(asset,assetentries);
		fieldArchive.getAssetSearcher().saveData(asset, null);
		List<Asset> pdfs = getFilesByType(asset,packagename,searchpath,"pdf");
		if (pdfs.isEmpty()){
			if (canLog("low")) {
				getLogger().info("<span style='color:red'>No screen pdfs found</span>");
			}
			return;
		}
		for(Asset pdf:pdfs){
			if (canLog("low")) {
				getLogger().info("<h4>${pdf.getName()}</h4>");
			}
			buf.delete(0,buf.toString().length());
			Iterator<String> itr = assetentries.keySet().iterator();
			while(itr.hasNext()){
				String key = itr.next();
				String value = assetentries.get(key);
				buf.append("<li><strong>$key</strong>Â <span style='color:green'>$value</span></li>");
				if (key == "keywords"){
					List keywords = pdf.getKeywords();
					boolean isadded = false;
					StringTokenizer tokenizer = new StringTokenizer(value,"|");
					while (tokenizer.hasMoreTokens()){
						String tok = tokenizer.nextToken().trim();
						if (tok){
							if (keywords.contains(tok) == false){
								keywords.add(tok);
								isadded = true;
							}
						}
					}
					if (isadded) {
						pdf.setKeywords(keywords);
					}
				} else {
					pdf.setProperty(key,value);
				}
			}
			if (canLog("medium")) {
				getLogger().info("<ul>${buf.toString()}</ul>");
			}
			fieldArchive.getAssetSearcher().saveData(pdf, null);
		}
		//do xml files too
		List<Asset> xmls = getFilesByType(asset,packagename,searchpath,"xml");
		if (xmls.isEmpty()){
			if (canLog("low")) {
				getLogger().info("<span style='color:red'>No XML files found</span>");
			}
			return;
		}
		for(Asset xml:xmls){
			if (canLog("low")) {
				getLogger().info("<h4>${xml.getName()}</h4>");
			}
			buf.delete(0,buf.toString().length());
			Iterator<String> itr = assetentries.keySet().iterator();
			while(itr.hasNext()){
				String key = itr.next();
				String value = assetentries.get(key);
				buf.append("<li><strong>$key</strong>Â <span style='color:green'>$value</span></li>");
				if (key == "keywords"){
					List keywords = xml.getKeywords();
					boolean isadded = false;
					StringTokenizer tokenizer = new StringTokenizer(value,"|");
					while (tokenizer.hasMoreTokens()){
						String tok = tokenizer.nextToken().trim();
						if (tok){
							if (keywords.contains(tok) == false){
								keywords.add(tok);
								isadded = true;
							}
						}
					}
					if (isadded) {
						xml.setKeywords(keywords);
					}
				} else {
					xml.setProperty(key,value);
				}
			}
			if (canLog("medium")) {
				getLogger().info("<ul>${buf.toString()}</ul>");
			}
			fieldArchive.getAssetSearcher().saveData(xml, null);
		}
	}
	
	public List<Asset> getFilesByType(Asset indd, String packagename, String searchpath, String filetype){
		List<Asset> list = new ArrayList<Asset>();
		AssetSearcher searcher = fieldArchive.getAssetSearcher();
		int index = indd.getName().lastIndexOf(".");
		if (index < 0){
			return list;
		}
		String searchname = indd.getName().substring(0,index);
		//exact: screen pdf
		String screenpdfname = searchname+".$filetype";
		SearchQuery query = searcher.createSearchQuery();
		query.addMatches("name", screenpdfname.replace("[", "\\[").replace("]", "\\]"));
		HitTracker hits = searcher.search(query);
		int positivehits = 0;
		hits.each{
			Data data = (Data) it;
			String sourcepath = data.getSourcePath();
			if (!sourcepath.startsWith(searchpath)){
				return;
			}
			positivehits++;
			if (canLog("high")) {
				getLogger().info("<span style='color:blue'>File search $filetype for ${indd.getName()}: ${data.getName()}, searched for \"${query.toFriendly()}\"</span>");
			}
			Asset asset = fieldArchive.getAssetBySourcePath(sourcepath);
			if (asset && !list.contains(asset)){
				list.add(asset);
			}
		}
		//press: <number>|<number range>_<rest of the name> where name starts with <packageName>
		//eg: 026_030_H5_06_14_Summer_Box, 008_09_HL_06_14_ASK_AZ
		if (searchname.contains(packagename)){
			String prefix = searchname.substring(0,searchname.indexOf(packagename));
			String suffix = searchname.substring(searchname.indexOf(packagename));
			query = searcher.createSearchQuery();
			if (prefix.matches("^[0-9_]*\$")){
				query.addContains("name",suffix);
			} else{
				query.addContains("name",searchname);
			}
			hits = searcher.search(query);
			hits.each{
				Data data = (Data) it;
				String sourcepath = data.getSourcePath();
				if (!sourcepath.startsWith(searchpath) || screenpdfname.equalsIgnoreCase(data.getName()) || !data.getName().toLowerCase().endsWith(".$filetype")){
					return;
				}
				if (canLog("high")) {
					getLogger().info("<span style='color:green'>File search $filetype for ${indd.getName()}: ${data.getName()}, searched for \"${query.toFriendly()}\"</span>");
				}
				Asset asset = fieldArchive.getAssetBySourcePath(sourcepath);
				if (asset && !list.contains(asset)){
					list.add(asset);
				}
			}
		}
		return list;
	}
	
	public Map<String,String> getMetaData(){
		return getMetaData("_default_");
	}
	
	public Map<String,String> getMetaData(String type){
		Map<String,String> metadata = new HashMap<String,String>();
		Searcher searcher = fieldArchive.getSearcher("k4toasset");
		HitTracker hits = searcher.getAllHits();
		hits.each{
			String k4id = it.k4id;//k4 specific field in one of the maps
			String siblingk4id = it.siblingk4id;//refers to an attachment sibling to search on
			String field = it.metadata;//corresponding field in asset table
			String datatype = it.datatype;//datatype of the field
			String object = it.object;//map to search for the k4id field
			String list = it.list;//a searcher to lookup a list value
			String dateformat = it.dateformat;//date format to parse
			boolean addToINDD = Boolean.parseBoolean(it.addtoindd).booleanValue();//add the field to INDD
			
			HashMap<String,Object> map = getAttachment();
			if ("issue" == object) map = getIssue();
			else if ("publication" == object) map = getPublication();
			
			String value = findValue(map,k4id,siblingk4id);
			if (value){
				//value found now format it
				if (datatype == "date"){
					Date formatted;
					String storeformat;
					if (dateformat){
						String parseformat;
						if (dateformat.contains("store=")){
							parseformat = dateformat.substring(0, dateformat.indexOf("store=")).trim();
							storeformat = dateformat.substring(dateformat.indexOf("store=")+"store=".length()).trim();
						} else {
							parseformat = dateformat;
						}
						try{
							SimpleDateFormat format = new SimpleDateFormat(parseformat);
							formatted = format.parse(value);
						}catch (Exception e){
							if (canLog("high")) {
								getLogger().error("DateFormat Exception ${e.getMessage()}",e);
							}
						}
					}
					if (formatted){
						if (storeformat){
							try{
								SimpleDateFormat format = new SimpleDateFormat(storeformat);
								value = format.format(formatted);
							}catch (Exception e){
								if (canLog("high")) {
									getLogger().error("DateFormat Exception ${e.getMessage()}",e);
								}
							}
						} else {
							value = DateStorageUtil.getStorageUtil().formatForStorage(formatted);
						}
					}
				}
				else if (datatype == "boolean"){
					value = Boolean.parseBoolean(value)+"";
				}
				else if (datatype == "list"){
					String delimiter = value.contains(",") ? "," : value.contains("|") ? "|" : value.contains(";") ? ";" : " ";
					StringBuilder buf = new StringBuilder();
					StringTokenizer tokenizer = new StringTokenizer(value,delimiter);
					while (tokenizer.hasMoreTokens()){
						String tok = tokenizer.nextToken().trim();
						if (tok){
							buf.append(tok);
							if (tokenizer.hasMoreTokens()) buf.append(" | ");
						}
					}
					if (canLog("high")){
						getLogger().info("<span style='color:blue;'>LIST $field: ${buf.toString()}</span>");
					}
					value = buf.toString();
				} else if (list){
					String searchtable = null;
					String searchfield = "name";
					if (list.contains(":")){
						String [] str = list.split(":");
						searchtable = str[0];
						searchfield = str[1];
					} else {
						searchtable = list;
					}
					Searcher listsearcher = fieldArchive.getSearcher(searchtable);
					SearchQuery q = listsearcher.createSearchQuery();
					q.addContains(searchfield,value);
					HitTracker listhits = listsearcher.search(q);
					if (listhits.isEmpty() == false)
					{
						Data d = listhits.first();
						value = d.id;
					}
				}
				if (addToINDD){
					addINDDField(field,value,type);
				}
				metadata.put(field, value);
			}
		}
		return metadata;
	}
	
	public ArrayList<HashMap<String,Object>> getHierarchy(String inParentId, String inK4Id, ArrayList<HashMap<String,Object>> inPublications, ArrayList<HashMap<String,Object>> inIssues, HashMap<String,Object> inAttachment){
		ArrayList<HashMap<String,Object>> maps = new ArrayList<HashMap<String,Object>>();
		if (inAttachment.containsKey(inParentId)){
			String attachmentParent = (String) inAttachment.get(inParentId);
			for(HashMap<String,Object> inIssue:inIssues){
				if (attachmentParent.equals(inIssue.get(inK4Id).toString())){
					String issueParent = (String) inIssue.get(inParentId);
					for(HashMap<String,Object> inPublication:inPublications){
						if (issueParent.equals(inPublication.get(inK4Id).toString())){
							maps.add(inPublication);
							maps.add(inIssue);
							break;
						}
					}
					break;
				}
			}
		}
		return maps;
	}
	
	public void buildAttachmentSiblings(List<Map<String,Object>> inAttachments){
		String k4id = getAttachment().get("k4id");
		String parent = getAttachment().get("_parent_");
		List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
		setAttachmentSiblings(list);
		if (k4id == null || parent == null){
			return;
		}
		Iterator<Map<String,Object>> itr = inAttachments.iterator();
		while (itr.hasNext()){
			Map<String,Object> current = itr.next();
			if (current.get("k4id") == k4id || current.get("_parent_") != parent){
				continue;
			}
			list.add(current);
		}
	}
	
	public void populateMetadata(Asset asset){
		if (canLog("low")) {
			getLogger().info("<h4>${asset.getName()}</h4>");
		}
		Map<String,String> metadata = getMetaData(asset.get("assettype"));
		if (metadata.keySet().isEmpty()){
			return;
		}
		StringBuilder buf = new StringBuilder();
		Map<String,String> assetentries = new HashMap<String,String>();
		String assettype = asset.getProperty("assettype");
		if (!assettype){
			assettype = "none";
		}
		Searcher searcher = fieldArchive.getSearcher("k4toasset");
		Iterator<String> itr = metadata.keySet().iterator();
		while(itr.hasNext()){
			String key = itr.next();
			MultiValued data = searcher.searchByField("metadata", key);
			List omits = data.getValues("omit");
			if (omits && omits.contains(assettype)){
				if (canLog("high")) {
					getLogger().info("<span style='color:red'>omitting $key, type=$assettype, omission list=$omits</span>");
				}
				continue;
			}
			String value = metadata.get(key);
			assetentries.put(key,value);
			if (key == "keywords"){
				List keywords = asset.getKeywords();
				boolean isadded = false;
				StringTokenizer tokenizer = new StringTokenizer(value,"|");
				while (tokenizer.hasMoreTokens()){
					String tok = tokenizer.nextToken().trim();
					if (tok){
						if (keywords.contains(tok) == false){
							keywords.add(tok);
							isadded = true;
						}
					}
				}
				if (isadded) {
					asset.setKeywords(keywords);
				}
			} else {
				asset.setProperty(key,value);
			}
			buf.append("<li><strong>$key</strong>Â <span style='color:green'>$value</span></li>");
		}
		if (canLog("high")) {
			getLogger().info("<ul>${buf.toString()}</ul>");
		}
		if (assetentries.keySet().isEmpty() == false){
			addLogEntry(asset,assetentries);
			fieldArchive.getAssetSearcher().saveData(asset, null);
		}
	}
	
	public String findValue(HashMap<String,Object> map, String K4ID, String siblingK4ID){
		if (map.containsKey(K4ID)) {
			return map.get(K4ID);
		}
		String value = null;
		Element element = (Element) map.get("_xml");
		value = element.elementText(K4ID);
		if (value || K4ID == "\$") {
			return value;
		}
		try{
			if (siblingK4ID){
				DefaultXPath xpath = new DefaultXPath(siblingK4ID);
				Map<String,String> namespaces  = new TreeMap<String,String>();
				namespaces.put("x","http://www.vjoon.com/K4Export/2.2");
				xpath.setNamespaceURIs(namespaces);
				Node node = xpath.selectSingleNode(element);
				if (node){
					String searchFor = node.asXML().replaceAll("<.*?>"," ").trim();
					List<Map<String,Object>> siblings = getAttachmentSiblings();
					for(Map<String,Object> sibling:siblings){
						if (sibling.get("k4id") != searchFor)
							continue;
						element = (Element) sibling.get("_xml");
						break;
					}
				}
			}
			DefaultXPath xpath = new DefaultXPath(K4ID);
			Map<String,String> namespaces = new TreeMap<String,String>();
			namespaces.put("x","http://www.vjoon.com/K4Export/2.2");
			xpath.setNamespaceURIs(namespaces);
			Node node = xpath.selectSingleNode(element);
			if (node){
				value = node.asXML().replaceAll("<.*?>"," ").trim();
			}
		}catch (Exception e){
			if (canLog("high")) {
				getLogger().info("Exception caught searching for $K4ID, ${e.getMessage()}");
			}
		}
		return value;
	}
	
	public Document convertToDocument(Element element)
	{
		String result = element.asXML();
		Document document = DocumentHelper.parseText(result);
		return document;
	}
	
	public void prettyPrint(Element element) {
		StringWriter sw = new StringWriter();
		org.dom4j.io.OutputFormat format = org.dom4j.io.OutputFormat.createPrettyPrint();
		org.dom4j.io.XMLWriter xw = new org.dom4j.io.XMLWriter(sw, format);
		xw.write(element);
		String result = sw.toString();
//		result = result.trim().replaceAll("<","<").replaceAll(">",">");
//		String result = element.asXML();
		System.out.println(result);
	}
	
	public void prettyPrint(Node node) {
		StringWriter sw = new StringWriter();
		org.dom4j.io.OutputFormat format = org.dom4j.io.OutputFormat.createPrettyPrint();
		org.dom4j.io.XMLWriter xw = new org.dom4j.io.XMLWriter(sw, format);
		xw.write(node);
		String result = sw.toString();
//		result = result.trim().replaceAll("<","<").replaceAll(">",">");
//		String result = node.asXML();
		System.out.println(result);
	}
		
	public void prettyPrint(Asset asset)
	{
		getLogger().info("<h4>$asset.name</h4>");
	}
	
	public void prettyPrint(Map<String,?> map){
		SortedSet<String> sortedkeys = new TreeSet<String>(map.keySet());
		Iterator<String> itr = sortedkeys.iterator();
		while(itr.hasNext()){
			String key = itr.next();
			if (key == "_xml" || key == "k4id" || key == "_parent_"){
				continue;
			}
			getLogger().info("$key: ${map.get(key)}");
		}
		getLogger().info("<br/>");
	}
	
	public void addLogEntry(Asset asset, Map<String,String> map)
	{
		getLogEntries().add(new LogEntry(asset,map,getCurrentXMLFile()));
	}
	
	public void saveLogEntries()
	{
		if (getLogEntries().isEmpty() || !isTestMode())
		{
			return;
		}
		Searcher searcher = fieldArchive.getSearcher("k4history");
		searcher.deleteAll(null);//nuke all entries -- maybe configurable?
		List list = new ArrayList();
		Iterator<LogEntry> itr = getLogEntries().iterator();
		while(itr.hasNext()){
			LogEntry entry = itr.next();
			Data data = searcher.createNewData();
			data.setName(getSessionName());
			data.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(entry.date));
			data.setProperty("assetid",entry.assetId);
			data.setProperty("filename",entry.fileName);
			data.setProperty("k4filename",entry.k4filename);
			data.setProperty("log",entry.mapToString());
			list.add(data);
			if (list.size() == 100)
			{
				searcher.saveAllData(list, null);
				list.clear();
			}
		}
		if (!list.isEmpty())
		{
			searcher.saveAllData(list, null);
		}
	}
}

class LogEntry {
	public String assetId;
	public String fileName;
	public String k4filename;
	public Map<String,String> map;
	public Date date;
	
	public LogEntry(Asset inAsset, Map<String,String> inMap, Page inPage)
	{
		assetId = inAsset.getId();
		fileName = inAsset.getName();
		map = inMap;
		date = new Date();
		k4filename = inPage.getName();
	}
	
	public String mapToString()
	{
		StringBuilder buf = new StringBuilder();
		SortedSet<String> sortedkeys = new TreeSet<String>(map.keySet());
		Iterator<String> itr = sortedkeys.iterator();
		while (itr.hasNext()){
			String key = itr.next();
			String entry = map.get(key);
			buf.append(key).append(": ").append("\"").append(entry).append("\"");
			if (itr.hasNext()) buf.append(", ");
		}
		return buf.toString();
	}
}

class PathBuilder
{
	public static final String PUBLICATION = "publication";
	public static final String ISSUE = "issue";
	public static final String SECTION = "section";
	public static final String ATTACHMENT = "package";
	public static final String SEP = "/";
	
	//eg. publication/issue or publication/issue/section
	
	HashMap<String,String> fieldPub;
	HashMap<String,String> fieldIss;
	HashMap<String,String> fieldSec;
	HashMap<String,String> fieldAtt;
	
	public PathBuilder()
	{
		fieldPub = new HashMap<String,String>();
		fieldIss = new HashMap<String,String>();
		fieldSec = new HashMap<String,String>();
		fieldAtt = new HashMap<String,String>();
	}
	public String convert(String type, String id, String name, String parentId)
	{
		String path = null;
		if (name == null || name.isEmpty())
			name = type;
		
		if (type.equals(PUBLICATION))
		{
			fieldPub.put(id, name);
			path = name;
		}
		else if (type.equals(ISSUE))
		{
			String parent = fieldPub.get(parentId);
			if (parent == null) parent = PUBLICATION;
			path = parent + SEP + name;
			fieldIss.put(id, path);
		}
		else if (type.equals(SECTION))
		{
			String parent = fieldIss.get(parentId);
			if (parent == null) parent = PUBLICATION + SEP + ISSUE;
			path = parent + SEP + name;
			fieldSec.put(id, path);
		}
		else if (type.equals(ATTACHMENT))
		{
			String parent = fieldSec.get(parentId);
			if (parent == null) parent = PUBLICATION + SEP + ISSUE + SEP + SECTION;
			path = parent + SEP + name;
			fieldAtt.put(id, path);
		}
		return path;
	}
	public void print()
	{
		printHashMap(fieldPub);
		printHashMap(fieldIss);
		printHashMap(fieldSec);
		printHashMap(fieldAtt);
	}
	public void printHashMap(HashMap<String,String> map)
	{
		Iterator<String> keys = map.keySet().iterator();
		while(keys.hasNext())
		{
			String key = keys.next();
			System.out.println(key+"\t"+map.get(key));
		}
	}
}

init();//run the script