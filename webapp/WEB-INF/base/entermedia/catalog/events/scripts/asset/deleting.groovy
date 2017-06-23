package asset;

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data

public void init() {
	String dataid = context.getRequestParameter("dataid");
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	Asset deleting = mediaArchive.getAsset(dataid);
	if(Boolean.valueOf( deleting.getValue("duplicate") ) )
	{
		//Search for guid
		String md5 = deleting.getValue("md5hex");
		Collection hits = mediaArchive.query("asset").match("md5hex", md5).search();
		if( hits.size() == 2)
		{
			for(Data asset in hits)
			{
				if( !dataid.equals( asset.getId()  ) )
				{
					asset = mediaArchive.getAssetSearcher().loadData(asset);
					asset.setValue("duplicate","false");
					mediaArchive.saveAsset(asset);
				}
			}
		}
	
	}
	
}

init();

