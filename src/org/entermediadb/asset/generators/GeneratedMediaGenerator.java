package org.entermediadb.asset.generators;

import java.util.HashMap;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.xmp.XmpWriter;
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
		String catalogid = inReq.findValue("catalogid");

		String path = null;
		String assetrootfolder = inPage.get("assetrootfolder");

		path = inPage.getPath().substring(assetrootfolder.length());
		path = PathUtilities.extractDirectoryPath(path);
		// make sure your path tacks a filename on the end.

		// Try the contentitem first. If misssing try a fake page
		ContentItem item = getPageManager().getRepository().getStub("/WEB-INF/data/" + catalogid + "/generated" + path);
		Page output = null;
		boolean existed = item.exists();
		boolean addmetadata = Boolean.parseBoolean(inReq.findValue("includemetadata"));
		if (existed && addmetadata)
		{
			MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");
			String sourcePath = inPage.getPath().substring(assetrootfolder.length() + 1);
			sourcePath = sourcePath.substring(0, sourcePath.lastIndexOf("/"));
			sourcePath = sourcePath.substring(0, sourcePath.lastIndexOf("/"));

			XmpWriter writer = (XmpWriter) getModuleManager().getBean("xmpWriter");
			Asset asset = archive.getAssetBySourcePath(sourcePath);
			if (asset != null)
			{
				try
				{
					writer.saveMetadata(archive, item, asset, new HashMap());
				}
				catch (Exception e)
				{
					throw new OpenEditException(e);
				}
			}
		}

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
			output = getPageManager().getPage("/WEB-INF/data/" + catalogid + "/generated" + path);
		}

		if (!existed && !output.exists())
		{
			throw new ContentNotAvailableException("Missing: " + output.getPath(), output.getPath());
		}
		else
		{
			WebPageRequest copy = inReq.copy(output);
			copy.putProtectedPageValue("content", output);
			super.generate(copy, output, inOut);
			// archive.logDownload(sourcePath, "success", inReq.getUser());
		}
	}

}
