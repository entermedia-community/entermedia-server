package org.entermediadb.asset.convert.transcoders;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.BaseTranscoder;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.ExecResult;
import org.openedit.util.PathUtilities;

public class ffmpegAudioTranscoder extends BaseTranscoder
{
	private static final Log log = LogFactory.getLog(ffmpegAudioTranscoder.class);

	public ConvertResult convert(ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();
		result.setOutput(inStructions.getOutputFile());

		Asset asset = inStructions.getAsset();
		Page inputpage = inStructions.getMediaArchive().getOriginalDocument(asset);

		if (inputpage == null || !inputpage.exists())
		{
			//no such original
			log.info("Original does not exist: " + asset.getSourcePath());
			result.setOk(false);

			return result;
		}
		/*
		 * 
		 * <property id="flac" rendertype="audio"
		 * synctags="false">Flac</property> <property id="m4a"
		 * rendertype="audio" synctags="false">M4A</property> <property id="aac"
		 * rendertype="audio" synctags="false">aac</property>
		 */
		String inputExt = PathUtilities.extractPageType(inputpage.getContentItem().getAbsolutePath());
		String outputExt = inStructions.getOutputExtension();
//		String useoriginalmediawhenpossible = inStructions.getProperty("useoriginalmediawhenpossible");
//		if (Boolean.parseBoolean(useoriginalmediawhenpossible) && outputExt != null && outputExt.equals(inputExt))
//		{
//			createFallBackContent(inputpage, inStructions.getOutputFile());
//			result.setOk(true);
//		}
//		else
//		{
		long timeout = inStructions.getConversionTimeout();
		String inOutputType = inStructions.getOutputExtension();
		if ("wma".equalsIgnoreCase(inputExt) || "aac".equalsIgnoreCase(inputExt) || "m4a".equalsIgnoreCase(inputExt) || "flac".equalsIgnoreCase(inputExt) || "ogg".equalsIgnoreCase(inputExt))
		{
			runFfmpeg(inputpage, inStructions, result, timeout);
		}
		else
		{
			runLame(inputpage, inStructions, result, timeout);
		}
//		}
		if (result.isOk())
		{
			result.setComplete(true);
		}

		return result;
	}

	private void runLame(Page input, ConvertInstructions inStructions, ConvertResult result, long inTimeout)
	{
		String inputExt = PathUtilities.extractPageType(input.getContentItem().getAbsolutePath());
		long start = System.currentTimeMillis();

		//InputStream inputstream = null;
		try
		{
			//inputstream = input.getInputStream();
			List args = new ArrayList();
			String bitRate = inStructions.getProperty("bitrate");
			if (bitRate == null)
			{
				bitRate = "96";
			}
			args.add("-b");
			args.add(bitRate);

			//11.025 kHz 22.050 kHz or 44.100 kHz.
			String resample = inStructions.getProperty("resample");
			if (resample == null)
			{
				resample = "22.05";
			}
			args.add("--resample");
			args.add(resample);

			if (inputExt == "mp2")
			{
				args.add("--mp2input");
			}

			if (inputExt == "mp3")
			{
				args.add("--mp3input");
			}
			args.add("--silent");
			//args.add("-");
			ContentItem output = inStructions.getOutputFile();
			if (isOnWindows())
			{
				args.add("\"" + input.getContentItem().getAbsolutePath() + "\"");
				args.add("\"" + output.getAbsolutePath() + "\"");
			}
			else
			{
				args.add(input.getContentItem().getAbsolutePath());
				args.add(output.getAbsolutePath());
			}
			//make sure this folder exists
			new File(output.getAbsolutePath()).getParentFile().mkdirs();

			ExecResult res = getExec().runExec("lame", args, inTimeout);
			result.setOk(res.isRunOk());
		}
		catch (Exception ex)
		{
			StringWriter out = new StringWriter();
			ex.printStackTrace(new PrintWriter(out));
			log.error(out.toString());
			result.setError(out.toString());
			result.setOk(false);
		}
		//		finally
		//		{
		//			FileUtils.safeClose(inputstream);
		//		}
		String message = "mp3 created";
		if (!result.isOk())
		{
			message = "mp3 creation failed";
		}
		log.info(message + " in " + (System.currentTimeMillis() - start) / 1000L + " seconds");
	}

	private void runFfmpeg(Page input, ConvertInstructions inStructions, ConvertResult result, long inTimeout)
	{
		long start = System.currentTimeMillis();

		ArrayList<String> comm = new ArrayList<String>();
		comm.add("-i");
		comm.add(input.getContentItem().getAbsolutePath());
		comm.add("-y");
		//audio
		comm.add("-acodec");
		comm.add("libmp3lame");

		//comm.add("libfaac"); //libfaac  libmp3lame
		comm.add("-ab");
		String bitRate = inStructions.getProperty("bitrate");
		if (bitRate == null)
		{
			bitRate = "96";
		}
		comm.add(bitRate + "k");
		//					comm.add("-ar");
		//					comm.add("44100");
		comm.add("-ac");
		comm.add("1"); //mono

		comm.add("-vn");
		String outpath = null;

		outpath = inStructions.getOutputFile().getAbsolutePath();
		comm.add(outpath);
		new File(outpath).getParentFile().mkdirs();
		//Check the mod time of the video. If it is 0 and over an hour old then delete it?

		//boolean ok =  runExec("ffmpeg", comm);
		boolean ok = runExec("avconv", comm, inTimeout);

		result.setOk(ok);
		log.info("ok: ${ok} in " + (System.currentTimeMillis() - start) / 1000L + " seconds");
	}



}