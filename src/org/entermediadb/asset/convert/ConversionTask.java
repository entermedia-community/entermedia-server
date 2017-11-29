package org.entermediadb.asset.convert;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;

public class ConversionTask
{
	MediaArchive mediaarchive;
	Searcher tasksearcher;
	Searcher presetsearcher;
	Searcher itemsearcher;
	Data hit;
	User user;
	Asset asset;
	ConvertResult result = null;
	private static final Log log = LogFactory.getLog(ConversionTask.class);
	
	public boolean isComplete()
	{
		if( result != null && (result.isComplete() || result.isError() ) )
		{
			return true;
		}
		return false;
	}
	public boolean isError()
	{
		if( result != null && result.isError() )
		{
			return true;
		}
		return false;
	}

public void convert()
{
	Data realtask = tasksearcher.loadData(hit);
	//log.info("should be ${hit.status} but was ${realtask.status}");
	if( asset == null)
	{
		asset = mediaarchive.getAsset(hit.get("assetid"));
	}
	String presetid = realtask.get("presetid");
	//log.debug("starting preset ${presetid}");
	Data preset = (Data)presetsearcher.searchById(presetid);
	Date started = new Date();
	String errorcondition = null;
	if(preset == null)
	{
		errorcondition = "Invalid presetid " + presetid;
	}
	if(asset == null)
	{
		errorcondition = "Asset could not be loaded " + realtask.getSourcePath() + " marking as error";
	}
	if(errorcondition != null)
	{
		log.info("Can't run conversion for task '" + realtask.getId() + " " + errorcondition);
		realtask.setProperty("status", "error");
		realtask.setProperty("errordetails",errorcondition );
		
		String completed = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
		realtask.setProperty("completed",completed);
		tasksearcher.saveData(realtask, user);
		mediaarchive.fireMediaEvent("conversions","conversionerror",realtask.getId(),user);
		return;
	}
	try
	{
		result = doConversion(mediaarchive, realtask, preset,asset);
	}
	catch(Throwable e)
	{
		result = new ConvertResult();
		result.setOk(false);
		result.setError(e.toString());
		log.error("Conversion Failed",e);
	}
		
	realtask.setValue("submitteddate", started);
	if(result.isOk())
	{
		if(result.isComplete())
		{
			realtask.setProperty("status", "complete");
			String itemid = realtask.get("itemid");
			if(itemid != null)
			{
				//The item should have a pointer to the conversion, not the other way around
				Data item = (Data)itemsearcher.searchById(itemid);
				item.setProperty("status", "converted");
				itemsearcher.saveData(item, null);
			}
			realtask.setProperty("externalid", result.get("externalid"));
			realtask.setValue("completed",new Date());
			realtask.setProperty("errordetails","");
			tasksearcher.saveData(realtask, user);
			//log.info("Marked " + hit.getSourcePath() +  " complete");
			
			//mediaarchive.fireMediaEvent("conversions","conversioncomplete",user,asset);
			//mediaarchive.updateAssetConvertStatus(hit.get("sourcepath"));
		}
		else
		{
			realtask.setProperty("status", "submitted");
			realtask.setProperty("externalid", result.get("externalid"));
			tasksearcher.saveData(realtask, user);
		}
		
	}
	else if ( result.isError() )
	{
		realtask.setProperty("status", "error");
		realtask.setProperty("errordetails", result.getError() );
		String completed = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
		realtask.setProperty("completed",completed);
		tasksearcher.saveData(realtask, user);
		
		//TODO: Remove this one day
		String itemid = realtask.get("itemid");
		if(itemid != null)
		{
			Data item = (Data)itemsearcher.searchById(itemid);
			item.setProperty("status", "error");
			item.setProperty("errordetails", result.getError() );
			itemsearcher.saveData(item, null);
		}

		//	conversionfailed  conversiontask assetsourcepath, params[id=102], admin
		mediaarchive.fireMediaEvent("conversions","conversionerror",realtask.getId(),user);
	}
	else
	{
		String assetid = realtask.get("assetid");
		Date olddate = DateStorageUtil.getStorageUtil().parseFromStorage(realtask.get("submitted"));
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DAY_OF_YEAR,-2);
		if( olddate != null && olddate.before(cal.getTime()))
		{
			realtask.setProperty("status", "error");
			realtask.setProperty("errordetails", "Missing input expired" );
		}
		else
		{
			log.debug("conversion had no error and will try again later for "+ assetid);
			realtask.setProperty("status", "missinginput");
		}	
		tasksearcher.saveData(realtask, user);
	}
}

protected ConvertResult doConversion(MediaArchive inArchive, Data inTask, Data inPreset, Asset inAsset)
{
	String status = inTask.get("status");
	ConvertResult tmpresult = null;
	//String type = inPreset.get("transcoderid"); //rhozet, ffmpeg, etc
	if( "7".equals( inAsset.get("editstatus")) ) 
	{
		tmpresult = new ConvertResult();
		tmpresult.setOk(false);
		tmpresult.setError("Could not run conversions on deleted asset " + inAsset.getSourcePath());
		return tmpresult;
	}

	ConversionManager manager = inArchive.getTranscodeTools().getManagerByFileFormat(inAsset.getFileFormat());
	//log.debug("Converting with type: ${type} using ${creator.class} with status: ${status}");
	
	if (manager == null)
	{
		log.info("Can't find media creator for type '" + inAsset.getFileFormat() + "'");
		tmpresult = new ConvertResult();
		tmpresult.setOk(false);
		tmpresult.setError("Can't find media creator for type '" + inAsset.getFileFormat() + "'");
		return tmpresult;
	}
	Map props = new HashMap();
	
	String guid = inPreset.get("guid");
	if( guid != null)
	{
		Searcher presetdatasearcher = inArchive.getSearcherManager().getSearcher(inArchive.getCatalogId(), "presetdata" );
		Data presetdata = (Data)presetdatasearcher.searchById(guid);
		//copy over the preset properties..
		props.put("guid", guid); //needed?
		props.put("presetdataid", guid); //needed?
		if( presetdata != null && presetdata.getProperties() != null)
		{
			props.putAll(presetdata.getProperties());
		}
	}
	String pagenumber = inTask.get("pagenumber");
	if( pagenumber != null )
	{
		props.put("pagenum",pagenumber);
	}
	if(Boolean.parseBoolean(inTask.get("crop")))
	{
		props.put("iscrop","true");
		props.putAll(inTask.getProperties() );
		
		if(inTask.get("prefwidth") == null)
		{
			props.put("prefwidth", inTask.get("cropwidth"));
		}
		if(inTask.get("prefheight") == null)
		{
			props.put("prefheight", inTask.get("cropheight"));
		}
		props.put("useoriginalasinput", "true");//hard-coded a specific image size (large)
		props.put("croplast", "true");//hard-coded a specific image size (large)
		
		if(Boolean.parseBoolean(inTask.get("force")))
		{
			props.put("isforced","true");
		}
	}

	ConvertInstructions inStructions = manager.createInstructions(inAsset,inPreset,props);
	
	//inStructions.setOutputExtension(inPreset.get("extension"));
	//log.info( inStructions.getProperty("guid") );
	//inStructions.setAssetSourcePath(asset.getSourcePath());
//		String extension = PathUtilities.extractPageType(inPreset.get("outputfile") );
//		inStructions.setOutputExtension(extension);

	//new submitted retry missinginput
	if("new".equals(status) || "submitted".equals(status) || "retry".equals(status)  || "missinginput".equals(status))
	{
		//String outputpage = "/WEB-INF/data/${inArchive.catalogId}/generated/${asset.sourcepath}/${inPreset.outputfile}";
//			String outputpage = creator.populateOutputPath(inArchive, inStructions, inPreset);
//			Page output = inArchive.getPageManager().getPage(outputpage);
//			log.debug("Running Media type: ${type} on asset ${inAsset.getSourcePath()}" );
		tmpresult = manager.createOutput(inStructions);
	}
	else if("submitted".equals(status))
	{
		tmpresult = manager.updateStatus(inTask,inStructions);
	}
	else
	{
		tmpresult = new ConvertResult();
		tmpresult.setOk(false);
		tmpresult.setError(inTask.getId() + " task id with status:" + status + " Should have been: submitted, new, missinginput or retry, is index out of date? ");
	}
	
	return tmpresult;
  }
}
