package org.openedit.entermedia.generators;

import org.openedit.repository.ContentItem;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.error.ContentNotAvailableException;
import com.openedit.generators.FileGenerator;
import com.openedit.generators.Output;
import com.openedit.page.Page;
import com.openedit.util.PathUtilities;

/**
 * This generator generates original asset documents from an MediaArchive
 * based on paths of the form
 * <tt>.../<var>assetid</var>/<var>filename.ext</var></tt>.
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
		//make sure your path tacks a filename on the end.
		
		//Try the contentitem first. If misssing try a fake page
		ContentItem item = getPageManager().getRepository().getStub("/WEB-INF/data/" + catalogid + "/generated" + path);
		Page output = null;
		boolean existed = item.exists();
		if( existed )
		{
			
			output = new Page();
			output.setContentItem(item);
		}
		else
		{
			output = getPageManager().getPage("/WEB-INF/data/" + catalogid + "/generated" + path);
		}
		
		if( !existed && !output.exists() )
		{
			throw new ContentNotAvailableException("Missing: " +output.getPath(),output.getPath());
		}
		else 
		{
			WebPageRequest copy = inReq.copy(output);
			copy.putProtectedPageValue("content", output);
			super.generate(copy, output, inOut);
			//	archive.logDownload(sourcePath, "success", inReq.getUser());
		}
	}

}
