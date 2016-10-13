package org.entermediadb.asset.generators;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.TranscodeTools;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.FileGenerator;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.util.PathUtilities;

/**
 * This generator generates original asset documents from an MediaArchive
 * based on paths of the form
 * <tt>.../<var>assetid</var>/<var>filename.ext</var></tt>.
 * 
 * @author Eric Galluzzo
 */
public class ConvertGenerator extends FileGenerator
{
	//private static final Log log = LogFactory.getLog(ConvertGenerator.class);

	protected ModuleManager fieldModuleManager;

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public void generate(WebPageRequest inReq, Page inPage, Output inOut) throws OpenEditException
	{
		//TODO: Revamp all API to use ContentItem instead of Page
		String catalogid = inReq.findValue("catalogid");
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");
		String sourcePath = inReq.getRequestParameter("sourcepath");

		if (sourcePath == null)
		{
			sourcePath = archive.getSourcePathForPage(inReq);
		}
		
//		outputype = outputype.toLowerCase();
//		if(outputype.contains("?")){
//			outputype = outputype.substring(0, outputype.indexOf("?"));
//		}
		
		//TODO: Use hard coded path lookups for these based on media type?
		
		//We use the output extension so that we don't have look up the original input file to find the actual type
		
//		MediaConverter creator = archive.getCreatorManager().getMediaCreatorByOutputFormat(outputype);
//		if( creator == null )
//		{
//			return;
//		}
		TranscodeTools transcodetools = archive.getTranscodeTools();
		Map all = new HashMap(); //TODO: Get parent ones as well
		for (Iterator iterator = inReq.getContentPage().getPageSettings().getAllProperties().iterator(); iterator.hasNext();)
		{
			PageProperty type = (PageProperty) iterator.next();
			all.put(type.getName(), type.getValue());
		}
		all.putAll( inReq.getPageMap()); //these could be objects, needed?
		Map args = inReq.getParameterMap();
		
		
		//return calculateInstructions(all, inArchive, inOutputType, inSourcePath);
		//convert is not null because this generator would not be called with invalid path .jpg or .mp3 only
		String name = inPage.get("exportname");
		if( name == null)
		{
			//throw new OpenEditException("exportname is not set on " + inPage.getPath() );
			name = inPage.getName();
		}
		String themeprefix = inReq.findValue("themeprefix");
		all.put("themeprefix", themeprefix);
		ConvertResult result = transcodetools.createOutputIfNeeded(all,args,sourcePath, name); //String inSourcePath, Data inPreset, String inOutputType);
		
		if( result.isComplete() )
		{
			Page output = new Page() //SPEED UP
					{
						public boolean isHtml() { return false;}
					};
			output.setPageSettings(inPage.getPageSettings());
			output.setContentItem(result.getOutput());
			WebPageRequest copy = inReq.copy(output);
			copy.putProtectedPageValue("content",output);
			super.generate(copy, output, inOut);
			ConvertInstructions instructions = result.getInstructions();

			//TODO: Find a better way to do this
			if (instructions != null && instructions.getMaxScaledSize() == null && !instructions.isWatermark() && instructions.getOutputExtension() == null)
			{
				archive.logDownload(sourcePath, "success", inReq.getUser()); //does this work?
			}
		}
		
		
		else 
		{
			String missingImage = inReq.getContentProperty("missingimagepath");
			if(missingImage == null)
			{
				
				missingImage =  "/mediadb/views/images/missing150.jpg"; //would a 404 be better?
			}
			Page missing = archive.getPageManager().getPage(missingImage);			//File temp = new File(missing.getContentItem().getAbsolutePath());
			super.generate(inReq, missing, inOut);
		}
	}

}
