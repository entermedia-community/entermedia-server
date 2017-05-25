package org.entermediadb.model;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.videoedit.VideoEditor;
import org.openedit.page.Page;
import org.openedit.util.FileUtils;

public class VideoEditTest extends BaseEnterMediaTest
{
	public void testSplit() throws Exception
	{
		String path = "/WEB-INF/data/entermedia/catalogs/testcatalog/generated/videoeditor/video.mp4";
		VideoEditor editor = (VideoEditor)getBean("videoEditor");
		Page video = getPage(path);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		editor.split(video,117,-1,output);
		long videosize = video.length();
		long outputsize = output.size();
		assertTrue( outputsize < videosize );

		OutputStream foutput = new FileOutputStream(new File(video.getContentItem().getAbsolutePath() + "tmp.mp4"));
		foutput = new BufferedOutputStream(foutput);
		editor.split(video,117,-1,foutput);
		FileUtils.safeClose(foutput);
		//long outputsize = foutput.size();
		//assertTrue( outputsize == videosize );

	}
}
