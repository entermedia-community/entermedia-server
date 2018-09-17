package org.entermediadb.asset.generators;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
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

		String catalogid = inReq.findValue("catalogid");
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");

		String assetrootfolder = inPage.get("assetrootfolder");
		Boolean watermark = (Boolean) inReq.getPageValue("canforcewatermark");
		String sourcePath = inPage.getPath().substring(assetrootfolder.length() + 1);
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
		sourcePath = PathUtilities.extractDirectoryPath(sourcePath);

		Asset asset = archive.getAssetBySourcePath(sourcePath);

		if (asset == null)
		{
			asset = archive.getAssetBySourcePath(sourcePath);
			if (asset == null)
			{
				asset = archive.createAsset("tmp",sourcePath);
				//throw new OpenEditException("No asset with source path " + sourcePath);
			}
		}

		String filename = asset.getSourcePath();
		if (asset.isFolder() && asset.getPrimaryFile() != null)
		{
			filename = filename + "/" + asset.getPrimaryFile();
		}

		Page content = archive.getOriginalDocument(asset);
		if( content.exists() )
		{
			//its a regular file
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
		try
		{
			in = archive.getOriginalDocumentStream(asset);
			if (in == null)
			{
				throw new OpenEditException("Could not find original document path");
			}
		}
		catch (Exception e)
		{
			archive.logDownload(filename, "missing", inReq.getUser());
			log.error("Error for " + asset.getName() + ": " + e);
			String applicationid = inReq.findValue("applicationid");
			if(applicationid != null){
				inReq.redirect("/" + applicationid + "/missingfile.html");

			}
			inReq.redirect(archive.getCatalogHome() + "/missingfile.html");
			return;
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
