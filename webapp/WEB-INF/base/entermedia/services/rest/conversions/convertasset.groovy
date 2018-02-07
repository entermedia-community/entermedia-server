import java.util.Date

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.OpenEditException
import org.openedit.data.Searcher
import org.openedit.repository.ContentItem
import org.openedit.util.DateStorageUtil

public void init()
{
	String catalogid = context.findValue("catalogid");
	MediaArchive archive = moduleManager.getBean(catalogid,"mediaArchive");
	
	//load up the preset id
	String presetid = context.findValue("presetid");
	Data preset = archive.getData("convertpreset", presetid);
	if( preset == null)
	{
		log.error("No such preset "+ presetid);
		return;
	}
	context.putPageValue("preset", preset);
	String assetid = context.findValue("assetid");
	Searcher tasksearcher = archive.getSearcher("conversiontask");
	int loop = 0;
	Asset asset = archive.getAsset(assetid);
	context.putPageValue("catalogid", catalogid);
	if( asset == null)
	{
		log.error("No such asset "+ assetid);
		return;
	}
	context.putPageValue("asset", asset);

	if(preset.get("transcoderid") == "original")
	{
		log.info("Getting original transcoder");
		return;
	}
	
	if( asset.getFileFormat() != null && asset.getFileFormat().endsWith("mp3"))
	{
		log.info("Mp3 uses original as output");
		return; //No output expected
	}

		
	while( true )
	{
		loop++;
		Data one = tasksearcher.query().exact("assetid", assetid).exact("presetid", presetid).searchOne();
		if( one == null)
		{
			one = tasksearcher.createNewData();
			log.error("Creating missing task for asset "+ assetid + "preset " + presetid);
			one.setSourcePath(asset.getSourcePath());
			one.setProperty("status", "new");
			one.setProperty("assetid", asset.getId() );
			one.setProperty("presetid", preset.getId() );
			one.setProperty("ordering", preset.get("ordering") );
			String nowdate = DateStorageUtil.getStorageUtil().formatForStorage(new Date() );
			one.setProperty("submitteddate", nowdate);
			tasksearcher.saveData(one, null);
			archive.fireSharedMediaEvent("conversions/runconversions");
			Thread.sleep(200);
			continue;
		}
		String status = one.get("status");
		
		/*
		 * <property id="missinginput">Input Missing</property>
	<property id="submitted">Processing</property>
	<property id="complete">Complete</property>
	<property id="retry">Retry</property>
	<property id="expired">Expired</property>
	
	<property id="error">Error</property>
		 */
		
		if("complete".equals( status) 
		|| "error".equals( status)
		|| "expired".equals( status)
		|| "missinginput".equals( status)
		){
			context.putPageValue("conversiontask", one);
			context.putPageValue("catalogid", catalogid);

			//log.info("finidhes" + preset);
			
			String exportname = preset.get("generatedoutputfile");
			ContentItem custom = archive.getContent( "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + asset.getSourcePath() + "/" + exportname);
			if( !custom.exists())
			{
				if("error".equals(status)){
					return;
				} else{
					one.setProperty("status", "retry");
				}
				tasksearcher.saveData(one, null);
				Thread.sleep(200);
				log.info("Generated output not found. Recreating asset "+ assetid + "preset " + presetid);
				continue;
			}
			
			return;
		}
		if( loop > 27000) //27000 * 200 = 5400S = 90Minutes
		{
			throw new OpenEditException("Conversion timeout on long running api call " + assetid + "  preset:" + presetid);
		}
		if( loop % 100 == 1)
		{
			log.info("Waiting " + (loop * 200 )/1000 + " seconds for " + presetid + " on asset: " + assetid );
		}
		Thread.sleep(200);
	}
	
}

init();