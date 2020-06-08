package org.entermediadb.asset.convert.transcoders;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.convert.BaseTranscoder;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.ExecResult;

//apt-get install libavcodec-extra-53

public class FfmpegVideoTranscoder extends BaseTranscoder
{
	private static final Log log = LogFactory.getLog(FfmpegVideoTranscoder.class);

	public ConvertResult convert(ConvertInstructions inStructions)
	{

		ContentItem inputpage = inStructions.getInputFile();

		if (inputpage == null || !inputpage.exists())
		{
			//no such original
			log.info("Original does not exist: " + inStructions.getAsset().getSourcePath());
			ConvertResult result = new ConvertResult();
			result.setOutput(inStructions.getOutputFile());
			result.setOk(false);
			return result;
		}
		ConvertResult result = null;
		String streams = inStructions.get("hlsstreams");
		
		if( streams != null )
		{
			String[] vals = MultiValued.VALUEDELMITER.split(streams);
			inStructions.getAsset().setValue("hlsstreams", Arrays.asList(vals));
			inStructions.getMediaArchive().saveAsset(inStructions.getAsset());
			result = createHlsOutput(inStructions, vals);
		}
		else
		{
			result = createMp4Output(inStructions);
		}
		return result;
	}


	
	protected ConvertResult createHlsOutput(ConvertInstructions inStructions, String[] streams)
	{
		
		long timeout = inStructions.getConversionTimeout();
		ContentItem inputpage = inStructions.getInputFile();
		ArrayList<String> comm = new ArrayList<String>();
		comm.add("-i");
		comm.add(inputpage.getAbsolutePath());
		comm.add("-y");

		StringBuffer path = new StringBuffer();
		path.append("/WEB-INF/data");
		path.append(inStructions.getMediaArchive().getCatalogHome());
		path.append("/generated/");
		path.append(inStructions.getAssetSourcePath());
		path.append("/video.m3u8");
		
		for (int i = 0; i < streams.length; i++)
		{
			String height = streams[i];
			String fullpath = path.toString() + "/" + height + "/video.m3u8";
			ContentItem item = inStructions.getMediaArchive().getContent(fullpath);//inStructions.getOutputFile().getAbsolutePath(); //video.hls 
			new File(item.getAbsolutePath()).getParentFile().mkdirs();
			append(comm,height, item.getAbsolutePath());
		}
		//Check the mod time of the video. If it is 0 and over an hour old then delete it?
		ConvertResult result = new ConvertResult();
		ExecResult execresult = getExec().runExec("avconv", comm, true,timeout);
		result.setOk(execresult.isRunOk());
		if (!execresult.isRunOk())
		{
			String output = execresult.getStandardError();
			result.setError("Error: " + output);
			return result;
		}
		else
		{
			result.setComplete(true);
		}
		return result;
	}

	protected void append(Collection comm, String inSize, String absoutputpath)
	{
		/*
		ffmpeg -hide_banner -y -i  input-file.ext\
		-vf scale=w=-2:h=360:force_original_aspect_ratio=decrease -c:a aac -ar 48000 -c:v h264 -profile:v baseline -sc_threshold 0 -g 48 -keyint_min 48 -hls_time 4 -hls_playlist_type vod -crf 28 -b:a 96k /mnt/hls//360p/.m3u8 \
		-vf scale=w=-2:h=480:force_original_aspect_ratio=decrease -c:a aac -ar 48000 -c:v h264 -profile:v baseline -sc_threshold 0 -g 48 -keyint_min 48 -hls_time 4 -hls_playlist_type vod -crf 28 -b:a 96k /mnt/hls//480p/.m3u8 \
		-vf scale=w=-2:h=720:force_original_aspect_ratio=decrease -c:a aac -ar 48000 -c:v h264 -profile:v baseline -sc_threshold 0 -g 48 -keyint_min 48 -hls_time 4 -hls_playlist_type vod -crf 28 -b:a 96k /mnt/hls//720p/.m3u8 \
		-vf scale=w=-2:h=1080:force_original_aspect_ratio=decrease -c:a aac -ar 48000 -c:v h264 -profile:v baseline -sc_threshold 0 -g 48 -keyint_min 48 -hls_time 4 -hls_playlist_type vod -crf 28 -b:a 96k /mnt/hls//1080p/.m3u8 \ 
		*/		
		String command = "-vf scale=w=-2:h=" + inSize;
		command = command + ":force_original_aspect_ratio=decrease -c:a aac -ar 48000 -c:v h264 -profile:v baseline -sc_threshold 0 -g 48 -keyint_min 48 -hls_time 4 -hls_playlist_type vod -crf 28 -b:a 96k";
		comm.addAll(Arrays.asList(command.split("\\ ")));
		//Add path
		comm.add(absoutputpath);
	}


	public ConvertResult createMp4Output(ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();
		result.setOutput(inStructions.getOutputFile());

		Asset inAsset = inStructions.getAsset();
		
		String outputExt = inStructions.getOutputExtension();
		long timeout = inStructions.getConversionTimeout();
		ContentItem inputpage = inStructions.getInputFile();
		ArrayList<String> comm = new ArrayList<String>();
		comm.add("-i");
		comm.add(inputpage.getAbsolutePath());
		comm.add("-y");

//this option and it's attribute is probably unnecessary for aac now. the native aac codec is considered as stable (if avconv source is younger than Dec 5 2015).
		comm.add("-strict");
		comm.add("experimental");

		//audio
		setValue("acodec", "aac", inStructions, comm); // libmp3lame libopus

		if (inStructions.get("pre") == null)  // changed to 'pre' in avconv. presetname: 'foo' -> 'libx264-foo.avpreset' in '~/.avconv' / filename: [codec]-[presetname].avpreset / http://libav.org/avconv.html#Preset-files
		{
			setValue("ab", "96k", inStructions, comm); //legacy. audio bit rate, alias for 'b:a', see code below.
			setValue("ar", "44100", inStructions, comm); //audio sample rate
			setValue("ac", "1", inStructions, comm); //audiochannels
		}
		else
		{
			comm.add("-pre");
			comm.add(inStructions.get("pre"));
		}
		comm.add("-nostats");

		setValue("threads", "2", inStructions, comm); // 0=auto, but leave some cores for the server's workload
		setValue("b", null, inStructions, comm); // Legacy. Overall bitrate of the file, it might be better to specify it individually for video and audio streams.
//video
		setValue("vcodec", "libx264", inStructions, comm); // libvpx libvpx-vp9 libx265 vaapi_h264/265 vaapi_vp8/9 hw codecs if supported
		setValue("preset", null, inStructions, comm); // codec-specific preset (e.g. for x264: ultrafast, superfast, veryfast, faster, fast, medium (default), slow, slower, veryslow)
//			setValue("vpre", null, inStructions, comm); // comes from ffmpeg, not supported in simplified avconv anymore. use 'pre' instead.
		setValue("b:v", null, inStructions, comm); // Bitrate video
		setValue("crf", "28", inStructions, comm); //constant rate factor (constant quality mode). Lower numbers mean higher quality and a larger output file size. A sane range is 18 to 28. Defaults to 23. A change of ±6 should result in about half/double the file size.
		setValue("qscale", null, inStructions, comm); // Use fixed quality scale (VBR).
		setValue("r", null, inStructions, comm); //the framerate setting. converts the video to the desired framerate, if set.
		setValue("profile:v", null, inStructions, comm); // libx264: baseline, main, high, ... 
		setValue("filter:v", null, inStructions, comm); //videofilters (yadif, hqn3d, ...) 
//more audio / why here?
		setValue("b:a", null, inStructions, comm); // Bitrate Audio. same as 'ab' above.
		setValue("profile:a", null, inStructions, comm); // aac_low (default) aac_main
		setValue("filter:a", null, inStructions, comm); //audiofilters (channelmap, volume, ...) 

		setValue("max_muxing_queue_size", null, inStructions, comm); // 0=auto, but leave some cores for the server's workload

		
		
		//comm.add("-aspect");
		//comm.add("640:480");


		if (inStructions.get("setpts") != null) //what is this?!?
//to slow down or speed up. this is actually an attribute to '-filter:v'.
//there is 'setpts' (video) and also 'setapts' (audio) to change the presetation time of the mediastream.
		{
			comm.add("-filter:v setpts=" + inStructions.get("setpts") + "*PTS"); //one block?
		}

		//add calculations to fix letterbox problems
		//http://howto-pages.org/ffmpeg/
		int width = inStructions.intValue("prefwidth", 640);
		int height = inStructions.intValue("prefheight", 360);

		//				if( inStructions.getMaxScaledSize() != null )
		//				{
		//					width = inStructions.getMaxScaledSize().width;
		//					height = inStructions.getMaxScaledSize().height;
		//				}

		int aw = inAsset.getInt("width");
		int ah = inAsset.getInt("height");
		if (aw > width || ah > height)
		{
			float ratio = (float) aw / (float) ah;
			float ratiodest = (float) width / (float) height;
			if (ratiodest > ratio) //is dest wider than the input
			{
				//original video has a wider ratio so we need to adjust height in proportion
				float change = (float) height / (float) ah;
				width = Math.round((float) aw * change);
			}
			else if (ratiodest < ratio)
			{
				//too wide, need to padd top
				float change = (float) width / (float) aw;
				height = Math.round((float) ah * change);
			}
			else
			{
				//no math needed
			}
		}
		else
		{
			// Asset has smaller size than destination

			boolean scaledownonly = true;
			if (inStructions.get("scaledownonly") != null)
			{
				scaledownonly = new Boolean(inStructions.get("scaledownonly"));
			}

			//log.info(scaledownonly);

			if (scaledownonly)
			{
				// Make destination size the same as original
				width = aw;
				height = ah;
			}
		}
		if (width > 1 && height > 1)
		{
			//must be even
			if ((width % 2) != 0)
			{
				width++;
			}
			if ((height % 2) != 0)
			{
				height++;
			}
			//http://stackoverflow.com/questions/20847674/ffmpeg-libx264-height-not-divisible-by-2
			comm.add("-s");
			comm.add(width + "x" + height);
		}

		
		
		//640x360 853x480 704x480 = 480p
		/*
		 * Here is a two pass mp4 convertion with mp3 audio The second pass
		 * lets the bit rate be more constant for buffering downloads
		 * 
		 * #ffmpeg -i smb_m48020080421.mov -vcodec mpeg4 -pass 1 -vtag xvid
		 * -r 25 -b 2000k -acodec libmp3lame -s vga -ab 96k -ar 44100 -ac 1
		 * bigmono2k960output2p.mp4 #ffmpeg -i smb_m48020080421.mov -vcodec
		 * mpeg4 -pass 2 -vtag xvid -r 25 -b 2000k -acodec libmp3lame -s vga
		 * -ab 96k -ar 44100 -ac 1 bigmono2k960output2p.mp4
		 * 
		 * Here is a simple PCM audio format for low CPU devices // ffmpeg
		 * -i smb_m48020080421.mov -vcodec mpeg4 -vtag xvid -r 25 -b 2000k
		 * -acodec pcm_s16le -s vga -ar 44100 -ac 1 pcmmono2k960output2p.avi
		 */
		String outpath = null;
		boolean h264 = outputExt.equalsIgnoreCase("mp4") || outputExt.equalsIgnoreCase("m4v");

		if (h264)
		{
			outpath = inStructions.getOutputFile().getAbsolutePath() + "tmp.mp4";
			File tmp = new File(outpath);
			tmp.deleteOnExit();
			if (tmp.exists())
			{
				long old = tmp.lastModified();
				if (System.currentTimeMillis() - old < (1000 * 60 * 60)) //something is processing this within the last hour
				{
					log.info("Existing video conversion trying again " + inStructions.getMediaArchive().getCatalogId() + " " + inStructions.getAssetId());
					result.setComplete(false);
					result.setOk(true);
					return result;
				}
				else
				{
					throw new OpenEditException("Older video existing conversion found, marking as error " + inStructions.getMediaArchive().getCatalogId() + " " + inStructions.getAssetId());
				}
			}
		}
		else
		{
			outpath = inStructions.getOutputFile().getAbsolutePath();
		}
		comm.add(outpath);
		new File(outpath).getParentFile().mkdirs();
		//Check the mod time of the video. If it is 0 and over an hour old then delete it?

		ExecResult execresult = getExec().runExec("avconv", comm, true,timeout);
		result.setOk(execresult.isRunOk());
		if (!execresult.isRunOk())
		{
			String output = execresult.getStandardError();
			result.setError("Error: " + output);
			return result;
		}
		if (h264)
		{
			comm = new ArrayList();
			comm.add(inStructions.getOutputFile().getAbsolutePath() + "tmp.mp4");
			comm.add(inStructions.getOutputFile().getAbsolutePath());
			boolean ok = runExec("qt-faststart", comm, timeout);
			result.setOk(ok);
			Page old = getPageManager().getPage(inStructions.getOutputFile().getPath() + "tmp.mp4");
			old.getContentItem().setMakeVersion(false);
			getPageManager().removePage(old);
		}
		result.setComplete(true);
		return result;
	}
	
}
