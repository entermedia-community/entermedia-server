package asset;

import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.Data
import org.openedit.OpenEditException
import org.openedit.WebPageRequest
import org.openedit.data.BaseSearcher
import org.openedit.hittracker.HitTracker
import org.openedit.util.Counter
import org.openedit.util.PathUtilities




public void init()
{
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");

	HitTracker hits = archive.getAssetSearcher().query().all().sort("sourcepath").search();
	hits.enableBulkOperations();
	log.info("Processing " + hits.size());
	
	List tosave = new ArrayList();
	Counter counter = new Counter();
	
	int savedsofar = 0;
	int loops = 0;
	ArrayList cats = new ArrayList();
	//archive.getCategorySearcher().deleteAll(null);
	AssetSearcher assetsearcher = (AssetSearcher)archive.getAssetSearcher();
	Collection collectionscatids = new HashSet();
	
	Category collectionsroot = archive.getCategorySearcher().createCategoryPath("Collections");
	
	populateChildren(collectionsroot, collectionscatids);
	
	collectionscatids.add("index");
	deleteAllExcept(archive.getCategorySearcher(),collectionscatids);
	
	Category root = archive.getCategorySearcher().getRootCategory();
	archive.getCategorySearcher().saveCategory(root);
	
	hits.each{
		Data hit = it;
		String path = hit.getValue("archivesourcepath");
		if(path == null){
			path = hit.getSourcePath();	
		}
		else
		{
			path = path.substring("sourcepath".length() + 1);
		}	
		//log.info("found " + path);
			
		//Asset found = archive.getAssetSearcher().loadData(hit);
		Data found = hit;
		//found.clearCategories();
		
		String folder = found.getValue("isfolder");
		if(folder != null && Boolean.parseBoolean( folder ) )   //   a/b/c.jpg -> a/b   if /a/b  --> a/b
		{
			path = PathUtilities.extractDirectoryPath(path);
		}
		Collection assetexactids = found.getValues("categories-exact");
		Collection tosaveExactcategory = new HashSet();
		Collection tosaveassetcategories = new HashSet();
		for (String id in assetexactids) {
			if( collectionscatids.contains(id))
			{
				Category foundcollection = archive.getCategory(id);
				tosaveExactcategory.add(foundcollection);
				tosaveassetcategories.addAll(foundcollection.getParentCategories());
			}
		}
		//only keep ones that are within collections
		
		org.entermediadb.asset.Category catparent = createCategoryPath(archive, cats, path, counter);
		if( catparent == null)
		{
			throw new OpenEditException("path made a null category " + path);
		}
		//found.addCategory(catparent);  //Load up with none since they are all deleted
		tosaveExactcategory.add(catparent);
		found.setValue("category-exact",tosaveExactcategory);
		found.setValue("category", tosaveassetcategories );
		//Get the path and fill in categories strcture
		
		tosave.add(found);
		savedsofar++;
		
		if( tosave.size() == 1000)
		{
			archive.getCategorySearcher().saveAllData(cats, null);
			cats.clear();		
			assetsearcher.updateIndex(tosave);
			tosave.clear();
			log.info("fixcategories saved: New Categories :${counter.getCount()} on ${hits.size()} assets");
		}
		
	}
	archive.saveAssets(tosave, null);
	archive.getCategorySearcher().saveAllData(cats, null);
	log.info("finishedcats saved: Categories :${counter.getCount()} on ${hits.size()} assets");
}

init();

public void deleteAllExcept(BaseSearcher searcher,collectionscatids)
{
	HitTracker all = searcher.getAllHits();
	all.enableBulkOperations();
	Collection todelete = new ArrayList(1000);
	for (Data cat in all) {
		if( !collectionscatids.contains(cat.getId()) )
		{
			todelete.add(cat);
		}
		if( todelete.size() == 1000)
		{
			searcher.deleteAll(todelete, null);
			todelete.clear();
		}
	}
	searcher.deleteAll(todelete, null);
}

public void populateChildren(Category inRoot, Set ids )
{
	ids.add(inRoot.getId());
	for (Category child in inRoot.getChildren()) 
	{
		populateChildren(child,ids);
	}
}




public Category createCategoryPath(MediaArchive archive, List cats, String inPath, Counter counter)
{
	//log.info("top " + inPath);
	if( inPath.length() == 0 || inPath.equals("Index"))
	{
		return archive.getCategorySearcher().getRootCategory();
	}
	Category cat = archive.getCacheManager().get("catfix", inPath);   ///Make sure we cache this
	
	if(cat != null)
	{
		return cat;
	}
	//log.info("before " + inPath);
	
	//TODO: Find right way to do this not matches
	Data hit = (Data)archive.getCategorySearcher().query().startsWith("categorypath", inPath).sort("categorypathUp").searchOne();
	
	Category found = (Category)archive.getCategorySearcher().loadData(hit);
	if( found == null)
	{
	//	log.info("not found " + inPath);
		found = (Category)archive.getCategorySearcher().createNewData();
		found.setId(counter.printNext());
		String name = PathUtilities.extractFileName(inPath);
		found.setName(name);
		//log.info("Created " + inPath);
		//create parents and itself
		cats.add(found);
		archive.getCacheManager().put("catfix", inPath, found );
		String parent = PathUtilities.extractDirectoryPath(inPath);
		if( parent != null && !parent.trim().isEmpty())
		{
			try
			{
				Category parentcategory = createCategoryPath(archive, cats, parent, counter);
				parentcategory.addChild(found);
			}
			catch ( StackOverflowError error)
			{
				log.error("Failed to load: " + inPath);
				return null;
			}
		}
		else if( found.getParentCategory() == null)
		{
			archive.getCategorySearcher().getRootCategory().addChild(found);
			archive.getCategorySearcher().saveCategory(found);
		}
	}
	
	return found;
}




