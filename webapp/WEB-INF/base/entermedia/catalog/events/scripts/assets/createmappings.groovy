package assets
import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.event.WebEvent

import com.openedit.hittracker.*

public void init() {
	MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	Searcher targetsearcher = mediaArchive.getAssetSearcher();
	
	
	SearchQuery q = targetsearcher.createSearchQuery();
	
	WebEvent webevent = context.getPageValue("webevent");
	if( webevent != null)
	{
		String sourcepath = webevent.getSourcePath();
		if( sourcepath != null )
		{
			q.addExact("sourcepath", sourcepath);
		}
	}
	
	//q.addSortBy("id");
	q.addMatches("id", "*");
	
	
	
	HitTracker assets = targetsearcher.search(q);

	assets.setHitsPerPage(100000);
	String mappingtable = context.findValue("mappingtable");
	String parentdetail = context.findValue("parentfield");
	String fields = context.findValue("childfields");
	String[] allfields = fields.split(",");
	Searcher mappings = mediaArchive.getSearcher(mappingtable);

	int count = 0;
	int edited = 0;
	log.info("Starting ${assets.size()}");
	List assetsToSave = new ArrayList();
	assets.each
	{

		Data hit =  it;
		String parentvalue = hit.get(parentdetail);
		if(parentvalue != null){

			count++;
			Asset asset = mediaArchive.getAssetBySourcePath(hit.getSourcePath());
			if( asset != null )
			{
				Data remotevalues = null;
				try{
				 remotevalues = mappings.searchByField(parentdetail, parentvalue);
				} catch (Exception e){
				println 'Cannot set values on multiples?';
				
				}
				if(remotevalues != null){
					allfields.each{
						String valtoset = remotevalues.get(it);
						asset.setProperty(it, valtoset);
					}
				}
				assetsToSave.add(asset);
				edited++;
			}
		}
	}

	mediaArchive.saveAssets assetsToSave;
	log.info("checked " + count + " records. Edited " + edited);

}

init();