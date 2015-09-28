package org.openedit.entermedia.scanner;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.repository.ContentItem;

import com.openedit.util.Exec;
import com.openedit.util.ExecResult;

public class FfmpegMetadataExtractor extends MetadataExtractor
{
	private static final Log log = LogFactory.getLog(FfmpegMetadataExtractor.class);
	protected Exec fieldExec;
	
	public boolean extractData(MediaArchive inArchive, ContentItem inFile, Asset inAsset)
	{
		String mediatype = inArchive.getMediaRenderType(inAsset.getFileFormat());
		if( "video".equals(mediatype )) 
		{
			//Run it again
			List args = new ArrayList();
			args.add(inFile.getAbsolutePath());
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
			//Stream #0.2: Video: G2M3 / 0x334D3247,
			int start = textinfo.indexOf("Video: ");
			if( start > -1)
			{
				start = start + 7;
				int end = textinfo.indexOf(",",start);
				String val = textinfo.substring(start,end);
				inAsset.setProperty("videocodec", val);
			}
			start = textinfo.indexOf("Audio: ");
			if( start > -1)
			{
				start = start + 7;
				int end = textinfo.indexOf(",",start);
				String val = textinfo.substring(start,end);
				inAsset.setProperty("audiocodec", val);
			}
			start = textinfo.indexOf(" Duration: ");
			if( start > -1 )
			{
				start = textinfo.indexOf(":",start);
				int end = textinfo.indexOf(",",start);
				String val = textinfo.substring(start + 1,end);
				try
				{
					inAsset.setProperty("duration", val);
					val = processDuration(val);
					inAsset.setProperty("length", val); //in seconds rounded
				} 
				catch ( Exception ex)
				{
					log.error("Could not read duration " + val);
				}
				
			}
			
			    
			
			return true;
		}
		return false;
	}
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
			double total = 0;
			for(int j = 0; j < parts.length; j++)
			{
				double adding = Math.pow(60, parts.length - 1 - j) * Double.parseDouble(parts[j]);				
				total = total + adding;
			}
			value = String.valueOf(Math.round( total ) );
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
