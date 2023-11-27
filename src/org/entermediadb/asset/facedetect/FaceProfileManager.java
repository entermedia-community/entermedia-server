package org.entermediadb.asset.facedetect;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.ListHitTracker;
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
			
			List<Map> faceprofiles = new ArrayList();
			
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
				List<Map> moreprofiles = makeProfilesForEachFace(inAsset,0L,input,json);
				faceprofiles.addAll(moreprofiles);
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
				
				if(videolength < 20) {
					timeline.setTotalTickCount(10);
				}
				
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
						List<Map> moreprofiles = makeProfilesForEachFace(inAsset,block.getStartOffset(),item,json);
						faceprofiles.addAll(moreprofiles);
					}
				}
				faceprofiles = combineVideoMatches(faceprofiles,mili);
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
			//inAsset.setValue("facematchcomplete","false");
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

	protected List<Map> makeProfilesForEachFace(Asset inAsset,long timecodestart, ContentItem inInput, List<Map> inJsonOfFaces) throws Exception
	{
		List<Map> faceprofiles = new ArrayList();
		if( inJsonOfFaces.isEmpty())
		{
			return faceprofiles;
		}
		
		double similaritycheck = .99D;
		String value = getMediaArchive().getCatalogSettingValue("facedetect_profile_confidence");
		if( value != null)
		{
			similaritycheck = Double.parseDouble(value);
		}

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
        BufferedImage imageImput = ImageIO.read(new File( inInput.getAbsolutePath()) );

		for (Iterator iterator = inJsonOfFaces.iterator(); iterator.hasNext();)
		{
			Map map = (Map) iterator.next();
			//faceprofile.put("facedata", map);
			//faceprofilegroup
			
//			Map mask = (Map)map.get("box");
//			if( mask != null)
//			{
//				Double probability = (Double)mask.get("probability");
//				if( probability < facedetect_detect_confidence)
//				{
//					continue;
//				}
//			}
			
			List subjects = (List)map.get("subjects");
			Map found = null;
			if( subjects != null && !subjects.isEmpty())
			{
				/*
				List sorted = new ArrayList(subjects);
				Collections.sort(sorted,new Comparator<Map>()
				{
					public int compare(Map arg0, Map arg1) 
					{
						double similrity0 = (Double)arg0.get("similarity");
						double similrity1 = (Double)arg1.get("similarity");
						if( similrity0 > similrity1)
						{
							return 1;
						}
						if( similrity0 < similrity1)
						{
							return -1;
						}
						return 0;
						
					};
				});
				for (Iterator iterator2 = sorted.iterator(); iterator2.hasNext();)
				{
					Map subject = (Map) iterator2.next();
					double similrity = (Double)subject.get("similarity");
					if( similrity > .80D)
					{
						found = subject;
						break;
					}
				}
				*/
				found  = (Map)subjects.get(0); //We only get up to one result
				log.info(found);
				double similrity = (Double)found.get("similarity");
				if( similrity < similaritycheck)
				{
					found = null;//
				}				
				
			}
			//Upload as another subject so we have plenty similar ones
//			curl -X POST "http://localhost:8000/api/v1/recognition/faces?subject=<subject>&det_prob_threshold=<det_prob_threshold>" \
//			-H "Content-Type: multipart/form-data" \
//			-H "x-api-key: <service_api_key>" \
			
			//tosendparams.put("det_prob_threshold",".6");
			ValuesMap box = new ValuesMap((Map)map.get("box"));
			int x = box.getInteger("x_min");
			int y = box.getInteger("y_min");
			int x2 = box.getInteger("x_max");
			int y2 = box.getInteger("y_max");
			int w = x2 - x;
			int h = y2 - y;
			if( w < 150 || h < 190)
			{
				log.info("Not enough data, small face detected w:" + w + " h:" + h );
				continue;
			}
			Map faceprofile = new HashMap();
			faceprofile.put("timecodestart",timecodestart);

			boolean morethan10 = false;
			String groupid = null;
			if( found != null)
			{
				groupid = (String)found.get("subject");
				//TODO: Count how many times I have used this group.
				MultiValued oldgroup = (MultiValued)getMediaArchive().getData("faceprofilegroup",groupid);
				if( oldgroup == null)
				{
					//is a new group
					oldgroup = (MultiValued)getMediaArchive().getSearcher("faceprofilegroup").createNewData();
					oldgroup.setValue("id", groupid);
					oldgroup.setValue("primaryimage", inAsset.getId());
					oldgroup.setValue("samplecount", 1);
				}
				else 
				{
					int count = oldgroup.getInt("samplecount");
					count++;
					if( count > 10)
					{
						morethan10 = true;
					}
					else
					{
						oldgroup.setValue("samplecount", count);
					}
				}
				getMediaArchive().getSearcher("faceprofilegroup").saveData(oldgroup);
				
				faceprofile.put("faceprofilegroup", groupid);
			}
			else
			{
				//Create new profilegroupid
				Data newgroup = getMediaArchive().getSearcher("faceprofilegroup").createNewData();		
				newgroup.setValue("primaryimage", inAsset.getId());
				newgroup.setValue("automatictagging", true);
				newgroup.setValue("creationdate", new Date());
				newgroup.setValue("samplecount",1);
				getMediaArchive().saveData("faceprofilegroup", newgroup);
				groupid = newgroup.getId();
				faceprofile.put("faceprofilegroup", groupid );
				
			}
			
			
			faceprofile.put("locationx",x);
			faceprofile.put("locationy",y);
			faceprofile.put("locationw",w);
			faceprofile.put("locationh",h);
			
	        faceprofile.put("inputwidth",imageImput.getWidth());
	        
	        faceprofile.put("inputheight",imageImput.getHeight());
			
			if( !morethan10)
			{
				uploadAProfile(faceprofile, timecodestart, imageImput, inAsset, groupid);
			}
			else
			{
				log.error("Already have 10 profiles from same subject: " + groupid);
			}
			//TODO: Make sure this is not already in there. For debug purposes
			faceprofiles.add(faceprofile);
		}
		return faceprofiles;
	}

	private void uploadAProfile(Map faceprofile, long timecodestart,BufferedImage originalImgage, Asset inAsset, String groupId ) throws Exception
	{
			int x = (Integer) faceprofile.get("locationx");
			int y = (Integer) faceprofile.get("locationy");
			int w = (Integer) faceprofile.get("locationw");
			int h = (Integer) faceprofile.get("locationh");
			
	        BufferedImage subImgage = originalImgage.getSubimage(x, y, w, h);
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        ImageIO.write(subImgage, "jpg", baos);
	        byte[] bytes = baos.toByteArray();
	        ByteArrayBody body = new ByteArrayBody(bytes,inAsset.getName() + "_" + timecodestart + "_" + "x"+ x + "y" + y + "w" + w + "h" + h + ".jpg");
			Map tosendparams = new HashMap();
	        tosendparams.put("file", body);
			tosendparams.put("subject",groupId );

		//tosendparams.put("file", new File(inInput.getAbsolutePath()));
		CloseableHttpResponse resp = null;
		String url = getMediaArchive().getCatalogSettingValue("faceprofileserver");
		if( url == null)
		{
			url = "http://localhost:8000/";
		}
		resp = getSharedConnection().sharedMimePost(url + "/api/v1/recognition/faces",tosendparams);
		if (resp.getStatusLine().getStatusCode() == 400)
		{
			//No faces found error
			getSharedConnection().release(resp);
			return;
		}
		JSONObject json = getSharedConnection().parseJson(resp);
		if( json.get("image_id") != null)
		{
			//OK
			log.info("Profile: "+groupId+" created at server. Image id" + json.get("image_id"));
		}
		else 
		{
			log.info("Could'nt upload image, response:" + json.toString());
		}
		/*
		{
			  "image_id": "6b135f5b-a365-4522-b1f1-4c9ac2dd0728",
			  "subject": "subject1"
			}
		*/
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
		tosendparams.put("prediction_count","1"); //Return only most likely subject
		//tosendparams.put("face_plugins","detector");
		
		double facedetect_detect_confidence = .999D;
		String detectvalue = getMediaArchive().getCatalogSettingValue("facedetect_detect_confidence");
		if( detectvalue != null)
		{
			facedetect_detect_confidence = Double.parseDouble(detectvalue);
		}


		tosendparams.put("det_prob_threshold",facedetect_detect_confidence);

		tosendparams.put("file", new File(input.getAbsolutePath()));
		CloseableHttpResponse resp = null;
		String url = getMediaArchive().getCatalogSettingValue("faceprofileserver");
		if( url == null)
		{
			url = "http://localhost:8000";
		}
		log.info("Sending " + input.getPath() );
		resp = getSharedConnection().sharedMimePost(url + "/api/v1/recognition/recognize",tosendparams);
		log.info("Returned " + resp.getStatusLine().getStatusCode() + " " + input.getPath() );
		if (resp.getStatusLine().getStatusCode() == 400)
		{
			//No faces found error
			getSharedConnection().release(resp);
			return Collections.EMPTY_LIST;
		}

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
	public Map getImageAndLocationForGroup(Data asset,Collection<Data> faceprofilegroups, Double thumbwidth, Double thumbheight)
	{
		
		for(Data group : faceprofilegroups)
		{
			Map found = getImageAndLocationForGroup(asset,group, thumbwidth, thumbheight);
			if( found != null)
			{
				return found;
			}
		}
		return null;
	}
	public Map getImageAndLocationForGroup(Data asset,Data infaceprofilegroup, Double thumbwidth, Double thumbheight)
	{
		return getImageAndLocationForGroup(asset,infaceprofilegroup.getId(),thumbwidth,thumbheight);
	}	
	public Map getImageAndLocationForGroup(Data asset,String infaceprofilegroupid, String thumbwidth, String thumbheight)
	{
		return getImageAndLocationForGroup(asset,infaceprofilegroupid, Double.valueOf(thumbwidth) , Double.valueOf(thumbheight));
	}

	public Map getImageAndLocationForGroup(Data asset,String infaceprofilegroupid, Double thumbwidth, Double thumbheight)
	{
		Collection profiles = (Collection)asset.getValue("faceprofiles");
		
		
		for (Iterator iterator = profiles.iterator(); iterator.hasNext();)
		{
			Map profile = (Map) iterator.next();
			ValuesMap values = new ValuesMap(profile);
			String groupid = (String)profile.get("faceprofilegroup");
			
			if( profile != null && infaceprofilegroupid.equals(groupid))
			{
				
				double x = values.getInteger("locationx");
				double y = values.getInteger("locationy");
				double w = values.getInteger("locationw");
				double h = values.getInteger("locationh");
				double inputwidth = values.getInteger("inputwidth");
				
				double scale = MathUtils.divide(thumbwidth , inputwidth);
				
				x = x * scale;
				y = y * scale;
				w = w * scale;
				h = h * scale;
				
				Double[] scaledxy = new Double[] { x, y, w , h};
				
				//Calculate the dimentions scaled to this image
				String json = JSONArray.toJSONString( Arrays.asList(scaledxy));
				Map result = new HashMap();
				result.put("locationxy",json);
				if( profile.get("timecodestart") != null )
				{
					double seconds = MathUtils.divide( profile.get("timecodestart").toString(),"1000");
					result.put("timecodestartseconds",seconds);
				}
				return result;
			}
		}
		
		return null;
	}
	
	
	public Map getImageAndLocationForGroup(Asset asset,String infaceprofilegroupid, Double thumbheight)
	{
		if(asset == null) {
			return null;
		}
		
		Collection profiles = (Collection)asset.getValue("faceprofiles");
		
		
		for (Iterator iterator = profiles.iterator(); iterator.hasNext();)
		{
			Map profile = (Map) iterator.next();
			ValuesMap values = new ValuesMap(profile);
			String groupid = (String)profile.get("faceprofilegroup");
			
			if( profile != null && infaceprofilegroupid.equals(groupid))
			{
				
				double x = values.getInteger("locationx");
				double y = values.getInteger("locationy");
				double w = values.getInteger("locationw");
				double h = values.getInteger("locationh");
				double inputheight = values.getInteger("inputheight");
				
				if (inputheight == 0) {
					double inputwidth = values.getInteger("inputwidth");
					double assetscale = MathUtils.divide(asset.getDouble("height") , asset.getDouble("width"));
					inputheight = inputwidth * assetscale;
				}
				double scale = MathUtils.divide(thumbheight , inputheight);
				
				x = x * scale;
				y = y * scale;
				w = w * scale;
				h = h * scale;
				
				Double[] scaledxy = new Double[] { x, y, w , h};
				
				//Calculate the dimentions scaled to this image
				String json = JSONArray.toJSONString( Arrays.asList(scaledxy));
				Map result = new HashMap();
				result.put("locationxy",json);
				if( profile.get("timecodestart") != null )
				{
					double seconds = MathUtils.divide( profile.get("timecodestart").toString(),"1000");
					result.put("timecodestartseconds",seconds);
				}
				return result;
			}
		}
		
		return null;
	}
	
	public List<Map> combineVideoMatches(List<Map> faceprofles, long inVideoLength)
	{
		long defaultlength = -1;
		if( inVideoLength < 60000)
		{
			defaultlength = (long)MathUtils.divide(inVideoLength, 10);
		}
		else
		{
			defaultlength = (long)MathUtils.divide(inVideoLength, 20);
		}
		Map previousprofile = null;
		List<Map> copy = new ArrayList();
		for (Iterator iterator = faceprofles.iterator(); iterator.hasNext();)
		{
			Map profile = (Map)iterator.next();
			profile.put("timecodelength",defaultlength);

			if( previousprofile == null)
			{
				copy.add(profile);
				previousprofile = profile;
			}
			else
			{
				String previousfaceprofilegroup = (String)previousprofile.get("faceprofilegroup");
				String faceprofilegroup = (String)profile.get("faceprofilegroup");

				long start1 = (Long)previousprofile.get("timecodestart");
				long start2 = (Long)profile.get("timecodestart");
				if( faceprofilegroup.equals(previousfaceprofilegroup))
				{
					long total = start2 - start1 + defaultlength;
					//TODO: Beyond the end?
					previousprofile.put("timecodelength",total);
				}
				else
				{
					long total = start2 - start1;
					previousprofile.put("timecodelength",total);
					copy.add(profile);
					previousprofile = profile;
				}
			}
		}
		return copy;
	}

	public Collection<FaceAsset> findAssetsForPerson(Data inPersonEntity, int maxnumber)
	{
		Collection<Data> profiles = getMediaArchive().query("faceprofilegroup").exact("entityperson", inPersonEntity.getId()).search();
		Collection<Data> assets = getMediaArchive().query("asset").orgroup("faceprofiles.faceprofilegroup", profiles).hitsPerPage(maxnumber).search();

		Map allprofiles = new HashMap();
		for(Data profile : profiles)
		{
			allprofiles.put(profile.getId(), profile);
		}
		
//
//		Collection profiles = archive.query("faceprofilegroup").exact("entityperson", person.getId()).search();
//		inPageRequest.putPageValue("faceprofiles", profiles);
//		
//		Collection assets = archive.query("asset").named("faceassets").orgroup("faceprofiles.faceprofilegroup", profiles).search();
//		
//		inPageRequest.putPageValue("entityassethits", assets);

		Collection<FaceAsset> faceassets = new ListHitTracker<FaceAsset>();

		Searcher searcher = getMediaArchive().getSearcher("asset");
		for(Data data: assets)
		{
			Asset asset = (Asset)searcher.loadData(data);
			Collection<Map> faceprofiles = (Collection)asset.getValue("faceprofiles");
			for( Map facedata : faceprofiles)
			{
				String faceprofilegroupid = (String)facedata.get("faceprofilegroup");
				Data group = (Data)allprofiles.get(faceprofilegroupid);
				if( group != null)
				{
					FaceAsset faceasset = new FaceAsset();
					faceasset.setAsset(asset);
					faceasset.setFaceProfileGroup(group);
					faceasset.setFaceLocationData(new ValuesMap(facedata));
					faceassets.add(faceasset);
				}
			}
		}
		
		
		return faceassets;
		
	}
	
	
	public Collection<FaceAsset> findAssetsForProfile(String inFaceProfileId, int maxnumber)
	{
		Collection<Data> profiles = getMediaArchive().query("faceprofilegroup").exact("id", inFaceProfileId).search();
		Collection<Data> assets = getMediaArchive().query("asset").orgroup("faceprofiles.faceprofilegroup", profiles).hitsPerPage(maxnumber).search();

		Map allprofiles = new HashMap();
		for(Data profile : profiles)
		{
			allprofiles.put(profile.getId(), profile);
		}
		
//
//		Collection profiles = archive.query("faceprofilegroup").exact("entityperson", person.getId()).search();
//		inPageRequest.putPageValue("faceprofiles", profiles);
//		
//		Collection assets = archive.query("asset").named("faceassets").orgroup("faceprofiles.faceprofilegroup", profiles).search();
//		
//		inPageRequest.putPageValue("entityassethits", assets);

		Collection<FaceAsset> faceassets = new ListHitTracker<FaceAsset>();

		Searcher searcher = getMediaArchive().getSearcher("asset");
		for(Data data: assets)
		{
			Asset asset = (Asset)searcher.loadData(data);
			Collection<Map> faceprofiles = (Collection)asset.getValue("faceprofiles");
			for( Map facedata : faceprofiles)
			{
				String faceprofilegroupid = (String)facedata.get("faceprofilegroup");
				Data group = (Data)allprofiles.get(faceprofilegroupid);
				if( group != null)
				{
					FaceAsset faceasset = new FaceAsset();
					faceasset.setAsset(asset);
					faceasset.setFaceProfileGroup(group);
					faceasset.setFaceLocationData(new ValuesMap(facedata));
					faceassets.add(faceasset);
				}
			}
		}
		
		
		return faceassets;
		
	}

	
	public Map getImageAndLocationForFaceAsset(FaceAsset faceasset, int thumbheight)
	{
		ValuesMap profiledata = faceasset.getFaceLocationData();
			
		double x = profiledata.getInteger("locationx");
		double y = profiledata.getInteger("locationy");
		double w = profiledata.getInteger("locationw");
		double h = profiledata.getInteger("locationh");
		double inputheight = profiledata.getInteger("inputheight");
		
		if (inputheight == 0) {
			double inputwidth = profiledata.getInteger("inputwidth");
			double assetscale = MathUtils.divide(faceasset.getAsset().getDouble("height") , faceasset.getAsset().getDouble("width"));
			inputheight = inputwidth * assetscale;
		}
		double scale = MathUtils.divide(thumbheight , inputheight);
				
		x = x * scale;
		y = y * scale;
		w = w * scale;
		h = h * scale;
		
		Double[] scaledxy = new Double[] { x, y, w , h};
		
		//Calculate the dimentions scaled to this image
		String json = JSONArray.toJSONString( Arrays.asList(scaledxy));
		Map result = new HashMap();
		result.put("locationxy",json);
		if( profiledata.get("timecodestart") != null )
		{
			double seconds = MathUtils.divide( profiledata.get("timecodestart").toString(),"1000");
			result.put("timecodestartseconds",seconds);
		}
		return result;
	}
	
}
