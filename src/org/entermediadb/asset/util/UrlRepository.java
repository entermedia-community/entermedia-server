package org.entermediadb.asset.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.openedit.repository.BaseRepository;
import org.openedit.repository.ContentItem;
import org.openedit.repository.InputStreamItem;
import org.openedit.repository.Repository;
import org.openedit.repository.RepositoryException;

public class UrlRepository extends  BaseRepository
{
	protected HttpClient fieldHttpClient;

	public org.apache.commons.httpclient.HttpClient getHttpClient()
	{
		if (fieldHttpClient == null)
		{
			fieldHttpClient = new HttpClient();
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
		protected Boolean existed;
		
		public InputStream getInputStream() throws RepositoryException 
		{
			try
			{
				String fullpath = getAbsolutePath().replace(" ", "%20");
				//fullpath = fullpath.replace(";", "%3b");
				GetMethod postMethod = new GetMethod(fullpath);
				int statusCode1 = getHttpClient().executeMethod(postMethod);
				if (statusCode1 == 200)
				{
					fieldInputStream = postMethod.getResponseBodyAsStream();
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
					HeadMethod postMethod = new HeadMethod(fullpath);
					int statusCode1 = getHttpClient().executeMethod(postMethod);
					existed = new Boolean(statusCode1 == 200);
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
