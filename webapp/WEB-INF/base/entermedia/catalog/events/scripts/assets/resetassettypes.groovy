package assets
import org.openedit.data.Searcher
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.*
import org.openedit.entermedia.creator.*
import org.openedit.entermedia.edit.*
import org.openedit.entermedia.episode.*
import org.openedit.entermedia.modules.*
import org.openedit.entermedia.search.AssetSearcher
import org.openedit.xml.*

import com.openedit.hittracker.*
import com.openedit.page.*
import com.openedit.util.*

import conversions.*


public void resetTypes() {
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos

	AssetSearcher searcher = mediaarchive.getAssetSearcher();

	Searcher typesearcher = mediaarchive.getSearcher("assettype");

	HitTracker types = typesearcher.getAllHits();
	HashMap typemap = new HashMap();
	types.each{
		String extentions = it.extensions;
		String type = it.id;
		if(extentions != null){
			String[] splits = extentions.split(" ");
			splits.each{
				typemap.put(it, type)
			}
		}
	}


	HitTracker allassets  = searcher.getAllHits();
	List tosave = new ArrayList();
	for (Data hit in allassets)
	{
		String fileformat = hit.fileformat;
		String currentassettype = hit.assettype;
		String assettype = typemap.get(fileformat);
		if(assettype == null){
			assettype="none";
		}
		if(!assettype.equals(currentassettype)){
			Asset real = mediaarchive.getAsset(hit.id);
			real.setProperty("assettype", assettype);
			tosave.add(real);
		}
		if(tosave.size() == 100){
			searcher.saveAllData(tosave, context.getUser());
			tosave.clear();
		}

	}
	searcher.saveAllData(tosave, context.getUser());



}
resetTypes();