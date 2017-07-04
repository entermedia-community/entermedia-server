package asset;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.WebPageRequest
import org.openedit.hittracker.HitTracker
import org.openedit.util.Counter
import org.openedit.util.PathUtilities




public void init(){
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	
	HitTracker hits = archive.getAssetSearcher().query().match("id", "*").sort("id").search();
	hits.enableBulkOperations();
	List tosave = new ArrayList();
	Counter counter = new Counter();
	int savedsofar = 0;
	int loops = 0;
	ArrayList cats = new ArrayList();
	hits.each{
		Data hit = it;
		String path = hit.getValue("archivesourcepath");
		if(path == null){
			path = hit.getSourcePath();	
		}		
		Asset found = archive.getAssetSearcher().loadData(hit);

		if(!found.isFolder())   //   a/b/c.jpg -> a/b   if /a/b  --> a/b
		{
			path = PathUtilities.extractDirectoryPath(path);
		}
		
		org.entermediadb.asset.Category catparent = createCategoryPath(archive, cats, path, counter);
		if( catparent != null)
		{
			found.addCategory(catparent);  //Load up with none since they are all deleted
			tosave.add(found);
			savedsofar++;
		}	
		
		if( tosave.size() == 1000)
		{
			archive.getCategorySearcher().saveAllData(cats, null);
			cats.clear();		
			archive.saveAssets(tosave, null);
			tosave.clear();
			log.info("fixcategories saved: New Categories :${counter.getCount()} on ${hits.size()} assets");
		}
		
	}
	archive.saveAssets(tosave, null);
	archive.getCategorySearcher().saveAllData(cats, null);
	log.info("finishedcats saved: Categories :${counter.getCount()} on ${hits.size()} assets");
}

init();






public Category createCategoryPath(MediaArchive archive, List cats, String inPath, Counter counter)
{
	if( inPath.length() == 0 || inPath.equals("Index"))
	{
		return archive.getCategorySearcher().getRootCategory();
	}
	Category cat = archive.getCacheManager().get("catfix", inPath);   ///Make sure we cache this
	
	if(cat != null)
	{
		return cat;
	}
	
	
	//TODO: Find right way to do this not matches
	Data hit = (Data)archive.getCategorySearcher().query().startsWith("categorypath", inPath).sort("categorypathUp").searchOne();
	
	Category found = (Category)archive.getCategorySearcher().loadData(hit);
	if( found == null)
	{
		found = (Category)archive.getCategorySearcher().createNewData();
		found.setId(counter.printNext());
		String name = PathUtilities.extractFileName(inPath);
		found.setName(name);
		//create parents and itself
		String parent = PathUtilities.extractDirectoryPath(inPath);
		if( parent != null && !parent.trim().isEmpty())
		{
			try
			{
				Category parentcategory = createCategoryPath(archive, cats, parent, counter);
				if( parentcategory != null)
				{
					parentcategory.addChild(found);
				}
				cats.add(found);
			}
			catch ( StackOverflowError error)
			{
				log.error("Failed to load: " + inPath);
				return null;
			}
		}
	}
	archive.getCacheManager().put("catfix", inPath, found );
	
	return found;
}




