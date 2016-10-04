package collections;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.projects.ProjectManager
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker




public void init(){

	MediaArchive archive = context.getPageValue("mediaarchive");
	Searcher collectionsearcher = archive.getSearcher("librarycollection");

	String collectionid = context.getRequestParameter("collectionid");
	HitTracker collections = null;
	if(collectionid == null){
		collections = collectionsearcher.getAllHits();
	} else{

		collections = collectionsearcher.fieldSearch("id", collectionid);
	}
	ProjectManager projects = archive.getProjectManager();
	collections.enableBulkOperations();
	Searcher catsearcher = archive.getSearcher("category");
	collections.each{
		String name = it.name;

		String [] splits = name.split("-");
		String searchstring = splits[splits.length -1];
		searchstring = searchstring.replaceFirst("^0+(?!\$)", "")

		String colid = it.id;
		Data collection = archive.getData("librarycollection", colid);
		HitTracker categories =  catsearcher.query().contains("categorypath", searchstring).sort("categorypathUp").search();
		log.info("Found ${categories.size()} existing categories");
		
		
		
		categories.enableBulkOperations();
		if(categories.size() > 0){
			ArrayList rootcats = findCommonRoots(categories);
			rootcats.remove(collection.get("rootcategory"));
			context.putPageValue("foundcategories", rootcats);
		}
	}
}


public List findCommonRoots(HitTracker inCategories){
	TreeMap allcats = new TreeMap();
	MediaArchive archive = context.getPageValue("mediaarchive");

	Searcher catsearcher = archive.getSearcher("category");

	inCategories.each{

		String catpath = it.categorypath;
		if(catpath){
			allcats.put(it.id, it.categorypath);
		}
	}


	ArrayList finalist = new ArrayList();
	ArrayList paths = new ArrayList();


	boolean removed = true;

	allcats.keySet().each{
		String id = (String)it;
		String catpath = allcats.get(id);
		boolean add = true;
		for (Iterator iterator = paths.iterator(); iterator.hasNext();){
			String existing = (String)iterator.next();
			if(catpath.startsWith(existing)){
				add = false;
			} else{
			add = true;
			}
		}
		if(add){
			finalist.add(id);
			paths.add(catpath);
			
		}
	}
	return finalist;
}

init();

