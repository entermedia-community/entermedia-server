package conversions;


import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.convert.ConversionManager
import org.entermediadb.asset.convert.ConvertInstructions
import org.entermediadb.asset.convert.ConvertResult
import org.entermediadb.scripts.ScriptLogger
import org.openedit.Data
import org.openedit.ModuleManager
import org.openedit.OpenEditException
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery
import org.openedit.locks.Lock
import org.openedit.users.User
import org.openedit.util.DateStorageUtil
import org.openedit.util.ExecutorManager

//class Finisher implements Runnable 
//{
//	MediaArchive fieldMediaArchive;
//	public Finisher(MediaArchive inArchive)
//	{
//		fieldMediaArchive = inArchive;
//	} 
//	public void run()
//	{
//		fieldMediaArchive.fireSharedMediaEvent("conversions/conversionscomplete");
//		//fieldMediaArchive.fireSharedMediaEvent("conversions/runconversions");
//	}
//}


class CompositeConvertRunner implements Runnable
{
	String fieldAssetId;
	MediaArchive fieldMediaArchive;
	List runners = new ArrayList();
	User user;
	ScriptLogger log;
	boolean fieldCompleted;
	
	public boolean hasComplete()
	{
		return fieldCompleted;
	}
	public CompositeConvertRunner(MediaArchive archive,String inAssetId)
	{
		fieldMediaArchive = archive;
		fieldAssetId = inAssetId;
	}
	
	public void run()
	{
		Lock lock = fieldMediaArchive.getLockManager().lockIfPossible("assetconversions/" + fieldAssetId, "CompositeConvertRunner.run");
		
		if( lock == null)
		{
			log.info("asset already being processed ${fieldAssetId}");
			return;
		}
		Asset asset = fieldMediaArchive.getAsset(fieldAssetId);
		try
		{
			for( ConvertRunner runner: runners )
			{
				runner.asset = asset;
				runner.run();
				if( runner.isComplete())
				{
					fieldCompleted = true;
					//Slow? fieldMediaArchive.fireSharedMediaEvent("conversions/conversioncomplete");
				}
				if( runner.isError())
				{
					fieldCompleted = true;
					//fieldMediaArchive.fireSharedMediaEvent("conversions/conversioncomplete");
					break;
				}
			}
		}
		catch(Exception e){
			log.error("ERRORS ${fieldAssetId}");
		}
		finally
		{
			fieldMediaArchive.releaseLock(lock);
			
			if( hasComplete())
			{
				fieldMediaArchive.conversionCompleted(asset);
				fieldMediaArchive.fireSharedMediaEvent("conversions/conversioncomplete");
			}
		}
	}

	
	public void add(Runnable runner )
	{
		runners.add(runner);
	}
}

class ConvertRunner implements Runnable
{
	MediaArchive mediaarchive;
	Searcher tasksearcher;
	Searcher presetsearcher;
	Searcher itemsearcher;
	Data hit;
	ScriptLogger log;
	User user;
	Asset asset;
	ModuleManager moduleManager;
	ConvertResult result = null;
	
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
	public void run()
	{
		try
		{
			convert();
		}
		catch (Throwable ex )
		{
			log.error(ex);
		}
	}
	public void convert()
	{
		Data realtask = tasksearcher.loadData(hit);
		//log.info("should be ${hit.status} but was ${realtask.status}");
		if( asset == null)
		{
			asset = mediaarchive.getAsset(hit.get("assetid"));
		}
		if (realtask != null)
		{
			String presetid = hit.get("presetid");
			//log.debug("starting preset ${presetid}");
			Data preset = presetsearcher.searchById(presetid);
			Date started = new Date();
			
			if(preset != null)
			{
				try
				{
					if(asset == null)
					{
						throw new OpenEditException("Asset could not be loaded ${realtask.getSourcePath()} marking as error");
					}
					result = doConversion(mediaarchive, realtask, preset,asset);
				}
				catch(Throwable e)
				{
					result = new ConvertResult();
					result.setOk(false);
					result.setError(e.toString());
					log.error("Conversion Failed",e);
				}
				
				if(result != null)
				{
					realtask.setValue("submitteddate", started);
					if(result.isOk())
					{
						if(result.isComplete())
						{
							realtask.setProperty("status", "complete");
							String itemid = realtask.get("itemid")
							if(itemid != null)
							{
								//The item should have a pointer to the conversion, not the other way around
								Data item = itemsearcher.searchById(itemid);
								item.setProperty("status", "converted");
								itemsearcher.saveData(item, null);
							}
							realtask.setProperty("externalid", result.get("externalid"));
							realtask.setValue("completed",new Date());
							realtask.setProperty("errordetails","");
							tasksearcher.saveData(realtask, user);
							//log.info("Marked " + hit.getSourcePath() +  " complete");
							
							mediaarchive.fireMediaEvent("conversions","conversioncomplete",user,asset);
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
						realtask.setProperty('status', 'error');
						realtask.setProperty("errordetails", result.getError() );
						String completed = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
						realtask.setProperty("completed",completed);
						tasksearcher.saveData(realtask, user);
						
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
							realtask.setProperty('status', 'error');
							realtask.setProperty("errordetails", "Missing input expired" );
						}
						else
						{
							log.debug("conversion had no error and will try again later for ${assetid}");
							realtask.setProperty('status', 'missinginput');
						}	
						tasksearcher.saveData(realtask, user);
					}
				}
			}
			else
			{
				log.info("Can't run conversion for task '${realtask.getId()}': Invalid presetid ${presetid}");
			}
		}
		else
		{
			log.info("Can't find task object with id '${hit.getId()}'. Index missing data?")
		}
	}

protected ConvertResult doConversion(MediaArchive inArchive, Data inTask, Data inPreset, Asset inAsset)
{
	String status = inTask.get("status");
	
	String type = inPreset.get("transcoderid"); //rhozet, ffmpeg, etc
	ConversionManager manager = inArchive.getTranscodeTools().getManagerByFileFormat(inAsset.getFileFormat());
	//log.debug("Converting with type: ${type} using ${creator.class} with status: ${status}");
	
	if (manager != null)
	{
		Map props = new HashMap();
		
		String guid = inPreset.get("guid");
		if( guid != null)
		{
			Searcher presetdatasearcher = inArchive.getSearcherManager().getSearcher(inArchive.getCatalogId(), "presetdata" );
			Data presetdata = presetdatasearcher.searchById(guid);
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
		String externalid = inTask.get("externalid");
		if( externalid != null)
		{
			props.put("externalid",externalid); //TIGO
		}
		
		ConvertInstructions inStructions = manager.createInstructions(inAsset,inPreset,props);
		inStructions.setConversionTask(inTask);
		
		//inStructions.setOutputExtension(inPreset.get("extension"));
		//log.info( inStructions.getProperty("guid") );
		if( inAsset.get("editstatus") == "7") 
		{
			throw new OpenEditException("Could not run conversions on deleted asset ${inAsset.getSourcePath()}");
		}
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
			result = manager.createOutput(inStructions);
		}
		else if("submitted".equals(status))
		{
			result = manager.updateStatus(inTask,inStructions);
		}
		else
		{
			log.info("${inTask.getId()} task id with ${status} status not submitted, new, missinginput or retry, is index out of date? ");
		}
	}
	else
	{
		log.info("Can't find media creator for type '${inAsset.getFileFormat()}'");
	}
	return result;
  }
}
protected ConvertRunner createRunnable(MediaArchive mediaarchive, Searcher tasksearcher, Searcher presetsearcher, Searcher itemsearcher, Data hit)
{
	   ConvertRunner runner = new ConvertRunner();
	   runner.mediaarchive = mediaarchive;
	   runner.tasksearcher = tasksearcher;
	   runner.presetsearcher = presetsearcher;
	   runner.itemsearcher = itemsearcher;
	   runner.hit = hit;
	   runner.log = log;
	   runner.user = user; //if you get errors here make sure they did not delete the admin user
	   runner.moduleManager= moduleManager;
	   return runner;
}
   
		
public void checkforTasks()
{
	mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");//Search for all files looking for videos
	
	Searcher tasksearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "conversiontask");
	Searcher itemsearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "orderitem");
	Searcher presetsearcher = mediaarchive.getSearcherManager().getSearcher (mediaarchive.getCatalogId(), "convertpreset");
	
	
	SearchQuery query = tasksearcher.createSearchQuery();
	query.addOrsGroup("status", "new submitted retry missinginput");
	query.addSortBy("assetidDown");
	query.addSortBy("ordering");
	
	String assetids = context.getRequestParameter("assetids");
	if(assetids != null)
	{
		assetids = assetids.replace(","," ");
		query.addOrsGroup( "assetid", assetids );
	}
	else
	{	
		String assetid = context.getRequestParameter("assetid");
		if(assetid != null)
		{
			query.addMatches("assetid", assetid);
		}
	}
	context.setRequestParameter("assetid", (String)null); //so we clear it out for next time. needed?
	HitTracker newtasks = tasksearcher.search(query);
	newtasks.enableBulkOperations();
	newtasks.setHitsPerPage(25); //We want to make sure scroll does not expire 
	//newtasks.setHitsPerPage(20000);  //This is a problem. Since the data is being edited while we change pages we skip every other page. Only do one page at a time
	if( newtasks.size() > 0)
	{
		log.info("processing ${newtasks.size()} new submitted retry missinginput conversions");
	}
	else
	{
		return;
	}
	List runners = new ArrayList();

	ExecutorManager executorQueue = getQueue(mediaarchive.getCatalogId());
	
	CompositeConvertRunner byassetid = null;
//		CompositeConvertRunner lastcomposite = null;
//		boolean runexec = false;
	String lastassetid = null;
	Iterator iter = newtasks.iterator();
	String sourcepath = null;
	long count = 0;
	for(Data hit:  iter)
	{
		ConvertRunner runner = createRunnable(mediaarchive,tasksearcher,presetsearcher, itemsearcher, hit );
		
		String id = hit.get("assetid"); //Since each converter locks the asset we want to group these into one sublist
		if( id == null )
		{
			log.info("No assetid set");
			Data missingdata = tasksearcher.loadData(hit)
			missingdata.setProperty("status", "error");
			missingdata.setProperty("errordetails", "asset id is null");
			tasksearcher.saveData(missingdata, null);
			continue;
		}
		if( id != lastassetid )
		{
			if( runners.size() > 100)
			{
				executorQueue.execute("conversions",runners);
				runners.clear();
				//log.info("Clearing " + id);
			}
			//lastcomposite = byassetid;
			byassetid = new CompositeConvertRunner(mediaarchive,hit.assetid );
			byassetid.log = log;
			byassetid.user = user;
			lastassetid = id;
			runners.add(byassetid);
			
		}
		byassetid.add(runner);
		count++;
		
	}
	executorQueue.execute("conversions",runners);
	log.debug("Queued up ${count} of ${newtasks.size()} conversion tasks for processing");
	
}

public ExecutorManager getQueue(String inCatalogId)
{
	ExecutorManager queue =  (ExecutorManager)moduleManager.getBean(inCatalogId,"executorManager");
	return queue;
}

checkforTasks();

