package org.entermedia.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.repository.ContentItem;
import org.openedit.repository.InputStreamItem;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.OutputFiller;
import com.openedit.util.PathUtilities;
import com.openedit.util.ZipUtil;



public class UploadRequest implements ProgressListener
{
	private static final Log log = LogFactory.getLog(UploadRequest.class);
	protected List fieldUploadItems;

	protected PageManager fieldPageManager;
	protected File fieldRoot;
	protected ZipUtil fieldZipUtil;
	protected OutputFiller fieldFiller;
	protected Data fieldUploadQueueData;
	protected Searcher fieldUploadQueueSearcher;
	
	public Searcher getUploadQueueSearcher()
	{
		return fieldUploadQueueSearcher;
	}
	public void setUploadQueueSearcher(Searcher inUploadQueueSearcher)
	{
		fieldUploadQueueSearcher = inUploadQueueSearcher;
	}
	public OutputFiller getFiller()
	{
		if (fieldFiller == null)
		{
			fieldFiller = new OutputFiller();
		}
		return fieldFiller;
	}
	public void setFiller(OutputFiller inFiller)
	{
		fieldFiller = inFiller;
	}
	public void addUploadItem(FileUploadItem inItem)
	{
		getUploadItems().add(inItem);
	}
	public List getUploadItems()
	{
		if (fieldUploadItems == null)
		{
			fieldUploadItems = new ArrayList();
		}
		return fieldUploadItems;
	}
	public void setUploadItems(List inUploadItems)
	{
		fieldUploadItems = inUploadItems;
	}
	
	public String getPathFor(String home, FileUploadItem inItem, WebPageRequest inReq) throws OpenEditException
	{
		String path = inItem.get("path");
		if( path == null)
		{
			path = inReq.getRequestParameter("path");
		}
		
		if (path == null )
		{
			log.error("No path specified in multipart form");
			//throw new OpenEditException("No path passed in with the upload");
			return null;
		}
		if( home != null && home.length() > 0 && path.startsWith(home))
		{
			path = path.substring(home.length());
		}
				
		String finalpath =  null;
		//TODO: Ok so on Windows we get passed in \\ in the file name
		String name = inItem.getName();
		name = name.replace('\\','/');			
		name = PathUtilities.extractFileName(name);
//		name = name.replaceAll(" ","");
		String targetname = inReq.getRequestParameter("targetname");
		if(targetname != null)
		{
			targetname = targetname.replace('\\', '/'); //Not sure I need to do this
			name = targetname;
		}
		
		if( path.endsWith("/"))
		{
			finalpath = path + name;
		}
		else
		{
			Page page = getPageManager().getPage(path);
			if ( page.isFolder() ) //upload to an exising folder
			{
				finalpath = path + "/" + name;
			}
			else
			{
				finalpath = path;
			}
		}
		return finalpath;
	}
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	public void saveFile(FileUploadItem inItem, String inHome, WebPageRequest inReq) throws OpenEditException
	{
		/**
		 * inPath is full filename
		 * @param inProps
		 */
			//From now on we are going to assume people always upload to a directory
			String inPath = getPathFor(inHome, inItem, inReq);
			if( inPath == null)
			{
				return; //already saved?
			}
			if (inPath.indexOf("..") > -1)
			{
				throw new OpenEditException("Illegal path name");
			}
			final String path = PathUtilities.resolveRelativePath( inPath, "/");
			saveFileAs(inItem, path, inReq.getUser());
	}
	public void saveFileAs(FileUploadItem inItem, final String path, User inUser)
			throws OpenEditException
	{
		Page page = getPageManager().getPage( path, true );
		InputStreamItem revision = new InputStreamItem();
//	final Date lastModified = new Date();
		if ( inUser == null)
		{
			throw new IllegalArgumentException("No user logged in");
		}
		revision.setAuthor( inUser.getUserName() );
		revision.setType( ContentItem.TYPE_ADDED );
		revision.setMessage( "Uploaded file");
		revision.setPath(path);
		InputStream input = null;
		try
		{
			FileItem item = inItem.getFileItem();
			if (item instanceof DiskFileItem)
			{
				DiskFileItem fileItem = (DiskFileItem) item;
				if ("gzip".equals(fileItem.getHeaders().getHeader("Content-Encoding")))
				{
					input = new GZIPInputStream(fileItem.getInputStream());
				}
			}
			if (input == null)
			{
				input = item.getInputStream();
			}
		}
		catch ( IOException ex)
		{
			throw new OpenEditException(ex);
		}
		revision.setInputStream(input);
		page.setContentItem(revision);
		log.info("Saved " + page);
		
		//OutputStreamItem item = new OutputStreamItem(page.getPath());
//		String offset = inItem.get("offset");
//		if( offset != null)
//		{getUploadItems
//			item.setSeek(Long.parseLong(offset));
//		}
		//page.setContentItem(item);
		getPageManager().putPage(page);  //FileReposityory will set the item.setOutputstream for us
		inItem.setSavedPage(page);
	}
	public List unzipFiles(boolean inForceUnzip) throws OpenEditException
	{
		List unzippedPages = new ArrayList();
		PageManager pm = getPageManager();
		for (Iterator iterator = getUploadItems().iterator(); iterator.hasNext();)
		{
			FileUploadItem item = (FileUploadItem) iterator.next();
			Page page = pm.getPage(item.getSavedPage().getPath());
			if ( page.getPath().toLowerCase().endsWith(".zip"))
			{
				List unzippedFiles = new ArrayList();
				String ok = (String)item.get("unzip");
				if ( inForceUnzip || "checked".equals(ok) || Boolean.parseBoolean(ok))
				{
					log.info("Unzipping " + page.getPath());
					File in = new File( page.getContentItem().getAbsolutePath() );//getPageManager().getRepository().getStub(inPath)//new File( getRoot(), page.getContentItem().getPath() );
					try
					{
						unzippedFiles.addAll(getZipUtil().unzip(in, in.getParentFile()));
					}
					catch (Exception ex)
					{
						log.error("Could not unzip : " + in.getAbsolutePath());
						log.error( ex );
						return unzippedPages;
					}
					getPageManager().removePage(page);
					for (Iterator iterator2 = unzippedFiles.iterator(); iterator2.hasNext();) {
						File file = (File) iterator2.next();
						String upath = file.getAbsolutePath();
						upath = upath.substring(in.getParentFile().getAbsolutePath().length() + 1);
						upath = page.getDirectory() + "/" + upath;
						Page unz = getPageManager().getPage(upath);
						
						unzippedPages.add(unz);
					}
				}
			}
		}
		return unzippedPages;
	}
	public File getRoot()
	{
		return fieldRoot;
	}
	public void setRoot(File inRoot)
	{
		fieldRoot = inRoot;
	}
	public ZipUtil getZipUtil()
	{
		if( fieldZipUtil == null)
		{
			fieldZipUtil = new ZipUtil();
		}
		return fieldZipUtil;
	}
	public void setZipUtil(ZipUtil inZipUtil)
	{
		fieldZipUtil = inZipUtil;
	}
	public Page saveFirstFileAs(String inPath, User inUser) throws OpenEditException
	{
		FileUploadItem item = getFirstItem();
		saveFileAs(item, inPath, inUser);
		return item.getSavedPage();
	}

	public FileUploadItem getFirstItem()
	{
		for (Iterator iterator = getUploadItems().iterator(); iterator.hasNext();)
		{
			FileUploadItem item = (FileUploadItem) iterator.next();
			return item;
		}
		return null;
	}
	public FileUploadItem getUploadItem(int inI) 
	{
		for (Iterator iterator = getUploadItems().iterator(); iterator.hasNext();)
		{
			FileUploadItem item = (FileUploadItem) iterator.next();
			if( item.getCount() == inI)
			{
				return item;
			}
		}
		return null;
	}
	public FileUploadItem getUploadItemByName(String inName) {
		for (Iterator iterator = getUploadItems().iterator(); iterator.hasNext();)
		{
			FileUploadItem item = (FileUploadItem) iterator.next();
			if( inName.equals(item.getFileItem().getFieldName()))
			{
				return item;
			}
		}
		return null;
	}
	
	public Data getUploadQueueData()
	{
		return fieldUploadQueueData;
	}
	public void setUploadQueueData(Data inUploadQueueData)
	{
		fieldUploadQueueData = inUploadQueueData;
	}
	public void update(long inBytesRead, long inContentLength, int inItemNumber)
	{
		String existing = getUploadQueueData().get("date");
		boolean update = true;
		if( existing != null && inBytesRead != inContentLength)
		{
			Date saved = DateStorageUtil.getStorageUtil().parseFromStorage(existing);
			Date recently = new Date(System.currentTimeMillis() - 2000);
			if( saved.before(recently))
			{
				update = true;
			}
			else
			{
				update = false;
			}
		}
		
		getUploadQueueData().setProperty("filesize", String.valueOf( inContentLength));
		getUploadQueueData().setProperty("filesizeuploaded", String.valueOf( inBytesRead));

		if(update)
		{
			//PRO tip: Put breakpoint here to slow down uploads 
			getUploadQueueData().setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			if( inBytesRead == inContentLength)
			{
				getUploadQueueData().setProperty("status", "complete");
			}
			getUploadQueueSearcher().saveData(getUploadQueueData(), null);
//			try
//			{
//				Thread.sleep(1000);
//			}
//			catch(Exception ewx)
//			{}
		}
	}
}