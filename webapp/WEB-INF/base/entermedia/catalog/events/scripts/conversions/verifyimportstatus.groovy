import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher

public void init()
{
		MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		Searcher searcher = archive.getAssetSearcher();
		Collection assets = searcher.fieldSearch("importstatus", "imported");
		
		for (Data hit in assets)
		{
			archive.updateAssetConvertStatus(hit.getSourcePath());
		}
		
}

init();