/*
 * Created on May 2, 2006
 */
package org.openedit.entermedia.generators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.generators.BaseGenerator;
import com.openedit.generators.Output;
import com.openedit.page.Page;

public class ZipAlbumGenerator extends BaseGenerator
{

	private static final Log log = LogFactory.getLog(ZipAlbumGenerator.class);
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
//		AlbumModule module = (AlbumModule) getModuleManager().getModule("AlbumModule");
//		EnterMedia matt = module.getMatt(inReq);
//
//		Album col = null;
//		try
//		{
//			col = module.loadAlbum(inReq);
//
//			for (Object object : col)
//			{
//				AlbumItem item  = (AlbumItem)object;
//				String assetpath = "/" + item.getAsset().getCatalogId() + "/assets/" + item.getAsset().getSourcePath();
//				if (inReq.getPageStreamer().canDo("forcewatermark", assetpath))
//				{
//					item.setWatermark(true);
//				}
//			}
//
//			// Save UsageHistory
//			ZipAlbum zip = new ZipAlbum();
//			zip.setMatt(matt);
//			zip.setUser(inReq.getUser());
//			zip.zipAlbumItems(col, inOut.getStream());
//
//		}
//		catch (Exception e)
//		{
//			log.error(e);
//			inReq.redirect("/" + matt.getApplicationId() + "/users/" + col.getUserName() + "/albums/" + col.getId() + "/download/imagemissing.html?error=" + e.getMessage());
//		}
//
	}

	public boolean canGenerate(WebPageRequest inReq)
	{
		return inReq.getPage().getMimeType().equals("application/zip");
	}

}
