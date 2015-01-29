package modules.convert;

import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.modules.BaseMediaModule
import org.openedit.util.DateStorageUtil

import com.openedit.WebPageRequest

public class ConvertModule extends BaseMediaModule
{
	
	public void convertAsset(WebPageRequest inReq) throws Exception
	{
		String catalogid = inReq.findValue("catalogid");
		
		//load up the preset id
		String presetid = inReq.findValue("presetid");
		String assetid = inReq.findValue("assetid");
		
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = archive.getAsset(assetid);
		
		Searcher presetsearcher = archive.getSearcher("convertpreset");
		Searcher tasksearcher = archive.getSearcher("conversiontask");
		Data preset = archive.getSearcher("convertpreset").searchById( presetid );
		
		Data one = tasksearcher.query().match("assetid", asset.getId() ).match("presetid", presetid ).searchOne();
		
		if( one == null )
		{
			one = tasksearcher.createNewData();
			one.setProperty("status", "new");
		}
		else
		{
			one = tasksearcher.searchById(one.getId());
		}
		String status = one.get("status");
		if( status != "complete")
		{
			//TODO: Lock the asset?
			one.setSourcePath(asset.getSourcePath());
			one.setProperty("status", "new");
			one.setProperty("assetid", asset.getId() );
			one.setProperty("presetid", preset.getId() );
			one.setProperty("ordering", preset.get("ordering") );
			String nowdate = DateStorageUtil.getStorageUtil().formatForStorage(new Date() );
			one.setProperty("submitted", nowdate);
			tasksearcher.saveData(one, null);
		}
		
		inReq.putPageValue("asset", asset);
		inReq.putPageValue("preset", preset);
		inReq.putPageValue("conversiontask", one);
		
		String export = archive.asExportFileName(asset,preset);
		inReq.putPageValue("exportname", export);
		
		if( status.equals("complete") )
		{
			return;
		}
		archive.fireMediaEvent("conversions/runconversion", inReq.getUser(), asset); //this blocks does not use the queue	
		
	}
		
}
