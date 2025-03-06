package org.entermediadb.asset.facedetect;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
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
import org.openedit.util.MathUtils;
import org.openedit.util.OutputFiller;
import org.openedit.util.PathUtilities;


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
			String url = getMediaArchive().getCatalogSettingValue("faceprofileserver");
			if( url == null)
			{
				log.error("No face server configured");
				return false;
			}
			String type = getMediaArchive().getMediaRenderType(inAsset);
			
			inAsset.setValue("facescancomplete","true");
			
			if (!"image".equalsIgnoreCase(type) && !"video".equalsIgnoreCase(type)  )
			{
				return false;
			}
			
			String api = getMediaArchive().getCatalogSettingValue("faceapikey");
			if(api == null)
			{
				log.info("faceapikey not set");
				return false;
			}
		
			//If its a video then generate all the images and scan them
			
			List<Map> faceprofiles = new ArrayList();
			
			if( "image".equalsIgnoreCase(type) && inAsset.getFileFormat()!= null)
			{
				ContentItem input = null;
				long filesize = inAsset.getLong("filesize");
				
				long imagew = inAsset.getLong("width");
				long imageh = inAsset.getLong("height");
				long imagesize = imagew * imageh;
				
				if(imagesize < 90000) {
					return false;
				}
				
				Boolean useoriginal = true;
				if (filesize > 6000000)
				{
					useoriginal = false;
				}
				else if (!inAsset.getFileFormat().equals("jpg") && !inAsset.getFileFormat().equals("jpeg")) 
				{
					useoriginal = false;
				}
				else if ( imagesize > 36000000) {   //4x 3000x0000 MAX: 178956970
					//Default Copreface imagesize limit
					useoriginal = false;
				}
				else {
					String colorpsace = inAsset.get("colorspace");
					if("4".equals(colorpsace) || "5".equals(colorpsace)) {
						useoriginal = false;
					}
				}
				if (useoriginal)
				{
					input = getMediaArchive().getOriginalContent(inAsset);
				}
				else 
				{
					input = getMediaArchive().getContent("/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image3000x3000.webp");
				}
				if( !input.exists() )
				{
					input = getMediaArchive().getContent("/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image3000x3000.jpg");
				}
				if( !input.exists() )
				{
					//problem
					log.info("Faceprofile scan, no thumbnail found for assetid: " +inAsset.getId() + " "+ inAsset.getSourcePath());
					//inAsset.setValue("facescancomplete","true");
					//getMediaArchive().saveAsset(inAsset);
					//may not be ready?
					return false;
				}
				List<Map> json = findFaces(inAsset, input);
				if(json == null) {
					return false;
				}
				List<Map> moreprofiles = makeProfilesForEachFace(inAsset,0L,input,json);
	
				faceprofiles.addAll(moreprofiles);
			}
			else if( "video".equalsIgnoreCase(type) )
			{
				//Look over them and save the timecode with it
				Double videolength = (Double)inAsset.getDouble("length");
				if( videolength == null)
				{
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
						log.info("Faceprofile scan, no thumbnail found for assetid: " +inAsset.getId() + " "+ inAsset.getSourcePath());
					}
					else
					{
						List<Map> json = findFaces(inAsset, item);	
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
			boolean detected = false;
			inAsset.setValue("faceprofiles",faceprofiles);  //box  and subjects
			if(faceprofiles != null &&  !faceprofiles.isEmpty())
			{
				inAsset.setValue("facehasprofile",true);
				detected = true;
			}
			log.info("Faceprofile found: "+faceprofiles.size());
			
			return detected;
		}
		catch( Throwable ex)
		{
			throw new OpenEditException("Error on: " + inAsset.getId() + " " + inAsset.getSourcePath(),ex);
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
		double boxprobability = .999D; 
		String value = getMediaArchive().getCatalogSettingValue("facedetect_profile_confidence");
		if( value != null)
		{
			similaritycheck = Double.parseDouble(value);
		}

		//
		//extract
		
		int inputw = 0;
		int inputh = 0;
		
		if(inInput.getName().endsWith("webp") )
		{
			Dimension size = getImageDimensionWebP(inInput.getInputStream());
			if(size == null) 
			{
				log.info("Can't get image size from Webp: " + inInput.getPath());
				return faceprofiles;
			}
				inputw = (int)Math.round( size.getWidth() );
				inputh = (int)Math.round( size.getHeight() );
		}	
		else
		{
			File inputfile = new File( inInput.getAbsolutePath() );
			Dimension size = getImageDimensionImageIO(inputfile);
			inputw = (int)Math.round( size.getWidth() );
			inputh = (int)Math.round( size.getHeight() );
		}       
        int minfacesize = 450;
		
		String minumfaceimagesize = getMediaArchive().getCatalogSettingValue("facedetect_minimum_face_size");
		if(minumfaceimagesize != null) {
			minfacesize = Integer.parseInt(minumfaceimagesize);
		}
		if (inJsonOfFaces.size() == 1) {
			minfacesize = minfacesize - 100;
		}
		
		
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
				//log.info(found);
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
			
			//verify probability that a found face is actually a face
			double boxp = box.getDouble("probability");
			if( boxp < boxprobability)
			{
				log.info("Low probability of found face (" + boxp  + "<"+ boxprobability+"): " + inInput.getPath());
				continue;
			}
			
			int x = box.getInteger("x_min");
			int y = box.getInteger("y_min");
			int x2 = box.getInteger("x_max");
			int y2 = box.getInteger("y_max");
			int w = x2 - x;
			int h = y2 - y;
			
			
			h = Math.min(h,inputh - y);
			w = Math.min(w,inputw - x);
			
			
			
			if( h < minfacesize)
			{
				log.info("Not enough data, small face detected assetid:" + inAsset.getId()+ " w:" + w + " h:" + h + " Min face size: " + minfacesize);
				continue;
			}
			Map faceprofile = new HashMap();
			faceprofile.put("timecodestart",timecodestart);

			boolean morethan20 = false;
			String groupid = null;
			if( found != null)
			{
				groupid = (String)found.get("subject");
				
				if( inAsset.containsValue("removedfaceprofilegroups",groupid))
				{
					log.info("Skipping group for asset: " + inAsset.getId());
					continue;
				}
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
					if( count > 20)
					{
						morethan20 = true;
					}
					oldgroup.setValue("samplecount", count);
				}
				
				oldgroup.setValue("entity_date", new Date());
				
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
				newgroup.setValue("entity_date", new Date());
				getMediaArchive().saveData("faceprofilegroup", newgroup);
				groupid = newgroup.getId();
				faceprofile.put("faceprofilegroup", groupid );
				
			}
			
			faceprofile.put("locationx",x);
			faceprofile.put("locationy",y);
			faceprofile.put("locationw",w);
			faceprofile.put("locationh",h);
				
	        faceprofile.put("inputwidth",inputw);
	        faceprofile.put("inputheight",inputh);
			
			if( !morethan20)
			{
				
				//Image Magic convert
				ConvertInstructions instructions =  getMediaArchive().createInstructions(inAsset, inInput);
				instructions.setCrop(true);
				instructions.setProperty("x1", Integer.toString(x) );
				instructions.setProperty("y1", Integer.toString(y));
				instructions.setProperty("cropwidth", Integer.toString(w));
				instructions.setProperty("cropheight", Integer.toString(h));
				instructions.setProperty("forcerendertype", "image");
				instructions.setProperty("inputextension", PathUtilities.extractPageType(inInput.getName()));
				
				

				ContentItem smallitem = getMediaArchive().getContent( "/WEB-INF/trash/" + getMediaArchive().getCatalogId()	+ "/small/" + inAsset.getSourcePath() + ".webp" );
				
				getMediaArchive().convertFile(instructions, smallitem);

				
				uploadAProfile(faceprofile, timecodestart, smallitem, inAsset, groupid);
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

	private void uploadAProfile(Map faceprofile, long timecodestart,ContentItem originalImgage, Asset inAsset, String groupId ) throws Exception
	{
			int x = (Integer) faceprofile.get("locationx");
			int y = (Integer) faceprofile.get("locationy");
			int w = (Integer) faceprofile.get("locationw");
			int h = (Integer) faceprofile.get("locationh");
//			
//	        BufferedImage subImgage = originalImgage.getSubimage(x, y, w, h);
//	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//	        ImageIO.write(subImgage, "jpg", baos);
//	        byte[] bytes = baos.toByteArray();
			byte[] bytes =	new OutputFiller().readAll(originalImgage.getInputStream());
	        ByteArrayBody body = new ByteArrayBody(bytes,inAsset.getName() + "_" + timecodestart + "_" + "x"+ x + "y" + y + "w" + w + "h" + h + ".webp");
			
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
			//log.info("Profile: "+groupId+" created at server. Image id" + json.get("image_id"));
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

	protected List<Map> findFaces(Asset inAsset, ContentItem input) throws ParseException
	{
		//Scan via REST and get faces
		//1. Take image and scan it for faces https://github.com/exadel-inc/CompreFace/blob/master/docs/Rest-API-description.md#recognize-faces-from-a-given-image
				
		Map tosendparams = new HashMap();
		Integer limit = 20;
		Integer assetwidth = inAsset.getInt("width"); 
		if(assetwidth > 0) {
			limit = assetwidth/ 600;
		}
		tosendparams.put("limit", limit.toString());
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
			log.error("No faceprofileserver URL configured" );
			return null;
			//url = "http://localhost:8000";
		}
		long start = System.currentTimeMillis();
		log.debug("Facial Profile Detection sending " + input.getPath() );
		resp = getSharedConnection().sharedMimePost(url + "/api/v1/recognition/recognize",tosendparams);
		if (resp.getStatusLine().getStatusCode() == 400)
		{
			//No faces found error
			getSharedConnection().release(resp);
			return Collections.EMPTY_LIST;
		}
		else if (resp.getStatusLine().getStatusCode() == 413)
		{
			getSharedConnection().release(resp);
			return null;
		}
		else if (resp.getStatusLine().getStatusCode() == 500)
		{
			//remote server error, may be a broken image
			getSharedConnection().release(resp);
			log.info("Face detection Remote Error on asset: " + inAsset.getId() + " " + resp.getStatusLine().toString() ) ;
			return null;
		}

		JSONObject json = getSharedConnection().parseJson(resp);
		//log.info(json.toString());
		
		JSONArray results = (JSONArray)json.get("result");
		
		log.info((System.currentTimeMillis() - start) + "ms face detection for asset: "+ inAsset.getId() + " " + input.getName() + " Found: " + results.size());
		
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
			Map found = getImageAndLocationForGroup(asset, group, thumbwidth, thumbheight);
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
				result.put("groupid", groupid);
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
	
	public Map getImageAndLocationForGroup(MultiValued asset, Collection<Data>  infaceprofilegroup, Double thumbheight) { 
		//Todo
		
		for (Iterator iterator = infaceprofilegroup.iterator(); iterator.hasNext();)
		{
			Data group = (Data)  iterator.next();
			if(group != null) {
				Map found = getImageAndLocationForGroup(asset, group.getId(), thumbheight);
				if( found != null)
				{
					return found;
				}
			}
		}
	
		return null;
	}
	
	
	public Map getImageAndLocationForGroup(MultiValued asset,String infaceprofilegroupid, Double thumbheight)
	{
		if(asset == null) {
			return null;
		}
		
		Collection profiles = (Collection)asset.getValue("faceprofiles");
		
		if (profiles != null) {
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
		if (profiles.isEmpty()) {
			return null;
		}
		
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

		Searcher searcher = getMediaArchive().getSearcher( "asset");
		for(Data data: assets)
		{
			Asset asset = (Asset)searcher.loadData(data);
			Collection<Map> faceprofiles = (Collection)asset.getValue("faceprofiles");
			if (faceprofiles != null) {
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
		}
		
		
		return faceassets;
		
	}
	
	
	public Map getImageBox(Collection<Data> faceprofilegroups, Data asset, String imagew, String imageh)
	{
		if(faceprofilegroups == null || faceprofilegroups.size() < 1)
		{
			return null;
		}
		for(Data group : faceprofilegroups)
		{
			Map found = getImageAndLocationForGroup(asset, group, Double.valueOf(imagew), Double.valueOf(imageh));
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}
	
	public Map getImageBoxById(String faceprofilegroupid, Data asset, String imagew, String imageh)
	{
		Map found = getImageAndLocationForGroup(asset, faceprofilegroupid, Double.valueOf(imagew), Double.valueOf(imageh));
		if (found != null)
		{
			return found;
		}
		return null;
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

		log.info(assets.size() +" assets found for profile id: " + inFaceProfileId);
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

	public FaceAsset loadFaceData(Data faceprofilegroup,Data inData)
	{
		Searcher searcher = getMediaArchive().getSearcher("asset");
		
		Asset asset = (Asset)searcher.loadData(inData);
		Collection<Map> faceprofiles = (Collection)asset.getValue("faceprofiles");
		for( Map facedata : faceprofiles)
		{
			String groupid = (String)facedata.get("faceprofilegroup");
			if( faceprofilegroup.getId().equals( groupid ))
			{
				FaceAsset faceasset = new FaceAsset();
				faceasset.setAsset(asset);
				faceasset.setFaceProfileGroup(faceprofilegroup);
				faceasset.setFaceLocationData(new ValuesMap(facedata));
				return faceasset;
			}
		}
		return null;
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
	

	/**
	 * Gets image dimensions for given file 
	 * @param imgFile image file
	 * @return dimensions of image
	 * @throws IOException if the file is not a known image
	 */
	public  Dimension getImageDimensionImageIO(File imgFile) throws IOException {
	  int pos = imgFile.getName().lastIndexOf(".");
	  if (pos == -1)
	    throw new IOException("No extension for file: " + imgFile.getAbsolutePath());
	  String suffix = imgFile.getName().substring(pos + 1);
	  Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
	  while(iter.hasNext()) {
	    ImageReader reader = iter.next();
	    try {
	      ImageInputStream stream = new FileImageInputStream(imgFile);
	      reader.setInput(stream);
	      int width = reader.getWidth(reader.getMinIndex());
	      int height = reader.getHeight(reader.getMinIndex());
	      return new Dimension(width, height);
	    } catch (IOException e) {
	      log.warn("Error reading: " + imgFile.getAbsolutePath(), e);
	    } finally {
	      reader.dispose();
	    }
	  }

	  throw new IOException("Not a known image file: " + imgFile.getAbsolutePath());
	}
	
    public java.awt.Dimension getImageDimensionWebP(InputStream is) throws IOException {
        byte[] data = is.readNBytes(30);
        if (new String(Arrays.copyOfRange(data, 0, 4)).equals("RIFF") && data[15] == 'X') {
            int width = 1 + get24bit(data, 24);
            int height = 1 + get24bit(data, 27);

            if ((long) width * height <= 4294967296L) return new Dimension(width, height);
        }
        return null;
    }

    private static int get24bit(byte[] data, int index) {
        return data[index] & 0xFF | (data[index + 1] & 0xFF) << 8 | (data[index + 2] & 0xFF) << 16;
    }
	
	
}
