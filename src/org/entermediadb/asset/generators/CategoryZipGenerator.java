/*
 * Created on May 2, 2006
 */
package org.entermediadb.asset.generators;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.MediaArchiveModule;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.BaseGenerator;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.FileUtils;
import org.openedit.util.OutputFiller;

public class CategoryZipGenerator extends BaseGenerator
{
	OutputFiller filler = new OutputFiller();
	private static final Log log = LogFactory.getLog(CategoryZipGenerator.class);
	protected ModuleManager fieldModuleManager;

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager moduleManager)
	{
		fieldModuleManager = moduleManager;
	}

	public void generate(WebPageRequest inReq, Page inPage, Output inOut) throws OpenEditException
	{
		MediaArchiveModule archiveModule = (MediaArchiveModule) getModuleManager().getBean("MediaArchiveModule");
		MediaArchive archive = archiveModule.getMediaArchive(inReq);
		String catid = inPage.getDirectoryName();
		Category fromcategory = archive.getCategory(catid);
		
		zipAssets(inReq, archive,fromcategory , inOut);
		
	}

	protected void zipAssets(WebPageRequest inReq, MediaArchive archive, Category inRootCat, Output inOut)
	{
		ZipOutputStream zos = null;
		try
		{
			zos = new ZipOutputStream(inOut.getStream());
			zos.setLevel(1); // for speed since these are jpegs
				
			addFolder(inReq,archive,inRootCat,"", zos);
		
		}
		finally
		{
			try
			{
				FileUtils.safeClose(zos); // This will fail if there was any

			}
			catch (Exception ex2)
			{
				// nothing
			}
		}
		
	}

	protected void addFolder(WebPageRequest inReq,MediaArchive archive, Category inCat,String folderprefix, ZipOutputStream zos)
	{
		Collection files = archive.query("asset").exact("category-exact",inCat).search(inReq);
		
		String thispath = folderprefix + inCat.getName() + "/";
		for (Iterator iterator = files.iterator(); iterator.hasNext();)
		{
			Data asset = (Data) iterator.next();
			
			String filename = thispath + asset.getName();
			ZipEntry entry = new ZipEntry(filename);
			ContentItem inFile = archive.getOriginalContent(asset);
			entry.setSize(inFile.getLength());
			entry.setTime(inFile.getLastModified());
			try
			{
				InputStream fis = inFile.getInputStream();
				zos.putNextEntry(entry);
				try
				{
					filler.fill(fis, zos);
				}
				finally
				{
					fis.close();
				}
				zos.closeEntry();
			}
			catch (Exception ex)
			{
				log.error(ex);
			}
		}
		for (Iterator iterator = inCat.getChildren().iterator(); iterator.hasNext();)
		{
			Category child = (Category) iterator.next();
			addFolder(inReq,archive,child,thispath, zos);
		}
		
	}
	
	public boolean canGenerate(WebPageRequest inReq)
	{
	
		boolean ok =  inReq.getPage().getMimeType().equals("application/x-zip");
		return ok;
	}

}
