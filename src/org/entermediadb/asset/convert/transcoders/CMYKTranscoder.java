package org.entermediadb.asset.convert.transcoders;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.convert.BaseTranscoder;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.ExecResult;
import org.openedit.util.PathUtilities;

public class CMYKTranscoder extends BaseTranscoder
{
	private static final Log log = LogFactory.getLog(CMYKTranscoder.class);

	protected String fieldPathToProfile;
	protected String fieldPathToCMYKProfile;

	public String getPathtoProfile()
	{
		if (fieldPathToProfile == null)
		{
			Page profile = getPageManager().getPage("/system/components/conversions/tinysRGB.icc");
			fieldPathToProfile = profile.getContentItem().getAbsolutePath();
		}
		return fieldPathToProfile;
	}

	public String getPathCMYKProfile()
	{
		if (fieldPathToCMYKProfile == null)
		{
			Page profile = getPageManager().getPage("/system/components/conversions/USWebCoatedSWOP.icc");
			fieldPathToCMYKProfile = profile.getContentItem().getAbsolutePath();
		}
		return fieldPathToCMYKProfile;
	}

	@Override
	public ConvertResult convert(ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();
		result.setOutput(inStructions.getOutputFile());
		//MediaArchive archive = inStructions.getMediaArchive();
		Asset asset = inStructions.getAsset();
		ContentItem inOutFile = inStructions.getOutputFile();
		String outputpath = inOutFile.getAbsolutePath();

		String tmpinput = PathUtilities.extractPageType(inStructions.getInputFile().getPath(),true);

		String ext = null;
		if(asset != null)
		{
			if( tmpinput == null)
			{
				tmpinput = asset.getFileFormat();
			}
			ext = asset.getFileFormat();
			if (ext == null)
			{
				ext = tmpinput;
			}
		}
		//File inputFile = new File(input.getContentItem().getAbsolutePath());
		//		String newext = PathUtilities.extractPageType( input.getPath() );
		//		if( newext != null && newext.length()> 1)
		//		{
		//			ext = newext.toLowerCase();
		//		}
		List<String> com = createCommand(inStructions);

		//be aware ImageMagick writes to a tmp file with a larger version of the file before it is finished
		if ("eps".equalsIgnoreCase(ext) || "pdf".equalsIgnoreCase(ext) || "ai".equalsIgnoreCase(ext))
		{
			//check input width
			int width = asset.getInt("width");
			if (width > 0)
			{
				// calculate output width
				int height = asset.getInt("height");
				double ratio = height / width;

				int prefw = 1500;
				int prefh = 1500;

				int distw = Math.abs(prefw - width);
				int disth = Math.abs(prefh - height);

				int outputw;
				if (disth < distw)
				{
					outputw = width * (prefh / height);
				}
				else
				{
					outputw = prefw;
				}

				if (width < outputw)
				{
					//for small input files we want to scale up the density
					float density = ((float) outputw / (float) width) * 300f;
					density = Math.max(density, 300);
					density = Math.min(density, 900);
					String val = String.valueOf(Math.round(density));
					com.add(0, val);
					com.add(0, "-density");
				}
				else
				{
					com.add(0, "300");
					com.add(0, "-density");
				}
			}
		}
			com.add("-resize");
			String resizestring = "1500x1500";

			if (isOnWindows())
			{
				com.add("\"" + resizestring + "\"");
			}
			else
			{
				com.add(resizestring);
			}

		com.add("-background");
		com.add("white");
		com.add("-layers");
		com.add("flatten");
		setValue("quality", "89", inStructions, com);
		
		//setValue("profile", getPathtoProfile(), inStructions, com);
		com.add("-auto-orient");

		if (isOnWindows())
		{
			// windows needs quotes if paths have a space
			com.add("\"" + outputpath + "\"");
		}
		else
		{
			com.add(outputpath);
		}

		long start = System.currentTimeMillis();
		new File(outputpath).getParentFile().mkdirs();

		long timeout = inStructions.getConversionTimeout();
		ExecResult execresult = getExec().runExec("convert", com, true, timeout);

		boolean ok = execresult.isRunOk();
		result.setOk(ok);

		if (ok)
		{
			result.setComplete(true);

			log.info("Convert complete in:" + (System.currentTimeMillis() - start) + " " + inOutFile.getName());

			return result;
		}
		//problems
		log.info("Could not exec: " + execresult.getStandardOut());
		if (execresult.getReturnValue() == 124)
		{
			result.setError("Exec timed out after " + timeout);
		}
		else
		{
			result.setError(execresult.getStandardOut());
		}
		return result;
	}

	

	protected List<String> createCommand(ConvertInstructions inStructions)
	{

		String tmpinput = PathUtilities.extractPageType(inStructions.getInputFile().getPath());
	//			ext = tmpinput;
	
		List<String> com = new ArrayList<String>();

		if ("eps".equals(tmpinput) || "pdf".equals(tmpinput) || "ai".equals(tmpinput))
		{
			setValue("colorspace", "sRGB", inStructions, com);
		}
		else //jpg
		{
			com.add("-strip");
			com.add("-profile");
			com.add(getPathCMYKProfile());
		}

		int page = inStructions.getPageNumber();
		page--;
		page = Math.max(0, page);

		String prefix = "";
		String extension = "";
		String filename = inStructions.getInputFile().getName(); //TODO: Remove this old crud?
		int dotIndex = filename.lastIndexOf('.');
		if (dotIndex > 0)
		{
			extension = filename.substring(dotIndex + 1);
		}
		if ("dng".equalsIgnoreCase(extension))
		{
			prefix = "dng:";
		}
		String absolutePath = inStructions.getInputFile().getAbsolutePath();
		if (isOnWindows())
		{
			com.add("\"" + prefix + absolutePath + "[" + page + "]\"");
		}
		else
		{
			com.add(prefix + absolutePath + "[" + page + "]");
		}
		com.add("-limit");
		com.add("thread");
		com.add("1");
		return com;
	}

}
