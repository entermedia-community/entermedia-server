package asset;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.WebPageRequest
import org.openedit.hittracker.HitTracker
import org.openedit.util.PathUtilities


class counterholder {
	public static int count = 0;
}

public void init(){
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	counterholder.count++;
	HitTracker hits = archive.getAssetSearcher().query().match("id", "*").sort("id").search();
	hits.enableBulkOperations();
	List tosave = new ArrayList();
	int savedsofar = 0;
	
	ArrayList cats = new ArrayList();
	hits.each{
		Data hit = it;
//		if( hit.getValue("category-exact") == null)
		String path = PathUtilities.extractDirectoryPath(hit.getSourcePath());
		org.entermediadb.asset.Category catparent = createCategoryPath(archive, cats, path);
		Asset found = archive.getAssetSearcher().loadData(hit);
		found.addCategory(catparent);
		tosave.add(found);
		savedsofar++;
		
		if( tosave.size() == 2000)
		{
			archive.getCategorySearcher().saveAllData(cats, null);
			cats.clear();		
			archive.saveAssets(tosave, null);
			tosave.clear();
			log.info("saved assets ${savedsofar}");
		}
		
	}
	archive.saveAssets(tosave, null);
	archive.getCategorySearcher().saveAllData(cats, null);
	log.info("Finished fixcategories saved: ${savedsofar} ${hits.size()}");
}

init();






public Category createCategoryPath(MediaArchive archive, List cats, String inPath)
{
	Category cat = archive.getCacheManager().get("catfix", inPath);
	if(cat != null){
		return cat;
	}
	
	if( inPath.length() == 0 || inPath.equals("Index"))
	{
		return archive.getCategorySearcher().getRootCategory();
	}
	//TODO: Find right way to do this not matches
	Data hit = (Data)archive.getCategorySearcher().query().startsWith("categorypath", inPath).sort("categorypathUp").searchOne();
	
	Category found = (Category)archive.getCategorySearcher().loadData(hit);
	if( found == null)
	{
		found = (Category)archive.getCategorySearcher().createNewData();
		found.setId(String.valueOf(counterholder.count++));
		String name = PathUtilities.extractFileName(inPath);
		found.setName(name);
		//create parents and itself
		String parent = PathUtilities.extractDirectoryPath(inPath);
		Category parentcategory = createCategoryPath(archive, cats, parent);
		if( parentcategory != null)
		{
			parentcategory.addChild(found);
		}
		cats.add(found);
		
	}
	archive.getCacheManager().put("catfix", inPath, found );
	
	return found;
}




