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

public class ImagemagickTranscoder extends BaseTranscoder
{
	private static final Log log = LogFactory.getLog(ImagemagickTranscoder.class);

	protected String fieldPathToProfile;

	public String getPathtoProfile()
	{
		if (fieldPathToProfile == null)
		{
			Page profile = getPageManager().getPage("/system/components/conversions/tinysRGB.icc");
			fieldPathToProfile = profile.getContentItem().getAbsolutePath();
		}
		return fieldPathToProfile;
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
		boolean maintaintransparency = inStructions.isTransparencyMaintained(tmpinput);

		String ext = null;
		if(asset != null)
		{
			if( tmpinput == null)
			{
				tmpinput = asset.getFileFormat();
			}
			ext = asset.getDetectedFileFormat();
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

		if (inStructions.getMaxScaledSize() != null)
		{
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

					int prefw = inStructions.getMaxScaledSize().width;
					int prefh = inStructions.getMaxScaledSize().height;

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
			if (!inStructions.isCrop())
			{
				com.add("-resize");
				String prefix = null;
				String postfix = null;
				prefix = String.valueOf(inStructions.getMaxScaledSize().width);
				postfix = String.valueOf(inStructions.getMaxScaledSize().height);
				
				String resizestring = prefix + "x" + postfix;
				Boolean fillarea = Boolean.parseBoolean(inStructions.get("minsize"));
				if(fillarea)
				{
					resizestring = resizestring + "^";
				}
				else
				{
					Boolean scaleup = Boolean.parseBoolean(inStructions.get("scaleup"));
					if(scaleup == false) 
					{
						resizestring = resizestring + ">"; //Default behavior
					}
				}

				if (isOnWindows())
				{
					com.add("\"" + resizestring + "\"");
				}
				else
				{
					com.add(resizestring);
				}
			}

		}

		if (inStructions.isCrop())
		{
			boolean croplast = Boolean.parseBoolean(inStructions.get("croplast"));
			//resize then cut off edges so end up with a square image
			if (!croplast)
			{
				com.add("-resize");
				StringBuffer resizestring = new StringBuffer();
				resizestring.append(inStructions.getMaxScaledSize().width);
				resizestring.append("x");
				resizestring.append(inStructions.getMaxScaledSize().height);
				resizestring.append("^");
				com.add(resizestring.toString());
			}

			//This gravity is the relative point of the crop marks
			setValue("gravity", "NorthWest", inStructions, com);

			createBackground(inStructions, com, maintaintransparency, ext);

			com.add("-crop");
			StringBuffer cropString = new StringBuffer();
			String cropwidth = inStructions.get("cropwidth");
			if (cropwidth == null)
			{
				cropwidth = String.valueOf(inStructions.getMaxScaledSize().getWidth());
			}
			cropString.append(cropwidth);
			cropString.append("x");
			String cropheight = inStructions.get("cropheight");

			if (cropheight == null)
			{
				cropheight = String.valueOf(inStructions.getMaxScaledSize().getHeight());
			}
			cropString.append(cropheight);

			String x1 = inStructions.get("x1");
			String y1 = inStructions.get("y1");

			cropString.append("+");
			if (x1 == null)
			{
				cropString.append("0");
			}
			else
			{
				cropString.append(x1);
			}
			cropString.append("+");
			if (y1 == null)
			{
				cropString.append("0");
			}
			else
			{
				cropString.append(y1);
			}
			com.add(cropString.toString());
			com.add("+repage");
			if (croplast)
			{
				com.add("-resize");
				StringBuffer resizestring = new StringBuffer();
				resizestring.append(inStructions.getMaxScaledSize().width);
				resizestring.append("x");
				resizestring.append(inStructions.getMaxScaledSize().height);
				resizestring.append("^");
				com.add(resizestring.toString());
			}
		}
		else
		{
			createBackground(inStructions, com, maintaintransparency, ext);
			Boolean extent = Boolean.parseBoolean(inStructions.get("extent"));
			if( extent)
			{
				//This gravity is the relative point of the crop marks
				setValue("gravity", "center", inStructions, com);

				String extentw = inStructions.get("extentwidth");
				String extenth = inStructions.get("extentheight");
				com.add("-extent");
				com.add(extentw + "x" + extenth);
				

			}
		}
		String dpi = inStructions.get("dpi");
		if (dpi != null)
		{
			//-set units PixelsPerInch -density 300
			com.add("-set");
			com.add("units");
			com.add("PixelsPerInch");
			
			com.add("-set");
			com.add("density");
			com.add(dpi);
		}
		
		setValue("quality", "89", inStructions, com);
		//add sampling-factor if specified
		if (inStructions.get("sampling-factor") != null)
		{
			//-sampling-factor 4:2:0
			com.add("-sampling-factor");
			com.add(inStructions.get("sampling-factor"));
		}

		if (!maintaintransparency)
		{
			if ("eps".equals(tmpinput) 
					|| "pdf".equals(tmpinput) 
					|| "ps".equals(tmpinput)
					|| "psd".equals(tmpinput)
					|| "ai".equals(tmpinput)
					|| "tif".equals(tmpinput)
					|| "tiff".equals(tmpinput)
					)
			{
				setValue("colorspace", "sRGB", inStructions, com);
				//Not compatible with profile at the same time with colorspace
				//setValue("profile", getPathtoProfile(), inStructions, com);
				
			}
			else
			{
				if(!(asset.get("colorprofiledescription")!= null && asset.get("colorprofiledescription").contains("ProPhoto"))) {
					com.add("-strip"); //This removes the extra profile info   TODO: Get rid of this fix
					setValue("profile", getPathtoProfile(), inStructions, com);
				}
			}
		}
		com.add("-auto-orient");

		if (inStructions.get("sepia-tone") != null)
		{
			com.add("-sepia-tone");
			com.add(inStructions.get("sepia-tone") + "%");
		}

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
			String output = execresult.getStandardOut();
			if(output != null && output.contains("warning/tiff.c")) {
				result.setComplete(true);
				log.info("Convert complete in:" + (System.currentTimeMillis() - start) + " " + inOutFile.getName());
				result.setOk(true);
			}else {
			result.setError(execresult.getStandardOut());
			}
		}
		return result;
	}

	protected void createBackground(ConvertInstructions inStructions, List<String> com, boolean usepng, String ext)
	{
		if (!usepng && ("eps".equals(ext) || "pdf".equals(ext) || "png".equals(ext) || "gif".equals(ext)))
		{
			com.add("-background");
			com.add("white");
			com.add("-flatten");
		
		}
		else if ("svg".equals(ext)) //add svg support; include transparency
		{
			com.add("-background");
			com.add("transparent");
			com.add("-flatten");
		}
		else
		{
			setValue("background", null, inStructions, com);
			setValue("layers", null, inStructions, com);
		}
	}

	protected List<String> createCommand(ConvertInstructions inStructions)
	{

		String tmpinput = PathUtilities.extractPageType(inStructions.getInputFile().getPath());

	//			ext = tmpinput;

	
		List<String> com = new ArrayList<String>();
	
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
