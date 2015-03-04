import org.openedit.Data
import org.openedit.entermedia.MediaArchive

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker



public void init(){
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	HitTracker hits = req.getPageValue("hits");
	if (hits){
		Data data = req.getPageValue("category");
		if (data){
			int max = toInt(archive.getCatalogSettingValue("category_hierarchy_peeknumber"),4);//give it a default of 4 if not specified
			if (max > 4 || max <= 0) max = 4;//only supports 4 at the most
			Map<Data,List<Data>> map = new HashMap<Data,List<Data>>();
			hits.each{
				Data hit = it;
				String cat = hit.get("category-exact").replace("|","").trim();
				if (cat.startsWith("${data.id}_")){
					cat = cat.substring("${data.id}_".length());
					String [] tokens = cat.split("_");
					if (tokens && tokens.length > 0){
						cat = tokens[0];
						Data categorydata = archive.getData("category","${data.id}_$cat");
						if (!categorydata){
							return;
						}
						if (!map.containsKey(categorydata)){
							map.put(categorydata,new ArrayList<Data>());
						}
						List<Data> list = map.get(categorydata);
						if (list.size() < max){
							list.add(hit);
						}
					}
				}
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