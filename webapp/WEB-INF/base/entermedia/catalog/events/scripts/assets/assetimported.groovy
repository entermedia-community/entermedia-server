package assets

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher

public void init()
{
	String assetids = context.getRequestParameter("assetids");
	if( assetids == null)
	{
		return;
	}
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	Collection assets = mediaarchive.getAssetSearcher().query().orgroup("id",assetids).search();
	
	Searcher searcher = mediaarchive.getSearcher("categorydefaultdata");
	assets.each
	{
		boolean foundone = false;
		boolean foundretention = false;
		Asset asset = mediaarchive.getAssetSearcher().loadData(it);
		asset.getCategories().each
		{
			Collection values = searcher.query().exact("categoryid",it.getId()).search();
			values.each
			{
				Data row = it;
				String field = row.get("fieldname");
				if( field.equals("retentionpolicy") )
				{
					foundretention = true;
				}
					
				String value = row.get("fieldvalue");
				asset.setProperty(field,value);
				foundone = true;
			}
		}
		if( foundone)
		{
			mediaarchive.saveAsset(asset,null);
			if( foundretention )
			{
				mediaarchive.fireMediaEvent("asset/archivefiles",null,asset);
			}
		}
	}	
}

init();



