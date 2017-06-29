package asset

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.page.manage.PageManager
import java.util.*;


public init()
{
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	PageManager pageManager = mediaarchive.getPageManager();
	
	List assets = new ArrayList();
	for(int i=0;i<2000;i++)
	{
		Asset newone = mediaarchive.getAssetSearcher().createNewData();
		newone.setSourcePath("newjunk/" + i);
		newone.setName("test" + i);
		assets.add(newone);
		if( assets.size() < 200)
		{
			mediaarchive.saveAssets(assets);
			assets.clear();
		}	
	}
	mediaarchive.saveAssets(assets);
	
}

init();
