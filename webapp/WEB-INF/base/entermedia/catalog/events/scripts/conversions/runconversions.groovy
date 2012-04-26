package conversions;

import org.openedit.data.Searcher 
import org.openedit.Data 
import org.openedit.entermedia.modules.*;
import org.openedit.entermedia.edit.*;
import com.openedit.page.*;
import org.openedit.entermedia.*;
import org.openedit.data.Searcher;
import com.openedit.hittracker.*;
import org.openedit.entermedia.creator.*;

import com.openedit.users.User;
import com.openedit.util.*;

import org.openedit.xml.*;
import org.openedit.entermedia.episode.*;
import conversions.*;
import java.util.*;

import org.entermedia.locks.Lock;

public void checkforTasks()
{
	mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos
	
	Searcher tasksearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "conversiontask");
	Searcher itemsearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "orderitem");
	Searcher presetsearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "convertpreset");
	
	log.info("checking for new and submitted conversions");
	
	SearchQuery query = tasksearcher.createSearchQuery();
	query.addOrsGroup("status", "new submitted retry");
	query.addSortBy("ordering");
	
	String assetid = context.getRequestParameter("assetid");
	if(assetid != null)
	{
		query.addMatches("assetid", assetid);
	}
	HitTracker newtasks = tasksearcher.search(query);
	List all = new ArrayList(newtasks);
	
	for (Data hit in all)
	{	
		Data realtask = tasksearcher.searchById(hit.getId());
		//log.info("should be ${hit.status} but was ${realtask.status}");
		
		if (realtask != null)
		{
			String presetid = hit.get("presetid");
			log.info("starting preset ${presetid} on ${hit}");
			Data preset = presetsearcher.searchById(presetid);
			if(preset != null)
			{
				ConvertResult result = null;
				try
				{
					String sourcepath = hit.get("sourcepath");
					Lock lock = mediaarchive.lockAssetIfPossible(sourcepath, user);
					if( lock == null)
					{
						log.info("asset already being processed ");
						continue;
					}
					
					try
					{
						result = doConversion(mediaarchive, realtask, preset,sourcepath);
					}
					finally
					{
						mediaarchive.releaseLock(lock);
					}
				}
				catch(Throwable e)
				{
					result = new ConvertResult();
					result.setOk(false);
					result.setError(e.toString());
					log.error("Conversion Failed", e);
				}
				
				if(result != null)
				{
					if(result.isOk())
					{
						if(result.isComplete())
						{
							realtask.setProperty("status", "complete");
							String itemid = realtask.get("itemid")
							if(itemid != null)
							{
								Data item = itemsearcher.searchById(itemid);
								item.setProperty("status", "converted");
								itemsearcher.saveData(item, null);
							}
							realtask.setProperty("externalid", result.get("externalid"));
							Asset asset = mediaarchive.getAssetBySourcePath(hit.get("sourcepath"));
							
							mediaarchive.fireMediaEvent("conversions/conversioncomplete",context.getUser(),asset);
						} 
						else
						{
							realtask.setProperty("status", "submitted");
							realtask.setProperty("externalid", result.get("externalid"));
						}
					} 
					else if ( result.isError() )
					{
						realtask.setProperty('status', 'error');
						realtask.setProperty("errordetails", result.getError() );
						
						//TODO: Remove this one day
						String itemid = realtask.get("itemid")
						if(itemid != null)
						{
							Data item = itemsearcher.searchById(itemid);
							item.setProperty("status", "error");
							item.setProperty("errordetails", result.getError() );
							itemsearcher.saveData(item, null);
						}
						//	conversionfailed  conversiontask assetsourcepath, params[id=102], admin
						Map params = new HashMap();
						params.put("taskid",realtask.getId());
						//String operation, String inMetadataType, String inSourcePath, Map inParams, User inUser)
						mediaarchive.fireMediaEvent("conversions/conversionerror","conversiontask", realtask.getSourcePath(), params, context.getUser());
						
					}
					else
					{
						log.info("not ok but no errors, continue");
						continue;
					}
					//tosave.add( realtask);
					tasksearcher.saveData(realtask, context.getUser());
				}
			}
			else
			{
				log.info("Can't run conversion for task '${realtask.getId()}': Invalid presetid ${presetid}");
			}
		}
		else
		{
			log.info("Can't find task object with id '${hit.getId()}'. Index out of date?")
		}
		//tasksearcher.saveAllData( tosave, context.getUser() );
	}
	context.setRequestParameter("assetid", (String)null); //so we clear it out for next time.
	
}
private ConvertResult doConversion(MediaArchive inArchive, Data inTask, Data inPreset, String inSourcepath)
{
	ConvertResult result = null;
	
	String status = inTask.get("status");
	
	String type = inPreset.get("type"); //rhozet, ffmpeg, etc
	MediaCreator creator = getMediaCreator(inArchive, type);
	log.info("Converting with type: ${type} using ${creator.class} with status: ${status}");
	
	if (creator != null)
	{
		Map props = new HashMap();
		String guid = inPreset.guid;
		if( guid != null)
		{
			Searcher presetdatasearcher = inArchive.getSearcherManager().getSearcher(catalogid, "presetdata" );
			Data presetdata = presetdatasearcher.searchById(guid);
			//copy over the preset properties..
			props.put("guid", guid); //needed?
			if( presetdata != null && presetdata.getProperties() != null)
			{
				props.putAll(presetdata.getProperties());
			}
		}
		ConvertInstructions inStructions = creator.createInstructions(props,inArchive,inPreset.get("extension"),inSourcepath);
		
		//inStructions.setOutputExtension(inPreset.get("extension"));
		//log.info( inStructions.getProperty("guid") );
		Asset asset = inArchive.getAssetBySourcePath(inSourcepath);
		if(asset == null)
		{
			return new ConvertResult();
		}
		inStructions.setAssetSourcePath(asset.getSourcePath());
		String extension = PathUtilities.extractPageType(inPreset.get("outputfile") );
		inStructions.setOutputExtension(extension);

		if("new".equals(status) || "retry".equals(status))
		{
			String outputpage = "/WEB-INF/data/${inArchive.catalogId}/generated/${asset.sourcepath}/${inPreset.outputfile}";
			Page output = inArchive.getPageManager().getPage(outputpage);
			log.info("Running Media type: ${type} on asset ${asset.getSourcePath()}" );
			result = creator.convert(inArchive, asset, output, inStructions);
		}
		else if("submitted".equals(status))
		{
			result = creator.updateStatus(inArchive, inTask, asset, inStructions);
		}
		else
		{
			log.info("${status} status not new or retry, is index out of date? ");
		}
	}
	else
	{
		log.info("Can't find media creator for type '${type}'");
	}
	return result;
}


//TODO: Cache in map
private MediaCreator getMediaCreator(MediaArchive inArchive, String inType)
{
	MediaCreator creator = moduleManager.getBean(inType + "Creator");

/*	GroovyClassLoader loader = engine.getGroovyClassLoader();
	Class groovyClass = loader.loadClass("conversions.creators.${inType}Creator");
	
	MediaCreator creator = (MediaCreator) groovyClass.newInstance();
	
	creator.setPageManager(mediaarchive.getPageManager());
	creator.setExec(mediaarchive.getModuleManager().getBean("exec"));

	//				<ref bean="ffMpegImageCreator" />
	//			<ref bean="exifToolThumbCreator" />
	if( inType == "imagemagick") //TODO:Use Spring
	{
		MediaCreator child = getMediaCreator(inArchive,"ffmpegimage");
		creator.addPreProcessor(child);
		child = getMediaCreator(inArchive,"exiftoolthumb");
		creator.addPreProcessor(child);

	}
	*/
	return creator;
}



checkforTasks();

