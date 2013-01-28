package org.openedit.entermedia.generators;

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
		
		Page output = getPageManager().getPage("/WEB-INF/data/" + catalogid + "/generated" + path);
		if( !output.exists() )
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
