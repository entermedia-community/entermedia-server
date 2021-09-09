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
							try
							{
								continuelooking = combineVideoMatches(continuelooking,more);
							}
							catch(IndexOutOfBoundsException ex)
							{
								log.info("Issue happened again on " + block.getSeconds());
								//ignoring...
							}
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
				//TODO: Add coordinates of where the face is on the image
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
			
			Collection faces = (Collection)inAsset.getValue("faceprofiles");
			if( faces == null || faces.isEmpty())
			{
				return false;
			}
			List<ValuesMap> pictures = createListMap(faces);
			
			//Search all the other assets minus myself
			HitTracker 	hits = inArchive.query("asset").exact("facehasprofile",true).not("id",inAsset.getId()).search();
			hits.enableBulkOperations();
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				Data otherasset = (Data) iterator.next();
				
				List<ValuesMap> otherprofiles = createListMap((Collection)otherasset.getValue("faceprofiles"));
				if( otherprofiles == null || otherprofiles.isEmpty())
				{
					//Should never happen
					log.info("no faces, continue");
					continue;
				}
				boolean didfoundmatch = checkHit(inArchive, inAsset, otherasset, pictures, otherprofiles);
				
				if( "video".equals( type ) )
				{
					createGroupsWithinVideo(inArchive,inAsset);
				}
				//TODO: Finally add groups for all leftover people
				createGroupsForAllProfiles(inArchive,inAsset);
				
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

	
	protected void createGroupsForAllProfiles(MediaArchive inArchive, Asset inAsset)
	{
		Collection faces = (Collection)inAsset.getValue("faceprofiles");
		for (Iterator iterator = faces.iterator(); iterator.hasNext();)
		{
			Map profile = (Map) iterator.next();
			if( profile.get("faceprofilegroup") == null)
			{
				//Create one
				Data group = inArchive.getSearcher("faceprofilegroup").createNewData();
				group.setValue("collectionimage", inAsset.getId()); //Use the picure that has less profiles
				group.setValue("creationdate", new Date());
				group.setValue("automatictagging", true);
				inArchive.getSearcher("faceprofilegroup").saveData(group);
				profile.put("faceprofilegroup",group.getId());  //Is this line really needed?
			}
		}
	}

	
	protected boolean checkHit(MediaArchive inArchive, Asset inAsset, Data otherasset, List<ValuesMap> thisassetprofiles, List<ValuesMap> otherprofiles) throws ParseException
	{
		boolean foundmatch = false;

		StringBuffer jsonoutput = new StringBuffer();
		jsonoutput.append("{\"picture\":[");
		for (Iterator iterator0 = thisassetprofiles.iterator(); iterator0.hasNext();)
		{
			ValuesMap onepicture = (ValuesMap) iterator0.next();
			String jsondata = (String)onepicture.get("facedata");
			jsonoutput.append(jsondata);
			if( iterator0.hasNext())
			{
				jsonoutput.append(",");
			}
		}
		jsonoutput.append("],");
		jsonoutput.append("\"profile\":[");
		for (Iterator iterator2 = otherprofiles.iterator(); iterator2.hasNext();)
		{
			Map map = (Map) iterator2.next();
			String fjsondata = (String)map.get("facedata");
			
			jsonoutput.append(fjsondata);
			if( iterator2.hasNext())
			{
				jsonoutput.append(",");
			}
		}
		jsonoutput.append("]}");
		
		JSONArray rowsofresults = runCompare(jsonoutput);
		if( rowsofresults == null)
		{
			return foundmatch;
		}
		
		int index = 0;
		for (Iterator iterator = rowsofresults.iterator(); iterator.hasNext();)
		{
			JSONArray rowsofresultsdone = (JSONArray)iterator.next();
			/**
			 * Can you refresh my memory on face matching? If I pass in 3 rows of profiles and try and match it with 4 rows how do I know what matches what? I guess would like it to return an list of 3 arrays for each with 4 cells in each array.  like [0,0,0,1][0,0,0,0][0,0,0,0] so this means the first profile matched the fourth image
			 */
			ValuesMap onepicture = (ValuesMap)thisassetprofiles.get(index);
			index++;
			for (int i = 0; i < rowsofresultsdone.size(); i++)
			{
				long istrue = (long) rowsofresultsdone.get(i); //Only this array level had a match
				if( istrue == 1)
				{
					//We have a match!!
					foundmatch = true;
					ValuesMap otherprofile = (ValuesMap)otherprofiles.get(i); //Error here means API not working right
					
					assignFaceGroupId(inArchive, inAsset, otherasset, thisassetprofiles, otherprofiles, onepicture, otherprofile);	
				}
			}
		}
			
		return foundmatch;
	}

	protected JSONArray runCompare(StringBuffer jsonoutput)
	{
		String finaljson = jsonoutput.toString();
//		try
//		{
//			Thread.currentThread().sleep(300);
//		}
//		catch (InterruptedException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		String jsonresults = getRunningCompareProcess().runExecStream(finaljson + "\n",60000); //EXTRA new line To account for new lines inside json

//		try
//		{
//			Thread.currentThread().sleep(300);
//		}
//		catch (InterruptedException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		if( jsonresults == null)
		{
			//TODO: Find out why the first ones always fail
			jsonresults = getRunningCompareProcess().runExecStream(finaljson + "\n",60000); //EXTRA new line To account for new lines inside json
			if( jsonresults == null)
			{				
				//throw new OpenEditException("Match should not be null for " + finaljson);
				return null;
			}
		}
		//This is a JSON of a grid of grids
		JSONParser parser = new JSONParser();
		try
		{
			JSONObject result = (JSONObject)parser.parse(jsonresults);
			JSONArray rowsofresults = (JSONArray)result.get("matches");
			return rowsofresults;
		} catch (ParseException ex)
		{
			log.error("Prblem",ex);
		}
		return null;
	}

	protected void assignFaceGroupId(MediaArchive inArchive,  Asset inAsset, Data otherasset, List<ValuesMap> thisassetprofiles, List<ValuesMap> otherprofiles, ValuesMap onepicture, ValuesMap otherprofile)
	{
		
		//See if either profile already has a group. If so then use that group
		String groupid = (String)onepicture.get("faceprofilegroup");
		if( groupid == null)
		{
			groupid = (String)otherprofile.get("faceprofilegroup"); 
		}
		//New group in between both pictures. Create a new group
		Searcher groupsearcher =  inArchive.getSearcher("faceprofilegroup");
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
		//Make sure they have the same group set
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

	private List<ValuesMap> createListMap(Collection inValues)
	{
		ArrayList copy = new ArrayList(inValues.size());
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
		if( firstprofiles.isEmpty() || secondprofiles.isEmpty() )
		{
			return tocontinuelooking;
		}

		StringBuffer jsonoutput = new StringBuffer();
		jsonoutput.append("{\"picture\":[");
		for (Iterator iterator0 = firstprofiles.iterator(); iterator0.hasNext();)
		{
			Map onepicture = (Map) iterator0.next();
			String jsondata = (String)onepicture.get("facedata");
			jsonoutput.append(jsondata);
			if( iterator0.hasNext())
			{
				jsonoutput.append(",");
			}
		}
		jsonoutput.append("],");
		jsonoutput.append("\"profile\":[");
		for (Iterator iterator2 = secondprofiles.iterator(); iterator2.hasNext();)
		{
			Map map = (Map) iterator2.next();
			String fjsondata = (String)map.get("facedata");
			
			jsonoutput.append(fjsondata);
			if( iterator2.hasNext())
			{
				jsonoutput.append(",");
			}
		}
		jsonoutput.append("]}");
		
		JSONArray rowsofresults = runCompare(jsonoutput);
		if( rowsofresults == null)
		{
			return tocontinuelooking;
		}

		int index = 0;
		for (Iterator iterator = rowsofresults.iterator(); iterator.hasNext();)
		{
			JSONArray rowsofresultsdone = (JSONArray)iterator.next();

			Map onepicture = (Map)firstprofiles.get(index);
			index++;
			for (int i = 0; i < rowsofresultsdone.size(); i++)
			{
				long istrue = (long) rowsofresultsdone.get(i); //Only this array level had a match
				if( istrue == 1)
				{
					Map otherprofile = (Map)secondprofiles.get(i);
					//We have a match!!
					//Remove this one and give the time to the one before it onepicture
					//Long start = (Long)otherprofile.get("facedatastarttime");
					//onepicture.put("facedataendtime",start);
					toremove.add(otherprofile);
					tocontinuelooking.remove(otherprofile);
					tocontinuelooking.add(onepicture); //put this one back
					break;
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

	
	public void createGroupsWithinVideo(MediaArchive inArchive, Asset inAsset)
	{
		List<ValuesMap> allprofiles = createListMap((List)inAsset.getValue("faceprofiles"));
		StringBuffer jsonoutput = new StringBuffer();
		jsonoutput.append("{\"picture\":[");
		for (Iterator iterator0 = allprofiles.iterator(); iterator0.hasNext();)
		{
			Map onepicture = (Map) iterator0.next();
			String jsondata = (String)onepicture.get("facedata");
			jsonoutput.append(jsondata);
			if( iterator0.hasNext())
			{
				jsonoutput.append(",");
			}
		}
		jsonoutput.append("],");
		jsonoutput.append("\"profile\":[");
		for (Iterator iterator2 = allprofiles.iterator(); iterator2.hasNext();)
		{
			Map map = (Map) iterator2.next();
			String fjsondata = (String)map.get("facedata");
			
			jsonoutput.append(fjsondata);
			if( iterator2.hasNext())
			{
				jsonoutput.append(",");
			}
		}
		jsonoutput.append("]}");
		
		JSONArray rowsofresults = runCompare(jsonoutput);
		if( rowsofresults == null)
		{
			return;
		}

		int index = 0;
		for (Iterator iterator = rowsofresults.iterator(); iterator.hasNext();)
		{
			JSONArray rowsofresultsdone = (JSONArray)iterator.next();

			ValuesMap otherprofile = (ValuesMap)allprofiles.get(index);
			index++;
			for (int i = 0; i < rowsofresultsdone.size(); i++)
			{
				
				if( index == i)
				{
					//Same person dont do anythuing
					continue;
				}
				long istrue = (long) rowsofresultsdone.get(i); //Only this array level had a match
				ValuesMap onepicture = (ValuesMap)allprofiles.get(i);
				if( istrue == 1)
				{
					//We have a match!!
					//See if either profile already has a group. If so then use that group
					String groupid = (String)onepicture.get("faceprofilegroup");
					if( groupid == null)
					{
						groupid = (String)otherprofile.get("faceprofilegroup"); 
					}
					//New group in between both pictures. Create a new group
					Searcher groupsearcher =  inArchive.getSearcher("faceprofilegroup");
					if( groupid == null)
					{
						//Create one
						Data group = groupsearcher.createNewData();
						
						//Make sure we use an image that has fewer profiles in it. Fewer is better
						group.setValue("collectionimage", inAsset.getId()); //Use the picure that has less profiles
						group.setValue("creationdate", new Date());
						group.setValue("automatictagging", true);
						
						groupsearcher.saveData(group);
						groupid = group.getId();
						
					}
					
					//Make sure its set on both
					if( !onepicture.containsInValues("faceprofilegroup",groupid) )
					{
						Collection pgroups = onepicture.addValue("faceprofilegroup", groupid);
						onepicture.put("faceprofilegroup",onepicture.toString(pgroups));  //Is this line really needed?
					}	
					if( !otherprofile.containsInValues("faceprofilegroup",groupid) )
					{
						Collection pgroups = otherprofile.addValue("faceprofilegroup", groupid);
						otherprofile.put("faceprofilegroup",otherprofile.toString(pgroups));  //Is this line really needed?
						//save data
					}
					inAsset.setValue("faceprofiles",allprofiles); //Save the group id
					inArchive.saveData("asset",inAsset);

				}
			}
			
		}
	}
	
	
}
