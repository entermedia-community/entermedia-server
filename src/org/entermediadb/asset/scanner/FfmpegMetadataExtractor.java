package org.entermediadb.asset.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONObject;
import org.openedit.util.JSONParser;
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public class FfmpegMetadataExtractor extends MetadataExtractor
{
	private static final Log log = LogFactory.getLog(FfmpegMetadataExtractor.class);
	protected Exec fieldExec;
	
	public boolean extractData(MediaArchive inArchive, ContentItem inFile, Asset inAsset)
	{
		String mediatype = inArchive.getMediaRenderType(inAsset.getDetectedFileFormat());
		if( "video".equals(mediatype ) || "audio".equals(mediatype)) 
		{
			//Run it again
			List args = new ArrayList();
			args.add("-loglevel");
			args.add("quiet");
			args.add("-show_format");
			args.add("-show_streams");
			args.add("-sexagesimal");
			args.add(inFile.getAbsolutePath());
			args.add("-of");
			args.add("json");
			
			ExecResult resulttext = getExec().runExec("avprobe", args, true);
			if( !resulttext.isRunOk())
			{
				String error = resulttext.getStandardError();
				log.info("error " + error);
				return false;
			}
			String textinfo = resulttext.getStandardOut();
			if( textinfo== null)
			{
				textinfo = resulttext.getStandardError();
			}
			if( textinfo == null)
			{
				return false;
			}
			int startjson = textinfo.indexOf("{");
			if( startjson == -1)
			{
				log.error("JSON not found");
				return false;
			}
			try
			{
				textinfo = textinfo.substring(startjson,textinfo.length());
				JSONObject config = (JSONObject)new JSONParser().parse(textinfo);
				Collection streams = (Collection)config.get("streams");
				
				Collection audiostreamids = new ArrayList();
				Collection videostreamids = new ArrayList();
				String videotimecodeid = null;
				
				for (Iterator iterator = streams.iterator(); iterator.hasNext();)
				{
					Map stream = (Map) iterator.next();
					if( "video".equals( stream.get("codec_type") ) )
					{
						inAsset.setProperty("videocodec", (String)stream.get("codec_name"));	
						inAsset.setProperty("width", String.valueOf( stream.get("width")) );	
						inAsset.setProperty("height", String.valueOf( stream.get("height")) );
						
						String val =  (String)stream.get("duration");
						if(val != null)
						{
							inAsset.setProperty("duration",val);
							val = processDuration(val);
							inAsset.setValue("length",  Double.parseDouble( val ) ); //in fractional seconds
							inAsset.setProperty("aspect_ratio", (String)stream.get("display_aspect_ratio"));
						}
						videostreamids.add(String.valueOf( stream.get("index")));
					}
					else if( "audio".equals( stream.get("codec_type") ) )
					{
						audiostreamids.add(String.valueOf( stream.get("index")));
						inAsset.setProperty("audiocodec", (String)stream.get("codec_name"));						
					}
					else if( "data".equals( stream.get("codec_type") ) )
					{
						videotimecodeid = String.valueOf( stream.get("index"));
					}
					//Subtitles?
				}
				inAsset.setValue("videostreamids",videostreamids);
				inAsset.setValue("audiostreamids",audiostreamids);
				inAsset.setValue("videotimecodeid",videotimecodeid);
				
				if("audio".equals(mediatype)){
					
					 Map format = (Map) config.get("format");
					 Object bitrate =  format.get("bit_rate");
					 inAsset.setValue("audiobitrate", bitrate);
				}
				
				if("video".equals(mediatype)){
					
					 Map format = (Map) config.get("format");
					 Object bitrate = format.get("bit_rate");
					 inAsset.setValue("videobitrate", bitrate);
				}
				
			} 
			catch ( Throwable ex)
			{
				log.error("Could not read metadata on " + inAsset.getName(), ex );
			}
			return true;
		}
		return false;
	}
	//66.116667  vs 0:01:06.116667 vs 00:01:06.12 -sexagesimal  HOURS:MM:SS.MICROSECONDS
	protected String processDuration(String value)
	{
		//00:00:19.89,
		if( value.contains("s") )
		{
			value = value.split("\\.")[0];
		}
		else
		{
			String[] parts = value.split(":");
			double totals = 	60L * 60L * Double.parseDouble(parts[0]);				
			totals = totals +  60L * Double.parseDouble(parts[1]);				
			totals = totals +  Double.parseDouble(parts[2]);
			value = Double.toString(totals); 
		}
		return value;
	}

	public Exec getExec()
	{
		return fieldExec;
	}

	public void setExec(Exec exec)
	{
		fieldExec = exec;
	}

}
