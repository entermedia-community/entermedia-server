package org.entermediadb.asset.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClients;
import org.openedit.repository.BaseRepository;
import org.openedit.repository.ContentItem;
import org.openedit.repository.InputStreamItem;
import org.openedit.repository.Repository;
import org.openedit.repository.RepositoryException;

public class UrlRepository extends  BaseRepository
{
	protected HttpClient fieldHttpClient;

	public HttpClient getHttpClient()
	{
		if (fieldHttpClient == null)
		{
			  RequestConfig globalConfig = RequestConfig.custom()
		                .setCookieSpec(CookieSpecs.DEFAULT)
		                .build();
			  fieldHttpClient = HttpClients.custom()
		                .setDefaultRequestConfig(globalConfig)
		                .build();
		}

		return fieldHttpClient;
	}

	public void setHttpClient(HttpClient inHttpClient)
	{
		fieldHttpClient = inHttpClient;
	}


	public ContentItem get(String inPath) throws RepositoryException
	{
		String path = inPath.substring(getPath().length());
		String url = getExternalPath() + path;
		UrlContentItem item = new UrlContentItem(); 
			item.setPath(inPath);
			item.setAbsolutePath(url);
		return item;
	}

	
	public void copy(ContentItem inSource, ContentItem inDestination) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	public void deleteOldVersions(String inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	public boolean doesExist(String inPath) throws RepositoryException
	{
		return true;
	}

	public List getChildrenNames(String inParent) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public ContentItem getLastVersion(String inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public ContentItem getStub(String inPath) throws RepositoryException
	{
		return get(inPath);
	}

	public List getVersions(String inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void move(ContentItem inSource, ContentItem inDestination) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	public void move(ContentItem inSource, Repository inSourceRepository, ContentItem inDestination) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	public void put(ContentItem inContent) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	public void remove(ContentItem inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	class UrlContentItem extends InputStreamItem
	{
		protected Boolean existed = null;
		
		public InputStream getInputStream() throws RepositoryException 
		{
			try
			{
				String fullpath = getAbsolutePath().replace(" ", "%20");
				//fullpath = fullpath.replace(";", "%3b");
				HttpGet postMethod = new HttpGet(fullpath);
				HttpResponse res = getHttpClient().execute(postMethod);
				if (res.getStatusLine().getStatusCode() == 200)
				{
					fieldInputStream = res.getEntity().getContent();
				}
			}
			catch ( IOException ex)
			{
				throw new RepositoryException(ex);
			}
			return fieldInputStream;
		}
		public boolean exists()
		{
			if( existed == null)
			{
				try
				{
					String fullpath = getAbsolutePath().replace(" ", "%20");
					HttpHead postMethod = new HttpHead(fullpath);
					//HeadMethod postMethod = new HeadMethod(fullpath);
					HttpResponse res = getHttpClient().execute(postMethod);
					if (res.getStatusLine().getStatusCode() == 200)
					{
						existed = Boolean.TRUE;
					}
					else
					{
						existed = Boolean.FALSE;
					}
				}
				catch ( IOException ex)
				{
					throw new RepositoryException(ex);
				}
				
			}
			return existed.booleanValue();
		}

	}
}
