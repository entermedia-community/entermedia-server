/*
 * Created on Sep 20, 2005
 */
package org.entermediadb.asset.convert.transcoders;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.convert.BaseTranscoder;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;

public class FfmpegImageTranscoder extends BaseTranscoder
{
	private static final Log log = LogFactory.getLog(FfmpegImageTranscoder.class);
	protected String fieldCommandName = "avconv"; // ffmpeg -itsoffset 10

	// -deinterlace -i $TRACK -y
	// -vframes 1 -f mjpeg
	// $OUTPUT


	public String getCommandName()
	{
		return fieldCommandName;
	}

	public void setCommandName(String inCommandName)
	{
		fieldCommandName = inCommandName;
	}
	public ConvertResult convert(ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();
		
		ContentItem outputFile = inStructions.getOutputFile();
		if(!inStructions.isForce() && outputFile.getLength() > 0 )
		{
			result.setOk(true);
			result.setComplete(true);
			return result;
		}
		result.setOk(true);
		
		Page customthumb = getPageManager().getPage("/WEB-INF/data" + inStructions.getMediaArchive().getCatalogHome() + "/generated/" + inStructions.getAssetSourcePath() + "/customthumb.jpg");
		if( customthumb.exists() )
		{
			Page destination = getPageManager().getPage(inStructions.getOutputPath());
			getPageManager().copyPage(customthumb,destination);
			result.setComplete(true);
			return result;
		}
		

		// We are going to take frames from the converted flv video
//		ConvertInstructions ci = new ConvertInstructions();
//		ci.setAssetSourcePath(inAsset.getSourcePath());
//		ci.setOutputExtension("flv");
//		inArchive.getCreatorManager().getMediaCreatorByOutputFormat("flv").populateOutputPath(inArchive, ci);
		Page input = getPageManager().getPage("/WEB-INF/data" + inStructions.getMediaArchive().getCatalogHome() + "/generated/" + inStructions.getAssetSourcePath() + "/video.mp4");
		
//		Page input = getPageManager().getPage(ci.getOutputPath());
		
		// Or the original file, if the flv does not exist
		if( !input.exists() || input.length() == 0)
		{
			result.setOk(false);
            log.info("Input not ready yet" + input.getPath() );
			return result;
		}
		
		//get timeout
		long timeout = inStructions.getConversionTimeout();
		
		String offset = inStructions.getProperty("timeoffset");
		if( offset == null)
		{
			offset = "2";
		}
		try
		{
			offset = String.valueOf(Integer.parseInt(offset));
		}
		catch( Exception e )
		{
			log.error(e);
			offset = "0";
		}
		if( input.length() < 1000000 )
		{
			offset = "0";
		}
		
		List<String> com = new ArrayList<String>();

		int jumpoff = Integer.parseInt(offset);
		if( jumpoff > 2 )
		{
			com.add("-ss");
			com.add(String.valueOf( jumpoff - 2 ) );
			offset = "2";
		}

		//com.add("-deinterlace");
		com.add("-i");
		com.add(input.getContentItem().getAbsolutePath()); // TODO: Might need [0] to pick the
		// first image only
		com.add("-y");
		com.add("-vframes");
		com.add("1");
		com.add("-f");
		com.add("mjpeg");

		com.add("-ss");
		com.add(offset);


		// -s 640x480
		// com.add("-s");
		// com.add( (int)inStructions.getMaxScaledSize().getWidth() + "x" +
		// (int)inStructions.getMaxScaledSize().getHeight() + ">" );
		
		String outputpath = outputFile.getAbsolutePath();
		new File(outputpath).getParentFile().mkdirs();
		com.add(outputpath);
		result.setOutputPath(null);
		long start = System.currentTimeMillis();
		if (runExec(getCommandName(), com, timeout))
		{
			log.info("Resize complete in:" + (System.currentTimeMillis() - start) + " " + outputFile.getName());
			result.setComplete(true);
			result.setOutputPath(outputFile.getAbsolutePath());
		}
		else
		{
			if(!outputFile.exists() || outputFile.getLength() == 0)
			{
				log.info("Thumnail creation failed " + outputpath);
				result.setOk(false);
				result.setError("creation failed" );
			}
		}

		return result;
	}
	
	public String createConvertPath(ConvertInstructions inStructions)
	{
		String frame = inStructions.getProperty("frame");
		if( frame == null )
		{
			frame="0";
		}
		String path = inStructions.getAssetSourcePath() + "frame" + frame + ".jpg";
		return path;
	}

	
}

