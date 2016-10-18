package assets

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher

public void init()
{
	String assetid = context.getRequestParameter("assetid");
	if( assetid == null)
	{
		return;
	}
	MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");
	Asset asset = mediaarchive.getAsset(assetid);
	
	Searcher searcher = mediaarchive.getSearcher("categorydefaultdata");
	boolean foundone = false;
	asset.getCategories().each
	{
		Collection values = searcher.query().exact("categoryid",it.getId()).search();
		values.each
		{
			Data row = it;
			String field = row.get("fieldname");
			String value = row.get("fieldvalue");
			asset.setProperty(field,value);
			foundone = true;
		}
	}
	if( foundone)
	{
		mediaarchive.saveAsset(asset);
	}
}

init();



