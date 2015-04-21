package conversions.creators;

import java.io.File
import java.util.ArrayList
import java.util.Iterator
import java.util.List

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.creator.BaseImageCreator
import org.openedit.entermedia.creator.ConvertInstructions
import org.openedit.entermedia.creator.ConvertResult
import org.openedit.entermedia.creator.MediaCreator

import com.openedit.OpenEditException
import com.openedit.page.Page
import com.openedit.util.ExecResult;
import com.openedit.util.PathUtilities

import java.awt.Dimension;

public class imagemagickCreator extends BaseImageCreator {
	private static final Log log = LogFactory.getLog(imagemagickCreator.class);
	protected String fieldPathToProfile;

	public String getPathtoProfile(){
		if(fieldPathToProfile == null){
			Page profile = getPageManager().getPage("/system/components/conversions/tinysRGB.icc");
			fieldPathToProfile = profile.getContentItem().getAbsolutePath();
		}
		return fieldPathToProfile;
	}

	public boolean canReadIn(MediaArchive inArchive, String inInput) {
		return !inInput.equals("mp3") || canPreProcess(inArchive, inInput); //Can read in most any input except mp3. Maybe in the future it will look up the album cover
	}

	public ConvertResult convert(MediaArchive inArchive, Asset inAsset, Page inOutFile, ConvertInstructions inStructions) throws OpenEditException {
		if(!inStructions.isForce() && inOutFile.length() > 1 ) {
			ConvertResult result = new ConvertResult();
			result.setOk(true);
			result.setComplete(true);
			return result;
		}

		if(inStructions.isWatermark()) {
			inStructions.setWatermark(false);
			String outputPath = populateOutputPath(inArchive, inStructions); //now does not contain wm
			Page outputPage = getPageManager().getPage(outputPath);
			if(!outputPage.exists())
			{
				createOutput(inArchive, inAsset, outputPage, inStructions); //this will create smaller version
			}
			inStructions.setWatermark(true);
			populateOutputPath(inArchive, inStructions);
			inStructions.setAssetSourcePath(outputPath);
		}
		return createOutput(inArchive, inAsset, inOutFile, inStructions);
	}

	protected ConvertResult createOutput(MediaArchive inArchive, Asset inAsset, Page inOutFile, ConvertInstructions inStructions) {
		ConvertResult result = new ConvertResult();
		String outputpath = inOutFile.getContentItem().getAbsolutePath();
		//if watermarking is set
		if(inStructions.isWatermark())
		{
			Page inputPage = inArchive.getOriginalDocument(inAsset);
			//Page inputPage = getPageManager().getPage(inStructions.getAssetSourcePath());
			if(inputPage == null || !inputPage.exists())
			{
				result.setOk(false);
				return result;
			}
			String fullInputPath = inputPage.getContentItem().getAbsolutePath();
			return applyWaterMark(inArchive, fullInputPath, outputpath, inStructions);
		}

		Page input = null;
		boolean autocreated = false; //If we already have a smaller version we just need to make a copy of that
		String offset = inStructions.getProperty("timeoffset");

		String tmpinput = PathUtilities.extractPageType( inOutFile.getPath() );
		boolean transparent = inStructions.isTransparencyMaintained(tmpinput);
		String ext = inStructions.getInputExtension();
		if( ext == null && input != null)
		{
			ext = PathUtilities.extractPageType( input.getPath() );
		}

		if( ext == null)
		{
			ext = inAsset.getFileFormat();
		}
		if( !transparent  && ( inStructions.getMaxScaledSize() != null && offset == null ) ) //page numbers are 1 based
		{
			String page = null;
			if( inStructions.getPageNumber() > 1 )
			{
				page = "page" + inStructions.getPageNumber();
			}
			else
			{
				page = "";
			}

			Dimension box = inStructions.getMaxScaledSize();
			//			if (input == null && inStructions.getProperty("useinput")!=null)
			//			{
			//				input = getPageManager().getPage("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/"+inStructions.getProperty("useinput") + page + ".jpg");
			//				if( !input.exists()  || input.length() < 2)
			//				{
			//					input = null;
			//				}
			//				else
			//				{
			//					autocreated = true;
			//				}
			//			}
			//			if( input == null &&  box.getWidth() < 300 )
			//			{
			//				input = getPageManager().getPage("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image640x480" + page + ".jpg");
			//				if( !input.exists()  || input.length() < 2)
			//				{
			//					input = null;
			//				}
			//				else
			//				{
			//					autocreated = true;
			//				}
			//			}
			if( input == null && box.getWidth() < 1024 )
			{
				input = getPageManager().getPage("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image1024x768" + page + ".jpg");
				if( input.length() < 2 )
				{
					input = null;
				}
				else
				{
					autocreated = true;
				}
			}
		}

		boolean hascustomthumb = false;
		Page customthumb = null;

		if("png".equals(ext)){
			customthumb = getPageManager().getPage("/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/customthumb.png");

		}	else
		{
			customthumb = getPageManager().getPage("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/customthumb.jpg");
		}
		String filetype = inArchive.getMediaRenderType(inAsset.getFileFormat());
		if(customthumb.exists()){
			hascustomthumb = true;
			if(input == null && !"document".equals(filetype)){
				input = customthumb;
				log.info("Length was ${input.length()}");
				if( input.length() < 2 )
				{
					input = null;
				}
				else
				{
					autocreated = true;
				}
			}
		}



		//get the original inut
		boolean useoriginal = Boolean.parseBoolean(inStructions.get("useoriginalasinput"));
		if( offset != null && input == null)
		{
			input = inArchive.findOriginalMediaByType("video",inAsset);
		}
		if( input == null || useoriginal)
		{
			input = inArchive.findOriginalMediaByType("image",inAsset);
		}
		if( input == null)
		{
			input = inArchive.findOriginalMediaByType("video",inAsset);
		}
		if(input == null){
			if(inStructions.getInputPath() != null){
				input = getPageManager().getPage(inStructions.getInputPath());
			}
		}
		//Look over to see if there is a creator that can do a better job of reading in this type

		MediaCreator preprocessor = getPreProcessor(inArchive, ext);
		Page original = inArchive.getOriginalDocument(inAsset);
		
		if(original.exists() &&  preprocessor != null && !hascustomthumb)
		{
			//This will output a native format. First one wins. it is not a loop.
			String tmppath = preprocessor.populateOutputPath(inArchive, inStructions);
			Page tmpout = getPageManager().getPage(tmppath);
			if( !tmpout.exists() || tmpout.getContentItem().getLength()==0)
			{
				//Create
				ConvertResult tmpresult = preprocessor.convert(inArchive, inAsset, tmpout, inStructions);
				if( !tmpresult.isOk() )
				{
					return tmpresult;
				}
				//					if( !inStructions.isWatermark() && out.getPath().equals(inOutFile.getPath())) //this is the same file we originally wanted
				//					{
				//						//return tmpresult;
				//					}
				if( tmpout.getContentItem().getLength() > 0)
				{
					//cmykpreprocessor returns an xconf
					//so this should be safe since there are probably
					//no other preprocessors that return xconf files
					if ("xconf" != tmpout.getPageType()){
						input = tmpout;
					}
					//					This is only useful for INDD at 1024. to complex to try and optimize
					//					if( input.getPath().equals(inOutFile.getPath()))
					//					{
					//preprosessor took care of the entire file. such as exiftol
					//						result.setOk(true);
					//						return result;
					//					}
				}
				else
				{
					//exifthumbtool probably did not work due to permissions
					//Highlights doesn't have the original - maybe we can revert back to an input if the original doesn't exist?  Or just
					String page = null;
					if( inStructions.getPageNumber() > 1 )
					{
						page = "page" + inStructions.getPageNumber();
					}
					else
					{
						page = "";
					}
					
					input = getPageManager().getPage("/WEB-INF/data" + inArchive.getCatalogHome() + "/generated/" + inAsset.getSourcePath() + "/image1024x768" + page + ".jpg");
					
					result.setError("Prepropessor could not create tmp file");
					result.setOk(false);
					return result;
				}
			}
			else if( input == null)
			{
				//we are looking for a working format to use as input
				
				//cmykpreprocessor returns an xconf
				//so this should be safe since there are probably
				//no other preprocessors that return xconf files
				if ("xconf" != tmpout.getPageType()){
					input = tmpout;
				}
			}
		}
		if( input == null)
		{
			//last chance
			input = inArchive.findOriginalMediaByType("document",inAsset);
		}

		if( input == null || !input.exists())
		{
			//no such original
			result.setOk(false);
			//This sucks,if the orignal is not available or we are waiting for a proxy
			//The fix is to do conversions by the asset like we do for push
			log.debug("input not yet available " + inAsset.getSourcePath() );
			return result;
		}


		File inputFile = new File(input.getContentItem().getAbsolutePath());
		String newext = PathUtilities.extractPageType( input.getPath() );
		if( newext != null && newext.length()> 1)
		{
			ext = newext.toLowerCase();
		}
		List<String> com = createCommand(inputFile, inStructions);
		com.add("-limit");
		com.add("thread");
		com.add("1");

		if (inStructions.getMaxScaledSize() != null)
		{
			//be aware ImageMagick writes to a tmp file with a larger version of the file before it is finished
			if( "eps".equalsIgnoreCase( ext) || "pdf".equalsIgnoreCase( ext) || "ai".equalsIgnoreCase( ext))
			{
				//check input width
				int width = inAsset.getInt("width");
				if( width > 0 )
				{
					// calculate output width
					int height = inAsset.getInt("height");
					double ratio = height/width;

					int prefw = inStructions.getMaxScaledSize().getWidth();
					int prefh = inStructions.getMaxScaledSize().getHeight();

					int distw = Math.abs(prefw - width);
					int disth = Math.abs(prefh - height);

					int outputw;
					if(disth < distw)
					{
						outputw = width*(prefh/height);
					}
					else
					{
						outputw = prefw;
					}

					if( width < outputw)
					{
						//for small input files we want to scale up the density
						float density = ((float)outputw / (float)width) * 300f;
						density = Math.max(density,300);
						String val = String.valueOf( Math.round(density) );
						com.add(0,val);
						com.add(0,"-density");
					}
					else
					{
						com.add(0,"300");
						com.add(0,"-density");
					}
				}
			}
			//else

			/** We dont need this any more?
			 //we need to rotate this before we start otherwise the width and heights might be flipped. This is only 5% slower
			 //since we now strip all metadata from resulting images the orientation will be lost
			 if( autocreated )
			 {
			 String rotation = inAsset.getProperty("imageorientation");
			 if(rotation != null)
			 {
			 try		//TODO: Use a local lookup map
			 {
			 Searcher searcher = inArchive.getSearcherManager().getSearcher(inArchive.getCatalogId(), "imageorientation");
			 Data rotationval = (Data)searcher.searchById(rotation);
			 if(rotationval != null )
			 {
			 String val = rotationval.get("rotation");
			 inStructions.setRotation(Integer.parseInt(val));
			 }
			 }
			 catch(Exception e)
			 {
			 log.error("Error: " + e,e);
			 }
			 }
			 }
			 */			
			if (!inStructions.isCrop())
			{
				//end of probably wrong section
				com.add("-resize");

				String prefix = null;
				String postfix = null;


				//We need to flip the width and height if we have a rotated image. This allows us to crop first to speed up the rotation on a smaller image
				//				if( inStructions.getRotation() == 90 || inStructions.getRotation() == 270)
				//				{
				//					prefix =  String.valueOf( inStructions.getMaxScaledSize().height );
				//					postfix =  String.valueOf( inStructions.getMaxScaledSize().width );
				//				}
				//				else
				//				{
				prefix =  String.valueOf( inStructions.getMaxScaledSize().width );
				postfix =  String.valueOf( inStructions.getMaxScaledSize().height );
				//				}


				if (isOnWindows())
				{

					com.add("\"" + prefix + "x" + postfix + "\"");
				}
				else
				{
					com.add(prefix + "x" + postfix);
				}
			}

		}

		//faster to do it after sizing
		//TODO: Is this needed any more? Seems that ImageMagik will use the orientation flag that is built in
		/** Dont need this
		 if (autocreated && inStructions.getRotation() != 0 && inStructions.getRotation() != 360)
		 {
		 com.add("-rotate");
		 com.add(String.valueOf(360 - inStructions.getRotation()));
		 }
		 */		

		if(inStructions.isCrop())
		{
			boolean croplast = Boolean.parseBoolean(inStructions.get("croplast"));
			//resize then cut off edges so end up with a square image
			if(!croplast)
			{
				com.add("-resize");
				StringBuffer resizestring = new StringBuffer();
				resizestring.append(inStructions.getMaxScaledSize().width);
				resizestring.append("x");
				resizestring.append(inStructions.getMaxScaledSize().height);
				resizestring.append("^");
				com.add(resizestring.toString());
			}

			//now let's crop
			String gravity = inStructions.get("gravity");
			if(!"default".equals(gravity))
			{
				com.add("-gravity");
				if( gravity == null )
				{
					String thistype = inAsset.getFileFormat();
					String found = inArchive.getMediaRenderType(thistype);
					if( "document".equals(found) )
					{
						gravity = "NorthEast";
					}
				}
				if( gravity == null )
				{
					gravity = "Center";
				}
				com.add(gravity);
			}

			if( !transparent && ("eps".equals(ext) || "pdf".equals(ext) || "png".equals(ext) ||  "gif".equals(ext)) )
			{
				com.add("-background");
				com.add("white");
				com.add("-flatten");
			}
			else if ("svg".equals(ext))//add svg support; include transparency
			{
				com.add("-background");
				com.add("transparent");
				com.add("-flatten");
			}


			com.add("-crop");
			StringBuffer cropString = new StringBuffer();
			String cropwidth = inStructions.get("cropwidth");
			if(!cropwidth){
				cropwidth = inStructions.getMaxScaledSize().width;
			}
			cropString.append(cropwidth);
			cropString.append("x");
			String cropheight = inStructions.get("cropheight");

			if(!cropheight){
				cropheight = inStructions.getMaxScaledSize().height;
			}
			cropString.append(cropheight);

			String x1 = inStructions.get("x1");
			String y1 = inStructions.get("y1");

			cropString.append("+");
			if(x1 == null)
			{
				cropString.append("0");
			}
			else
			{
				cropString.append(x1);
			}
			cropString.append("+");
			if(y1 == null)
			{
				cropString.append("0");
			}
			else
			{
				cropString.append(y1);
			}
			com.add(cropString.toString());
			com.add("+repage");
			if(croplast)
			{
				com.add("-resize");
				StringBuffer resizestring = new StringBuffer();
				resizestring.append(inStructions.getMaxScaledSize().width);
				resizestring.append("x");
				resizestring.append(inStructions.getMaxScaledSize().height);
				resizestring.append("^");
				com.add(resizestring.toString());
			}
		}
		else if( !transparent && ("eps".equals(ext) || "pdf".equals(ext) || "png".equals(ext) || "gif".equals(ext) ) )
		{
			com.add("-background");
			com.add("white");
			com.add("-flatten");
		}
		else if ("svg".equals(ext))//add svg support; include transparency
		{
			com.add("-background");
			com.add("transparent");
			com.add("-flatten");
		}
		//				String colorspace = inStructions.get("colorspace");
		//				if(colorspace != null){
		//					com.add("-colorspace");
		//					com.add(colorspace);
		//				} else{
		//					com.add("-colorspace");
		//					com.add("sRGB");
		//				}

		setValue("quality", "89", inStructions, com);
		//add sampling-factor if specified
		if (inStructions.get("sampling-factor")!=null)
		{
			com.add("-sampling-factor");
			com.add(inStructions.get("sampling-factor"));
		}
		if( autocreated )  //we are using a color corrected input
		{
			com.add("-strip"); //This does not seem to do much
		}
		else
		{
			//			TODO: use parameters to specify the color space
			//Make sure we use 8 bit output and
			//http://entermediasoftware.com/views/learningcenter/wiki/wiki/ImageMagick.html
			//		com.add("-quality");
			//		com.add("90"); I think the default is about 80
			//			setValue("colorspace", "sRGB", inStructions, com);
			//			String colorspace = inStructions.get("colorspace");
			//			if(colorspace != null){
			//				com.add("-colorspace");
			//				com.add(colorspace);
			//			} else{
			//				com.add("-colorspace");
			//				com.add("sRGB");
			//			}

			/*			String type = inAsset.get("colortype");
			 if( type != null && type.equalsIgnoreCase("RGB") )
			 {
			 String cmyk = inAsset.get("colorspace");  
			 if( cmyk != null && cmyk.equalsIgnoreCase("CMYK") )  //Edge case where someone has the wrong colorspace set in the file
			 {
			 com.add("-colorspace");
			 com.add("RGB");
			 }	
			 }
			 */	

			String _colorspace = inAsset.get("colorspace");
			log.info("Colorspace: " + _colorspace)
			
			Data colorspacedata  = _colorspace!=null ? inArchive.getData("colorspace",_colorspace) : null;
			if (colorspacedata!=null && colorspacedata.getName().equalsIgnoreCase("cmyk")) //Edge case where someone has the wrong colorspace set in the file 
			{
				setValue("profile", getPathtoProfile(), inStructions, com);
				com.add("-auto-orient"); //Needed for rotate tool
				com.add("-strip"); //This does not seem to do much
				setValue("profile", getPathtoProfile(), inStructions, com);
			}
			else
			{
				setValue("colorspace", "sRGB", inStructions, com);
			}
			

			//Some old images have a conflict between a Color Mode of CMYK but they have an RGB Profile embeded. Make sure we check for this case


		}




		if (isOnWindows() )
		{
			// windows needs quotes if paths have a space
			com.add("\"\\\\?\\" + outputpath + "\"");
		}
		else
		{
			com.add(outputpath);
		}

		long start = System.currentTimeMillis();
		new File(outputpath).getParentFile().mkdirs();
		
		long timeout = getConversionTimeout(inArchive, inAsset);
		ExecResult execresult = getExec().runExec("convert", com, true, timeout);

		boolean ok = execresult.isRunOk();
		result.setOk(ok);

		if (ok)
		{
			result.setComplete(true);

			log.info("Convert complete in:" + (System.currentTimeMillis() - start) + " " + inOutFile.getName());

			return result;
		}
		//problems
		log.info("Could not exec: " + execresult.getStandardError() );
		result.setError(execresult.getStandardError());
		return result;
	}


	protected List<String> createCommand(File inFile, ConvertInstructions inStructions)
	{
		List<String> com = new ArrayList<String>();

		//		if( inStructions.getParameters() != null )
		//		{
		//			for (Iterator iterator = inStructions.getParameters().iterator(); iterator.hasNext();)
		//			{
		//				Data data = (Data) iterator.next();
		//				com.add(data.getName());
		//				com.add(data.get("value") );
		//			}
		//		}
		// New version of Image Magik are 0 based
		int page = inStructions.getPageNumber();
		page--;
		page = Math.max(0, page);

		String prefix = "";
		String extension = "";
		int dotIndex = inFile.getName().lastIndexOf('.');
		if (dotIndex > 0)
		{
			extension = inFile.getName().substring(dotIndex + 1);
		}
		if ("dng".equalsIgnoreCase(extension))
		{
			prefix = "dng:";
		}
		else if (inStructions.getInputExtension() != null)
		{
			prefix = inStructions.getInputExtension() + ":";

		}
		if (isOnWindows())
		{
			com.add("\"\\\\?\\" + prefix + inFile.getAbsolutePath() + "[" + page + "]\"");
		}
		else
		{
			com.add(prefix + inFile.getAbsolutePath() + "[" + page + "]");
		}
		return com;
	}

	public void resizeThumbnailImage(File inFile, File inOutFile, ConvertInstructions inStructions) throws Exception
	{

		List<String> com = new ArrayList<String>();
		com.add(inFile.getAbsolutePath() + "[" + inStructions.getPageNumber() + "]");
		com.add("-thumbnail");
		com.add((int) inStructions.getMaxScaledSize().getWidth() + "x" + (int) inStructions.getMaxScaledSize().getHeight() + ">");
		com.add(inOutFile.getAbsolutePath());
		inOutFile.getParentFile().mkdirs();
		long start = System.currentTimeMillis();
		if (runExec("convert", com))
		{
			log.info("Resize complete in:" + (System.currentTimeMillis() - start) + " " + inOutFile.getName());
		}
	}

	public List<String> getConvertCommand(File inIn, File inOut, int inPageNumber)
	{
		List<String> com = new ArrayList<String>();
		com.add(inIn.getAbsolutePath() + "[" + inPageNumber + "]");
		com.add(inOut.getAbsolutePath());
		return com;
	}
	/*
	 protected File getOriginalDocument(MediaArchive inArchive, ConvertInstructions inStructions, Asset asset)
	 {
	 String original = null;
	 if (inStructions.getSourceFile() != null)
	 {
	 original = inArchive.getOriginalFileManager().getFilePath(inStructions.getSourceFile(), asset);
	 }
	 else
	 {
	 original = inArchive.getOriginalFileManager().getOriginalFilePath(asset);
	 }
	 if(original == null)
	 {
	 return null; //we haven't uploaded anything yet
	 }
	 String extension = PathUtilities.extractPageType(original);
	 inStructions.setInputType(extension);
	 String orientation = asset.get("imageorientation");
	 if (orientation != null)
	 {
	 Data ed = (Data) inArchive.getSearcherManager().getSearcher(asset.getCatalogId(), "imageorientation").searchById(orientation);
	 if (ed != null)
	 {
	 String rotation = ed.get("rotation");
	 if (rotation != null && !rotation.equals(""))
	 {
	 inStructions.setRotation(Integer.parseInt(rotation));
	 }
	 }
	 }
	 File input = new File(original);
	 return input;
	 }
	 */
	public ConvertResult applyWaterMark(MediaArchive inArchive, String inInputAbsPath, String inOutputAbsPath, ConvertInstructions inStructions)
	{
		// composite -dissolve 15 -tile watermark.png src.jpg dst.jpg
		List<String> com = new ArrayList<String>();
		com.add("-dissolve");
		com.add("100");

		String placement = inStructions.getWatermarkPlacement();
		if(placement == null)
		{
			placement = "tile";//"SouthWest";
		}

		if (placement.equals("tile"))
		{
			com.add("-tile");

		}
		else
		{
			com.add("-gravity");
			com.add(placement);
		}

		com.add(getWaterMarkPath(inArchive.getThemePrefix()));
		com.add(inInputAbsPath);
		com.add(inOutputAbsPath);
		
		boolean ok =  runExec("composite", com);
		ConvertResult result = new ConvertResult();
		result.setOk(ok);
		return result;

	}



	/*
	 private File makeWaterMark(ConvertInstructions inStructions, File input, String watermarkPath)
	 {
	 File watermarked = null;
	 ContentItem stub = getPageManager().getRepository().getStub(watermarkPath);
	 watermarked = new File(stub.getAbsolutePath());
	 if (watermarked.exists() && watermarked.length() > 0)
	 {
	 return watermarked;
	 }
	 else
	 {
	 watermarked.getParentFile().mkdirs();
	 try
	 {
	 watermarked.createNewFile();
	 }
	 catch (IOException e)
	 {
	 throw new OpenEditException(e);
	 }
	 // watermark it
	 MediaCreator convert = getConverterByFileFormat(inStructions.getInputType());
	 convert.applyWaterMark(getMediaArchive(), input, watermarked, inStructions);
	 return watermarked;
	 }
	 }
	 */	
	protected void setValue(String inName, String inDefault,ConvertInstructions inStructions, List comm)
	{
		String value = inStructions.get(inName);
		if( value != null || inDefault != null)
		{
			comm.add("-" + inName );
			if( value != null)
			{
				comm.add(value);
			}
			else if( inDefault != null)
			{
				comm.add(inDefault);
			}
		}

	}
}
