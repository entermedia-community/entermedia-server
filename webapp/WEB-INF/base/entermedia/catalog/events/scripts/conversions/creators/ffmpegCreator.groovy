package conversions.creators;


import java.io.File;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.creator.BaseCreator 
import org.openedit.entermedia.creator.ConvertInstructions 
import org.openedit.entermedia.creator.ConvertResult 
import org.openedit.entermedia.creator.FfMpegVideoCreator 
import org.openedit.entermedia.creator.MediaCreator 

import com.openedit.page.Page;
import com.openedit.util.PathUtilities;
import org.openedit.Data;


public class ffmpegCreator extends BaseCreator implements MediaCreator
{
	private static final Log log = LogFactory.getLog(this.class);

	public boolean canReadIn(MediaArchive inArchive, String inInput)
	{
		return true;//"flv".equals(inOutput) || mpeg; //This has a bunch of types
	}
	

	public ConvertResult convert(MediaArchive inArchive, Asset inAsset, Page converted, ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();
		Page inputpage = inArchive.findOriginalMediaByType("video",inAsset);
		
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
			
				ArrayList<String> comm = new ArrayList<String>();
				comm.add("-i");
				comm.add(abspath);
				comm.add("-y");

				//audio
				comm.add("-acodec");
				if( inStructions.get("acodec") == null )
				{
					comm.add("libfaac"); //libfaac  libmp3lame
				}
				else
				{
					comm.add(inStructions.get("acodec") );
				}

				//general settings?
				if( inStructions.get("fpre") == null )
				{
					comm.add("-ab");
					comm.add("96k");
					comm.add("-ar");
					comm.add("44100"); 
					comm.add("-ac");
					comm.add("1"); //mono
				}
				else
				{
					comm.add("-fpre");
					comm.add(inStructions.get("fpre"));				
				}
				
				//video
				comm.add("-vcodec");
				if( inStructions.get("vcodec") == null )
				{
					comm.add("libx264");
				}
				else
				{
					comm.add(inStructions.get("vcodec") );
				}

				if( inStructions.get("vpre") == null )
				{	
					comm.add("-vpre");
					comm.add("normal");
				}
				else
				{
					comm.add("-vpre");
					comm.add(inStructions.get("vpre"));
				}
				comm.add("-crf");
				comm.add("28");  
				//One-pass CRF (Constant Rate Factor) using the slow preset. One-pass CRF is good for general encoding and is what I use most often. Adjust -crf to change the quality. Lower numbers mean higher quality and a larger output file size. A sane range is 18 to 28.
				//ffmpeg -i input.avi -acodec libfaac -ab 128k -ac 2 -vcodec libx264 -vpre slow -crf 22 -threads 0 output.mp4
				
				//comm.add("-aspect");
				//comm.add("640:480");
				comm.add("-threads");
				comm.add("0");

				//add calculations to fix letterbox problems
				//http://howto-pages.org/ffmpeg/
				int width = 640; //this is the player size. Now we need to change aspect
				int height = 360;
				
				if( inStructions.getMaxScaledSize() != null )
				{
					width = inStructions.getMaxScaledSize().width;
					height = inStructions.getMaxScaledSize().height;
				}
				int aw = inAsset.getInt("width");
				int ah = inAsset.getInt("height");
				if( aw > width || ah > height)
				{
					float ratio = (float)aw / (float)ah;
					float ratiodest = (float)width / (float)height;
					if( ratiodest > ratio ) //is dest wider than the input
					{
						//original video has a wider ratio so we need to adjust height in proportion
						float change = (float)height / (float)ah;
						width = Math.round((float)aw * change);
					}
					else if ( ratiodest < ratio)
					{
						//too wide, need to padd top
						float change = (float)width / (float)aw;
						height = Math.round((float)ah * change);
					}
					else
					{
						//no math needed
					}
					//must be even
					if( ( width % 2 ) != 0 )
					{
						width++;
					}
					if( ( height % 2 ) != 0 )
					{
						height++;
					}
				}
				comm.add("-s");
				comm.add(width + "x"  + height);					
				
				
				//640x360 853x480
/*
 Here is a two pass mp4 convertion with mp3 audio
 The second pass lets the bit rate be more constant for buffering downloads
 
  				#ffmpeg -i smb_m48020080421.mov  -vcodec mpeg4  -pass 1 -vtag xvid -r 25 -b 2000k -acodec libmp3lame -s vga -ab 96k -ar 44100  -ac 1   bigmono2k960output2p.mp4
				#ffmpeg -i smb_m48020080421.mov  -vcodec mpeg4  -pass 2 -vtag xvid -r 25 -b 2000k -acodec libmp3lame -s vga -ab 96k -ar 44100  -ac 1   bigmono2k960output2p.mp4

Here is a simple PCM audio format for low CPU devices
//				ffmpeg -i smb_m48020080421.mov  -vcodec mpeg4   -vtag xvid -r 25 -b 2000k -acodec pcm_s16le -s vga  -ar 44100  -ac 1   pcmmono2k960output2p.avi
*/
				String outpath = null;
				boolean h264 = outputExt.equalsIgnoreCase("mp4") || outputExt.equalsIgnoreCase("m4v");
				
				if( h264)
				{
					outpath = converted.getContentItem().getAbsolutePath() + "tmp.mp4";
					File tmp = new File(outpath);
					if( tmp.exists())
					{
						long old = tmp.lastModified();
						if( System.currentTimeMillis() - old < (1000 * 60 * 60)   ) //something is processing this
						{
							log.info("Video still being processed, skipping 2nd request");
							return result;
						}
					}
				}
				else
				{
					outpath = converted.getContentItem().getAbsolutePath();
				}
				comm.add(outpath);
				new File( outpath).getParentFile().mkdirs();
				//Check the mod time of the video. If it is 0 and over an hour old then delete it?
				
				boolean ok =  runExec("ffmpeg", comm);
				result.setOk(ok);
				result.setComplete(true);
				if(ok && h264)
				{
					comm = new ArrayList();
					comm.add(converted.getContentItem().getAbsolutePath()  + "tmp.mp4");					
					comm.add(converted.getContentItem().getAbsolutePath());
					ok =  runExec("qt-faststart", comm);
					result.setOk(ok);					
					Page old = getPageManager().getPage(converted.getContentItem().getPath() + "tmp.mp4");
					old.getContentItem().setMakeVersion(false);
					getPageManager().removePage(old);
				}				

			
		}
		else
		{
			log.info("FFMPEG Conversion not required, already complete. ");
			result.setOk(true);
			result.setComplete(true);
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
		
		path.append("/video." + inStructions.getOutputExtension());
		inStructions.setOutputPath(path.toString());
		return path.toString();
	}
	
}