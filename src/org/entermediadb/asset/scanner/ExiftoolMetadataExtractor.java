package org.entermediadb.asset.scanner;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.Searcher;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;
import org.openedit.util.PathUtilities;

import groovy.json.JsonSlurper;

public class ExiftoolMetadataExtractor extends MetadataExtractor
{
	String[] supportedTypes = new String[] { "audio", "video", "image", "document" };

	private static final String EMPTY_STRING = "";
	private static final Log log = LogFactory.getLog(ExiftoolMetadataExtractor.class);
	//protected MediaTranscoder fieldExiftoolThumbTranscoder;
	protected Exec fieldExec;
	protected Set fieldTextFields;

	public Set getTextFields()
	{
		if (fieldTextFields == null)
		{
			fieldTextFields = new HashSet();
			fieldTextFields.add("LensID");
			fieldTextFields.add("ShutterSpeed");
			fieldTextFields.add("FocalLength");

		}

		return fieldTextFields;
	}



	public void setTextFields(Set inTextFields)
	{
		fieldTextFields = inTextFields;
	}

	public synchronized boolean extractAll(MediaArchive inArchive, Collection<ContentItem> inputFiles, Collection<Asset> inAssets)
	{
		
		//Make a temp file 
		File tmp = writeList(inArchive, inputFiles);
		if(tmp == null)
		{
			return false;
		}
		try
		{
			PropertyDetails details = inArchive.getAssetPropertyDetails();
			ArrayList<String> base = new ArrayList<String>();


			Page etConfig = inArchive.getPageManager().getPage(inArchive.getCatalogHome() + "/configuration/exiftool.conf");

			if (etConfig.exists())
			{
				base.add("-config");
				base.add(etConfig.getContentItem().getAbsolutePath());
			}
			base.add("-S");
			base.add("-fast2");
			base.add("-d");
			base.add("\"%Y-%m-%d %H:%M:%S\""); //yyyy-MM-dd HH:mm:ss
			base.add("-@");
			base.add(tmp.getAbsolutePath());
			//base.add(inputFile.getAbsolutePath());
//			ArrayList<String> comm = new ArrayList(base);
			base.add("-n");
			
			//--
			long start = System.currentTimeMillis();
			//--
			//ExecResult result = getExec().runExec("exiftool", base, true);
			//public ExecResult runExecStream(String inCommandKey, List<String> args, OutputStream inOutput, long inTimeout) throws OpenEditException
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			
			ExecResult result = getExec().runExecStream("exiftool", base, output, -1);
			//This will write a bunch of lines out
			
			String out = new String(output.toByteArray(), "UTF-8");

			String[] eachresult = out.split("//n//n");
//			
//			String line = null;
//			while ((line = br.readLine()) != null) {
//				System.out.println(line);
//			}
			tmp.delete();
			
			//--
			long end = System.currentTimeMillis();
			double total = (end - start) / 1000.0;
			log.info("Exiftool Done in: "+total);
			//--
			
			if (!result.isRunOk())
			{
				String error = result.getStandardError();
				log.info("error " + error);
				return false;
			}

			//System.out.println(eachresult);
			int i=0;
			for (Iterator iterator = inAssets.iterator(); iterator.hasNext();)
			{
				Asset asset = (Asset) iterator.next();
				if( canProcess(inArchive, asset.getName()))
				{
					String numberinfo = eachresult[i++];//result.getStandardOut();
					parseNumericValues(inArchive, asset, details, numberinfo);
					//log.debug("Exiftool found " + asset.getSourcePath() + " returned " + numberinfo.length());
				}
			}
		}
		catch (Exception e1)
		{
			log.error("Could not read metada from assets: " + e1, e1);
		}

		//extractThumb(inArchive, inputFile, inAsset);

		return true;
	}



	protected File writeList(MediaArchive inArchive, Collection<ContentItem> inputFiles) 
	{
		try
		{
			File tmp = File.createTempFile("exiftool", EMPTY_STRING);
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(tmp));
			for(ContentItem item : inputFiles)
			{
				if(canProcess(inArchive, item.getName()))
				{
					writer.write(item.getAbsolutePath() + "\n");
				}
			}
		    writer.close();
			return tmp;
		}
		catch( Throwable ex)
		{
			log.error("Could not write", ex);
		}
		return null;
	}
	private boolean canProcess(MediaArchive inArchive,String inName)
	{
		String type = PathUtilities.extractPageType(inName);

		if (type != null)
		{
			String mediatype = inArchive.getMediaRenderType(type);
			if (!Arrays.asList(supportedTypes).contains(mediatype))
			{
				return true;
			}
		}
		return false;
	}

	public synchronized boolean extractData(MediaArchive inArchive, ContentItem inputFile, Asset inAsset)
	{
		Collection supportedTypes = inArchive.getCatalogSettingValues("metadata_exiftool_formats");
		if (supportedTypes == null)
		{
			String[] defaultSupportedTypes = new String[] { "audio", "video", "image", "document", "default" };
			supportedTypes = Arrays.asList(defaultSupportedTypes);
		}
		
		
		String type = PathUtilities.extractPageType(inputFile.getName());

		if (type != null)
		{
			String mediatype = inArchive.getMediaRenderType(type); 
			if (!supportedTypes.contains(mediatype))
			{
				return false;
			}
		}
		PropertyDetails details = inArchive.getAssetPropertyDetails();
		ArrayList<String> base = new ArrayList<String>();


		Page etConfig = inArchive.getPageManager().getPage(inArchive.getCatalogHome() + "/configuration/exiftool.conf");

		if (etConfig.exists())
		{
			base.add("-config");
			base.add(etConfig.getContentItem().getAbsolutePath());
		}
		base.add("-fast2");
		base.add("-S");
		base.add("-d");
		base.add("\"%Y-%m-%d %H:%M:%S\""); //yyyy-MM-dd HH:mm:ss

		base.add(inputFile.getAbsolutePath());
		base.add("-n");
		
		runExif(inArchive, inputFile, inAsset, base);
		extractThumb(inArchive, inputFile, inAsset);

		return true;
	
	}
	
	/**
	 * synchronized because ExifTool is not thread safe
	 */
	public synchronized void runExif(MediaArchive inArchive, ContentItem inputFile, Asset inAsset, List comm)
	{
		try
		{

		//--
		long start = System.currentTimeMillis();
		//log.info("Runnning identify");
		//--
		ExecResult result = getExec().runExec("exiftool", comm, true);
		//--
		long end = System.currentTimeMillis();
		double total = (end - start) / 1000.0;
		//log.info("Exiftool Done in: "+total);
		//--
		
		if (!result.isRunOk())
		{
			String error = result.getStandardError();
			log.info("error " + error);
			return;
		}
		String numberinfo = result.getStandardOut();
		if (numberinfo == null)
		{
			log.info("Exiftool found " + inAsset.getSourcePath() + " returned null");
		}
		else
		{
			log.debug("Exiftool found " + inAsset.getSourcePath() + " returned " + numberinfo.length());
		}
		PropertyDetails details = inArchive.getAssetPropertyDetails();

		//boolean foundtext = 
		parseNumericValues(inArchive, inAsset, details, numberinfo);
//		if (foundtext)
//		{
//			//Run it again TODO: Use text values all the time
//			ExecResult resulttext = getExec().runExec("exiftool", base, true);
//			if (!resulttext.isRunOk())
//			{
//				String error = resulttext.getStandardError();
//				log.info("error " + error);
//				return false;
//			}
//			String textinfo = resulttext.getStandardOut();
//			//parseTextValues(inAsset, details, textinfo);  //TODO: Do we skip anything already set
//		}
	}
	catch (Exception e1)
	{
		log.error("Could not read metada from asset: " + inAsset.getSourcePath() + e1, e1);
	}
		
	}

	protected void parseNumericValues(MediaArchive inArchive, Asset inAsset, PropertyDetails details, String numberinfo)
	{
		Pattern p = Pattern.compile("(\\w+):\\s+(.+)"); //clean whitespace TODO: handle lower/mixed case
		//boolean foundtextvalues = false;
		String lat = null;
		String lng = null;
		if (numberinfo != null)
		{
			String[] numbers = numberinfo.split("\n");
			for (int i = 0; i < numbers.length; i++)
			{
				
				String input = numbers[i];
				Matcher m = p.matcher(input);
				if (!m.find())
				{
					continue;
				}
				String key = m.group(1);
				String value = m.group(2);
				//log.info(key + " = " + value);
				
				if (key == null || value == null || value.isEmpty() )
				{
					continue;
				}

				if ("ImageSize".equals(key))
				{
					try
					{
						String[] dims = value.split("x");
						if( dims.length < 2)
						{
							dims = value.split(" ");
						}
						String width = dims[0];
						String height = dims[1];
						//width & heights can have decimals if converted from vectors, e.g., SVGs
						if (width.contains("."))
						{//round off to the nearest integer
							Float fwidth = Float.parseFloat(width);
							width = String.valueOf(fwidth.intValue());
						}
						if (height.contains("."))
						{
							Float fheight = Float.parseFloat(height);
							height = String.valueOf(fheight.intValue());
						}
						inAsset.setProperty("width", width);
						inAsset.setProperty("height", height);
					}
					catch (Exception e)
					{
						log.warn("Could not parse ImageSize string: " + value);
					}
				}
				else if ("ImageWidth".equals(key))
				{
					if (inAsset.get("width") == null)
					{
						float wide = Float.parseFloat(value);
						inAsset.setProperty("width", String.valueOf(Math.round(wide)));
					}
				}
				else if ("ImageHeight".equals(key))
				{
					if (inAsset.get("height") == null)
					{
						float height = Float.parseFloat(value);
						inAsset.setProperty("height", String.valueOf(Math.round(height)));
					}
				}
				else if ("MaxPageSizeW".equals(key))
				{
					if (inAsset.get("width") == null)
					{
						float wide = Float.parseFloat(value);
						wide = wide * 72f;
						inAsset.setProperty("width", String.valueOf(Math.round(wide)));
					}
				}
				else if ("MaxPageSizeH".equals(key))
				{
					if (inAsset.get("height") == null)
					{
						float height = Float.parseFloat(value);
						height = height * 72f;
						inAsset.setProperty("height", String.valueOf(Math.round(height)));
					}
				}
				else if ("PageCount".equals(key))
				{
					inAsset.setProperty("pages", value); 
				}
				else if ("Duration".equals(key) || "SendDuration".equals(key))
				{
					try
					{
						inAsset.setProperty("duration", value);
						value = processDuration(value);
						inAsset.setProperty("length", value);
					}
					catch (Exception e)
					{
						log.warn("Could not parse file length: " + value);
					}
				}
				//				else if("Subject".equals(key))
				//				{
				//					String[] kwords = value.split(",");
				//					for( String kword : kwords )
				//					{
				//						inAsset.addKeyword(kword.trim());
				//					}
				//				}
				else if ("FileType".equals(key))
				{
					String mediatype = inArchive.getMediaRenderType(value.toLowerCase());
					
					if (!mediatype.equals("default"))
					{
						inAsset.setProperty("fileformat", value.toLowerCase());
					}
					inAsset.setProperty("detectedfileformat", value.toLowerCase());
				}
				else if ("Subject".equals(key) || "Keyword".equals(key) || "Keywords".equals(key))
				{
					String[] kwords = value.split(",");
					for (String kword : kwords)
					{
						inAsset.addKeyword(kword.trim());
					}
				}
				else if ("VideoFrameRate".equals(key))
				{
					inAsset.setProperty("framerate", roundFrameRate(value));
				}
				else if ("ColorSpace".equals(key))
				{
					if ("65535".equals(value) || "-1".equals(value))
					{
						//not valid
						continue;
					}
					inAsset.setProperty("colorspace", value);
				}
				else if ("ProfileDescription".equals(key))
				{
					inAsset.setProperty("colorprofiledescription", value);
				}
				else if ( "PhotometricInterpretation".equals(key) )
				{
					if( "5".equalsIgnoreCase(value) )
					{
						inAsset.setProperty("colorspace", "4");
					}
				}
				else if( "ColorMode".equals(key) ||  "ColorComponents".equals(key) || "ColorSpaceData".equals(key) || "SwatchGroupsColorantsMode".equals(key) || "SwatchColorantMode".equals(key) )
				{
					if(value != null) {
					value = value.toLowerCase();
					if( "CMYK".equalsIgnoreCase(value) ||  "4".equalsIgnoreCase(value)  || value.contains("cmyk"))
					{
						inAsset.setProperty("colorspace", "4");
					}
					else if( "ColorMode".equals(key) )
					{
						//? useful
					}
					
					}
				}
				else if ("GPSLatitude".equals(key))
				{
					lat = value;
					//inAsset.setProperty("position_lat", value);
				}
				else if ("GPSLongitude".equals(key))
				{
					lng = value;
					//inAsset.setProperty("position_lng", value);
				}
//				else if (getTextFields().contains(key))
//				{
//					foundtextvalues = true;
//				}
				else
				{
					PropertyDetail property = details.getDetailByExternalId(key);

					if (property == null)
					{
						continue;
					}
					else if (property.isDate())
					{
						//Date dateValue = externalFormat.parse(value);
						//value = value + " -0000"; //added offset of 0 since that seems to be the default
						//TODO: Should we clean up dates on their way in? Right now it uses a close format but not the perfect format
						value = DateStorageUtil.getStorageUtil().checkFormat(value);
						inAsset.setProperty(property.getId(), value);
					}
					else if (property.isList() || property.isMultiValue())  //|| property.isDataType("number")
					{
						m = p.matcher(input);
						if (m.find())
						{
							Searcher searcher = inArchive.getSearcherManager().getSearcher(property.getListCatalogId(), property.getListId());
							Data lookup = (Data) searcher.query().exact("name", value).searchOne();
							if (lookup != null)
							{
								inAsset.setProperty(property.getId(), lookup.getId());
								continue;
							}
							else if(Boolean.parseBoolean(property.get("autocreatefromexif"))){
								lookup = searcher.createNewData();
								lookup.setName(value);
								//lookup.setId(searcher.nextId());
								searcher.saveData(lookup, null);
								inAsset.setProperty(property.getId(), lookup.getId());
							}
							else
							{
								value = value.replace("]", "");
								value = value.replace("[", "");
								String[] values = value.split(",");
								if(values.length == 1){
								
								inAsset.setProperty(property.getId(), value);
								} else{
									ArrayList arrayList = new ArrayList(Arrays.asList(values));
									inAsset.setValue(property.getId(), arrayList);
								}
							}
						}
					}
					else if( property.isMultiLanguage())
					{
						LanguageMap map = new LanguageMap();
						if( value.contains("{"))
						{
							Map object = (Map)new JsonSlurper().parseText(value);
							map.putAll(object);
						}
						else
						{
							map.setText("en", value);
						}
						inAsset.setValue(property.getId(), map);						
					}
					else
					{
						saveValue(inAsset,property.getId(), value);
					}
				}
			}
		}
		
		if(	lat != null	&&
			lng != null &&
			lat.contains(".") &&
			lng.contains(".") 
		){
			
			inAsset.setProperty("geo_point", lat + " , " + lng);  //TODO makesure we dont have junk in here
		}
	}

//	protected void parseTextValues(Asset inAsset, PropertyDetails details, String numberinfo)
//	{
//		Pattern p = Pattern.compile("(\\w+):\\s+(.+)"); //clean whitespace
//		if (numberinfo != null)
//		{
//			String[] numbers = numberinfo.split("\n");
//			for (int i = 0; i < numbers.length; i++)
//			{
//				Matcher m = p.matcher(numbers[i]);
//				if (!m.find())
//				{
//					continue;
//				}
//				String key = m.group(1);
//				String value = m.group(2);
//
//				if (key == null || value == null || !getTextFields().contains(key))
//				{
//					continue;
//				}
//				PropertyDetail property = details.getDetailByExternalId(key);
//				if (property == null)
//				{
//					continue;
//				}
//				
//				saveValue(inAsset,property.getId(), value);
//			}
//		}
//	}

	protected void saveValue(Asset inAsset, String inName, Object value)
	{
		String status = inAsset.get("importstatus");
		if( status != null && status.equals("needsmetadata"))  //This does not update existing values
		{
			//Skip vales that are already set from the upload
			if( inAsset.getValue(inName) != null)
			{
				return;
			}
		}
		inAsset.setValue(inName, value);
	}
	protected String processDuration(String value)
	{
		if (value.contains("s"))
		{
			value = value.split("\\.")[0];
		}
		else
		{
			String[] parts = value.split(":");
			double total = 0;
			for (int j = 0; j < parts.length; j++)
			{
				total += Math.pow(60, parts.length - 1 - j) * Double.parseDouble(parts[j]);
			}
			value = String.valueOf(Math.round(total));
		}
		return value;
	}

	protected void extractThumb(MediaArchive inArchive, ContentItem inInputFile, Asset inAsset)
	{
//		String format = inAsset.getFileFormat();
//		if ("indd".equalsIgnoreCase(format))  //TODO: Move to image
//		{
//			log.info("Extracting thumb from "+ inInputFile.getAbsolutePath());
//
//			ContentItem custom = inArchive.getContent( "/WEB-INF/data/" + inArchive.getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/customthumb.jpg");
//	
//			//if we have embdeded thumb 
//			ConvertInstructions instructions = new ConvertInstructions(inArchive);
//			instructions.setForce(true);
//			instructions.setInputFile(inInputFile);
//			instructions.setOutputFile(custom);
//			ConvertResult res = getExiftoolThumbTranscoder().convert(instructions);
//			if (res.isOk())
//			{
//				return;
//			}
//		}
//		if( format == null)
//		{
//			return;
//		}
//		if ("jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format) ||
//				"tiff".equalsIgnoreCase(format) || "tif".equalsIgnoreCase(format) ||  "pdf".equalsIgnoreCase(format) )
//		{	
//			//OR if we have CMYK with no profile input
//			String colorspace =  inAsset.get("colorspace");
//			if( colorspace == null)
//			{
//				if( isCMYKColorSpace(inInputFile) )
//				{
//					colorspace = "4";
//					inAsset.setProperty("colorspace", colorspace);
//				}
//			}
//		}
	}
	protected boolean isCMYKColorSpace(ContentItem inOriginal)
	{
		List<String> command = new ArrayList<String>();
		//command.add("-verbose");
		
		 //identify -format '%[colorspace]'
		command.add("-format");
		command.add("'%[colorspace]'");
		command.add(inOriginal.getAbsolutePath());
		//--
		//long start = new Date().getTime();
		//log.info("Runnning identify");
		//--
		ExecResult result = getExec().runExec("identify",command, true, 60000);
		//--
		//long end = new Date().getTime();
		//double total = (end - start) / 1000.0;
		//log.info("Identify Done in:"+total);
		//--
		
		String sout = result.getStandardOut();
//		String[] tokens = sout.split("\n");
//		if (tokens.length > 0){
//			for(String token:tokens)
//			{
//				if (token != null && token.trim().startsWith("Colorspace:")){//Colorspace: CMYK
//					boolean isCMYK = token.toLowerCase().contains("cmyk");
//					return isCMYK;
//				}
//			}
//		}
		if(sout.toLowerCase().contains("cmyk")){
			return true;
		}
			
		return false;
	}

	public Exec getExec()
	{
		return fieldExec;
	}

	public void setExec(Exec exec)
	{
		fieldExec = exec;
	}

	protected String roundFrameRate(String val)
	{
		if (val == null || EMPTY_STRING.equals(val.trim()))
			return EMPTY_STRING;
		BigDecimal result = new BigDecimal(val);
		result = result.setScale(2, RoundingMode.HALF_UP);
		return result.toString();
	}

}
