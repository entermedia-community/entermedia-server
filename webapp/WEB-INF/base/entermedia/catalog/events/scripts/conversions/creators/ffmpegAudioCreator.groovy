package conversions.creators;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.creator.*

import com.openedit.page.Page;
import com.openedit.util.PathUtilities;

public class ffmpegAudioCreator extends BaseCreator implements MediaCreator
{
	private static final Log log = LogFactory.getLog(ffmpegAudioCreator.class);

	public boolean canReadIn(MediaArchive inArchive, String inPut)
	{
		return "wma".equalsIgnoreCase(inPut) || "acc".equalsIgnoreCase(inPut);//"flv".equals(inOutput) || mpeg; //This has a bunch of types
	}
	

	public ConvertResult convert(MediaArchive inArchive, Asset inAsset, Page converted, ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();

		Page inputpage = inArchive.findOriginalMediaByType("audio",inAsset);

		if( inputpage == null || !inputpage.exists())
		{
			//no such original
			log.info("Original does not exist: " + inAsset.getSourcePath());
			result.setOk(false);
			return result;
		}
		String abspath = inputpage.getContentItem().getAbsolutePath();
		
		if (inStructions.isForce() || !converted.exists() || converted.getContentItem().getLength() == 0)
		{
			String inputExt = PathUtilities.extractPageType(inputpage.getContentItem().getAbsolutePath());
			String outputExt = inStructions.getOutputExtension();
			if( outputExt != null && outputExt.equals(inputExt))
			{
				createFallBackContent(inputpage, converted);
				result.setOk(true);
			}
			else
			{
				ArrayList<String> comm = new ArrayList<String>();
				comm.add("-i");
				comm.add(abspath);
				comm.add("-y");
					//audio
					comm.add("-acodec");
					//comm.add("libmp3lame"); 
					//comm.add("libfaac"); //libfaac  libmp3lame
					comm.add("libmp3lame");
					comm.add("-ab");
					comm.add("96k");
//					comm.add("-ar");
//					comm.add("44100"); 
					comm.add("-ac");
					comm.add("1"); //mono
			
				String outpath = null;
			
					outpath = converted.getContentItem().getAbsolutePath();
				comm.add(outpath);
				new File( outpath).getParentFile().mkdirs();
				//Check the mod time of the video. If it is 0 and over an hour old then delete it?
				
				boolean ok =  runExec("ffmpeg", comm);
				result.setOk(ok);
				
			

			}
		}
		return result;
	}
	public ConvertResult applyWaterMark(MediaArchive inArchive, File inConverted, File inWatermarked, ConvertInstructions inStructions)
	{
		return null;
	}
	
	public String createConvertPath(ConvertInstructions inStructions)
	{
		String path = inStructions.getAssetSourcePath() + "video." + inStructions.getOutputExtension();
		
		return path;
	}
	public String populateOutputPath(MediaArchive inArchive, ConvertInstructions inStructions)
	{
		StringBuffer path = new StringBuffer();
		String prefix = inStructions.getProperty("pathprefix");
		if( prefix != null)
		{
			path.append(prefix);
		}
		else
		{
			path.append("/WEB-INF/data");
			path.append(inArchive.getCatalogHome());
			path.append("/generated/");
		}
		path.append(inStructions.getAssetSourcePath());
		
		path.append("/audio." + inStructions.getOutputExtension());
		inStructions.setOutputPath(path.toString());
		return path.toString();
	}


	

	
}