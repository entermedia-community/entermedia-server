package org.entermediadb.asset.facedetect;

import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
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
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.repository.ContentItem;
import org.openedit.util.FileUtils;
import org.openedit.util.MathUtils;
import org.openedit.util.OutputFiller;


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
	
	public int extractFaces(Asset inAsset)
	{
		try
		{
			String url = getMediaArchive().getCatalogSettingValue("faceprofileserver");
			if( url == null)
			{
				log.error("No face server configured");
				return 0;
			}
			String type = getMediaArchive().getMediaRenderType(inAsset);
			
			inAsset.setValue("facescancomplete","true");
			
			if (!"image".equalsIgnoreCase(type) && !"video".equalsIgnoreCase(type)  )
			{
				return 0;
			}
			
			String api = getMediaArchive().getCatalogSettingValue("faceapikey");
			if(api == null)
			{
				log.info("faceapikey not set");
				return 0;
			}
		
			//If its a video then generate all the images and scan them
			
			List<Data> foundfaces = new ArrayList();
			Searcher faceembeddingsearcher = getMediaArchive().getSearcherManager().getSearcher("system/facedb","faceembedding");

			if( "image".equalsIgnoreCase(type) && inAsset.getFileFormat()!= null)
			{
				long filesize = inAsset.getLong("filesize");
				
				long imagew = inAsset.getLong("width");
				long imageh = inAsset.getLong("height");
				long imagesize = imagew * imageh;
				
				if(imagesize < 90000) {
					return 0;
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
					if("4".equals(colorpsace) || "5".equals(colorpsace)) 
					{
						useoriginal = false;
					}
					else if( inAsset.isPropertyTrue("hasthumbnail") )
					{
						useoriginal = false;  //deepface has a bug that it uses the image thumbnail for sizing
					}
				}
				
//				ContentItem input = getMediaArchive().getContent("/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image3000x3000.webp");
//				if( !input.exists() )
//				{
//					input = getMediaArchive().getContent("/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image3000x3000.jpg");
//				}
//				if( !input.exists() )
//				{
//					log.error("No such image"); //TODO: Search within videos
//					return false;
//				}
				
				//Send orginal JPEG directly if they are small?
				ContentItem input = null;
				if( useoriginal )
				{
					input = getMediaArchive().getOriginalContent(inAsset);
				}
				else
				{
					String filename = getMediaArchive().generatedOutputName(inAsset,"image3000x3000");
					input = getMediaArchive().getContent("/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/" + filename);
				}
				if( !input.exists() )
				{
					throw new OpenEditException("Input not available " + input.getPath());
				}
				List<Map> json = findFaces(inAsset, input);
				if(json == null) {
					return 0;
				}
				Collection<Data> moreprofiles = makeDataForEachFace(faceembeddingsearcher,inAsset,0L,input,json);
				if( moreprofiles != null)
				{
					saveNewFacesWithParents(moreprofiles);
					foundfaces.addAll(moreprofiles);
				}
				
			}
			else if( "video".equalsIgnoreCase(type) )
			{
				//Look over them and save the timecode with it
				Collection<Data> allfacesinvideo = findAllFacesInVideo(inAsset);
				Collection<Data> moreprofiles = combineVideoMatches(allfacesinvideo);
				if(moreprofiles != null)
				{
					saveNewFacesWithParents(moreprofiles);
					foundfaces.addAll(moreprofiles);
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
			boolean hasfaces = !foundfaces.isEmpty();
			inAsset.setValue("facehasprofile",hasfaces);
			log.info("Faceprofile found: "+foundfaces.size());
			//faceembeddingsearcher.saveAllData(tosave,null);
			return foundfaces.size();
		}
		catch( Throwable ex)
		{
			
			inAsset.setValue("facescancomplete","true");
			inAsset.setValue("facescanerror","true");
			log.error("Error on: " + inAsset.getId() + " " + inAsset.getSourcePath(), ex);
			return 0;
			//throw new OpenEditException("Error on: " + inAsset.getId() + " " + inAsset.getSourcePath(),ex);
		}
		
	}

	protected Collection<Data> findAllFacesInVideo(Asset inAsset) throws Exception
	{
		Searcher faceembeddingsearcher = getMediaArchive().getSearcherManager().getSearcher("system/facedb","faceembedding");

		Double videolength = (Double)inAsset.getDouble("length");
		if( videolength == null)
		{
			//Log it
			throw new OpenEditException("Invalid Video: " + inAsset.getName());
		}
		Timeline timeline = new Timeline();
		long mili = Math.round( videolength*1000d );
		timeline.setLength(mili);
		timeline.setPxWidth(1200); //This divides in 10 or 20
		
		if(videolength < 20) {
			timeline.setTotalTickCount(10);
		}
		Collection<Block> ticks = timeline.getTicks();
		List<Map> continuelooking = null;
		
		Collection<Data> allfacesinvideo = new ArrayList();
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
				Collection<Data> moreprofiles = makeDataForEachFace(faceembeddingsearcher,inAsset,block.getSeconds(),item,json);
				if( moreprofiles != null)
				{
					allfacesinvideo.addAll(moreprofiles);
				}
			}
		}
		return allfacesinvideo;
	}

	private Collection<Data> combineVideoMatches(Collection<Data> inAllfacesinvideo)
	{
		Collection<Data> uniquefaces = new ArrayList();

		Collection<Data> remainingfaces = new ArrayList(inAllfacesinvideo);
		
		for (Iterator iterator = inAllfacesinvideo.iterator(); iterator.hasNext();)
		{
			Data embedded = (Data) iterator.next();
			
			if( !remainingfaces.contains(embedded) ) //Might already got dropped
			{
				continue;
			}
			else
			{
				remainingfaces.remove(embedded);
				uniquefaces.add(embedded);
			}
			//Add it back after removing all the duplicated
			double[] facedoubles = (double[])embedded.getValue("facedatadoubles");
			
			Collection<Data> remainingfacestmp = new ArrayList(remainingfaces);
			for (Iterator iterator2 = remainingfacestmp.iterator(); iterator2.hasNext();)
			{
				Data otherface = (Data) iterator2.next();
				double[] othervalues = (double[])otherface.getValue("facedatadoubles");
				boolean same = compareVectors(facedoubles, othervalues, getVectorScoreLimit() - 0.3D );
				if( same )
				{
					//TODO: Keep larger facebox
					remainingfaces.remove(otherface);
				}
			}			
		}
		return uniquefaces;
	}

	private double getVectorScoreLimit()
	{
		double similaritycheck = .6D;
		String value = getMediaArchive().getCatalogSettingValue("facedetect_profile_confidence");
		if( value != null)
		{
			similaritycheck = Double.parseDouble(value);
		}
		return similaritycheck;
	}

	protected void saveNewFacesWithParents(Collection<Data> moreprofiles)
	{
		for (Iterator iterator = moreprofiles.iterator(); iterator.hasNext();)
		{
			Data addedface = (Data) iterator.next();
			Collection parentids = new ArrayList();
			Data startdata = addedface; 
			getMediaArchive().saveData("faceembedding", addedface);
			while( startdata != null)
			{
				parentids.add(startdata.getId());
				String parent = startdata.get("parentembeddingid");
				startdata = getMediaArchive().getCachedData("faceembedding", parent);
			}
			addedface.setValue("parentids",parentids);
			
			getMediaArchive().saveData("faceembedding", addedface);
		}
	}
	
	public boolean compareVectors(double[] inputVector, double[] inCompareVector, double cutoff)
	{
		//Magnitude
		double queryVectorNorm = 0.0;
        // compute query inputVector norm once
        for (double v : inputVector) {
            queryVectorNorm += v * v;
        }
        double magnitude =  Math.sqrt(queryVectorNorm);
		
        double finalscore = 0d;
        //Compare
        double docVectorNorm = 0.0f;
        double score = 0;
        for (int i = 0; i < inputVector.length; i++) 
        {
            // doc inputVector norm
            docVectorNorm += inCompareVector[i]*inCompareVector[i];  //This is cosine stuff
            // dot product
            score += inCompareVector[i] * inputVector[i];
        }
        // cosine similarity score
        if (docVectorNorm == 0 || magnitude == 0)
        {
        	finalscore = 0f;
        }
        else 
        {
        	finalscore = score / (Math.sqrt(docVectorNorm) * magnitude);
        }
        if( finalscore < cutoff )
        {
        	return false;
        }
        return true;
	}
	/**
	 * 
	 * { 
    "results": [
        {
            "embedding": [
                -0.8517391681671143,
                1.6998907327651978,
                           0.8266710042953491,/...
                -0.03634177893400192
            ],
            "face_confidence": 0.9,
            "facial_area": {
                "h": 836,
                "left_eye": [
                    1684,
                    801
                ],
                "right_eye": [
                    1363,
                    801
                ],
                "w": 836,
                "x": 1103,
                "y": 475
            }
        }
    ]
	 * */
	protected List<Data> makeDataForEachFace(Searcher facedb, Asset inAsset,double timecodestart, ContentItem inInput, List<Map> inJsonOfFaces) throws Exception
	{
		if( inJsonOfFaces.isEmpty())
		{
			return null;
		}
		double facedetect_detect_confidence = .999D;
		String detectvalue = getMediaArchive().getCatalogSettingValue("facedetect_detect_confidence");
		if( detectvalue != null)
		{
			facedetect_detect_confidence = Double.parseDouble(detectvalue);
		}

		int inputw = 0;
		int inputh = 0;
		
		if(inInput.getName().endsWith("webp") )
		{
			Dimension size = getImageDimensionWebP(inInput.getInputStream());
			if(size == null) 
			{
				//This is a bug... That is not the real image size
				throw new OpenEditException("Can't get image size from Webp: " + inInput.getPath());
			}
			else
			{
				inputw = (int)Math.round( size.getWidth() );
				inputh = (int)Math.round( size.getHeight() );
			}
		}	
		else
		{
			File inputfile = new File( inInput.getAbsolutePath() );
			Dimension size = getImageDimensionImageIO(inputfile);
			inputw = (int)Math.round( size.getWidth() );
			inputh = (int)Math.round( size.getHeight() );
		}       
        int minfacesize = 200; //was 450
		
		String minumfaceimagesize = getMediaArchive().getCatalogSettingValue("facedetect_minimum_face_size");
		if(minumfaceimagesize != null) 
		{
			minfacesize = Integer.parseInt(minumfaceimagesize);
		}
		if (inJsonOfFaces.size() == 1) {
			minfacesize = minfacesize - 100;
		}
		
//		double boxp = facejson.getDouble("face_confidence");
//		if( boxp < facedetect_detect_confidence)
//		{
//			log.info("Low probability of found face (" + boxp  + "<"+ facedetect_detect_confidence+"): " + inInput.getPath());
//			continue;
//		}
		List<Data> tosave = new ArrayList();

		for (Iterator iterator = inJsonOfFaces.iterator(); iterator.hasNext();)
		{
			Map facejson = (Map) iterator.next();  //Each person in the picrture

			ValuesMap box = new ValuesMap((Map)facejson.get("facial_area"));
//			
			Collection left = box.getValues("left_eye");
			Collection right = box.getValues("right_eye");
			
			if( left == null || right == null)
			{
				log.info("Skipping, Eyes are required  " + inAsset.getName() );
				continue;
			}
			Double confidence = (Double)facejson.get("face_confidence");  //This is useless
			if( confidence < .94D)
			{
				log.info("Invalid face  " + inAsset.getName() + " and confidence " + inAsset.getName() );
				continue;
			}
//			if( similarembedding != null)
//			{
//				//TODO: copy the profile field
//			}
			//Save to DB
			double h = box.getInteger("h");
			double w  = box.getInteger("w");
			if( h < minfacesize)
			{
				log.info("Not enough data, small face detected assetid:" + inAsset.getId()+ " w:" + w + " h:" + h + " Min face size: " + minfacesize);
				continue;
			}
			JSONArray collection = (JSONArray)facejson.get("embedding");
			
			double[] vector = collectDoubles(collection);
			String encoded = encodeDoubles(vector);

			String jsontext = collection.toJSONString();
			
			MultiValued addedface = null;
			
			Collection others = facedb.query().exact("assetid",inAsset).search();
			for (Iterator iterator2 = others.iterator(); iterator2.hasNext();)
			{
				MultiValued existing = (MultiValued) iterator2.next();
				String otherencoded = existing.get("facedatajson"); //Manually done because Search did not work
				if(otherencoded != null && otherencoded.equals(jsontext) )
				{
					addedface = existing;
					break;
				}
			}
			if( addedface == null)
			{
				addedface = (MultiValued)facedb.createNewData(); //TODIO: Search by Vector first so we dont lose assignments
			}
			addedface.setValue("face_confidence", confidence );
			addedface.setValue("facedata",encoded);
			addedface.setValue("facedatajson",jsontext); 
			addedface.setValue("facedatadoubles",vector); //Not saved to index
			addedface.setValue("assetid",inAsset);
			addedface.setValue("left_eye", left );
			addedface.setValue("right_eye", right);

			int assetwidth = getMediaArchive().getRealImageWidth(inAsset); 
			int assetheight = getMediaArchive().getRealImageHeight(inAsset); 
			addedface.setValue("originalwidth",assetwidth);
			addedface.setValue("originalheight",assetheight);
			
			double scale = MathUtils.divide(assetwidth , inputw);//Scale up to orginal image sizes
			double x = box.getInteger("x");
			double y = box.getInteger("y");
			x = x * scale;
			y = y * scale;
			w = w * scale;
			h = h * scale;
			addedface.setValue("locationx",Math.round(x));
			addedface.setValue("locationy",Math.round(y));
			addedface.setValue("locationw",Math.round(w));
			addedface.setValue("locationh",Math.round(h));
			addedface.setValue("timecodestart",timecodestart);

			//TODO: Make sure this is not already in there. For debug purposes
			double[] embedding = collectDoubles((Collection)facejson.get("embedding"));
			SearchQuery query = facedb.createSearchQuery();
			query.addVector("facedata", embedding, getVectorScoreLimit() );
			HitTracker results = facedb.search(query); //Only search Limited list
			//if I find myself then dont save again
			
			//TODO: Decode the numbers and see if this is already in there so we dont make duplicates?
			
			if( !results.isEmpty() )
			{
				Data similarembedding = findSimilar(facedb, addedface, inAsset, results); //Pass in how similar
				if( similarembedding  != null)
				{
					addedface.setValue("parentembeddingid",similarembedding.getId());
					String parentassetid = similarembedding.get("assetid");
					addedface.setValue("parentassetid",parentassetid);
					Asset parentasset = getMediaArchive().getAsset(parentassetid);
					log.info("Conected between child " + inAsset.getName() + " and parent " + parentasset.getName() );
				}
			}
			tosave.add(addedface);
		}
		return tosave;
	}

	protected Data findSimilar(Searcher facedb, MultiValued inFaceEmbbed, Asset inAsset, HitTracker inResults)
	{
		Integer sourceh = inFaceEmbbed.getInt("locationh");
		if( sourceh < 300 )  //Dont link to small image no matter what
		{
			return null;
		}
		for (Iterator iterator = inResults.iterator(); iterator.hasNext();)
		{
			MultiValued hit = (MultiValued) iterator.next();
			String assetid = hit.get("assetid");
			if( assetid == null || !assetid.equals(inAsset.getId()) )
			{
				//save this as the parent
				Integer h = hit.getInt("locationh");
				if( h > 300 )  //Dont link to small image no matter what
				{
					Double score = hit.getDouble("_score");
					inFaceEmbbed.setValue("parentscore", score);
					return hit;
				}
			}
		}
		//These are scored only if they have the right number and type. doubles
		//exclude myself
		
		//log.info(results);
		//Search the DB for most similar match
//		"query": {
//		       "function_score": {
//		         "boost_mode": "replace",
//		         "script_score": {
//		           "lang": "knn",
//		           "params": {
//		             "cosine": false,
//		             "field": "embedding_vector",
//		             "vector": [
//		               -0.09217305481433868, 0.010635560378432274, -0.02878434956073761, 0.06988169997930527, 0.1273992955684662, -0.023723633959889412, 0.
		
		return null;
	}
	
	
	public float[] collectFloats(Collection vector) 
	{
		float[] floats = new float[vector.size()];
		int i = 0;
		for (Iterator iterator = vector.iterator(); iterator.hasNext();)
		{
			Object floatobj = iterator.next();
			float f;
			if( floatobj instanceof Float)
			{
				f = (Float)floatobj;
			}
			else
			{
				f = Float.parseFloat(floatobj.toString());
			}
			floats[i++] = f;
		}
		return floats;
	}
	 //Used for saving data
	public static String encodeFloats(float[] vector)
	{
		final int capacity = Float.BYTES * vector.length;
	    final ByteBuffer bb = ByteBuffer.allocate(capacity);
	    for (float v : vector) {
	        bb.putFloat(v);
	    }
	    bb.rewind();
	    final ByteBuffer encodedBB = Base64.getEncoder().encode(bb);

	    return new String(encodedBB.array());
	}

	public double[] collectDoubles(Collection vector) 
	{
		double[] floats = new double[vector.size()];
		int i = 0;
		for (Iterator iterator = vector.iterator(); iterator.hasNext();)
		{
			Object floatobj = iterator.next();
			double f;
			if( floatobj instanceof Double)
			{
				f = (Double)floatobj;
			}
			else
			{
				f = Double.parseDouble(floatobj.toString());
			}
			floats[i++] = f;
		}
		return floats;
	}
	 //Used for saving data
	public static String encodeDoubles(double[] vector)
	{
		final int capacity = Double.BYTES * vector.length;
	    final ByteBuffer bb = ByteBuffer.allocate(capacity);
	    for (double v : vector) {
	        bb.putDouble(v);
	    }
	    bb.rewind();
	    final ByteBuffer encodedBB = Base64.getEncoder().encode(bb);

	    return new String(encodedBB.array());
	}

//	    public static void main(String[] args) {
//	        float[] vector = new float[] {
//	            -0.09217305481433868f, 0.010635560378432274f, -0.02878434956073761f, 0.06988169997930527f
//	        };
//	        String base64 = encode(vector);
//	        System.out.println(base64);
//	    }
	public Collection collectFaceBoxesAllPeople(Data inAsset)
	{
		//TODO Add Search type for finder
		
		Searcher searcher = getMediaArchive().getSearcherManager().getSearcher("system/facedb","faceembedding");
		HitTracker allthepeopleinasset = searcher.query().exact("assetid",inAsset).search();

		Collection boxes = new ArrayList();
		
		for (Iterator iterator = allthepeopleinasset.iterator(); iterator.hasNext();)
		{
			MultiValued embedding = (MultiValued) iterator.next(); //One person
			
			Data entityperson = loadPersonOfEmbedding(embedding);
			FaceBox box = makeBox(embedding, entityperson);
			boxes.add(box);
		}
		return boxes;	
	}

	public Data loadPersonOfEmbedding(MultiValued embedding)
	{
		Searcher searcher = getMediaArchive().getSearcherManager().getSearcher("system/facedb","faceembedding");

		String entitypersonid = embedding.get("entityperson");
		if( entitypersonid == null)
		{
			entitypersonid = (String)getMediaArchive().getCacheManager().get("facepersonlookup",embedding.getId());
			if( entitypersonid == null )
			{
				Collection parentids = embedding.getValues("parentids");
				if( parentids != null && !parentids.isEmpty() )
				{
					HitTracker personlookup = searcher.query().orgroup("parentids",parentids).search();
					Collection<String> peopleids = personlookup.collectValues("entityperson");
					if( !peopleids.isEmpty() )
					{
						entitypersonid = peopleids.iterator().next();
					}
				}
				if( entitypersonid != null)
				{
					getMediaArchive().getCacheManager().put("facepersonlookup",embedding.getId(),entitypersonid);
				}
			}
		}
		Data entityperson = getMediaArchive().getCachedData("entityperson", entitypersonid); //Might be null
		return entityperson;
	}
	
	private void uploadAProfile(Map faceprofile, long timecodestart,ContentItem originalImgage, Asset inAsset, String groupId ) throws Exception
	{
		int x = (Integer) faceprofile.get("locationx");
		int y = (Integer) faceprofile.get("locationy");
		int w = (Integer) faceprofile.get("locationw");
		int h = (Integer) faceprofile.get("locationh");
 
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
			throw new OpenEditException("No server set " );
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
		String filename = getMediaArchive().generatedOutputName(inAsset,"image1900x1080");
		ConvertInstructions instructions = manager.createInstructions(inAsset, filename);
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

	protected List<Map> findFaces(Asset inAsset, ContentItem inItem) throws Exception
	{
		//Scan via REST and get faces
		//1. Take image and scan it for faces https://github.com/exadel-inc/CompreFace/blob/master/docs/Rest-API-description.md#recognize-faces-from-a-given-image
				
		JSONObject tosendparams = new JSONObject();
		//tosendparams.put("face_plugins","detector");
		
		tosendparams.put("model_name","Facenet512");
		tosendparams.put("detector_backend","retinaface");
		//tosendparams.put("img","http://localhost:8080" + inUrl);
		
		
		String base64 = inputStreamToBase64(inItem.getInputStream());
		
		String mime = "image/webp";
		if( inItem.getName().endsWith("jpg"))
		{
			mime = "image/jpeg";
		}
		String tosend = "data:" + mime + ";charset=utf-8;base64, " + base64;
		tosendparams.put("img",tosend);
		
		//tosendparams.put("img","https://cinecraft.entermediadb.org/cinecraft/mediadb/services/module/asset/generate/Projects/Pure%20Dental%20Group/Published/christopher-campbell-rDEOVtE7vOs-unsplash.jpg/image3000x3000.jpg/christopher-campbell-rDEOVtE7vOs-unsplashL.webp");
		//tosendparams.put("img","https://cinecraft.entermediadb.org/cinecraft/mediadb/services/module/asset/generate/Projects/A%20Dream%20of%20Waves/pexels-alohaphotostudio-8836645.jpg/image3000x3000.webp/pexels-alohaphotostudio-8836645L.webp");
		CloseableHttpResponse resp = null;
		String url = getMediaArchive().getCatalogSettingValue("faceprofileserver");
		if( url == null)
		{
			log.error("No faceprofileserver URL configured" );
			return null;
			//url = "http://localhost:8000";
		}
		long start = System.currentTimeMillis();
		log.debug("Facial Profile Detection sending " + inAsset.getName() );
		resp = getSharedConnection().sharedPostWithJson(url + "/represent",tosendparams);
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
		
		JSONArray results = (JSONArray)json.get("results");
		
		log.info((System.currentTimeMillis() - start) + "ms face detection for asset: "+ inAsset.getId() + " " + inAsset.getName() + " Found: " + results.size());
		
		return results;
	}
	
	public static String inputStreamToBase64(InputStream is) throws Exception {
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    byte[] buffer = new byte[8192];
	    int bytesRead;
	    while ((bytesRead = is.read(buffer)) != -1) {
	        os.write(buffer, 0, bytesRead);
	    }
	    is.close();
	    byte[] imageBytes = os.toByteArray();
	    //= new String(imageBytes);
	    //String text = Base64.getEncoder().encodeToString(imageBytes);
	    String text = Base64.getEncoder().encodeToString(imageBytes);
	    return text;
	}

	
	/**
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
				
				Double x = values.getDouble("locationx");
				Double y = values.getDouble("locationy");
				Double w = values.getDouble("locationw");
				Double h = values.getDouble("locationh");
				Double inputwidth = values.getDouble("inputwidth");
				
				if (x == null || y == null || w == null || h == null)
				{
					continue;
				}
				
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
	*/

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
    	
    	try
    	{
    		DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
    		
    		byte[] header = new byte[12];
            dis.readFully(header);
	        
	        if (!new String(header, 0, 4).equals("RIFF") || !new String(header, 8, 4).equals("WEBP")) 
	        {
	        	return null;
	        }	        	
	        
	        
	        while (true) {
	            byte[] chunkHeader = new byte[8];
	            int read = dis.read(chunkHeader);
	            if (read < 8) break;

	            String chunkType = new String(chunkHeader, 0, 4);
	            int chunkSize = ByteBuffer.wrap(chunkHeader, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();

	            byte[] chunkData = new byte[chunkSize + (chunkSize % 2)]; // padding if odd
	            dis.readFully(chunkData);

	            switch (chunkType) {
	                case "VP8X":
	                    return parseVP8X(chunkData);
	                case "VP8 ":
	                    return parseVP8(chunkData);
	                case "VP8L":
	                    return parseVP8L(chunkData);
	            }
	        }
	        
	        return null;
    	}
    	finally
    	{
    		FileUtils.safeClose(is);
    	}
    }
    
    private static java.awt.Dimension parseVP8X(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(4);
        int width = ((buffer.get() & 0xFF) | ((buffer.get() & 0xFF) << 8) | ((buffer.get() & 0xFF) << 16)) + 1;
        int height = ((buffer.get() & 0xFF) | ((buffer.get() & 0xFF) << 8) | ((buffer.get() & 0xFF) << 16)) + 1;
        return new Dimension(width, height);
    }

    private static java.awt.Dimension parseVP8(byte[] data) { 
        int width = ((data[6] & 0xFF) | ((data[7] & 0xFF) << 8)) & 0x3FFF;
        int height = ((data[8] & 0xFF) | ((data[9] & 0xFF) << 8)) & 0x3FFF;
        return new Dimension(width, height);
    }

    private static java.awt.Dimension parseVP8L(byte[] data) { 
        int b0 = data[1] & 0xFF;
        int b1 = data[2] & 0xFF;
        int b2 = data[3] & 0xFF;
        int b3 = data[4] & 0xFF;
        int width = ((b1 & 0x3F) << 8 | b0) + 1;
        int height = (((b3 & 0x0F) << 10) | (b2 << 2) | ((b1 & 0xC0) >> 6)) + 1;
        return new Dimension(width, height);
    }

	public Collection<FaceBox> viewAllRelatedFaces(String inFaceembeddedid)
	{
		MultiValued startdata = (MultiValued)getMediaArchive().getCachedData("faceembedding", inFaceembeddedid);
		
		//Get all parents 
//		Collection parentids = new ArrayList();
//		while( startdata != null)
//		{
//			parentids.add(startdata.getId());
//			String parent = startdata.get("parentembeddingid");
//			startdata = getMediaArchive().getCachedData("faceembedding", parent);
//		}
//		if( parentids.isEmpty() )
//		{
//			return null;
//		}
		Collection parentids = startdata.getValues("parentids");
		//Search all children
		HitTracker allthepeopleinasset = getMediaArchive().getSearcherManager().getSearcher("system/facedb","faceembedding").query().orgroup("parentids", parentids).search();
		
		//Look for a personid anyplace
		Collection<String> person = allthepeopleinasset.collectValues("entityperson");
		Collection allids = allthepeopleinasset.collectValues("id");
		Data entityperson = null;
		if( !person.isEmpty() )
		{
			String entitypersonid  = null;
			entitypersonid = person.iterator().next();
			if( entitypersonid != null)
			{
				entityperson = getMediaArchive().getCachedData("entityperson", entitypersonid);
			}
		}
		//What happens is another person is matched? We should have never allowed that in the UI
		
		//Make boxes?
		Collection<FaceBox> boxes = new ArrayList();

		for (Iterator iterator = allthepeopleinasset.iterator(); iterator.hasNext();)
		{
			MultiValued embedding = (MultiValued) iterator.next(); //One person
			FaceBox box = makeBox(embedding, entityperson);
			boxes.add(box);
		}
		
		if( entityperson != null)
		{
			HitTracker morepeople = getMediaArchive().getSearcherManager().getSearcher("system/facedb","faceembedding").query().exact("entityperson", entityperson).search();
			for (Iterator iterator = morepeople.iterator(); iterator.hasNext();)
			{
				MultiValued embedding = (MultiValued) iterator.next(); //One person
				if( !allids.contains(embedding.getId() ) )
				{
					FaceBox box = makeBox(embedding, entityperson);
					boxes.add(box);
				}
			}
		}

		
		return boxes;		
	}

	protected FaceBox makeBox(MultiValued inEmbedding, Data inPerson)
	{
		FaceBox box = new FaceBox();
		box.setEmbeddedData(inEmbedding);
		box.setPerson(inPerson);
		int x = inEmbedding.getInt("locationx");
		int y = inEmbedding.getInt("locationy");
		int w = inEmbedding.getInt("locationw");
		int h = inEmbedding.getInt("locationh");
		Integer[] scaledxy = new Integer[] { x, y, w , h};
		List<Integer> points = Arrays.asList(scaledxy);
		box.setBoxArea(points);

		if( inEmbedding.get("timecodestart") != null )
		{
			double seconds = MathUtils.divide( inEmbedding.get("timecodestart").toString(),"1000");
			box.setTimecodeStartSeconds(seconds);
		}
		return box;
	}

	public void rescanAsset(Asset inAsset)
	{
		Searcher faceembeddingsearcher = getMediaArchive().getSearcherManager().getSearcher("system/facedb","faceembedding");
		Collection others = faceembeddingsearcher.query().exact("assetid",inAsset).search();
		
		faceembeddingsearcher.deleteAll(others, null);
		inAsset.setValue("facescancomplete","false");
		inAsset.setValue("facescanerror","false");
		getMediaArchive().saveAsset(inAsset);
		
		getMediaArchive().fireSharedMediaEvent("asset/facescan");
		//extractFaces(inAsset);
		
	}

	
}
