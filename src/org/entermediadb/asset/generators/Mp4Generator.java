package org.entermediadb.asset.generators;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.entermediadb.videoedit.VideoEditor;
import org.openedit.WebPageRequest;
import org.openedit.generators.Output;
import org.openedit.page.Page;

public class Mp4Generator extends ConvertGenerator
{
	protected VideoEditor fieldVideoEditor;
	
	public VideoEditor getVideoEditor()
	{
		return fieldVideoEditor;
	}

	public void setVideoEditor(VideoEditor inVideoEditor)
	{
		fieldVideoEditor = inVideoEditor;
	}

	protected InputStream streamBinary(WebPageRequest inReq, Page inPage, InputStream in, long start, long end, long length, Output inOut) throws IOException
	{
		if( start == -1)
		{
			//Only used for flash flv files never used for mp4
			String startbytes = inReq.getRequestParameter("start");
			if( startbytes != null)
			{
				double timeoffset = Double.parseDouble(startbytes);
				OutputStream stream = inOut.getStream();
				
				String cutending = inReq.getRequestParameter("endtime");
				double cutto = -1;
				if( cutending != null)
				{
					cutto = Double.parseDouble(cutending);
				}
				
				getVideoEditor().split(inPage, timeoffset,cutto, stream);
				stream.flush();
				return in;
			}
		}
		return super.streamBinary(inReq, inPage, in, start, end, length, inOut);
	}
}
