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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.knn.KMeansIndexer;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.scripts.ScriptLogger;
import org.entermediadb.video.Block;
import org.entermediadb.video.Timeline;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.cache.CacheManager;
import org.openedit.data.Searcher;
import org.openedit.data.ValuesMap;
import org.openedit.hittracker.HitTracker;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.FileUtils;
import org.openedit.util.MathUtils;


public class FaceProfileManager extends InformaticsProcessor implements CatalogEnabled
{
	private static final Log log = LogFactory.getLog(FaceProfileManager.class);

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
	
	protected MediaArchive getMediaArchive()
	{
		return (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
	}
	
	public FaceScanInstructions createInstructions()
	{
		FaceScanInstructions instructions = new FaceScanInstructions();
		
		double facedetect_detect_confidence = .7D;
		String detectvalue = getMediaArchive().getCatalogSettingValue("facedetect_detect_confidence");
		if( detectvalue != null)
		{
			facedetect_detect_confidence = Double.parseDouble(detectvalue);
		}
		instructions.setConfidenceLimit(facedetect_detect_confidence);

		int minfacesize = 250; //was 450
		
		String minumfaceimagesize = getMediaArchive().getCatalogSettingValue("facedetect_minimum_face_size");
		if(minumfaceimagesize != null) 
		{
			minfacesize = Integer.parseInt(minumfaceimagesize);
		}
		instructions.setMinimumFaceSize(minfacesize);
		return instructions;
	}

	public int extractFaces(ScriptLogger inLog, FaceScanInstructions instructions , Collection<MultiValued> inAssets)  //Page of assets
	{
//			String url = getMediaArchive().getCatalogSettingValue("faceprofileserver");
//			if( url == null)
//			{
//				log.error("No face server configured");
//				return 0;
//			}

			HitTracker existingfaces = getMediaArchive().query("faceembedding").orgroup("assetid", inAssets).search();
			existingfaces.enableBulkOperations();

			HashMap<String,Collection<MultiValued>> byassetid = new HashMap(existingfaces.size());
			
			//log.error("Loading giant list of assetid");
			for (Iterator iterator = existingfaces.iterator(); iterator.hasNext();)
			{
				MultiValued face = (MultiValued) iterator.next();
				Collection<MultiValued> existing =  byassetid.get( face.get("assetid"));
				if( existing == null)
				{
					existing = new ArrayList(2);
				}
				existing.add(face);
				byassetid.put(face.get("assetid"),existing);
			}
			
			instructions.setExistingFacesByAssetId(byassetid);

			List<MultiValued> foundfacestosave = new ArrayList();
			List<Data> tosave = new ArrayList();

			for (Iterator iterator = inAssets.iterator(); iterator.hasNext();)
			{
				Asset asset = (Asset) iterator.next();	

				asset.setValue("facescancomplete", true); //ToDo: Remove this and relay in LLM Booleans
				asset.setValue("facescanerror", false);

				if( instructions.isSkipExistingFaces() )  //This is the default, but when rescanning one asset dont skip
				{
					String assetid = asset.getId();
					if( instructions.getExistingFacesByAssetId().containsKey(assetid ) )
					{
						log.error("Skipping, Already have assetid " + assetid);
						continue;
					}		
				}	
				try
				{
					asset.setValue("facehasprofile",false);
					extractFaces(inLog, instructions, asset,foundfacestosave);
				}
				catch( Throwable ex)
				{
					log.error("Marking Error on one asset: " + asset + " error:" + ex);
					asset.setValue("facescanerror",true);
					//throw new OpenEditException("Error on: " + inAssets.size(),ex);
				}
			}  
			
			getMediaArchive().saveData("faceembedding",foundfacestosave);
			
			log.info("Saved Assets " + tosave.size() + " added faces:  " + foundfacestosave.size());
			if( inLog != null)
			{	
				inLog.info(" Found " + foundfacestosave.size() + " faces");
			}
			
			getKMeansIndexer().checkReinit();
			
			if( !foundfacestosave.isEmpty() )
			{
				clearAllCaches();
			}
			
			return foundfacestosave.size();
	}
	

	protected int extractFaces(ScriptLogger inLog, FaceScanInstructions instructions, Asset inAsset, List<MultiValued> inFoundfaces) throws Exception
	{
		
		String type = getMediaArchive().getMediaRenderType(inAsset);
		
		inAsset.setValue("facescancomplete","true");
		
		if (!"image".equalsIgnoreCase(type) && !"video".equalsIgnoreCase(type)  )
		{
			log.info("Skipping non images: " + inAsset.getName() );
			return 0;
		}
		//If its a video then generate all the images and scan them
		
		Searcher faceembeddingsearcher = getMediaArchive().getSearcher("faceembedding");

		if( "image".equalsIgnoreCase(type) && inAsset.getFileFormat()!= null)
		{
			long filesize = inAsset.getLong("filesize");
			
			long imagew = inAsset.getLong("width");
			long imageh = inAsset.getLong("height");
			long imagesize = imagew * imageh;
			
			if(imagesize < 90000) {
				return 0;
			}
			
			String fileformat = inAsset.getFileFormat();
			
			Boolean useoriginal = true;
			if (filesize > 600000)
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
				if( fileformat.equals("jpg") || fileformat.equals("jpeg") || fileformat.equals("webp")) 
				{
					//Its ok
				}
				else
				{
					useoriginal = false;
				}
			}
			
//			ContentItem input = getMediaArchive().getContent("/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image3000x3000.webp");
//			if( !input.exists() )
//			{
//				input = getMediaArchive().getContent("/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image3000x3000.jpg");
//			}
//			if( !input.exists() )
//			{
//				log.error("No such image"); //TODO: Search within videos
//				return false;
//			}
			
			//Send orginal JPEG directly if they are small?
			ContentItem input = null;
			if( useoriginal )
			{
				input = getMediaArchive().getOriginalContent(inAsset);
			}
			else
			{
				String filename = "image3000x3000.webp";
				input = getMediaArchive().getContent("/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/" + filename);

				if( !input.exists() )
				{
					filename = "image3000x3000.jpg";
					input = getMediaArchive().getContent("/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/" + filename);
				}
			}
			if( !input.exists() )
			{
				throw new OpenEditException("Input not available " + input.getPath());
			}

			if(inLog != null)
			{
				inLog.headline("Scanning faces in: " + inAsset.getName());
			}
				
			List<Map> json = findFaces(inAsset, input);
			if(json == null || json.isEmpty()) 
			{
				return 0;
			}
			Collection<MultiValued> moreprofiles = makeDataForEachFace(instructions,faceembeddingsearcher,inAsset,0L,input,json);
			if( moreprofiles != null)
			{
				//saveNewFacesWithParents(moreprofiles);
				inFoundfaces.addAll(moreprofiles);
				
				if( !moreprofiles.isEmpty() )
				{
					inAsset.setValue("facehasprofile",true);
				}
			}
			
		}
		else if( "video".equalsIgnoreCase(type) )
		{
			//Look over them and save the timecode with it
			Collection<MultiValued> allfacesinvideo = findAllFacesInVideo(instructions, inAsset);
			Collection<MultiValued> moreprofiles = combineVideoMatches(allfacesinvideo);
			if(moreprofiles != null)
			{
				//saveNewFacesWithParents(moreprofiles);
				inFoundfaces.addAll(moreprofiles);
			}
//			updateEndTimes(continuelooking,block.getStartOffset()); //Brings them up to date
//			try
//			{
//				continuelooking = combineVideoMatches(continuelooking,more);
//			}
//			catch(IndexOutOfBoundsException ex)
//			{
//				log.info("Issue happened again on " + block.getSeconds());
//				//ignoring...
//			}
		}
		//faceembeddingsearcher.saveAllData(tosave,null);
		return inFoundfaces.size();
	}

	protected Collection<MultiValued> findAllFacesInVideo(FaceScanInstructions instructions, Asset inAsset) throws Exception
	{
		Searcher faceembeddingsearcher = getMediaArchive().getSearcher("faceembedding");

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
		
		FaceScanInstructions videoinstructions = new FaceScanInstructions();
		
		videoinstructions.setExistingFacesByAssetId(instructions.getExistingFacesByAssetId());
		videoinstructions.setConfidenceLimit(instructions.getConfidenceLimit() * .85D);
		videoinstructions.setMinimumFaceSize(instructions.getMinimumFaceSize() * .75D);

		
		Collection<MultiValued> allfacesinvideo = new ArrayList();
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
			if (item == null || !item.exists() )
			{
				//probblem
				log.info("Faceprofile scan, no thumbnail found for : " +inAsset.getId() + " "+ inAsset.getSourcePath());
			}
			else
			{
				List<Map> json = findFaces(inAsset, item);	
				Collection<MultiValued> moreprofiles = makeDataForEachFace(videoinstructions,faceembeddingsearcher,inAsset,block.getSeconds(),item,json);
				if( moreprofiles != null)
				{
					allfacesinvideo.addAll(moreprofiles);
				}
			}
		}
		return allfacesinvideo;
	}

	protected Collection<MultiValued> combineVideoMatches(Collection<MultiValued> inAllfacesinvideo)
	{
		Collection<MultiValued> uniquefaces = new ArrayList();

		Collection<MultiValued> remainingfaces = new ArrayList(inAllfacesinvideo);
		
		for (Iterator iterator = inAllfacesinvideo.iterator(); iterator.hasNext();)
		{
			MultiValued embedded = (MultiValued) iterator.next();
			
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
			List<Double> facedoubles = (List<Double>)embedded.getValue("facedatadoubles");
			
			Collection<Data> remainingfacestmp = new ArrayList(remainingfaces);
			for (Iterator iterator2 = remainingfacestmp.iterator(); iterator2.hasNext();)
			{
				Data otherface = (Data) iterator2.next();
				List<Double> othervalues = (List<Double>)otherface.getValue("facedatadoubles");
				boolean same = getKMeansIndexer().compareVectors(facedoubles, othervalues, getVectorScoreLimit() + 0.3D ); //Be more flexible
				if( same )
				{
					//TODO: Keep larger facebox
					//TODO: Assign the endtime for the last time we saw the person
					remainingfaces.remove(otherface);
				}
			}			
		}
		return uniquefaces;
	}

	private double getVectorScoreLimit()
	{
		double similaritycheck = .5D;
		String value = getMediaArchive().getCatalogSettingValue("facedetect_profile_confidence");
		if( value != null)
		{
			similaritycheck = Double.parseDouble(value);
		}
		return similaritycheck;
	}

	public void reinitClusters(ScriptLogger logger)
	{
		if( true )
		{
		//	return;
		}
		
		logger.info(new Date() +  " reinitNodes Start reinit ");
		

		clearAllCaches();
		
		
		getKMeansIndexer().reinitClusters(logger);
		logger.info(new Date() +  " reinitNodes Completed ");

	}

	public KMeansIndexer getKMeansIndexer()
	{
		KMeansIndexer fieldKMeansIndexer = (KMeansIndexer)getMediaArchive().getCacheManager().get("facedetect","facedetectindexer");
		if (fieldKMeansIndexer == null)
		{
			fieldKMeansIndexer = (KMeansIndexer)getModuleManager().getBean(getCatalogId(),"kMeansIndexer",false);
			MultiValued settings = (MultiValued)getMediaArchive().getData("informatics","facedetect");
			fieldKMeansIndexer.loadSettings(settings);
			//fieldKMeansIndexer.setSearchType("faceembedding");
			fieldKMeansIndexer.setRandomSortBy("face_confidence");
			fieldKMeansIndexer.setFieldSaveVector("facedatadoubles");//vectorarray facedatadoubles
			getMediaArchive().getCacheManager().put("facedetect","facedetectindexer",fieldKMeansIndexer);
		}
		return fieldKMeansIndexer;
	}
	
	public void disableFaceBox(User inUser, MultiValued inEmbedding)
	{
		inEmbedding.setValue("isremoved", true);
		inEmbedding.setValue("removedby", inUser.getId());
		getMediaArchive().saveData("faceembedding",inEmbedding);
		
		getMediaArchive().getCacheManager().remove("faceboxes",inEmbedding.get("assetid"));
		
		//reconnect parents and children and other faces
		
		Collection<FaceBox> boxes = viewAllRelatedFaces(inEmbedding.getId());
		
		if( boxes == null || boxes.isEmpty())
		{
			getMediaArchive().saveData("faceembedding",inEmbedding);
			return;
		}
		getMediaArchive().saveData("faceembedding",inEmbedding);
//		List<MultiValued> somerecords = new ArrayList();
//		
//		for (Iterator iterator = boxes.iterator(); iterator.hasNext();)
//		{
//			FaceBox box = (FaceBox) iterator.next();
//			somerecords.add(box.getEmbeddedData());
//		}
//		//fixSomeNewParents(somerecords);
		
	}
	

	protected List<MultiValued> makeDataForEachFace(FaceScanInstructions instructions, Searcher facedb, Asset inAsset,double timecodestart, ContentItem inInput, List<Map> inJsonOfFaces) throws Exception
	{
		if( inJsonOfFaces.isEmpty())
		{
			return null;
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
			Dimension size = getImageDimensionImageIO(inputfile);  //This needed for originals?
			inputw = (int)Math.round( size.getWidth() );
			inputh = (int)Math.round( size.getHeight() );
		}       
 //		if (inJsonOfFaces.size() == 1) {
//			minfacesize = minfacesize - 100;
//		}
	
		List<MultiValued> tosave = new ArrayList();

		for (Iterator iterator = inJsonOfFaces.iterator(); iterator.hasNext();)
		{
			Map facejson = (Map) iterator.next();  //Each person in the picrture

			ValuesMap box = new ValuesMap((Map)facejson.get("facial_area"));
//			
//			Collection left = box.getValues("left_eye");
//			Collection right = box.getValues("right_eye");
			
			Double confidence = (Double)facejson.get("face_confidence");  //This is useless
			// if( left == null || right == null)
			// {
			// 	log.info("Eyes are required, confidence = " + confidence  + " file: " + inAsset.getName());
			// 	continue;
			// }
			if( confidence < instructions.getConfidenceLimit())
			{
				log.info("Not enough confidence for " + confidence + " -> " + inAsset.getName() );
				continue;
			}
//			if( similarembedding != null)
//			{
//				//TODO: copy the profile field
//			}
			//Save to DB
			double h = box.getDouble("h");
			double w  = box.getDouble("w");
			if( h < instructions.getMinimumFaceSize())
			{
				log.info("small face w:" + w + " h:" + h + " Min face size: " + instructions.getMinimumFaceSize() + " " + inAsset.getName());
				continue;
			}
			JSONArray collection = (JSONArray)facejson.get("embedding");
			
			List<Double> vector = getKMeansIndexer().collectDoubles(collection);
			//String encoded = encodeDoubles(vector);

//			String jsontext = collection.toJSONString();
			
			MultiValued addedface = null;
			
			if( instructions.getExistingFacesByAssetId() != null) 
			{
				String assetid = inAsset.getId();
				Collection<MultiValued> existingfaces = instructions.getExistingFacesByAssetId().get(inAsset.getId());
				if( existingfaces != null)
				{
					for (Iterator iterator2 = existingfaces.iterator(); iterator2.hasNext();)
					{
						MultiValued existing = (MultiValued) iterator2.next();
						List<Double> othervalues = (List<Double>)existing.getValue("facedatadoubles"); //Manually done
						if(othervalues != null && othervalues.equals(vector) )
						{
							addedface = existing;
							log.info("Found existing embedding for asset " + inAsset.getName() ); 
							break;
						}
					}	
				}
			}
			if( addedface == null)
			{
				addedface = (MultiValued)facedb.createNewData(); //TODIO: Search by Vector first so we dont lose assignments
			}
			addedface.setValue("face_confidence", confidence );
			//addedface.setValue("facedata",encoded);
			//addedface.setValue("facedatajson",jsontext); 
			addedface.setValue("facedatadoubles",vector); //Not saved to index
			addedface.setValue("assetid",inAsset.getId() );
//			addedface.setValue("left_eye", left );
//			addedface.setValue("right_eye", right);

			int assetwidth = getMediaArchive().getRealImageWidth(inAsset); 
			int assetheight = getMediaArchive().getRealImageHeight(inAsset); 
			addedface.setValue("originalwidth",assetwidth);
			addedface.setValue("originalheight",assetheight);
			
			double scale = MathUtils.divide(assetwidth , inputw);//Scale up to orginal image sizes
			double x = box.getDouble("x");
			double y = box.getDouble("y");
			x = x * scale;
			y = y * scale;
			w = w * scale;
			h = h * scale;
			addedface.setValue("locationx",Math.round(x));
			addedface.setValue("locationy",Math.round(y));
			addedface.setValue("locationw",Math.round(w));
			addedface.setValue("locationh",Math.round(h));
			addedface.setValue("timecodestart",timecodestart);
			
			getKMeansIndexer().setCentroids(addedface);
			
			Collection centroids = addedface.getValues("nearbycentroidids");
			
			int size = 0;
			if( centroids != null)
			{
				size = centroids.size();
			}
					
			log.info("added face with confidence: " + confidence + " centroids count: " + size  + " " + inAsset.getName());
			tosave.add(addedface);
		}
		return tosave;
	}

	public Data addFaceEmbedded(User inUser, Asset inAsset, List<Integer> inBox )
	{
		Searcher faceembeddingsearcher = getMediaArchive().getSearcher("faceembedding");
		Data addedface = faceembeddingsearcher.createNewData();
		
		addedface.setValue("assetid",inAsset.getId());

		int assetwidth = getMediaArchive().getRealImageWidth(inAsset); 
		int assetheight = getMediaArchive().getRealImageHeight(inAsset); 
		addedface.setValue("originalwidth",assetwidth);
		addedface.setValue("originalheight",assetheight);
		
		addedface.setValue("locationx",inBox.get(0));
		addedface.setValue("locationy",inBox.get(1));
		addedface.setValue("locationw",inBox.get(2));
		addedface.setValue("locationh",inBox.get(3));

		addedface.setValue("owner",inUser.getId());

		faceembeddingsearcher.saveData(addedface, inUser);
		
		getMediaArchive().getCacheManager().remove("faceboxes",inAsset.getId());
		
		return addedface;
	}
//	
//	public Data findSimilar(MultiValued inFace)
//	{
//		//log.info("Checking for parents for " + inFace.get("locationh") );
//		Searcher fsearcher = getMediaArchive().getSearcher("faceembedding");
//		HitTracker results = fsearcher.query().between("locationh",299L,(long)Integer.MAX_VALUE).search(); //Cache this?
//		Data found = findSimilar(fsearcher,inFace,inFace.get("assetid"),results);
//		return found;
//	}
//	
	

//	    public static void main(String[] args) {
//	        float[] vector = new float[] {
//	            -0.09217305481433868f, 0.010635560378432274f, -0.02878434956073761f, 0.06988169997930527f
//	        };
//	        String base64 = encode(vector);
//	        System.out.println(base64);
//	    }
	
	/**
	 * This is used in the asset UI to show the face a name
	 */
	public Collection<FaceBox> collectFaceBoxesAllPeople(Data inAsset)
	{
		if (inAsset == null)
		{
			return Collections.EMPTY_LIST;
		}
		Collection<FaceBox> boxes = (Collection<FaceBox>)getMediaArchive().getCacheManager().get("faceboxes", inAsset.getId());
		if( boxes != null )
		{
			return (Collection<FaceBox>)boxes;
		}
		boxes = new ArrayList();
		
		Searcher searcher = getMediaArchive().getSearcher("faceembedding");
		HitTracker allthepeopleinasset = searcher.query().exact("assetid",inAsset).exact("isremoved", false).search();

		for (Iterator iterator = allthepeopleinasset.iterator(); iterator.hasNext();)
		{
			MultiValued embedding = (MultiValued) iterator.next(); //One person
			
			Data entityperson = loadPersonOfEmbedding(embedding);
			FaceBox box = makeBox(embedding, entityperson);
			boxes.add(box);
		}
		getMediaArchive().getCacheManager().put("faceboxes", inAsset.getId(), boxes);
		return boxes;	
	}

	public Data loadPersonOfEmbedding(MultiValued embedding)
	{

		Data entityperson = (Data)getMediaArchive().getCacheManager().get("aifacedetect",embedding.getId());
		if(entityperson == CacheManager.NULLDATA )
		{
			return null;
		}

		String entitypersonid = embedding.get("entityperson");
		if( entitypersonid == null)
		{
			Collection<MultiValued> faces =  getKMeansIndexer().searchNearestItems(embedding);
			for (Iterator iterator = faces.iterator(); iterator.hasNext();)
			{
				MultiValued face = (MultiValued) iterator.next();
				entitypersonid = face.get("entityperson");
				if( entitypersonid != null)
				{
					break;
				}
			}
		}
		if( entitypersonid != null)
		{
			entityperson = getMediaArchive().getCachedData("entityperson", entitypersonid ); //Might be null
		}
		
		if( entityperson == null)
		{
			getMediaArchive().getCacheManager().put("aifacedetect" ,embedding.getId(),CacheManager.NULLDATA);
		}
		else
		{
			getMediaArchive().getCacheManager().put("aifacedetect",embedding.getId(),entityperson);
		}
		
		return entityperson;
	}

	protected ContentItem generateInputFile(Asset inAsset, Block inBlock)
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
		
		String base64 = inputStreamToBase64(inItem.getInputStream());
		
		String mime = "image/webp";
		if( inItem.getName().endsWith("jpg"))
		{
			mime = "image/jpeg";
		}
		String tosend = "data:" + mime + ";charset=utf-8;base64, " + base64;

		JSONObject tosendparams = new JSONObject();
		// tosendparams.put("face_plugins","detector");
		// tosendparams.put("model_name","Buffalo_L"); 
		// tosendparams.put("detector_backend","skip");
		// tosendparams.put("enforce_detection", false);
		// tosendparams.put("img","http://localhost:8080" + inUrl);
		tosendparams.put("img",tosend);

//		long start = System.currentTimeMillis();
		//log.debug("Facial Profile Detection sending " + inAsset.getName() );
		LlmConnection connection = getMediaArchive().getLlmConnection("faceDetect");
		LlmResponse resp = connection.callJson("/face", null, tosendparams);
		
		List<Map> results = (List<Map>) resp.getRawCollection();
		
		//log.info((System.currentTimeMillis() - start) + "ms face detection for asset: "+ inAsset.getId() + " " + inAsset.getName() + " Found: " + results.size());
		
		return results;
	}
	
	protected static String inputStreamToBase64(InputStream is) throws Exception {
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
	 * Gets image dimensions for given file 
	 * @param imgFile image file
	 * @return dimensions of image
	 * @throws IOException if the file is not a known image
	 */
	protected  Dimension getImageDimensionImageIO(File imgFile) throws IOException {
	  int pos = imgFile.getName().lastIndexOf(".");
	  if (pos == -1)
	    throw new IOException("No extension for file: " + imgFile.getAbsolutePath());
	  String suffix = imgFile.getName().substring(pos + 1);
	  Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
	  while(iter.hasNext()) {
	    ImageReader reader = iter.next();
	    ImageInputStream stream = null;
	    try {
	      stream = new FileImageInputStream(imgFile);
	      reader.setInput(stream);
	      int width = reader.getWidth(reader.getMinIndex());
	      int height = reader.getHeight(reader.getMinIndex());
	      return new Dimension(width, height);
	    } catch (IOException e) {
	      log.warn("Error reading: " + imgFile.getAbsolutePath(), e);
	    } finally {
	      reader.dispose();
	      FileUtils.safeClose(stream);
	    }
	  }

	  throw new IOException("Not a known image file: " + imgFile.getAbsolutePath());
	}
	
    protected java.awt.Dimension getImageDimensionWebP(InputStream is) throws IOException {
    	
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
		Collection<FaceBox> faces = viewAllRelatedFaces(startdata);
		return faces;
	}
    /**
     * Used by UI to find all related people in the dialog
     */
	public Collection<FaceBox> viewAllRelatedFaces(MultiValued startdata)
	{
		Collection<FaceBox> boxes = new ArrayList();

		String entitypersonid  = startdata.get("entityperson");

		Data entityperson = null;
		//Search all children
		List<Double> vectorA = (List<Double>)startdata.getValue("facedatadoubles");
		if( vectorA == null )  //Use selected face
		{
			MultiValued newdata = null;
			if(  entitypersonid != null)
			{
				newdata = (MultiValued)getMediaArchive().getSearcher("faceembedding").query().exact("isremoved",false).exists("facedatadoubles").exact("entityperson", entitypersonid).searchOne();
			} 
			if(	newdata == null )
			{
				log.info("No faceembedding found for " + startdata.getId());
				FaceBox box = makeBox(startdata, entityperson);
				boxes.add(box);
				return boxes;
			}
			startdata = newdata; //We have a faceembedding with a vector
		}
		Collection allthepeopleinasset = getKMeansIndexer().searchNearestItems(startdata);
		
		Collection<String> dedupids = new HashSet();
		
		//Look for a personid anyplace
		for (Iterator iterator = allthepeopleinasset.iterator(); iterator.hasNext();)
		{
			MultiValued hit = (MultiValued) iterator.next();
			String foundentitypersonid  = hit.get("entityperson");
			if( foundentitypersonid != null)
			{
				entityperson = getMediaArchive().getCachedData("entityperson", foundentitypersonid);
				if( entityperson != null)
				{
					entitypersonid = foundentitypersonid; //We have a person
					break;
				}
			}
		}
		//What happens is another person is matched? We should have never allowed that in the UI
		for (Iterator iterator = allthepeopleinasset.iterator(); iterator.hasNext();)
		{
			MultiValued embedding = (MultiValued) iterator.next(); //One person
			dedupids.add(embedding.getId());
			FaceBox box = makeBox(embedding, entityperson);
			boxes.add(box);
		}
		
		if( entityperson != null)
		{
			HitTracker morepeople = getMediaArchive().getSearcher("faceembedding").query().exact("isremoved",false).exact("entityperson", entityperson).search();
			for (Iterator iterator = morepeople.iterator(); iterator.hasNext();)
			{
				MultiValued embedding = (MultiValued) iterator.next(); //One person
				if( !dedupids.contains(embedding.getId() ) )
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
			double seconds = inEmbedding.getDouble("timecodestart");
			box.setTimecodeStartSeconds(seconds);
		}
		return box;
	}

	public void rescanAsset(Asset inAsset)
	{
//		Searcher faceembeddingsearcher = getMediaArchive().getSearcher("faceembedding");
//		Collection others = faceembeddingsearcher.query().exact("assetid",inAsset).search();
//
//		faceembeddingsearcher.deleteAll(others, null);
//		inAsset.setValue("facescancomplete","false");
//		inAsset.setValue("facescanerror","false");
//		getMediaArchive().saveAsset(inAsset);
		
		//getMediaArchive().fireSharedMediaEvent("asset/facescan");
		List one = new ArrayList();
		one.add(inAsset);
		
		FaceScanInstructions instructions = createInstructions();
		instructions.setSkipExistingFaces(false); //Will look it up just in case
		instructions.setConfidenceLimit(instructions.getConfidenceLimit() * .75D);
		instructions.setMinimumFaceSize(instructions.getMinimumFaceSize() * .50D);
		
		extractFaces(null, instructions, one);
		getMediaArchive().saveData("asset",inAsset);
		
	}
	
	
	protected void assignPerson(String faceembeddingid, String assetid, String personid, String inUserId)
	{
		if (faceembeddingid != null && personid != null)
		{
			//Should we go disconnect the previous face? 

			MultiValued face = (MultiValued) getMediaArchive().getData("faceembedding",faceembeddingid);
			if (face != null)
			{
				Data personfacelookup = loadFaceMappingWithEnitityPerson(face); //Always update the old one

				if( personfacelookup == null)
				{
					personfacelookup = face;
				}
				String oldpersonid = personfacelookup.get("entityperson");
				
				if (oldpersonid != null)
				{
					Data oldperson = getMediaArchive().getData("entityperson", oldpersonid);
					String previousassetid = oldperson.get("primaryimage");
					if (previousassetid != null && previousassetid.equals(assetid)) 
					{
						oldperson.setValue("primaryimage", null);
						getMediaArchive().saveData("entityperson", oldperson);
					}
				}
				personfacelookup.setValue("entityperson", personid);
				personfacelookup.setValue("assignedby",inUserId );
				personfacelookup.setValue("hasotherfaces",true);
				getMediaArchive().saveData("faceembedding",personfacelookup);
				
				//always reset image
				Data person = getMediaArchive().getData("entityperson", personid);
				person.setValue("primaryimage", assetid);
				getMediaArchive().saveData("entityperson", person);
				
				clearAllCaches();
			}
		}
	}

	protected Data loadFaceMappingWithEnitityPerson(MultiValued embedding)
	{
		String entitypersonid = embedding.get("entityperson");
		if( entitypersonid != null)
		{
			return embedding;
		}
		Collection<MultiValued> faces =  getKMeansIndexer().searchNearestItems(embedding);
		for (Iterator iterator = faces.iterator(); iterator.hasNext();)
		{
			MultiValued face = (MultiValued) iterator.next();
			entitypersonid = face.get("entitypersonid");
			if( entitypersonid != null)
			{
				return face;
			}
		}
		return null;
	}

	
	protected void clearAllCaches()
	{

		getMediaArchive().getCacheManager().clear("aifacedetect"); //Standard cache for this fieldname
		getMediaArchive().getCacheManager().clear("faceboxes"); //All related boxes. TODO: Limit to this record

	}

	@Override
	public void processInformaticsOnAssets(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inAssets)
	{
		Collection validAssets = new ArrayList();
		for (Iterator iterator = inAssets.iterator(); iterator.hasNext();) {
			Asset asset = (Asset) iterator.next();
			if(!asset.getBoolean("facescancomplete"))
			{
				validAssets.add(asset);
			}
		}
		if(validAssets.isEmpty())
		{
			return;
		}
		inLog.headline("Scanning faces in " + validAssets.size() + " assets");
		FaceScanInstructions instructions = createInstructions();
		int found = extractFaces(inLog, instructions, validAssets);
		if(found > 0)
		{
			inLog.info("Found " + found + " faces in " + validAssets.size() + " assets");
		}
		else
		{
			inLog.info("No faces found in " + validAssets.size() + " assets");
		}
 
		
	}

	@Override
	public void processInformaticsOnEntities(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inRecords)
	{
		//Do nothing
		
	}
	
}
