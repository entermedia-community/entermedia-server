package org.entermediadb.ooffice;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.BaseTranscoder;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.ExecResult;
import org.openedit.util.PathUtilities;

public class OofficeDocumentTranscoder extends BaseTranscoder
{
//	protected final def formats = ["doc","docx","rtf","ppt","pptx","wps","odt","html","xml","csv", "xls", "xlsx", "odp"];
	private static final Log log = LogFactory.getLog(OofficeDocumentTranscoder.class);
	
	public synchronized ConvertResult convert(ConvertInstructions inStructions)
	{
		MediaArchive inArchive = inStructions.getMediaArchive();
		Asset inAsset = inStructions.getAsset();
		ContentItem inOut = inStructions.getOutputFile();
		
		ConvertResult result = new ConvertResult();
		result.setOutput(inOut);
		result.setOk(false);
		
		Page input = inArchive.findOriginalMediaByType("document",inAsset);
		if( input == null )
		{
			return result;
		}

		List command = new ArrayList();
		
		command.add("-invisible");
		command.add("-headless");
		command.add("-nologo");
		command.add("-norestore");		
		command.add("-nolockcheck");
		
		command.add("-convert-to");		
		command.add("pdf");

		command.add("-outdir");
		
		Page out = getPageManager().getPage(inOut.getPath());
		String outfolder = out.getDirectory();
		String dir = out.getDirectory();
		dir = getPageManager().getPage(dir).getContentItem().getAbsolutePath();
		//log.info("{$inOut} turns into ${dir}");
		//String dir = inStructions.getOutputFile().getAbsolutePath();
		new File( dir ).mkdirs();
		command.add(dir);
		
		command.add(input.getContentItem().getAbsolutePath());
		
		long timeout = inStructions.getConversionTimeout();
		ExecResult done = getExec().runExec("soffice",command, timeout);
		
		result.setOk(done.isRunOk());
		if( done.isRunOk() )
		{
			String newname = PathUtilities.extractPageName(input.getName()) + ".pdf";
			
			Page tmpfile = getPageManager().getPage(outfolder+ "/" + newname);
			if( !tmpfile.exists() || tmpfile.length() == 0)
			{
				throw new OpenEditException("OpenOffice did not create output file " + tmpfile);
			}
			Page output = getPageManager().getPage(inOut.getPath());
			inStructions.setOutputFile(output.getContentItem());
			getPageManager().movePage(tmpfile, output);
			log.info("Completed: " + input.getName());
		}
		else
		{
			log.error("Error running command on : " + input.getName() + " output:" + done.getStandardOut()  + " returned: " + done.getReturnValue() );
		}
	    return result;
	}

//	public String populateOutputPath(MediaArchive inArchive, ConvertInstructions inStructions)
//	{
//		//we only generate PDF for now
//		StringBuffer path = new StringBuffer();
//
//		//legacy for people who want to keep their images in the old location
//		String prefix = inStructions.getProperty("pathprefix");
//		if( prefix != null)
//		{
//			path.append(prefix);
//		}
//		else
//		{
//			path.append("/WEB-INF/data");
//			path.append(inArchive.getCatalogHome());
//			path.append("/generated/");
//		}
//		path.append(inStructions.getAssetSourcePath());
//		path.append("/");
//
//		String postfix = inStructions.getProperty("pathpostfix");
//		if( postfix != null)
//		{
//			path.append(postfix);
//		}	
//		inStructions.setOutputExtension("pdf");
//		path.append("document." + inStructions.getOutputExtension());
//
//		inStructions.setOutputPath(path.toString());
//		return path.toString();
//	}

}
