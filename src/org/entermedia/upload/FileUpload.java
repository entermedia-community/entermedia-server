/*
 Copyright (c) 2003 eInnovation Inc. All rights reserved

 This library is free software; you can redistribute it and/or modify it under the terms
 of the GNU Lesser General Public License as published by the Free Software Foundation;
 either version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU Lesser General Public License for more details.
 */

/*
 * FileUploadAction.java
 *
 * Created on July 31, 2002, 11:41 AM
 */
package org.entermedia.upload;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;

/**
 * DOCUMENT ME!
 *
 * @author cnelson
 */
public class FileUpload 
{
	protected PageManager fieldPageManager;
	protected File fieldRoot;
	/** Defaults to 1MB */
	public static final int BUFFER_SIZE = 1000000;
	protected SearcherManager fieldSearcherManager;
	
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	private static final Log log = LogFactory.getLog(FileUpload.class);

	/* (non-Javadoc)
	 * @see com.openedit.action.Command#execute(java.util.Map, java.util.Map)
	 */
	public UploadRequest uploadFiles(WebPageRequest inContext) throws OpenEditException
	{
		
		if (inContext.getUser() == null)
		{
			throw new OpenEditException("You must be logged in to upload files");
		} 
		
//		Object canUpload = inContext.getPageValue("canupload");
//		if (!Boolean.parseBoolean(String.valueOf(canUpload)))
//		{
//			throw new OpenEditException("You don't have enough permissions to upload files");
//		}
//		
		UploadRequest props = parseArguments(inContext);
		if( props == null)
		{
			return null;
		}
		saveFiles(inContext, props);
		return props;
	}

	public void saveFiles(WebPageRequest inContext, UploadRequest props) throws OpenEditException
	{
		String home = (String)inContext.getPageValue("home");

		for (Iterator iterator = props.getUploadItems().iterator(); iterator.hasNext();)
		{
			FileUploadItem item = (FileUploadItem) iterator.next();
			if( item.getSavedPage() == null)
			{
				props.saveFile(item,home,inContext);
			}
			//Page page = saveFile( props, finalpath, inContext );
		}
		inContext.putPageValue("uploadrequest",props);
		//inContext.setRequestParameter("path", page.getPath());			
	}

	/**
	 * @param inContext
	 * @return
	 */
	public UploadRequest parseArguments(WebPageRequest inContext) throws OpenEditException
	{
		UploadRequest upload = new UploadRequest();
		upload.setPageManager(getPageManager());
		upload.setRoot(getRoot());
		
		//upload.setProperties(inContext.getParameterMap());
		if ( inContext.getRequest() == null) //used in unit tests
		{
			return upload;
		}

		String type = inContext.getRequest().getContentType();
		if ( type == null || !type.startsWith("multipart"))
		{
			addAlreadyUploaded(inContext, upload);
			return upload;
		}
		String uploadid = inContext.getRequestParameter("uploadid");
		String catalogid = inContext.findValue("catalogid");
		upload.setUploadId(uploadid);
		upload.setCatalogId(catalogid);
		upload.setUserName(inContext.getUserName());
		upload.setUploadQueueSearcher(loadQueueSearcher(catalogid));

		FileItemFactory factory = (FileItemFactory)inContext.getPageValue("uploadfilefactory");
		if( factory == null)
		{
			factory = new DiskFileItemFactory();
		}
		
		ServletFileUpload uploadreader = new ServletFileUpload(factory);
		//upload.setSizeThreshold(BUFFER_SIZE);
		if( uploadid != null)
		{
			uploadreader.setProgressListener(upload);
		}
		HttpServletRequest req = inContext.getRequest();
		String encode = req.getCharacterEncoding();
		if ( encode == null)
		{
			//log.info("Encoding not set.");
			encode = "UTF-8";
		}
		//log.info("Encoding is set to " + encode);
		uploadreader.setHeaderEncoding(encode);
		
		//upload.setHeaderEncoding()
		//Content-Transfer-Encoding: binary
		//upload.setRepositoryPath(repository.pathToFile("admin
		uploadreader.setSizeMax(-1);

		try {
			readParameters(inContext, uploadreader, upload, encode);
		} catch (UnsupportedEncodingException e) {
			throw new OpenEditException(e);
		}
		if( uploadid != null)
		{
			expireOldUploads(catalogid);
		}
		
		return upload;
	}

	protected Searcher loadQueueSearcher(String catalogid)
	{
		return getSearcherManager().getSearcher(catalogid, "uploadqueue");
	}

	protected void expireOldUploads(String inCatalogId)
	{
		Searcher searcher = loadQueueSearcher(inCatalogId);
		SearchQuery q = searcher.createSearchQuery();
		
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.HOUR, -1);
		q.addBefore("date", cal.getTime());
		q.addMatches("status", "completed");
		HitTracker hits = searcher.search(q);
		
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data queue = (Data) iterator.next();
			searcher.delete(queue, null);
		}
	}

	protected void readParameters(WebPageRequest inContext, ServletFileUpload uploadreader, UploadRequest upload, String encoding) throws UnsupportedEncodingException
	{
		List fileItems;
		FileItemIterator itemIterator;
		try 
		{
			fileItems = uploadreader.parseRequest(inContext.getRequest());

			// This is a multipart MIME-encoded request, so the request
			// parameters must all be parsed from the POST body, not
			// gotten directly off the HttpServletRequest.

			for (int i = 0; i < fileItems.size(); i++)
			{
				org.apache.commons.fileupload.FileItem tmp = (org.apache.commons.fileupload.FileItem) fileItems.get(i);
				int count = 0;				
				
				if (!tmp.isFormField())
				{
					FileUploadItem foundUpload = new FileUploadItem();
					foundUpload.setFileItem(tmp);
					String name = tmp.getName();
					if( name !=  null && name.contains("\\"))
					{
						name = name.substring(name.lastIndexOf("\\")+1);
					}
					foundUpload.setName(name);
					//String num = "0";
					//int index = tmp.getFieldName().indexOf(".");
//					if(  index > -1)
//					{
//						num = tmp.getFieldName().substring(index+1);
//					}
					
					foundUpload.setCount(count);
					count++;
					upload.addUploadItem(foundUpload);
				}
			}
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		//TODO: Find out why Apache creates tmp files for each parameter attached to the body of the upload
		Map arguments = inContext.getParameterMap();
		for (int i = 0; i < fileItems.size(); i++)
		{
			org.apache.commons.fileupload.FileItem tmp = (org.apache.commons.fileupload.FileItem) fileItems.get(i);
			if (tmp.isFormField() )
			{
				Object vals = arguments.get(tmp.getFieldName());
				String[] values = null;//(String[])

				if( vals instanceof String)
				{
					
					values = new String[1];
					
					values[0] = (String)vals; //the old value?
				}
				else if ( vals != null)
				{
					values = (String[])vals;
				}
				
				if( values == null)
				{
					values = new String[1];
				}
				else
				{
					//grow by one
					String[] newvalues = new String[values.length + 1];
					System.arraycopy(values,0, newvalues,0, values.length);
					values = newvalues;
				}
				
				values[values.length -1] = tmp.getString(encoding).trim();
				
				arguments.put(tmp.getFieldName(), values);
			}
		}
		for (Iterator iterator = arguments.keySet().iterator(); iterator.hasNext();)
		{
			String param = (String)iterator.next();
			Object vals = arguments.get(param);
			if( vals instanceof String[])
			{
				inContext.setRequestParameter(param, (String[])vals);
			}
		}
		addAlreadyUploaded(inContext, upload);

	}

	private void addAlreadyUploaded(WebPageRequest inContext, UploadRequest upload)
	{
		// These file where already uploaded recently using the applet.
		String[] paths = inContext.getRequestParameters("uploadpath");
		if (paths != null)
		{
			for (int i = 0; i < paths.length; i++)
			{
				String name = paths[i].replace('\\', '/');
				Page page = getPageManager().getPage(name);
				FileUploadItem item = new FileUploadItem();
				item.setSavedPage(page);
				item.setCount(i);
				item.setName(page.getName());
				upload.addUploadItem(item);
			}
		}
	}
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager( PageManager pageManager )
	{
		fieldPageManager = pageManager;
	}

	public File getRoot() {
		return fieldRoot;
	}

	public void setRoot(File inRoot) {
		fieldRoot = inRoot;
	}
}
