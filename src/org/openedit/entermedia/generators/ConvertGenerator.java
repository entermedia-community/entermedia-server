package org.openedit.entermedia.generators;

import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.creator.ConvertInstructions;
import org.openedit.entermedia.creator.MediaCreator;
import org.openedit.repository.ContentItem;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.generators.FileGenerator;
import com.openedit.generators.Output;
import com.openedit.page.Page;
import com.openedit.util.PathUtilities;

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
		//convert is not null because this generator would not be called with invalid path .jpg or .mp3 only
		ConvertInstructions inStructions = creator.createInstructions(inReq, archive, outputype, sourcePath);
		if( inStructions == null )
		{
			return;
		}
		creator.populateOutputPath(archive, inStructions);
		
		
		ContentItem stub = getPageManager().getRepository().getStub(inStructions.getOutputPath());
		Page output = null;
		boolean existed = stub.exists();
		if( existed )
		{
			output = new Page() //SPEED UP
			{
				public boolean isHtml() { return false;}
			};
			output.setPageSettings(inPage.getPageSettings());
			output.setContentItem(stub);
		}
		else
		{
			 output = getPageManager().getPage(inStructions.getOutputPath());  //mp3 and other types
		}
		if( !existed || output.getContentItem().getLength() == 0 )
		{
			//TODO: Return the quick embeded jpg thumbnails then queue up the larger thumbs for later
			//synchronized (this)
			//{
				output = creator.createOutput(archive,inStructions);//archive.getCreatorManager().createOutput( inStructions);				
			//}
			//make sure we hide thumbs that are not ready
		}
		// getDefaultImage(inType, archive.getCatalogId());
		if (output == null)
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
		else 
		{
			WebPageRequest copy = inReq.copy(output);
			copy.putProtectedPageValue("content", output);
			super.generate(copy, output, inOut);
			if (inStructions.getMaxScaledSize() == null && !inStructions.isWatermark() && inStructions.getOutputExtension() == null)
			{
				archive.logDownload(sourcePath, "success", inReq.getUser());
			}

		}
	}

}
