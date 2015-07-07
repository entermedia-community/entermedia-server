import org.openedit.Data
import org.openedit.entermedia.MediaArchive

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker



public void init(){
	log.info("Here");
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	HitTracker hits = req.getPageValue("hits");
	if (hits){
		org.openedit.entermedia.Category data = req.getPageValue("category");
		if( data == null)
		{
			String catid = req.getRequestParameter("category");
			if( catid != null)
			{
				data = archive.getCategory(catid);
			}
		}
		if (data){
			String categoryid = "${data.id}";
			int max = toInt(archive.getCatalogSettingValue("category_hierarchy_peeknumber"),4);//give it a default of 4 if not specified
			if (max > 4 || max <= 0) max = 4;//only supports 4 at the most
			Map<Data,List<Data>> map = new HashMap<Data,List<Data>>();
			
			data.getChildren().each
			{
				Data cat = it;
				HitTracker assets = archive.getAssetSearcher().query().match("category", cat.getId()).sort("uploadeddate").search();
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