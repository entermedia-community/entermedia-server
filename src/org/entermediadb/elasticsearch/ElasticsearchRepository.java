package org.entermediadb.elasticsearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.openedit.repository.ContentItem;
import org.openedit.repository.InputStreamItem;
import org.openedit.repository.Repository;
import org.openedit.repository.RepositoryException;

public class ElasticsearchRepository implements Repository
{

	@Override
	public boolean matches(String inPath)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ContentItem get(String inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContentItem getStub(String inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean doesExist(String inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void put(ContentItem inContent) throws RepositoryException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void copy(ContentItem inSource, ContentItem inDestination) throws RepositoryException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(ContentItem inSource, ContentItem inDestination) throws RepositoryException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(ContentItem inSource, Repository inSourceRepository, ContentItem inDestination) throws RepositoryException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove(ContentItem inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public List getVersions(String inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ContentItem getLastVersion(String inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPath(String inPath)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getPath()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setExternalPath(String inRootAbsolutePath)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getExternalPath()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFilterIn(String inFilters)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFilterOut(String inFilters)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getFilterIn()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFilterOut()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List getChildrenNames(String inParent) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteOldVersions(String inPath) throws RepositoryException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getRepositoryType()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRepositoryType(String inType)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getMatchesPostFix()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMatchesPostFix(String inMatchesPostFix)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setProperty(String inPropName, String inValue)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getProperty(String inPropName)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map getProperties()
	{
		// TODO Auto-generated method stub
		return null;
	}

	
	class ElasticContentItem extends InputStreamItem
	{
		protected Boolean existed;
		
		public InputStream getInputStream() throws RepositoryException 
		{
			
			return fieldInputStream;
		}
		public boolean exists()
		{
			return true;
		}

	}
	
	
}
