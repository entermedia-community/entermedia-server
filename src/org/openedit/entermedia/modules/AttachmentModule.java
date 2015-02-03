package org.openedit.entermedia.modules;

import java.util.Iterator;

import org.entermedia.attachments.AttachmentManager;
import org.entermedia.upload.FileUpload;
import org.entermedia.upload.FileUploadItem;
import org.entermedia.upload.UploadRequest;
import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.edit.AssetEditor;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;

public class AttachmentModule extends BaseMediaModule
{

	protected AttachmentManager fieldAttachmentManager;

	public AttachmentManager getAttachmentManager()
	{
		return fieldAttachmentManager;
	}

	public void setAttachmentManager(AttachmentManager inAttachmentManager)
	{
		fieldAttachmentManager = inAttachmentManager;
	}

	public void syncAttachments(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		if (asset != null && asset.isFolder() )
		{
			getAttachmentManager().syncAttachments(inReq, archive, asset, false);
		}
	}
	public void listChildren(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String parentsourcepath = inReq.getRequestParameter("parentsourcepath");
		if( parentsourcepath == null ) 
		{
			parentsourcepath = inReq.getRequestParameter("sourcepath");
		}
		HitTracker hits = getAttachmentManager().listChildren(inReq, archive, parentsourcepath);
		inReq.putPageValue("attachments",hits);
	}

	public void reSyncAttachments(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		if (asset != null)
		{
			getAttachmentManager().clearAttachmentData(inReq, archive, asset, true); //this is bad since we will lose state
			getAttachmentManager().syncAttachments(inReq, archive, asset, true);
		}
	}

	public void uploadAttachments(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);

		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inReq);
		Asset asset = getAsset(inReq);
		if (properties == null)
		{
			return;
		}
		String firstfile = null;

		for (Iterator iterator = properties.getUploadItems().iterator(); iterator.hasNext();)
		{
			if (!asset.isFolder())
			{
				AssetEditor editor = (AssetEditor) getModuleManager().getBean("assetEditor");
				editor.setMediaArchive(archive);
				editor.makeFolderAsset(asset, inReq.getUser());
			}
			String folder = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + asset.getSourcePath();
			if (!folder.endsWith("/"))
			{
				folder = folder + "/";
			}
			FileUploadItem item = (FileUploadItem) iterator.next();
			String name = item.getName();

			if (firstfile == null)
			{
				firstfile = name;
				String fieldname = item.getFieldName();
				//remove file. 
				fieldname = fieldname.replace("file.", "");
				inReq.setRequestParameter(fieldname + ".value", firstfile);

			}
			String finalpath = folder + name;
			properties.saveFileAs(item, finalpath, inReq.getUser());
		}		
		getAttachmentManager().processAttachments(archive, asset, true);

		//inReq.putPageValue("newattachments", newattachments);
		inReq.putPageValue("first", firstfile);
		// inIn.delete();

	}

	/**
	 * Creates sub folders in attachment area
	 * @param inReq
	 * @throws Exception
	 */
	public void createFolder(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		if (asset != null)
		{
			String parentid = inReq.getRequestParameter("fileid");
			String foldername = inReq.getRequestParameter("foldername");
			getAttachmentManager().createFolder(inReq, archive, asset, parentid, foldername);
		}
		//reSyncAttachments(inReq);
	}

	public void delete(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		if (asset != null)
		{
			String fileid = inReq.getRequestParameter("fileid");
			getAttachmentManager().delete(inReq, archive, asset, fileid);
		}
		reSyncAttachments(inReq);
	}

	public void renameFolder(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		if (asset != null)
		{
			String parentid = inReq.getRequestParameter("fileid");
			String foldername = inReq.getRequestParameter("foldername");
			getAttachmentManager().renameFilder(inReq, archive, asset, parentid, foldername);
		}
		reSyncAttachments(inReq);
	}
	public void loadFile(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		Asset asset = getAsset(inReq);
		String parentid = inReq.getRequestParameter("fileid");
		Data file = (Data)getAttachmentManager().getAttachmentSearcher(archive.getCatalogId()).searchById(parentid);
		inReq.putPageValue("file", file);
	}

}
