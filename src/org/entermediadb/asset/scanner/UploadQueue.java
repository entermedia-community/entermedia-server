package org.entermediadb.asset.scanner;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.EnterMedia;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.UploadRequest;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;

public class UploadQueue
{
	protected static final Log log = LogFactory.getLog(UploadQueue.class);

	protected Queue<Upload> fieldQueue;
	protected PageManager fieldPageManager;
	protected AssetImporter fieldAssetImporter;

	protected AssetImporter getAssetImporter()
	{
		return fieldAssetImporter;
	}

	public void setAssetImporter(AssetImporter inAssetImporter)
	{
		fieldAssetImporter = inAssetImporter;
	}

	protected PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public Queue<Upload> getQueue()
	{
		if (fieldQueue == null)
		{
			fieldQueue = new LinkedList<Upload>();
		}
		return fieldQueue;
	}

	public void add(String inApplet, Page inDestPath)
	{
		Upload up = new Upload();
		up.fieldAppletName = inApplet;
		up.fieldDestPath = inDestPath;
		up.fieldSize = inDestPath.getContentItem().getLength();
		up.fieldAddedTime = System.currentTimeMillis();
		getQueue().add(up);
	}

	public boolean isLocked(String inApplet, String inSourcePath)
	{
		expire();
		for (Upload up: getQueue())
		{
			if (up.fieldDestPath.equals(inSourcePath))
			{
				if (!up.fieldAppletName.equals(inApplet))
				{
					return true;
				}
			}
		}
		return false;
	}

	public void remove(String inSourcePath)
	{
		for (Upload up: getQueue())
		{
			if (up.fieldDestPath != null && up.fieldDestPath.equals(inSourcePath))
			{
				getQueue().remove(up);
				return;
			}
		}
	}

	// called before each
	public void expire()
	{
		// 1. Upload starts 0:00 Applet is reloaded in browser. No error is
		// throw on server it just hangs
		// 2. Browse asked for list of uploads. Starts upload again with same
		// hung file
		// 3. Server sees that applet name matches and continues else
		// 4. Applet got a new name. Dont let it upload this file for 5 min from
		// time of last non-error upload. Grabs new lock
		long limit = System.currentTimeMillis();
		limit = limit - (1000 * 60 * 2);
		// maybe expire after 5 min

		// If they hit refresh they can check the applet name and pass that back
		// in
		List<Upload> copy = new ArrayList<Upload>(getQueue());
		for (Upload up: copy)
		{
			if (up.fieldAddedTime < limit)
			{
				//check the size change
				if( up.fieldSize  == up.fieldDestPath.getContentItem().getLength())
				{
					getQueue().remove(up);
				}
			}
		}

	}

	class Upload
	{
		String fieldAppletName;
		long fieldAddedTime;
		Page fieldDestPath;
		long fieldSize;
	}

	public void processUpload(WebPageRequest inReq, FileUpload fileUpload, String inAppletname, EnterMedia inEntermedia)
	{
		UploadDiskFileItemFactory uploadfilefactory = new UploadDiskFileItemFactory();
		String seek = inReq.getRequest().getHeader("x-seekrange");
		long  seekval = 0;
		if ( seek != null)
		{
			seekval = Long.parseLong(seek);
			uploadfilefactory.setSeek(seekval);
		}
		String destinationpath = inReq.getRequest().getHeader("x-destinationpath");
		Page absuploaded = getPageManager().getPage(destinationpath);
		add(inAppletname, absuploaded);
		uploadfilefactory.setDestinationPath(absuploaded.getContentItem().getAbsolutePath());
		inReq.putPageValue("uploadfilefactory", uploadfilefactory);
				
		UploadRequest map = null;
		String catid = null;
		try
		{
			map = fileUpload.parseArguments(inReq);
			catid = inReq.getRequestParameter("catalogid");
			if( destinationpath == null )
			{
				log.error("No destination path for upload");
				return;
			}
			if (isLocked(inAppletname, destinationpath))
			{
				// dont let them upload
				log.info("Tried to upload to locked sourcepath. Retry in 5 min");
			}

			log.info("saving to: " + destinationpath);
			if (map == null || map.getUploadItems().size() == 0)
			{
				throw new OpenEditException("No upload included");
			}
		}
		catch (Exception e)
		{
			log.error("Upload Interrupted", e);
			return;
		}
		finally
		{
			if(map == null)
			{
				return;
			}
			String sourcepath = inReq.getRequestParameter("sourcepath");
			Asset asset = inEntermedia.getAssetBySourcePath(catid, sourcepath);
			
			// Now check if we got the end of the expected file size. If
			// we did move the file over
			String total = inReq.getRequestParameter("totalsize");
			MediaArchive archive = inEntermedia.getMediaArchive(catid);
			if (absuploaded.length() == Long.parseLong(total))
			{
				asset.setProperty("importstatus", "uploaded");
				String dest = "/WEB-INF/data/" + catid + "/originals/" + sourcepath;
				Page destination = getPageManager().getPage(dest);
				log.info("Saved Asset to Originals " + dest);
				getPageManager().movePage(absuploaded, destination);
				archive.saveAsset(asset, inReq.getUser());
				asset.setProperty("uploadprogress", String.valueOf(destination.length())); 
				
				asset = getAssetImporter().getAssetUtilities().populateAsset(asset, destination.getContentItem(), archive, sourcepath, inReq.getUser());
				asset.setProperty("importstatus", "imported");
				archive.saveAsset(asset, inReq.getUser());
				archive.fireMediaEvent("importing","assetsuploaded", inReq.getUser(), asset);
			}
			else
			{
				log.error("Final size did not match asset");
			}
		}
	}
}
