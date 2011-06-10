package org.openedit.entermedia;

import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.repository.ContentItem;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;

public class OriginalFileManager
{
	private static final Log log = LogFactory.getLog(OriginalFileManager.class);
	protected PageManager fieldPageManager;
	protected MediaArchive fieldMediaArchive;

	public InputStream getOriginalDocumentStream(Asset inAsset) throws OpenEditException
	{
		InputStream in = null;

		Page fullpath = getOriginalDocument(inAsset);
		if (fullpath == null)
		{
			return null;
		}
		return fullpath.getInputStream();
	}
	public Page getOriginalDocument(String inSourcePath)
	{
		Asset asset = getMediaArchive().getAssetBySourcePath(inSourcePath);
		return getOriginalDocument(asset);
	}
	public Page getOriginalDocument(Asset inAsset)
	{
		String originalpath = "/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/originals";
		//String fullpath = inAsset.get("originalpath");//absolute path might be a http server
		originalpath = originalpath + "/" + inAsset.getSourcePath();
		String primaryname = inAsset.getPrimaryFile();
		if( primaryname != null)
		{
			originalpath = originalpath + "/" + primaryname;
		}
		Page page = getPageManager().getPage(originalpath);
		//ContentItem content = page.getContentItem();
		return page;

		/*
		String fullpath = inAsset.get("originalpath");//absolute path might be a http server

		if (fullpath != null && fullpath.indexOf("://") != -1)
		{
			originalpath = originalpath + "/http/" + inAsset.getSourcePath();
			try
			{
				fullpath = fullpath.replace(" ", "%20");
				GetMethod postMethod = new GetMethod(fullpath);

				// postMethod.addParameter("accountname", getUsername());
				// postMethod.addParameter("password", getPassword());

				// client.getHttpConnectionManager().getParams().
				// setConnectionTimeout(0);
				int statusCode1 = getHttpClient().executeMethod(postMethod);

				// postMethod.releaseConnection(); //Is this needed?
				if (statusCode1 == 200)
				{
					in = postMethod.getResponseBodyAsStream();
					// Reader reader = new InputStreamReader(body,"UTF-8");
				}
		}
*/				

	}
	
	/**
	 * This is where attachments might be found
	 * @param inSourceImage
	 * @param inAsset
	 * @return
	 
	public String getFilePath(String inSourceImage, Asset inAsset)
	{
		if (inAsset == null)
		{
			log.info("No such asset");
			return null;
		}
		
		String path = getMediaArchive().getCatalogHome() + "/data/originals/";
		
		//Looks for attachments near the original path
		path = path + inAsset.getSourcePathToFile(inSourceImage);
		
		Page page = getPageManager().getPage(path);
		ContentItem content = page.getContentItem();
		return content.getAbsolutePath();
	}
	*/
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
		
	/**
	 * This only works if inSourcePath has an extension,
	 * i.e. newassets/admin/118/picture.jpg
	 * @param inSourcePath
	 * @return
	 */
	public String getDataAssetsPath(String inSourcePath)
	{
		String prefix = "/WEB-INF/data" + getMediaArchive().getCatalogHome() + "/originals/";
		String path = prefix + inSourcePath;
		Page page = getPageManager().getPage(path);
		ContentItem content = page.getContentItem();
		return content.getAbsolutePath();
	}

	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}


}