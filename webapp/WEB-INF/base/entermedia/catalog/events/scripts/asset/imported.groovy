package asset;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.Category
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher

public void init()
{
	String assetids = context.getRequestParameter("assetids");
	if( assetids == null)
	{
		log.info("No assetid found");
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
			//Look for any parent values
			Category cat = it;
			Collection values = searcher.query().orgroup("categoryid",cat.getParentCategories()).search();
			values.each
			{
				Data row = it;
				String field = row.get("fieldname");
				if( field.equals("retentionpolicy") )
				{
					foundretention = true;
				}
				if( asset.getValue(field) == null )
				{	
					String value = row.get("fieldvalue");
					asset.setProperty(field,value);
					foundone = true;
				}	
			}
		}
	}	
}

init();



