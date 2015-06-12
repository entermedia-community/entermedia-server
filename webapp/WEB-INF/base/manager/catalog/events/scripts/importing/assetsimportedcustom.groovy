package importing;

import model.assets.LibraryManager

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.xmp.XmpWriter

import assets.model.AssetTypeManager
import assets.model.EmailNotifier

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.manage.*
import com.openedit.users.User
import com.openedit.users.UserManager

import java.util.StringTokenizer


public void setAssetTypes()
{
	log.info("Starting Assets Imported Custom")
	String ids = context.getRequestParameter("assetids");
	if( ids == null)
	{
	   log.info("AssetIDS required");
	   return;
	}
	
	//TODO: Get rid of the need to search all the assets use the Asset Cache
	String assetids = ids.replace(","," ");

	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher assetsearcher = mediaArchive.getAssetSearcher();
	SearchQuery q = assetsearcher.createSearchQuery();
	q.addOrsGroup( "id", assetids );

	HitTracker assets = assetsearcher.search(q);
	AssetTypeManager manager = new AssetTypeManager();
	manager.context = context;
	manager.log = log;
	manager.saveAssetTypes(assets);
	
	setupProjects(assets);
	
}
public void sendEmail()
{
	EmailNotifier emailer = new EmailNotifier();
	emailer.context = context;
	emailer.emailOnImport();
}

public void setupProjects(HitTracker assets)
{
	//Look at source path for each asset?
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos
	LibraryManager librarymanager = new LibraryManager();
	librarymanager.log = log;
	librarymanager.assignLibraries(mediaarchive, assets);
		
	
}

public void verifyRules()
{
	String ids = context.getRequestParameter("assetids");
	if( ids == null)
	{
	   log.info("AssetIDS required");
	   return;
	}
	String assetids = ids.replace(","," ");

	MediaArchive mediaArchive = context.getPageValue("mediaarchive");
	Searcher assetsearcher = mediaArchive.getAssetSearcher();
	SearchQuery q = assetsearcher.createSearchQuery();
	q.addOrsGroup( "id", assetids );

	HitTracker assets = assetsearcher.search(q);
	assets.each{
		 Asset asset = mediaArchive.getAsset("${it.id}");
		 if(asset.width != null){
			 int width = Integer.parseInt(asset.width);
			 if(width < 1024){
				 asset.setProperty("editstatus", "rejected");
				 asset.setProperty("notes", "Asset did not meet minimum width criteria.  Width was ${asset.width}");
				 
			 }
			 assetsearcher.saveData(asset, null);
		 }
	}
}

//sets default metadata fields on assets where required
public void setDefaultMetadataFields(){
	String ids = context.getRequestParameter("assetids");
	if( ids == null)
	{
	   log.info("AssetIDS required");
	   return;
	}
	String assetids = ids.replace(","," ");
	MediaArchive archive = context.getPageValue("mediaarchive");
	XmpWriter xmpWriter = (XmpWriter) archive.getModuleManager().getBean("xmpWriter");
	Searcher assetsearcher = archive.getAssetSearcher();
	SearchQuery q = assetsearcher.createSearchQuery();
	q.addOrsGroup( "id", assetids );
	HitTracker assets = assetsearcher.search(q);
	assets.each{
		 Asset asset = archive.getAsset("${it.id}");
		 setMetadata(archive,asset,xmpWriter);
	}
}

public boolean setMetadata(MediaArchive inArchive,Asset inAsset, XmpWriter inXmpWriter) {
	log.info("setting default metadata for "+inAsset);
	String ASSET_METADATA_TABLE = "defaultassetmetadata";
	String owner = inAsset.get("owner");
	Data ownerdata = null;
	if (owner != null){
		Searcher userprofilesearcher = inArchive.getSearcher("userprofile");
		ownerdata = (Data) userprofilesearcher.searchById(owner);
	}
	UserManager usermanager = inArchive.getModuleManager().getBean("userManager");
	Searcher searcher = inArchive.getSearcher(ASSET_METADATA_TABLE);
	if (searcher == null){
		log.info(ASSET_METADATA_TABLE+" not defined, aborting");
		return false;
	}
	HitTracker hits = searcher.getAllHits();
	Iterator<?> itr = hits.iterator();
	boolean hasModified = false;
	while (itr.hasNext()){
		Data data = (Data) itr.next();
		String field = data.get("field");
		String value = data.get("value");
		String userrole = data.get("settingsgroup");//user role
		String fieldlookup = data.get("userfield");//field in user table
		//check if permissions are enabled for this rule
		if (userrole!=null && ownerdata!=null){
			String role = ownerdata.get("settingsgroup");
			if (!role.equals(userrole)){//user does not belong to particular role
				continue;
			}
		}
		//check if user table lookup is required
		if (field!=null && fieldlookup!=null && owner!=null){
			User userobj = usermanager.getUser(owner);
			String uservalue = userobj.get(fieldlookup);
			if (uservalue!=null){
				inAsset.setProperty(field,uservalue);
				log.info("setting "+field+":"+value+" on "+inAsset);
				hasModified = true;
			}
		} else if (field!=null && value!=null){
			inAsset.setProperty(field,value);
			log.info("setting "+field+":"+value+" on "+inAsset);
			hasModified = true;
		}
	}
	if (hasModified){
		log.info("modifications made to asset "+inAsset+", saving");
		inArchive.saveAsset(inAsset,null);
		try{
			log.info("writing metadata mods to original file, "+inAsset);
			return inXmpWriter.saveMetadata(inArchive,inAsset);
		}catch(Exception e){
			log.info("exception caught updating metadata for "+inAsset+", "+e.getMessage());
		}
	} else{
		log.info("no new metadata modifications made to asset, returning");
	}
	return false;
}

public void setDefaultLibrary(){
	log.info("setting default library");
	String ids = context.getRequestParameter("assetids");
	if( ids == null)
	{
	   log.info("AssetIDS required");
	   return;
	}
	String assetids = ids.replace(","," ");
	MediaArchive archive = context.getPageValue("mediaarchive");
	//get library
	Data library = null;
	String libraryid = archive.getCatalogSettingValue("default_library");
	if (libraryid != null && !libraryid.isEmpty()){
		Searcher libsearcher = archive.getSearcher("library");
		library = libsearcher.searchById(libraryid);
	}
	if (library == null){
		log.info("Unable to find default library, exiting");
		return;
	}
	Searcher assetsearcher = archive.getAssetSearcher();
	SearchQuery q = assetsearcher.createSearchQuery();
	q.addOrsGroup( "id", assetids );
	HitTracker assets = assetsearcher.search(q);
	List assetsToSave = new ArrayList();
	assets.each{
		 Asset asset = archive.getAsset("${it.id}");
		 if (asset!=null)
		 {
			 if (asset.getLibraries().isEmpty()){
				 asset.addLibrary(library.getId());
				 assetsToSave.add(asset);
				 if(assetsToSave.size() == 100)
				 {
					 archive.saveAssets( assetsToSave );
					 assetsToSave.clear();
				 }
			 }
		 }
	}
	if (!assetsToSave.isEmpty()){
		archive.saveAssets( assetsToSave );
	}
	
}

public void setDefaultTags(){
	log.info("setting default tags");
	WebPageRequest req = context;
	String ids = req.getRequestParameter("assetids");
	if( ids == null)
	{
	   log.info("AssetIDS required");
	   return;
	}
	String assetids = ids.replace(","," ");
	MediaArchive archive = req.getPageValue("mediaarchive");
	//check how tags are enabled
	String enabled = archive.getCatalogSettingValue("hotfolder_tags_enabled");
	if (Boolean.parseBoolean(enabled) == false){
		log.info("hotfolder tags not enabled, exiting");
		return;
	}
	// entry1 | entry2 | etc.
	String entries = archive.getCatalogSettingValue("hotholder_tags_origins");//getCatalogSettingValues() doesn't work
	if (entries == null || entries.isEmpty()){
		log.info("hotfolder tags origins not defined, exiting");
		return;
	}
	//custom splitting
	List<String> origins = new ArrayList<String>();
	StringTokenizer toks = new StringTokenizer(entries,"|",false);
	while(toks.hasMoreTokens()){
		String tok = toks.nextToken().trim();
		if (tok.isEmpty()){//shouldn't happen
			continue;
		}
		if (!tok.endsWith("/")){	
			tok = "${tok}/";
		}
		origins.add(tok);
	}
	log.info("Searching for sourcepaths with the following substrings, $origins");
	Searcher assetsearcher = archive.getAssetSearcher();
	SearchQuery q = assetsearcher.createSearchQuery();
	q.addOrsGroup( "id", assetids );
	HitTracker assets = assetsearcher.search(q);
	List assetsToSave = new ArrayList();
	assets.each{
		 Asset asset = archive.getAsset("${it.id}");
		 if (asset!=null)
		 {
			 String sourcepath = asset.getSourcePath();
			 for(String origin:origins){
				 if (sourcepath.contains(origin)){
					 List<String> keywords = asset.getKeywords();
					 int len = keywords.size();
					 String tagstring = sourcepath.substring(sourcepath.indexOf(origin)+origin.length());
					 toks = new StringTokenizer(tagstring,"/",false);
					 while(toks.hasMoreTokens()){
						 String tag = toks.nextToken().trim();
						 if (tag == asset.getName()){
							 continue;
						 }
						 keywords.add(tag);
					 }
					 if (len == keywords.size()){
						 continue;
					 }
					 log.info("${asset.id}: keywords changed to $keywords");
	 				 assetsToSave.add(asset);
	 				 if(assetsToSave.size() == 100)
	 				 {
	 					 archive.saveAssets( assetsToSave );
	 					 assetsToSave.clear();
	 				 }
				 }
			 }
		 }
	}
	if (!assetsToSave.isEmpty()){
		archive.saveAssets( assetsToSave );
	}
}


setAssetTypes();
//setDefaultMetadataFields();
//setDefaultLibrary();
setDefaultTags();
//verifyRules();
//sendEmail();

