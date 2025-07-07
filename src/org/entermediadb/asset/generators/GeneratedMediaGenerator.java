package org.entermediadb.asset.generators;

import org.apache.commons.collections.map.HashedMap;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.TranscodeTools;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.error.ContentNotAvailableException;
import org.openedit.generators.FileGenerator;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;

/**
 * This generator generates original asset documents from an MediaArchive based
 * on paths of the form <tt>.../<var>assetid</var>/<var>filename.ext</var></tt>.
 */
public class GeneratedMediaGenerator extends FileGenerator
{
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
		String catalogid = inReq.findPathValue("catalogid");

		MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");
		
		
		// make sure your path tacks a filename on the end.
		String path = inPage.getPath();
		
		String assetrootfolder = inPage.get("assetrootfolder");
		String endingpath = inPage.getPath().substring(assetrootfolder.length());
	
		
		String hasexportname = inPage.get("exportnameinpath");
		if( Boolean.parseBoolean( hasexportname) )
		{
			String download = inReq.findValue("download");
			if( Boolean.parseBoolean(download))
			{
				String filename = PathUtilities.extractFileName(endingpath);
				forceDownload(inReq,filename);
			}
			endingpath = PathUtilities.extractDirectoryPath(endingpath);
		}
		
		String ondemand = inReq.findValue("ondemand");
		
		Boolean forcewatermark = inReq.hasPermission("forcewatermarkasset");
		if (forcewatermark && endingpath.contains("image3000x3000.")) 
		{
			String filename = PathUtilities.extractPageName(endingpath);
			String fileext = PathUtilities.extractPageType(endingpath);
			String basepath = PathUtilities.extractDirectoryPath(endingpath);
			
			endingpath = basepath + "/" + filename + "wm." + fileext;
			
			ondemand = "true";
			
		}
		
		// Try the contentitem first. If misssing try a fake page

		//String sourcepath = archive.getSourcePathForPage(inPage);
		ContentItem item = getPageManager().getRepository().getStub("/WEB-INF/data/" + catalogid + "/generated" + endingpath);
		//ContentItem item = getPageManager().getRepository().getStub("/WEB-INF/data/" + catalogid + "/generated/" + sourcepath +"/" + outputfile);
		Page output = null; 
		boolean existed = item.exists();
		
		if( !existed )
		{
			
			if( Boolean.parseBoolean(ondemand))
			{
				TranscodeTools transcodetools = archive.getTranscodeTools();
				String sourcepath = archive.getSourcePathForPage(inPage);
				String outputfile = PathUtilities.extractFileName(endingpath); //image1200x1200.jpg
				
				ConvertResult result = transcodetools.createOutputIfNeeded(null,null,sourcepath, outputfile ); //String inSourcePath, Data inPreset, String inOutputType);
			}
		}
		/*
		boolean addmetadata = Boolean.parseBoolean(inReq.findValue("includemetadata"));
		if (existed && addmetadata)
		{
			XmpWriter writer = (XmpWriter) getModuleManager().getBean("xmpWriter");
			Asset asset = archive.getAssetBySourcePath(sourcepath);
			if (asset != null)
			{
				try
				{
					writer.saveMetadata(archive, item, asset, new HashMap());  //This is slow and dumb. 
				}
				catch (Exception e)
				{
					throw new OpenEditException(e);
				}
			}
		}
		 */
		if (existed)
		{

			output = new Page()
			{
				public boolean isHtml()
				{
					return false;
				}
			};
			output.setPageSettings(inPage.getPageSettings());
			output.setContentItem(item);
		}
		else
		{
			output = getPageManager().getPage("/WEB-INF/data/" + catalogid + "/generated" + endingpath);
		}

		if (!existed && !output.exists())
		{
			throw new ContentNotAvailableException("Missing: " + output.getPath(), output.getPath());
		}
		else
		{
			WebPageRequest copy = inReq.copy(output);
			copy.putProtectedPageValue("content", output);
			
			/*
			if(existed && inReq.getResponse() != null )
			{
				inReq.getResponse().setHeader("ETag", String.valueOf( output.lastModified() ));
			}
			*/
			
			super.generate(copy, output, inOut);
			// archive.logDownload(sourcePath, "success", inReq.getUser());
		}
	}

}
