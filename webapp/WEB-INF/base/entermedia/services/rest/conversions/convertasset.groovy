import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.OpenEditException
import org.openedit.data.Searcher

public void init()
{
	String catalogid = context.findValue("catalogid");
	MediaArchive archive = moduleManager.getBean(catalogid,"mediaArchive");
	
	//load up the preset id
	String presetid = context.findValue("presetid");
	String assetid = context.findValue("assetid");
	Searcher tasksearcher = archive.getSearcher("conversiontask");
	int loop = 0;
	Asset asset = archive.getAsset(assetid);
	if( asset == null)
	{
		log.error("No such asset "+ assetid);
		return;
	}
	context.putPageValue("asset", asset);
	
	while( true )
	{
		loop++;
		Data one = tasksearcher.query().exact("assetid", assetid).exact("presetid", presetid).searchOne();
		if( one == null)
		{
			//Queing?
			log.error("No task found on asset "+ assetid + "preset " + presetid);
			return;
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
			Data preset = archive.getData("convertpreset", presetid);
			context.putPageValue("preset", preset);
			context.putPageValue("conversiontask", one);
			context.putPageValue("catalogid", catalogid);
			//log.info("finidhes" + preset);
			return;
		}
		if( loop > 27000)
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