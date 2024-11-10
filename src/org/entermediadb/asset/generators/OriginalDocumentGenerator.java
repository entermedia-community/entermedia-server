package org.entermediadb.asset.generators;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.edit.Version;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.error.ContentNotAvailableException;
import org.openedit.generators.FileGenerator;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.PageRequestKeys;
import org.openedit.util.OutputFiller;
import org.openedit.util.PathUtilities;

/**
 * This generator generates original asset documents from an MediaArchive
 * based on paths of the form
 * <tt>.../<var>assetid</var>/<var>filename.ext</var></tt>.
 * 
 * @author Eric Galluzzo
 */
public class OriginalDocumentGenerator extends FileGenerator
{
	private static final Log log = LogFactory.getLog(OriginalDocumentGenerator.class);
	protected ModuleManager moduleManager;

	public ModuleManager getModuleManager()
	{
		return moduleManager;
	}

	public void setModuleManager(ModuleManager moduleManager)
	{
		this.moduleManager = moduleManager;
	}

	public void generate(WebPageRequest inReq, Page inPage, Output inOut) throws OpenEditException
	{
		
		// log.info("Downloading " + inPage.getPath());
		// this depends on the URL path to be /stuff/1/2/3/abc.eps/abs.eps This
		// way we support weird source paths

		String catalogid = inReq.findPathValue("catalogid");
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");

		String assetrootfolder = inPage.get("assetrootfolder");
		Boolean watermark = (Boolean) inReq.getPageValue("canforcewatermark");
		String sourcePath = null;
		String collectionidinpath = inPage.get("collectionidinpath");
		if( Boolean.parseBoolean(collectionidinpath))
		{
			sourcePath = inPage.getPath().substring(assetrootfolder.length() + 1);
			int endslash = sourcePath.indexOf("/");
			//String collectionid = sourcePath.substring(0, endslash); ignore it
			sourcePath = sourcePath.substring(endslash + 1);
		}
		else
		{
			sourcePath = inPage.getPath().substring(assetrootfolder.length() + 1);
		}
		if (watermark != null && watermark.booleanValue())
		{
//			ConvertGenerator generator = (ConvertGenerator) getModuleManager().getBean("convertArchiveDocument");
//			generator.generate(inReq, inPage, inOut);
			//send them to the jpg version
			String name = PathUtilities.extractFileName(sourcePath);
			inReq.redirect("/" + catalogid + "/downloads/converted/cache/" +sourcePath + "/" + name + "wm.jpg");
			return;
		}
		// source path cut off the beginning
		// source path cut off the parent folder name. Put a / back in there?
		String exactsourcepath  = inPage.get("exactsourcepath");
		if(!Boolean.parseBoolean(exactsourcepath) )
		{
			sourcePath = PathUtilities.extractDirectoryPath(sourcePath);
		}
		Asset asset = archive.getAssetBySourcePath(sourcePath);

		if (asset == null  )
		{
			if(Boolean.parseBoolean(exactsourcepath))
			{
				//use regular downloading?
				Page realpage = archive.getPageManager().getPage("/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + sourcePath);
				super.generate(inReq, realpage, inOut);
				return;
			}
			asset = archive.createAsset("tmp",sourcePath);
		}
	
		//	String fileName = URLEncoder.encode(asset.getName(), "UTF-8");
		String	 fileName = asset.getName();

		String filename = asset.getSourcePath();
		if (asset.isFolder() && asset.getPrimaryFile() != null)
		{
			filename = filename + "/" + asset.getPrimaryFile();
		}
		
		Page content = archive.getOriginalDocument(asset);
		
		String version = inReq.getRequestParameter("version");
		if (version != null) {
			Version revision = archive.getAssetEditor().getVersion(content.getPath(), version);
			content = archive.getPageManager().getPage(revision.getBackUpPath());
		}
		
		if( content.exists() )
		{
			//its a regular file
			boolean skipheader = Boolean.parseBoolean(inReq.findValue("skipheader"));
		    if(inReq.getResponse() != null && !skipheader )
			{
		    	inReq.getResponse().setHeader("Content-Type", "application/octet-stream; charset=utf-8");
				inReq.getResponse().setHeader("Content-disposition", "attachment; filename=\""+ fileName +"\"");  //This seems to work on firefox
		    	//inReq.getResponse().setHeader("Content-disposition", "attachment; filename*=utf-8''\""+ fileName +"\"");
				
				String md5 = asset.get("md5hex");
				if( md5 != null)
				{
			    	inReq.getResponse().setHeader("ETag", md5);					
				}
			}
			WebPageRequest req = inReq.copy(content);
			req.putProtectedPageValue(PageRequestKeys.CONTENT, content);
			super.generate(req, content, inOut);
			archive.logDownload(filename, "success", inReq.getUser());

		}
		else
		{
			stream(inReq, archive, inOut, asset, filename);
		}
	}

	private void stream(WebPageRequest inReq, MediaArchive archive, Output inOut, Asset asset, String filename)
	{
		InputStream in = null;
//		try
//		{
			in = archive.getOriginalDocumentStream(asset);
			if (in == null)
			{
				
				throw new ContentNotAvailableException("Could not find original document path " , asset.getSourcePath() );
			}
//		}
//		catch (Exception e)
//		{
//			archive.logDownload(filename, "missing", inReq.getUser());
//			if( inReq.getRequest() != null)
//			{
//				inReq.getResponse().setStatus(404);
//			}
//			
//			log.error("Error for " + asset.getName() + ": " + e);
//			String applicationid = inReq.findValue("applicationid");
//			if(applicationid != null){
//				inReq.redirect("/" + applicationid + "/missingfile.html");
//
//			}
//			inReq.redirect(archive.getCatalogHome() + "/missingfile.html");
//			return;
//		}
			boolean skipheader = Boolean.parseBoolean(inReq.findValue("skipheader"));
		    if(inReq.getResponse() != null && !skipheader )
			{
				inReq.getResponse().setHeader("Content-disposition", "attachment; filename=\""+ filename +"\"");  //This seems to work on firefox
			}

			
		try
		{
			// FileInputStream fis = new FileInputStream( originalDocumentFile
			// );
			try
			{
				OutputFiller filler = new OutputFiller();
				//filler.setBufferSize(40000);
				filler.fill(in, inOut.getStream());
			}
			finally
			{
				in.close();
				inOut.getStream().close();
				log.info("Document sent");
				archive.logDownload(filename, "success", inReq.getUser());
			}
		}
		catch (IOException ioe)
		{
			archive.logDownload(filename, "error", inReq.getUser());
			throw new OpenEditException(ioe);
		}
	}

	public boolean canGenerate(WebPageRequest inReq)
	{
		return true;
	}

}
