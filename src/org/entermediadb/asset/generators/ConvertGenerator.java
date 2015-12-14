package org.entermediadb.asset.generators;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.creator.ConvertInstructions;
import org.entermediadb.asset.creator.ConvertResult;
import org.entermediadb.asset.creator.MediaCreator;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.FileGenerator;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.repository.ContentItem;
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

		
		String outputype = PathUtilities.extractPageType(inPage.getPath());
		if( outputype == null )
		{
			return;
		}
		outputype = outputype.toLowerCase();
		
		String name = inPage.getName();
		
		if( name.startsWith("image") && name.length() > 10 && name.contains("x"))
		{
			//see if there is a with and height?
			String size = name.substring(5,name.length() - outputype.length() - 1 );
			int cutoff= size.indexOf("x");
			String width = size.substring(0,cutoff);
			String height = size.substring(cutoff + 1,size.length());	
			inReq.setRequestParameter("prefwidth", width);
			inReq.setRequestParameter("prefheight", height);
		}
		
		//TODO: Use hard coded path lookups for these based on media type?
		
		//We use the output extension so that we don't have look up the original input file to find the actual type
		MediaCreator creator = archive.getCreatorManager().getMediaCreatorByOutputFormat(outputype);
		if( creator == null )
		{
			return;
		}
		Map all = new HashMap(); //TODO: Get parent ones as well
		for (Iterator iterator = inReq.getContentPage().getPageSettings().getAllProperties().iterator(); iterator.hasNext();)
		{
			PageProperty type = (PageProperty) iterator.next();
			all.put(type.getName(), type.getValue());
		}
		all.putAll( inReq.getPageMap()); //these could be objects, needed?
		all.putAll( inReq.getParameterMap() );
			
		//return calculateInstructions(all, inArchive, inOutputType, inSourcePath);
		//convert is not null because this generator would not be called with invalid path .jpg or .mp3 only
		ConvertInstructions inStructions = creator.createInstructions(archive,all,null, sourcePath, null, outputype); //String inSourcePath, Data inPreset, String inOutputType);

		ConvertResult result = creator.createOutputIfNeeded(inStructions);
		if( result.isComplete() )
		{
			Page output = new Page() //SPEED UP
					{
						public boolean isHtml() { return false;}
					};
			output.setPageSettings(inPage.getPageSettings());
			output.setContentItem(result.getOutput());
			WebPageRequest copy = inReq.copy(output);
			copy.putProtectedPageValue("content", result.getOutput());
			super.generate(copy, output, inOut);
			if (inStructions.getMaxScaledSize() == null && !inStructions.isWatermark() && inStructions.getOutputExtension() == null)
			{
				archive.logDownload(sourcePath, "success", inReq.getUser()); //does this work?
			}
		}
		else 
		{
			String missingImage = inReq.getContentProperty("missingimagepath");
			if(missingImage == null)
			{
				String themeprefix = (String) inReq.findValue("themeprefix");
				missingImage = themeprefix + "/images/missing150.jpg"; //would a 404 be better?
			}
			Page missing = archive.getPageManager().getPage(missingImage);			//File temp = new File(missing.getContentItem().getAbsolutePath());
			super.generate(inReq, missing, inOut);
		}
	}

}
