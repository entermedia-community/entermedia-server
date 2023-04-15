package org.entermediadb.asset.facedetect;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.util.MathUtils;
import org.entermediadb.net.HttpSharedConnection;
import org.entermediadb.video.Block;
import org.entermediadb.video.Timeline;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
import org.openedit.repository.ContentItem;


public class FaceProfileManager implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(FaceProfileManager.class);

	protected HttpSharedConnection fieldSharedConnection;
	protected String fieldCatalogId;
	protected String savedapikey = "";
	
	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	protected ModuleManager fieldModuleManager;
	
	public HttpSharedConnection getSharedConnection()
	{
		String api = getMediaArchive().getCatalogSettingValue("faceapikey");
		
		if (fieldSharedConnection == null || !savedapikey.equals(api))
		{
			HttpSharedConnection connection = new HttpSharedConnection();
			connection.addSharedHeader("x-api-key", api);
			fieldSharedConnection = connection;
			savedapikey = api;
		}

		return fieldSharedConnection;
	}

	public void setSharedConnection(HttpSharedConnection inSharedConnection)
	{
		fieldSharedConnection = inSharedConnection;
	}
	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
	}
	public boolean extractFaces(Asset inAsset)
	{
		try
		{
			String type = getMediaArchive().getMediaRenderType(inAsset);
				
			if (!"image".equalsIgnoreCase(type) && !"video".equalsIgnoreCase(type)  )
			{
				inAsset.setValue("facescancomplete","true");
				return true;
			}
			
			String api = getMediaArchive().getCatalogSettingValue("faceapikey");
			if(api == null)
			{
				log.info("faceapikey not set");
				return false;
			}
		
			//If its a video then generate all the images and scan them
			
			List<Map> faceprofiles = null;
			if( "image".equalsIgnoreCase(type) )
			{
				ContentItem input = getMediaArchive().getContent("/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image1500x1500.jpg");
				if( !input.exists() )
				{
					//probblem
					log.debug("No thumbnail " + inAsset.getSourcePath());
					inAsset.setValue("facescancomplete","true");
					return true;
				}
				List<Map> json = findFaces(input);	
				faceprofiles = makeProfilesForEachFace(inAsset,0L,input,json);
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
				faceprofiles = new ArrayList<Map>();
				
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
					ContentItem item = generateInputFile(inAsset,block);
					if (item == null) {
						continue;
					}
					if( !item.exists() )
					{
						//probblem
						log.debug("No thumbnail " + inAsset.getSourcePath());
					}
					else
					{
						List<Map> json = findFaces(item);	
						faceprofiles = makeProfilesForEachFace(inAsset,block.getStartOffset(),item,json);
					}
				}
				
//				updateEndTimes(continuelooking,block.getStartOffset()); //Brings them up to date
//				try
//				{
//					continuelooking = combineVideoMatches(continuelooking,more);
//				}
//				catch(IndexOutOfBoundsException ex)
//				{
//					log.info("Issue happened again on " + block.getSeconds());
//					//ignoring...
//				}

				
			}

			inAsset.setValue("faceprofiles",faceprofiles);  //box  and subjects
			inAsset.setValue("facescancomplete","true");
			inAsset.setValue("facematchcomplete","false");
			if(faceprofiles != null &&  !faceprofiles.isEmpty())
			{
				inAsset.setValue("facehasprofile",true);
			}
			//For each box make sure we have a profile
			getMediaArchive().saveAsset(inAsset);
			
			return true;
		}
		catch( Throwable ex)
		{
			throw new OpenEditException("Error on: " + inAsset.getSourcePath(),ex);
		}
		
	}

	private List<Map> makeProfilesForEachFace(Asset inAsset,long timecodestart, ContentItem inInput, List<Map> inJsonOfFaces)
	{
		/*
		 * <property id="faceprofiles" index="true" keyword="false" stored="true" editable="false" viewtype="faceprofiles" datatype="nested" > 
 <name> 
  <language id="en"><![CDATA[Face Profiles]]></language>  
</name> 
    <property id="timecodestart" index="true" viewtype="timelength" stored="true" datatype="double" searchcomponent="numbersearch"   editable="false" type="double"> 
      <name> 
        <language id="en"><![CDATA[Face Data Time Code]]></language>  
      </name> 
	</property>  
    <property id="timecodelength" index="true" viewtype="timelength" stored="true" datatype="double" searchcomponent="numbersearch"   editable="false" type="double"> 
      <name> 
        <language id="en"><![CDATA[Face Data Length]]></language>  
      </name> 
	</property>  
    <property id="facedata" index="true" indextype="not_analyzed" keyword="true" stored="true" editable="true" >
    <name> 
      <language id="en"><![CDATA[Face Data]]></language>  
    </name> 
    </property>
	<property id="faceprofilegroup" index="true" indextype="not_analyzed" stored="true" viewtype="multiselect" editable="true" datatype="list" type="list"> 
	    <name> 
	      <language id="en"><![CDATA[Face Profile Group]]></language>  
	    </name> 
	</property>  
</property>
		 */
		List<Map> faceprofiles = new ArrayList();
		
		for (Iterator iterator = inJsonOfFaces.iterator(); iterator.hasNext();)
		{
			Map map = (Map) iterator.next();
			Map faceprofile = new HashMap();
			faceprofile.put("timecodestart",timecodestart);
			faceprofile.put("facedata", map);
			//faceprofilegroup
			List subjects = (List)map.get("subjects");
			Map found = null;
			if( subjects != null && !subjects.isEmpty())
			{
				found  = (Map)found.get(0); //TODO: Pick best 
			}
			//Upload as another subject so we have plenty similar ones
//			curl -X POST "http://localhost:8000/api/v1/recognition/faces?subject=<subject>&det_prob_threshold=<det_prob_threshold>" \
//			-H "Content-Type: multipart/form-data" \
//			-H "x-api-key: <service_api_key>" \
			Map tosendparams = new HashMap();
			//tosendparams.put("det_prob_threshold",".6");
			if( found != null)
			{
				String groupid = (String)found.get("subject");
				tosendparams.put("subject",groupid );
				//TODO: Count how many times I have used this group.
				
				faceprofile.put("faceprofilegroup", groupid);
			}
			else
			{
				//Create new profilegroupid
				Data newgroup = getMediaArchive().getSearcher("faceprofilegroup").createNewData();
				newgroup.setValue("primaryimage", inAsset.getId());
				newgroup.setValue("automatictagging", true);
				newgroup.setValue("creationdate", new Date());
				getMediaArchive().saveData("faceprofilegroup", newgroup);
				tosendparams.put("subject",newgroup.getId() );
				faceprofile.put("faceprofilegroup", newgroup.getId() );
			}
			
			//Crop this down
			try {

				ValuesMap box = new ValuesMap((Map)map.get("box"));
				int x = box.getInteger("x_min");
				int y = box.getInteger("y_min");
				int x2 = box.getInteger("x_max");
				int y2 = box.getInteger("y_max");
				int w = x2 - x;
				int h = y2 - y;
				
				faceprofile.put("locationx",x);
				faceprofile.put("locationy",y);
				faceprofile.put("locationw",w);
				faceprofile.put("locationh",h);
				
		        BufferedImage originalImgage = ImageIO.read(new File( inInput.getAbsolutePath()) );
		        BufferedImage subImgage = originalImgage.getSubimage(x, y, w, h);
		        ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        ImageIO.write(subImgage, "jpg", baos);
		        byte[] bytes = baos.toByteArray();
		        ByteArrayBody body = new ByteArrayBody(bytes,inAsset.getName() + "_" + timecodestart + "_" + "x"+ x + "y" + y + "w" + w + "h" + h + ".jpg");
		        tosendparams.put("file", body);
		    } catch (IOException e) {
		        e.printStackTrace();
		        return null;
		    }
			
			//tosendparams.put("file", new File(inInput.getAbsolutePath()));
			CloseableHttpResponse resp = null;
			String url = getMediaArchive().getCatalogSettingValue("faceprofileserver");
			if( url == null)
			{
				url = "http://localhost:8000/";
			}
			resp = getSharedConnection().sharedMimePost(url + "/api/v1/recognition/faces",tosendparams);
			JSONObject json = getSharedConnection().parseJson(resp);
			if( json.get("image_id") != null)
			{
				//OK
			}
			/*
			{
				  "image_id": "6b135f5b-a365-4522-b1f1-4c9ac2dd0728",
				  "subject": "subject1"
				}
			*/
		}
		return faceprofiles;
	}

	protected void updateEndTimes(List<Map> pendingprofiles, long cutofftime)
	{
		if (pendingprofiles != null) {
			for (Iterator iterator = pendingprofiles.iterator(); iterator.hasNext();)
			{
				Map runningprofile = (Map) iterator.next(); //Put ends times because they got this far
				
				long length = cutofftime-(long)runningprofile.get("timecodestart");
				runningprofile.put("timecodelength",length);
			}
		}
	}
	
	private ContentItem generateInputFile(Asset inAsset, Block inBlock)
	{
		ConversionManager manager = getMediaArchive().getTranscodeTools().getManagerByRenderType("video");
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

	protected List<Map> findFaces(ContentItem input) throws ParseException
	{
		//Scan via REST and get faces
		//1. Take image and scan it for faces https://github.com/exadel-inc/CompreFace/blob/master/docs/Rest-API-description.md#recognize-faces-from-a-given-image
		
		Map tosendparams = new HashMap();
		tosendparams.put("limit","20");
		//tosendparams.put("face_plugins","detector");
		tosendparams.put("file", new File(input.getAbsolutePath()));
		CloseableHttpResponse resp = null;
		String url = getMediaArchive().getCatalogSettingValue("faceprofileserver");
		if( url == null)
		{
			url = "http://localhost:8000";
		}
		resp = getSharedConnection().sharedMimePost(url + "/api/v1/recognition/recognize",tosendparams);
		JSONObject json = getSharedConnection().parseJson(resp);
		JSONArray results = (JSONArray)json.get("result");
		
		return results;
	}
	

/*
	public List<Map> combineVideoMatches(List<Map> firstprofiles, List<Map> secondprofiles)
	{
		
		List<Map> tocontinuelooking = new ArrayList();

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
*/
	
	public Map getImageAndLocationForGroup(Data asset,String inGroupId, int thumbwidth, int thumbheight)
	{
		Collection profiles = (Collection)asset.getValue("faceprofiles");
		
		Map results = new HashMap();
		
		for (Iterator iterator = profiles.iterator(); iterator.hasNext();)
		{
			Map profile = (Map) iterator.next();
			ValuesMap values = new ValuesMap(profile);
			String groupid = (String)profile.get("faceprofilegroup");
			
			if( profile != null && inGroupId.equals(groupid))
			{
				double x = values.getInteger("locationx");
				double y = values.getInteger("locationy");
				double w = values.getInteger("locationw");
				double h = values.getInteger("locationh");
				
				double scale = MathUtils.divide(thumbwidth , w);
				
				x = x * scale;
				y = y * scale;
				w = w * scale;
				h = h * scale;
				
				Double[] scaledxy = new Double[] { x, y, w , h};
				
				//Calculate the dimentions scaled to this image
				String json = JSONArray.toJSONString( Arrays.asList(scaledxy));
				results.put("locationxy",json);
				if( profile.get("timecodestart") != null )
				{
					double seconds = MathUtils.divide( profile.get("timecodestart").toString(),"1000");
					results.put("timecodestartseconds",seconds);
				}
			}
		}
		
		return results;
	}
	
	
}