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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
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
				
			if (!"image".equalsIgnoreCase(type) )
			{
				return false;
			}
		
			ContentItem input = inArchive.getContent("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image1024x768.jpg");
			if( !input.exists() )
			{
				//probblem
				log.info("No thumbnail " + inAsset.getSourcePath());
				return false;
			}
			long start = System.currentTimeMillis();
			//getRunningProfileProcess().start("faceprofile"); //FOR TESTING NLY
			getRunningProfileProcess();
			String jsonresults = getRunningProfileProcess().runExecStream(input.getAbsolutePath());
			long end = System.currentTimeMillis();
			double total = (end - start) / 1000.0;
			log.info("faceprofile Done in:"+total);
			
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject)parser.parse(jsonresults);
			
			List<Map> profilemap = new ArrayList<Map>();
			
			JSONArray array = (JSONArray)json.get("profile");
			if( array != null)
			{
				log.info("Found profile! " + inAsset.getSourcePath());
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
			inAsset.setValue("faceprofiles",profilemap);
			inAsset.setValue("facescancomplete","true");
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

	public boolean matchFaces(MediaArchive inArchive, Asset inAsset)
	{
		try
		{
			String type = inArchive.getMediaRenderType(inAsset);
				
			if (!"image".equalsIgnoreCase(type) )
			{
				log.info("not an image");
				return false;
			}
			boolean foundmatch = false;
			List<ValuesMap> pictures = createListMap((Collection)inAsset.getValue("faceprofiles"));
			//Search all the other assets minus myself
			HitTracker 	hits = inArchive.query("asset").exact("facehasprofile",true).exact("assettype","photo").not("id",inAsset.getId()).search();
			hits.enableBulkOperations();
			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				
				List<ValuesMap> profiles = createListMap((Collection)hit.getValue("faceprofiles"));
				if( profiles == null)
				{
					//Should never happen
					log.error("face missing");
					continue;
				}
				boolean didfoundmatch = checkHit(inArchive, inAsset, hit, pictures, profiles);
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

	protected boolean checkHit(MediaArchive inArchive, Asset inAsset, Data hit, List<ValuesMap> pictures, List<ValuesMap> profiles) throws ParseException
	{
		Searcher groupsearcher =  inArchive.getSearcher("faceprofilegroup");

		boolean foundmatch = false;

		//--
		StringBuffer profilebuffer = new StringBuffer();
		profilebuffer.append("\"profile\":[");
		for (Iterator iterator2 = profiles.iterator(); iterator2.hasNext();)
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
		
		for (Iterator iterator0 = pictures.iterator(); iterator0.hasNext();)
		{
			StringBuffer picturetocompare = new StringBuffer();
			picturetocompare.append("\"picture\":[");
			ValuesMap onepicture = (ValuesMap) iterator0.next();
			String jsondata = (String)onepicture.get("facedata");
			picturetocompare.append(jsondata);
			picturetocompare.append("]");
		
			String finaljson = "{" + picturetocompare.toString() + " , " + profilesjson + "}";
			
			String jsonresults = getRunningCompareProcess().runExecStream(finaljson + "\n",6000); //EXTRA new line To account for new lines inside json
			if( jsonresults == null)
			{
				throw new OpenEditException("Match should not be null for " + finaljson);
			}
			//This is a JSON of a grid of grids
			JSONParser parser = new JSONParser();
			JSONObject result = (JSONObject)parser.parse(jsonresults);
			JSONArray rowsofresults = (JSONArray)result.get("matches");
			JSONArray rowsofresultsdone = (JSONArray)rowsofresults.iterator().next();
			
			for (int i = 0; i < rowsofresultsdone.size(); i++)
			{
				long istrue = (long) rowsofresultsdone.get(i);
				if( istrue == 1)
				{
					//We have a match!!
					foundmatch = true;
					ValuesMap otherprofile = (ValuesMap)profiles.get(i);
					String groupid = (String)onepicture.get("faceprofilegroup");
					if( groupid == null)
					{
						groupid = (String)otherprofile.get("faceprofilegroup");
					}
					if( groupid == null)
					{
						//Create one
						Data group = groupsearcher.createNewData();
						group.setValue("collectionimage", inAsset.getId());
						group.setValue("creationdate", new Date());
						group.setValue("automatictagging", true);
						
						groupsearcher.saveData(group);
						groupid = group.getId();
					}
					if( !onepicture.containsInValues("faceprofilegroup",groupid) )
					{
						Collection pgroups = onepicture.getValues("faceprofilegroup");
						if( pgroups == null) { pgroups = new ArrayList(); }
						pgroups.add(groupid);
						onepicture.put("faceprofilegroup",onepicture.toString(pgroups));
						inAsset.setValue("faceprofiles",pictures);
						//save asset at the end?
						inArchive.saveData("asset",inAsset);
					}	
					if( !otherprofile.containsInValues("faceprofilegroup",groupid) )
					{
						Collection pgroups = otherprofile.getValues("faceprofilegroup");
						if( pgroups == null) { pgroups = new ArrayList(); }
						pgroups.add(groupid);
						otherprofile.put("faceprofilegroup",otherprofile.toString(pgroups));
						Asset tosave = (Asset)inArchive.getAssetSearcher().loadData(hit);
						tosave.setValue("faceprofiles",profiles);
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
		ArrayList copy = new ArrayList(inValues.size());
		for (Iterator iterator = inValues.iterator(); iterator.hasNext();)
		{
			Map map = (Map) iterator.next();
			copy.add(new ValuesMap(map));
		}
		return copy;
	}
}
