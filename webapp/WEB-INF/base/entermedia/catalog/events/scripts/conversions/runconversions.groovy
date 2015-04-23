package conversions;


import java.util.Date;

import model.assets.ConvertQueue

import org.entermedia.locks.Lock
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.*
import org.openedit.entermedia.creator.*
import org.openedit.entermedia.edit.*
import org.openedit.entermedia.modules.*
import org.openedit.util.DateStorageUtil;
import org.openedit.xml.*

import com.openedit.*
import com.openedit.entermedia.scripts.ScriptLogger
import com.openedit.hittracker.*
import com.openedit.page.*
import com.openedit.users.User
import com.openedit.util.*

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
		Lock lock = fieldMediaArchive.getLockManager().lockIfPossible(fieldMediaArchive.getCatalogId(), "assetconversions/" + fieldAssetId, "runconversions61");
		
		if( lock == null)
		{
			log.info("asset already being processed ${fieldAssetId}");
			return;
		}
		try
		{
			Asset asset = fieldMediaArchive.getAsset(fieldAssetId);
			for( ConvertRunner runner: runners )
			{
				runner.asset = asset;
				runner.run();
				if( runner.isComplete())
				{
					fieldCompleted = true;
				}
			}
			
		}
		catch(Exception e){
			log.error("ERRORS ${fieldAssetId}");
		}
		finally
		{
			
			//log.info("updating conversion status on ${fieldSourcePath} - runner size was: " + runners.size());
			fieldMediaArchive.updateAssetConvertStatus(fieldAssetId);
			//log.info("Result on ${fieldSourcePath} was ${result}");
			
			fieldMediaArchive.releaseLock(lock);
			fieldMediaArchive.fireSharedMediaEvent("conversions/conversioncomplete");
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
		if( result != null && result.isComplete() )
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
			log.debug("starting preset ${presetid}");
			Data preset = presetsearcher.searchById(presetid);
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
							String completed = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
							realtask.setProperty("completed",completed);
							tasksearcher.saveData(realtask, user);
							//log.info("Marked " + hit.getSourcePath() +  " complete");
							
							mediaarchive.fireMediaEvent("conversions/conversioncomplete",user,asset);
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
						mediaarchive.fireMediaEvent("conversions/conversionerror","conversiontask", realtask.getId(), user);
					}
					else
					{
						String assetid = realtask.get("assetid");
						Date olddate = DateStorageUtil.getStorageUtil().parseFromStorage(realtask.get("submitted"));
						Calendar cal = new GregorianCalendar();
						cal.add(Calendar.DAY_OF_YEAR,-2);
						if( olddate.before(cal.getTime()))
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
	
	String type = inPreset.get("type"); //rhozet, ffmpeg, etc
	MediaCreator creator = getMediaCreator(inArchive, type);
	log.debug("Converting with type: ${type} using ${creator.class} with status: ${status}");
	
	if (creator != null)
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

		ConvertInstructions inStructions = creator.createInstructions(props,inArchive,inPreset.get("extension"),inAsset.getSourcePath());
		
		//TODO: Copy the task properties into the props so that crop stuff can be handled in the createInstructions
		if(Boolean.parseBoolean(inTask.get("crop")))
		{
//			log.info("HERE!!!");
			inStructions.setCrop(true);
			inStructions.setProperty("x1", inTask.get("x1"));
			inStructions.setProperty("y1", inTask.get("y1"));
			inStructions.setProperty("cropwidth", inTask.get("cropwidth"));
			inStructions.setProperty("cropheight", inTask.get("cropheight"));
			if(inStructions.getProperty("prefwidth") == null){
				inStructions.setProperty("prefwidth", inTask.get("cropwidth"));
			}
			if(inStructions.getProperty("prefheight") == null){
				inStructions.setProperty("prefheight", inTask.get("cropheight"));
			}
			//inStructions.setProperty("useinput", "cropinput");//hard-coded a specific image size (large)
			inStructions.setProperty("useoriginalasinput", "true");//hard-coded a specific image size (large)
			
			inStructions.setProperty("gravity", "default");//hard-coded a specific image size (large)
			inStructions.setProperty("croplast", "true");//hard-coded a specific image size (large)
			
			if(Boolean.parseBoolean(inTask.get("force"))){
				inStructions.setForce(true);
			}
		}
		
		//inStructions.setOutputExtension(inPreset.get("extension"));
		//log.info( inStructions.getProperty("guid") );
		if( inAsset.get("editstatus") == "7") 
		{
			throw new OpenEditException("Could not run conversions on deleted asset ${inAsset.getSourcePath()}");
		}
		//inStructions.setAssetSourcePath(asset.getSourcePath());
		String extension = PathUtilities.extractPageType(inPreset.get("outputfile") );
		inStructions.setOutputExtension(extension);

		//new submitted retry missinginput
		if("new".equals(status) || "submitted".equals(status) || "retry".equals(status)  || "missinginput".equals(status))
		{
			//String outputpage = "/WEB-INF/data/${inArchive.catalogId}/generated/${asset.sourcepath}/${inPreset.outputfile}";
			String outputpage = creator.populateOutputPath(inArchive, inStructions, inPreset);
			Page output = inArchive.getPageManager().getPage(outputpage);
			log.debug("Running Media type: ${type} on asset ${inAsset.getSourcePath()}" );
			result = creator.convert(inArchive, inAsset, output, inStructions);
		}
		else if("submitted".equals(status))
		{
			result = creator.updateStatus(inArchive, inTask, inAsset, inStructions);
		}
		else
		{
			log.info("${inTask.getId()} task id with ${status} status not submitted, new, missinginput or retry, is index out of date? ");
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
	return creator;
 }
} //End Runnable methods

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
	newtasks.setHitsPerPage(20000);  //This is a problem. Since the data is being edited while we change pages we skip every other page. Only do one page at a time
	log.info("processing ${newtasks.size()} conversions ${newtasks.getHitsPerPage()} at a time");
	
	List runners = new ArrayList();

	ConvertQueue executorQueue = getQueue();
	
	CompositeConvertRunner byassetid = null;
//		CompositeConvertRunner lastcomposite = null;
//		boolean runexec = false;
	String lastassetid = null;
	Iterator iter = newtasks.iterator();
	String sourcepath = null;
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
				executorQueue.execute(runners);
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
		
	}
	executorQueue.execute(runners);
	if( runners.size() > 0)
	{
		for( CompositeConvertRunner runner: runners )
		{
			if( runner.hasComplete() )
			{
				mediaarchive.fireSharedMediaEvent("conversions/conversionscomplete");
				break;
			}
		}
	}
	log.debug("Added ${newtasks.size()} conversion tasks for processing");
	
}

//Temporary work around for a lack of an interface
public ConvertQueue getQueue(String inCatalogId)
{
	//(ConvertQueue)moduleManager.getBean(mediaarchive.getCatalogId(),"convertQueue");
	ConvertQueue queue = null;
	if( moduleManager.contains( inCatalogId, "convertQueue") )
	{
		queue =  (ConvertQueue)moduleManager.getBean(mediaarchive.getCatalogId(),"convertQueue");
	}
	else
	{
		queue = new ConvertQueue();
		ExecutorManager manager = (ExecutorManager)moduleManager.getBean("executorManager");
		queue.setExecutorManager(manager);
		moduleManager.getCatalogIdBeans().put(inCatalogId + "_" + "convertQueue", queue);
	}
	return queue;
}

checkforTasks();

