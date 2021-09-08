package org.entermediadb.asset.facedetect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.cluster.IdManager;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.TranscodeTools;
import org.entermediadb.video.Block;
import org.entermediadb.video.Timeline;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;
import org.openedit.util.RunningProcess;


public class FaceDetectManager 
{
	private static final Log log = LogFactory.getLog(FaceDetectManager.class);

	protected Exec fieldExec;

	public Exec getExec() 
	{
		return fieldExec;
	}

	public void setExec(Exec fieldExec) 
	{
		this.fieldExec = fieldExec;
	}
	RunningProcess	 fieldRunningProfileProcess;
	RunningProcess	 fieldRunningCompareProcess;
	
	public RunningProcess getRunningCompareProcess()
	{
		if (fieldRunningCompareProcess == null)
		{
			fieldRunningCompareProcess = new RunningProcess();
			fieldRunningCompareProcess.setExecutorManager(getExec().getExecutorManager());
			try
			{
				fieldRunningCompareProcess.start("facecompare");
			}
			catch ( OpenEditException ex)
			{
				fieldRunningCompareProcess = null;
				throw ex;
			}
		}
		return fieldRunningCompareProcess;
	}

	public RunningProcess getRunningProfileProcess()
	{
		if (fieldRunningProfileProcess == null)
		{
			fieldRunningProfileProcess = new RunningProcess();
			fieldRunningProfileProcess.setExecutorManager(getExec().getExecutorManager());
			//fieldRunningProfileProcess.start("faceprofile", Collections.EMPTY_LIST);
			try
			{
				fieldRunningProfileProcess.start("faceprofile");
			}
			catch ( OpenEditException ex)
			{
				fieldRunningProfileProcess = null;
				throw ex;
			}
		}
		return fieldRunningProfileProcess;
	}
	public boolean extractFaces(MediaArchive inArchive, Asset inAsset)
	{
		try
		{
			String type = inArchive.getMediaRenderType(inAsset);
				
			if (!"image".equalsIgnoreCase(type) && !"video".equalsIgnoreCase(type)  )
			{
				inAsset.setValue("facescancomplete","true");
				return true;
			}
		
			//If its a video then generate all the images and scan them
			
			List<Map> profilemap = null;
			if( "image".equalsIgnoreCase(type) )
			{
				ContentItem input = inArchive.getContent("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image1500x1500.jpg");
				if( !input.exists() )
				{
					//probblem
					log.debug("No thumbnail " + inAsset.getSourcePath());
					inAsset.setValue("facescancomplete","true");
					return true;
				}
				profilemap = findFaceData(input);	
			}
			else if( "video".equalsIgnoreCase(type) )
			{
				//Look over them and save the timecode with it
				Double videolength = (Double)inAsset.getDouble("length");
				if( videolength == null)
				{
					inAsset.setValue("facescancomplete","true");
					return true;
				}
				Timeline timeline = new Timeline();
				long mili = Math.round( videolength*1000d );
				timeline.setLength(mili);
				timeline.setPxWidth(1200); //This divides in 10 or 20
				Collection<Block> ticks = timeline.getTicks();
				profilemap = new ArrayList<Map>();
				
				List<Map> continuelooking = null;
				
				for (Iterator iterator = ticks.iterator(); iterator.hasNext();)
				{
					Block block = (Block) iterator.next();
					if( block.getSeconds() >= videolength)
					{
						break;
					}
					//ContentItem input = inArchive.getContent("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image1500x1500.jpg");
					//Convert quickly
					ContentItem item = generateInputFile(inArchive,inAsset,block);
					if( !item.exists() )
					{
						//probblem
						log.debug("No thumbnail " + inAsset.getSourcePath());
					}
					else
					{
						List<Map> more = findFaceData(item);
						
						for (Iterator iterator2 = more.iterator(); iterator2.hasNext();)
						{
							Map map = (Map) iterator2.next();
							map.put("timecodestart",block.getStartOffset());
						}
						//If its the same person we can consolidate. 
						//Compare all of them for matches to the previous frame
						//and if match then just add an end time to the face and remove a profile  
						//This allows us show bars on the top of the timeline tool for faces
						//and speeds up the processing when comparing lots of faces to a big database

						if( continuelooking == null)
						{
							continuelooking = more; //First set
						}
						else 
						{
							updateEndTimes(continuelooking,block.getStartOffset()); //Brings them up to date
							continuelooking = combineVideoMatches(continuelooking,more);  //WARNING: This will update times on some, and remove from others
						}
						if( !more.isEmpty() )
						{
							profilemap.addAll(more);
						}
					}
				}
				updateEndTimes(continuelooking,mili); //Closes out the last ones

			}
			inAsset.setValue("faceprofiles",profilemap);
			inAsset.setValue("facescancomplete","true");
			inAsset.setValue("facematchcomplete","false");
			if( !profilemap.isEmpty())
			{
				inAsset.setValue("facehasprofile",true);
			}
			//Parse the JSON and break it down to profiles in the asset
			
		
			return true;
		}
		catch( Throwable ex)
		{
			throw new OpenEditException("Error on: " + inAsset.getSourcePath(),ex);
		}
		
	}

	protected void updateEndTimes(List<Map> pendingprofiles, long cutofftime)
	{
		for (Iterator iterator = pendingprofiles.iterator(); iterator.hasNext();)
		{
			Map runningprofile = (Map) iterator.next(); //Put ends times because they got this far
			
			long length = cutofftime-(long)runningprofile.get("timecodestart");
			runningprofile.put("timecodelength",length);
		}

	}
	
	private ContentItem generateInputFile(MediaArchive inArchive, Asset inAsset, Block inBlock)
	{
		ConversionManager manager = inArchive.getTranscodeTools().getManagerByRenderType("video");
		ConvertInstructions instructions = manager.createInstructions(inAsset, "image1900x1080.jpg");
		ContentItem item = manager.findInput(instructions);
		//needed?
		//		if (item == null) {
		//				item = archive.getOriginalContent(inAsset);
		//		}
		instructions.setInputFile(item);
		instructions.setProperty("timeoffset", String.valueOf(inBlock.getSeconds()));  
		//instructions.setProperty("duration", "58");
		//instructions.setProperty("compressionlevel", "12");
		//instructions.setOutputFile(tempfile); //Needed?

		ConvertResult result = manager.createOutput(instructions); //skips if it already there
		
		return result.getOutput();
	}

	protected List<Map> findFaceData(ContentItem input) throws ParseException
	{
		long start = System.currentTimeMillis();
		//getRunningProfileProcess().start("faceprofile"); //FOR TESTING NLY
		getRunningProfileProcess();
		String jsonresults = getRunningProfileProcess().runExecStream(input.getAbsolutePath(),60000);
		long end = System.currentTimeMillis();
		double total = (end - start) / 1000.0;
		log.info("faceprofile Done in:"+total);
		
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject)parser.parse(jsonresults);
		
		List<Map> profilemap = new ArrayList<Map>();
		
		JSONArray array = (JSONArray)json.get("profile");
		if( array != null)
		{
			log.info("Found profile! " + input.getPath());
			for (Iterator iterator = array.iterator(); iterator.hasNext();)
			{
				JSONArray profilejson = (JSONArray) iterator.next();
				//Array of numbers?
				Map profile = new HashMap();
				profile.put("facedata",profilejson.toJSONString());
				//TODO: Look for faces now??? No wait..  
				profilemap.add(profile);
			}
		}
		return profilemap;
	}

	public boolean matchFaces(MediaArchive inArchive, Asset inAsset)
	{
		try
		{
			String type = inArchive.getMediaRenderType(inAsset);
				
			if (!"image".equalsIgnoreCase(type) && !"video".equalsIgnoreCase(type))
			{
				log.info("not an image or video");
				return false;
			}
			boolean foundmatch = false;
			List<ValuesMap> pictures = createListMap((Collection)inAsset.getValue("faceprofiles"));
			//Search all the other assets minus myself
			HitTracker 	hits = inArchive.query("asset").exact("facehasprofile",true).not("id",inAsset.getId()).search();
			hits.enableBulkOperations();
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				Data otherasset = (Data) iterator.next();
				
				List<ValuesMap> otherprofiles = createListMap((Collection)otherasset.getValue("faceprofiles"));
				if( otherprofiles == null)
				{
					//Should never happen
					log.error("face missing");
					continue;
				}
				boolean didfoundmatch = checkHit(inArchive, inAsset, otherasset, pictures, otherprofiles);
				if( didfoundmatch)
				{
					foundmatch = didfoundmatch;
				}
			}
			inAsset.setValue("facematchcomplete",true);
			inArchive.saveData("asset",inAsset);
			return foundmatch;
		}
		catch( Throwable ex)
		{
			throw new OpenEditException("Error on: " + inAsset.getSourcePath(),ex);
		}
	}

	
	public void combineGroups()
	{
		//TODO: Go over all the groups and see if any two have common asset profiles. Then put them into one of the groups. If the remaining has no more hits delete it	
	}
	
	protected boolean checkHit(MediaArchive inArchive, Asset inAsset, Data otherasset, List<ValuesMap> thisassetprofiles, List<ValuesMap> otherprofiles) throws ParseException
	{
		Searcher groupsearcher =  inArchive.getSearcher("faceprofilegroup");

		boolean foundmatch = false;

		//--
		StringBuffer profilebuffer = new StringBuffer();
		profilebuffer.append("\"profile\":[");
		for (Iterator iterator2 = otherprofiles.iterator(); iterator2.hasNext();)
		{
			Map map = (Map) iterator2.next();
			String fjsondata = (String)map.get("facedata");
			
			profilebuffer.append(fjsondata);
			if( iterator2.hasNext())
			{
				profilebuffer.append(",");
			}
		}
		profilebuffer.append("]");
		String profilesjson = profilebuffer.toString();
		
		for (Iterator iterator0 = thisassetprofiles.iterator(); iterator0.hasNext();)
		{
			StringBuffer picturetocompare = new StringBuffer();
			picturetocompare.append("\"picture\":[");
			ValuesMap onepicture = (ValuesMap) iterator0.next();
			String jsondata = (String)onepicture.get("facedata");
			picturetocompare.append(jsondata);
			picturetocompare.append("]");
		
			//Low level comparison using a running process
			String finaljson = "{" + picturetocompare.toString() + " , " + profilesjson + "}";
			
			String jsonresults = getRunningCompareProcess().runExecStream(finaljson + "\n",60000); //EXTRA new line To account for new lines inside json
			if( jsonresults == null)
			{
				//TODO: Find out why the first ones always fail
				jsonresults = getRunningCompareProcess().runExecStream(finaljson + "\n",60000); //EXTRA new line To account for new lines inside json
				if( jsonresults == null)
				{				
					//throw new OpenEditException("Match should not be null for " + finaljson);
					return false;
				}
			}
			//This is a JSON of a grid of grids
			JSONParser parser = new JSONParser();
			JSONObject result = (JSONObject)parser.parse(jsonresults);
			JSONArray rowsofresults = (JSONArray)result.get("matches");
			JSONArray rowsofresultsdone = (JSONArray)rowsofresults.iterator().next();
			
			for (int i = 0; i < rowsofresultsdone.size(); i++)
			{
				long istrue = (long) rowsofresultsdone.get(i); //Only this array level had a match
				if( istrue == 1)
				{
					//We have a match!!
					foundmatch = true;
					ValuesMap otherprofile = (ValuesMap)otherprofiles.get(i);
					
					//See if either profile already has a group. If so then use that group
					String groupid = (String)onepicture.get("faceprofilegroup");
					if( groupid == null)
					{
						groupid = (String)otherprofile.get("faceprofilegroup"); 
					}
					//New group in between both pictures. Create a new group
					if( groupid == null)
					{
						//Create one
						Data group = groupsearcher.createNewData();
						
						//Make sure we use an image that has fewer profiles in it. Fewer is better
						if(otherprofiles.size() > thisassetprofiles.size() )
						{
							group.setValue("collectionimage", inAsset.getId()); //Use the picure that has less profiles
						}
						else
						{
							group.setValue("collectionimage", otherasset.getId());
						}
						
						group.setValue("creationdate", new Date());
						group.setValue("automatictagging", true);
						
						groupsearcher.saveData(group);
						groupid = group.getId();
					}
					if( !onepicture.containsInValues("faceprofilegroup",groupid) )
					{
						Collection pgroups = onepicture.addValue("faceprofilegroup", groupid);
						onepicture.put("faceprofilegroup",onepicture.toString(pgroups));  //Is this line really needed?
						inAsset.setValue("faceprofiles",thisassetprofiles); //Save the group id
						//save asset at the end?
						inArchive.saveData("asset",inAsset);
					}	
					if( !otherprofile.containsInValues("faceprofilegroup",groupid) )
					{
						Collection pgroups = otherprofile.addValue("faceprofilegroup", groupid);
						otherprofile.put("faceprofilegroup",otherprofile.toString(pgroups));  //Is this line really needed?
						Asset tosave = (Asset)inArchive.getAssetSearcher().loadData(otherasset);
						tosave.setValue("faceprofiles",otherprofiles);
						//save data
						inArchive.saveData("asset",tosave);
					}	
				}
			}
		}
		return foundmatch;
	}

	private List<ValuesMap> createListMap(Collection inValues)
	{
		ArrayList copy = new ArrayList();
		if (inValues != null) 
		{
			for (Iterator iterator = inValues.iterator(); iterator.hasNext();)
			{
				Map map = (Map) iterator.next();
				copy.add(new ValuesMap(map));
			}
		}
		return copy;
	}
	
	public HitTracker searchForAssets(MediaArchive inArchive, String faceprofilegroupid)
	{
		HitTracker tracker = inArchive.query("asset").exact("faceprofiles.faceprofilegroup", faceprofilegroupid).search();
		return tracker;
	}
	
	

	public List<Map> combineVideoMatches(List<Map> firstprofiles, List<Map> secondprofiles)
	{
		List<Map> tocontinuelooking = new ArrayList();

		//Assume we want to continue with all of em
		tocontinuelooking.addAll(secondprofiles);

		Collection<Map> toremove = new ArrayList();

		//First just compare the two. Sometimes they are exactly the same profile. Just remove them from second list
		for (Iterator iterator2 = firstprofiles.iterator(); iterator2.hasNext();)
		{
			Map firstprofile = (Map) iterator2.next();
			String fjsondata = (String)firstprofile.get("facedata");
			for (Iterator iterator = secondprofiles.iterator(); iterator.hasNext();)
			{
				Map secondmap = (Map) iterator.next();
				String seconddata = (String)secondmap.get("facedata");
				if( fjsondata.equals(seconddata))
				{
					secondprofiles.remove(secondmap);
					tocontinuelooking.remove(secondmap);
					tocontinuelooking.add(firstprofile);
					break;
				}
			}
		}
		if( secondprofiles.isEmpty() )
		{
			return tocontinuelooking;
		}
		
		StringBuffer profilebuffer = new StringBuffer();
		profilebuffer.append("\"profile\":[");
		for (Iterator iterator2 = secondprofiles.iterator(); iterator2.hasNext();)
		{
			Map map = (Map) iterator2.next();
			String fjsondata = (String)map.get("facedata");
			
			profilebuffer.append(fjsondata);
			if( iterator2.hasNext())
			{
				profilebuffer.append(",");
			}
		}
		profilebuffer.append("]");
		String profilesjson = profilebuffer.toString();  //Check each profile in the main asset to the group in the second
				
		for (Iterator iterator0 = firstprofiles.iterator(); iterator0.hasNext();)
		{
			StringBuffer picturetocompare = new StringBuffer();
			picturetocompare.append("\"picture\":[");
			Map onepicture = (Map) iterator0.next();
			String jsondata = (String)onepicture.get("facedata");
			picturetocompare.append(jsondata);
			picturetocompare.append("]");
		
			//Low level comparison using a running process
			String finaljson = "{" + picturetocompare.toString() + " , " + profilesjson + "}";
			
			String jsonresults = getRunningCompareProcess().runExecStream(finaljson + "\n",60000); //EXTRA new line To account for new lines inside json
			if( jsonresults == null)
			{
				//TODO: Find out why the first ones always fail
				jsonresults = getRunningCompareProcess().runExecStream(finaljson + "\n",60000); //EXTRA new line To account for new lines inside json
				if( jsonresults == null)
				{				
					//throw new OpenEditException("Match should not be null for " + finaljson);
					log.error("Match should not be null for " + finaljson);
					return null;
				}
			}
			//This is a JSON of a grid of grids
			JSONParser parser = new JSONParser();
			JSONObject result = null;
			try
			{
				result = (JSONObject)parser.parse(jsonresults);
			}
			catch (ParseException e)
			{
				log.error("Parese problem",e);
				continue;
			}
			JSONArray rowsofresults = (JSONArray)result.get("matches");
			JSONArray rowsofresultsdone = (JSONArray)rowsofresults.iterator().next();
			
			for (int i = 0; i < rowsofresultsdone.size(); i++)
			{
				long istrue = (long) rowsofresultsdone.get(i); //Only this array level had a match
				//We have a match!!
				Map otherprofile = (Map)secondprofiles.get(i);
				if( istrue == 1)
				{
					//Remove this one and give the time to the one before it onepicture
					//Long start = (Long)otherprofile.get("facedatastarttime");
					//onepicture.put("facedataendtime",start);
					toremove.add(otherprofile);
					tocontinuelooking.remove(otherprofile);
					tocontinuelooking.add(onepicture); //put this one back
				}
			}
		}
		for (Iterator iterator = toremove.iterator(); iterator.hasNext();)
		{
			Map redundantprofile = (Map) iterator.next();
			secondprofiles.remove(redundantprofile);
		}
		return tocontinuelooking;
	}
	
	
	
}
