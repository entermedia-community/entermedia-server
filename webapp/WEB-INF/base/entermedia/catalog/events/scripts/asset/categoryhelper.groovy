package asset;

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.WebPageRequest
import org.openedit.hittracker.HitTracker

public void init(){
	log.info("Here");
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	HitTracker hits = req.getPageValue("hits");
	if (hits != null)
	{
		Collection categories = (Collection)req.getPageValue("collectioncategories");
		if( categories == null)
		{
			org.entermediadb.asset.Category parent = req.getPageValue("category");
			if( parent == null)
			{
				String catid = req.getRequestParameter("category");
				if( catid != null)
				{
					parent = archive.getCategory(catid);
				}
			}
			if( parent != null)
			{
				categories  = parent.getChildren();
			}
		}
		
		if (categories != null)
		{
			int max = toInt(archive.getCatalogSettingValue("category_hierarchy_peeknumber"),4);//give it a default of 4 if not specified
			if (max > 4 || max <= 0) max = 4;//only supports 4 at the most
			Map<Data,List<Data>> map = new HashMap<Data,List<Data>>();
			
			categories.each
			{
				Data cat = it;
				HitTracker assets = archive.getAssetSearcher().query().match("category", cat.getId()).sort("uploadeddate").search(context);
				assets.setHitsPerPage(max);
				map.put(cat,assets.getPageOfHits());
			}
			req.putPageValue("categorymap", map);
		}
	}
}

public int toInt(String inValue, int inDefault){
	try{
		return Integer.parseInt(inValue);
	}catch (Exception e){}
	return inDefault;
}

init();