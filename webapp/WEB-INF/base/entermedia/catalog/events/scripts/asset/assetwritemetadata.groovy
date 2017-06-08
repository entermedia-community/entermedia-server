package asset;


import java.util.Iterator

import org.entermediadb.asset.Asset
import org.entermediadb.asset.CompositeAsset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.search.AssetSearcher
import org.entermediadb.asset.xmp.XmpWriter
import org.openedit.Data



public void init()
{

	MediaArchive archive = context.getPageValue("mediaarchive");
	AssetSearcher searcher = archive.getAssetSearcher();
	

	String assetid = context.findValue("assetid");
	log.info("Writing metadata for asset $assetid");
	
	XmpWriter writer = (XmpWriter) archive.getModuleManager().getBean("xmpWriter");
	Asset asset = archive.getAsset("$assetid",context);
	if( asset instanceof CompositeAsset)
	{
		CompositeAsset multi = (CompositeAsset)asset;
		for (Iterator iterator = multi.getSelectedResults().iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset entry = searcher.loadData(data);
			writeAsset(archive,writer,entry);
		}	
	}
	else
	{
		writeAsset(archive,writer,asset);
	}
		
}

public void writeAsset(MediaArchive archive,XmpWriter writer, Asset asset)
{
	if( archive.isTagSync(asset.getFileFormat() ) )
		{
			boolean didSave = writer.saveMetadata(archive, asset);
			if(!didSave){
				log.info("Failed to write metadata for asset " + asset.getId());
			
			}
		}
	
}

init();