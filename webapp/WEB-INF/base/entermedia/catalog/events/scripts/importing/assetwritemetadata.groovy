package importing

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.xmp.XmpWriter
import org.openedit.Data
import org.openedit.data.CompositeData
import org.openedit.data.Searcher

public void init()
{
		MediaArchive archive = context.getPageValue("mediaarchive");
		String assetid = context.findValue("assetid");
		log.info("Writing metadata for asset $assetid");
		
		if (assetid != null && assetid.startsWith("multiedit:"))
		{
			CompositeData assets = (CompositeData) inReq.getSessionValue(assetid);
			for( Data data in assets)
			{
				Asset asset = archive.getAssetSearcher().loadData(data);
				writeAsset(asset);
			}
		}
		else
		{
			Asset asset = archive.getAsset("$assetid");
			if( asset != null)
			{
				writeAsset(asset);
			}	
		}	
		
}

public void writeAsset(MediaArchive archive, Asset asset)
{	
	XmpWriter writer = (XmpWriter) archive.getBean("xmpWriter");
	
	if( archive.isTagSync(asset.getFileFormat() ) )
	{
		boolean didSave = writer.saveMetadata(archive, asset);
		if(!didSave)
		{
			log.info("Failed to write metadata for asset " + asset.getId());
		}
	}

	log.info("metadata reading complete");
}

init();