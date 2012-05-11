package org.openedit.entermedia.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.util.Exec;
import com.openedit.util.ExecResult;

public class FfmpegMetadataExtractor extends MetadataExtractor
{
	private static final Log log = LogFactory.getLog(FfmpegMetadataExtractor.class);
	protected Exec fieldExec;
	
	public boolean extractData(MediaArchive inArchive, File inFile, Asset inAsset)
	{
		String mediatype = inArchive.getMediaRenderType(inAsset.getFileFormat());
		if( "video".equals(mediatype )) 
		{
			//Run it again
			List args = new ArrayList();
			args.add(inFile.getAbsolutePath());
			ExecResult resulttext = getExec().runExec("ffprobe", args, true);
			if( !resulttext.isRunOk())
			{
				String error = resulttext.getStandardError();
				log.info("error " + error);
				return false;
			}
			String textinfo = resulttext.getStandardError();
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

}
