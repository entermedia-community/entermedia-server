package org.openedit.entermedia.scanner;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.creator.ConvertInstructions;
import org.openedit.entermedia.creator.ConvertResult;
import org.openedit.entermedia.creator.ExifToolThumbCreator;

import com.openedit.page.Page;
import com.openedit.page.PageProperty;
import com.openedit.util.Exec;
import com.openedit.util.ExecResult;
import com.openedit.util.PathUtilities;

public class ExiftoolMetadataExtractor extends MetadataExtractor
{
	private static final String EMPTY_STRING = "";
	private static final Log log = LogFactory.getLog(ExiftoolMetadataExtractor.class);
	protected ExifToolThumbCreator fieldExifToolThumbCreator;
	protected Exec fieldExec;

	public ExifToolThumbCreator getExifToolThumbCreator()
	{
		return fieldExifToolThumbCreator;
	}

	public void setExifToolThumbCreator(ExifToolThumbCreator inExifToolThumbCreator)
	{
		fieldExifToolThumbCreator = inExifToolThumbCreator;
	}

	public boolean extractData(MediaArchive inArchive, File inputFile, Asset inAsset)
	{
		String[] supportedTypes = new String[] {"audio", "video", "image", "document"};
		String type = PathUtilities.extractPageType(inputFile.getName());
		if (type != null)
		{
			String mediatype = inArchive.getMediaRenderType(type);
			if (!Arrays.asList(supportedTypes).contains(mediatype))
			{
				return false;
			}
		}
		try
		{
			PropertyDetails details = inArchive.getAssetPropertyDetails();
			ArrayList<String> comm = new ArrayList<String>();
			
			Page etConfig = inArchive.getPageManager().getPage(inArchive.getCatalogHome() + "/configuration/exiftool.conf");
			if( etConfig.exists() )
			{
				comm.add("-config");
				comm.add(etConfig.getContentItem().getAbsolutePath());
			}
			
			comm.add("-S");
			comm.add("-d");
			comm.add("%Y-%m-%d %H:%M:%S"); //yyyy-MM-dd HH:mm:ss
			
			comm.add(inputFile.getAbsolutePath());
			comm.add("-n");
			ExecResult result = getExec().runExec("exiftool", comm, true);
			if( !result.isRunOk())
			{
				String error = result.getStandardError();
				log.info("error " + error);
				return false;
			}
			String numberinfo = result.getStandardOut();
			
			Pattern p = Pattern.compile("(\\w+):\\s+(.+)"); //clean whitespace
			
			if (numberinfo != null)
			{
				String[] numbers = numberinfo.split("\n");
				for (int i = 0; i < numbers.length; i++)
				{
					Matcher m = p.matcher(numbers[i]);
					if(!m.find())
					{
						continue;
					}
					String key = m.group(1);
					String value = m.group(2);
					
					if(key == null || value == null)
					{
						continue;
					}
					
					if("ImageSize".equals(key))
					{
						try
						{
							String[] dims = value.split("x");
							inAsset.setProperty("width", dims[0]);
							inAsset.setProperty("height", dims[1]);
						}
						catch( Exception e )
						{
							log.warn("Could not parse ImageSize string: " + value);
						}
					}
					else if( "MaxPageSizeW".equals(key))
					{
						if( inAsset.get("width") == null)
						{
							float wide = Float.parseFloat(value );
							wide = wide * 72f;
							inAsset.setProperty("width", String.valueOf(Math.round(wide ) ) );
						}
					}
					else if( "MaxPageSizeH".equals(key))
					{
						if( inAsset.get("height") == null)
						{
							float height = Float.parseFloat( value );
							height = height * 72f;
							inAsset.setProperty("height", String.valueOf(Math.round( height) ));
						}
					}
					else if("Duration".equals(key) || "SendDuration".equals(key))
					{
						try
						{
							value = processDuration(value);
							inAsset.setProperty("length", value);
						}
						catch( Exception e )
						{
							log.warn("Could not parse file length: " + value);
						}
					}
					else if("Subject".equals(key))
					{
						String[] kwords = value.split(",");
						for( String kword : kwords )
						{
							inAsset.addKeyword(kword.trim());
						}
					}
					else if("FileType".equals(key))
					{
						if( inAsset.getProperty("fileformat") == null)
						{
							inAsset.setProperty("fileformat", value.toLowerCase() );
						}
					}
					else if("Keyword".equals(key))
					{
						String[] kwords = value.split(",");
						for( String kword : kwords )
						{
							inAsset.addKeyword(kword.trim());
						}
					}
					else if("VideoFrameRate".equals(key))
					{
						inAsset.setProperty("framerate", roundFrameRate(value) );
					}
					else
					{
						PropertyDetail property = details.getDetailByExternalId(key);
	
						if(property == null)
						{
							continue;
						}
						else if(property.isDate())
						{
							//Date dateValue = externalFormat.parse(value);
							//value = value + " -0000"; //added offset of 0 since that seems to be the default
							inAsset.setProperty(property.getId(), value );
						}
						else if(property.isList() || property.isDataType("number"))
						{
							m = p.matcher(numbers[i]);
							if(m.find())
							{
								inAsset.setProperty(property.getId(), m.group(2));
							}
						}
						else
						{
							inAsset.setProperty(property.getId(), value);
						}
					}
				}
			}
		}
		catch (Exception e1)
		{
			log.error("Could not read metada from asset: " + inAsset.getSourcePath() +  e1,e1);
		}
		
		extractThumb(inArchive, inputFile, inAsset);
		
		return true;
	}

	protected String processDuration(String value) {
		if( value.contains("s") )
		{
			value = value.split("\\.")[0];
		}
		else
		{
			String[] parts = value.split(":");
			long total = 0;
			for(int j = 0; j < parts.length; j++)
			{
				total += Math.pow(60, parts.length - 1 - j) * Double.parseDouble(parts[j]);
			}
			value = String.valueOf(total);
		}
		return value;
	}

	protected void extractThumb(MediaArchive inArchive, File inInputFile, Asset inAsset)
	{
		Page def = inArchive.getPageManager().getPage( "/" + inArchive.getCatalogId() + "/downloads/preview/thumb/thumb.jpg");
		ConvertInstructions ins = getExifToolThumbCreator().createInstructions(def, inArchive, "jpg", inAsset.getSourcePath());
		String path = ins.getOutputPath();
		Page thumb = inArchive.getPageManager().getPage(path);
		if( !thumb.exists() || thumb.length() == 0 )
		{
			ConvertResult res = getExifToolThumbCreator().convert(inArchive, inAsset, thumb, ins);
			if( res.isOk())
			{
				if( !"generated".equals( inAsset.get("previewstatus") ) )
				{
					inAsset.setProperty("previewstatus", "exif");
				}
			}
		}
	}

	public Exec getExec()
	{
		return fieldExec;
	}

	public void setExec(Exec exec)
	{
		fieldExec = exec;
	}

	protected String roundFrameRate(String val) {
		if (val==null||EMPTY_STRING.equals(val.trim()))
			return EMPTY_STRING;
		BigDecimal result = new BigDecimal(val);
		result = result.setScale(2, RoundingMode.HALF_UP);
		return result.toString();
	}
	
}
