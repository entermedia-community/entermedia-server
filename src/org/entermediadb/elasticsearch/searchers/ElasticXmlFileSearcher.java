package org.entermediadb.elasticsearch.searchers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.entermediadb.asset.SourcePathCreator;
import org.entermediadb.data.DataArchive;
import org.entermediadb.data.XmlDataArchive;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.OpenEditRuntimeException;
import org.openedit.data.PropertyDetails;
import org.openedit.locks.Lock;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.IntCounter;
import org.openedit.util.PathProcessor;
import org.openedit.xml.ElementData;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

/**
 * This is not going to be used much longer
 * All other classes can just use base
 * 
 * Base and Transient are the same
 * 
 * @author shanti
 *
 */


public class ElasticXmlFileSearcher extends BaseElasticSearcher
{
	protected Log log = LogFactory.getLog(ElasticXmlFileSearcher.class);
	protected XmlArchive fieldXmlArchive;
	protected DataArchive fieldDataArchive; //lazy loaded
	protected String fieldPrefix;
	protected String fieldDataFileName;
	protected SourcePathCreator fieldSourcePathCreator;
	protected PageManager fieldPageManager;
	protected IntCounter fieldIntCounter;
//	GetMappingsRequest find = new GetMappingsRequest().types(getSearchType()); 
//	GetMappingsResponse found = admin.indices().getMappings(find).actionGet();
//	if( !found.isContextEmpty())

	public synchronized String nextId()
	{
		Lock lock = getLockManager().lock(loadCounterPath(), "ElasticXmlFileSearcher.nextId");
		try
		{
			return String.valueOf(getIntCounter().incrementCount());
		}
		finally
		{//		GetMappingsRequest find = new GetMappingsRequest().types(getSearchType()); 
//			GetMappingsResponse found = admin.indices().getMappings(find).actionGet();
//			if( !found.isContextEmpty())

			getLockManager().release(lock);
		}
	}

	protected IntCounter getIntCounter()
	{
		if (fieldIntCounter == null)
		{
			fieldIntCounter = new IntCounter();
			// fieldIntCounter.setLabelName(getSearchType() + "IdCount");
			Page prop = getPageManager().getPage(loadCounterPath());
			File file = new File(prop.getContentItem().getAbsolutePath());
			file.getParentFile().mkdirs();//		GetMappingsRequest find = new GetMappingsRequest().types(getSearchType()); 
//			GetMappingsResponse found = admin.indices().getMappings(find).actionGet();
//			if( !found.isContextEmpty())

			fieldIntCounter.setCounterFile(file);
		}
		return fieldIntCounter;
	}
	public SourcePathCreator getSourcePathCreator()
	{
		return fieldSourcePathCreator;
	}

	public void setSourcePathCreator(SourcePathCreator inSourcePathCreator)
	{
		fieldSourcePathCreator = inSourcePathCreator;
	}//		GetMappingsRequest find = new GetMappingsRequest().types(getSearchType()); 
//	GetMappingsResponse found = admin.indices().getMappings(find).actionGet();
//	if( !found.isContextEmpty())


	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager pageManager)
	{
		fieldPageManager = pageManager;
	}

	public String getPathToData()
	{//		GetMappingsRequest find = new GetMappingsRequest().types(getSearchType()); 
//		GetMappingsResponse found = admin.indices().getMappings(find).actionGet();
//		if( !found.isContextEmpty())

		return "/WEB-INF/data/" + getCatalogId() + "/" + getPrefix();
	}

	public String getDataFileName()
	{
		if (fieldDataFileName == null)
		{
			fieldDataFileName = getSearchType() + ".xml";
		}
		return fieldDataFileName;
	}

	public void setDataFileName(String inName)//		GetMappingsRequest find = new GetMappingsRequest().types(getSearchType()); 
//	GetMappingsResponse found = admin.indices().getMappings(find).actionGet();
//	if( !found.isContextEmpty())

	{
		fieldDataFileName = inName;
	}

	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}

	public Data createNewData()
	{
		if (getNewDataName() == null)
		{

			ElementData data = new ElementData();

			return data;
		}
		return (Data) getModuleManager().getBean(getNewDataName());
	}

	public void reIndexAll() throws OpenEditException
	{

		if (isReIndexing())
		{
			return;
		}
		setReIndexing(true);
		try
		{
			final List buffer = new ArrayList(100);
			final String pathToData = getPathToData();
			PathProcessor processor = new PathProcessor()
			{
				public void processFile(ContentItem inContent, User inUser)
				{
					if (!inContent.getName().equals(getDataFileName()))
					{
						return;
					}
					String sourcepath = inContent.getPath();
					sourcepath = sourcepath.substring(pathToData.length() + 1, sourcepath.length() - getDataFileName().length() - 1);
					hydrateData(inContent, sourcepath, buffer);
					incrementCount();
				}
			};
			processor.setRecursive(true);
			processor.setRootPath(pathToData);
			processor.setPageManager(getPageManager());
			processor.setIncludeMatches("xml");
			processor.process();
			if (buffer.size()  > 0)
			{
				updateIndex(buffer, null);
				buffer.clear();
				flushChanges();
			}
			log.info("reindexed " + processor.getExecCount());
		}
		catch (Exception e)
		{
			throw new OpenEditRuntimeException(e);
		}
		finally
		{
			setReIndexing(false);
		}
	}
	public void restoreSettings()
	{
		getPropertyDetailsArchive().clearCustomSettings(getSearchType());
		reIndexAll();
	}

	
	protected void hydrateData(ContentItem inContent, String sourcepath, List buffer)
	{
		String path = inContent.getPath();
		//TODO: Create new api to load up assets
		XmlFile content = getDataArchive().getXmlArchive().getXml(path, getSearchType());

		// TODO Auto-generated method stub
		for (Iterator iterator = content.getElements().iterator(); iterator.hasNext();)
		{
			Element element = (Element) iterator.next();
			ElementData data = new ElementData();
			data.setElement(element);
			data.setSourcePath(sourcepath);
			buffer.add(data);
			if (buffer.size() > 1000)
			{
				updateIndex(buffer, null);
				buffer.clear();
			}
		}

	}

	public String getPrefix()
	{
		if (fieldPrefix == null)
		{
			fieldPrefix = getPageManager().getPage("/" + getCatalogId()).get("defaultdatafolder");
			if (fieldPrefix == null)
			{
				fieldPrefix = getSearchType(); //legacy support
			}
		}
		return fieldPrefix;
	}

	public void setPrefix(String prefix)
	{
		fieldPrefix = prefix;
	}

	public void delete(Data inData, User inUser)
	{
//		if (inData instanceof SearchHitData)
//		{
//			inData = (Data) searchById(inData.getId());
//		}
		if (inData == null || inData.getSourcePath() == null || inData.getId() == null)
		{
			throw new OpenEditException("Cannot delete null data.");
		}
		//getDataArchive().delete(inData, inUser);
		super.delete(inData, inUser);
	}

	//This is the main APU for saving and updates to the index
	public void saveAllData(Collection<Data> inAll, User inUser)
	{
//		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());
//		for (Object object : inAll)
//		{
//			Data data = (Data) object;
//			try
//			{
//				updateElasticIndex(details, data);
//			}
//			catch (Throwable ex)
//			{
//				log.error("problem saving " + data.getId(), ex);
//				throw new OpenEditException(ex);
//			}
//		}
		updateIndex(inAll, inUser);
		//getDataArchive().saveAllData(inAll, inUser);
	}

	//TODO: Deal with non XML saves
	
	
	public void saveData(Data inData, User inUser)
	{
		//update the index
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());

		updateElasticIndex(details, inData);
		if (inData.getSourcePath() == null)
		{
			log.error( getSearchType() + " Missing sourcepath" + inData);
			String sourcepath = getSourcePathCreator().createSourcePath(inData, inData.getId());
			inData.setSourcePath(sourcepath);
			updateElasticIndex(details, inData);
		}
		clearIndex();

		//getDataArchive().saveData(inData, inUser);//Don't save - yikes :)
	}

	protected DataArchive getDataArchive()
	{
		if (fieldDataArchive == null)
		{
			DataArchive archive = (DataArchive)getModuleManager().getBean(getCatalogId(),"xmlDataArchive");
			archive.setDataFileName(getDataFileName());
			archive.setElementName(getSearchType());
			archive.setPathToData(getPathToData());
			fieldDataArchive = archive;
		}

		return fieldDataArchive;
	}

	public Object searchByField(String inField, String inValue)
	{
		if (inValue == null)
		{
			throw new OpenEditException("Can't search for null value on field " + inField);
		}
		Data newdata = (Data) super.searchByField(inField, inValue);
		if( inField.equals("id"))
		{
			if( getNewDataName() != null )
			{
				Data typed = createNewData();		
				typed.setId(newdata.getId());
				typed.setName(newdata.getName());
				typed.setSourcePath(newdata.getSourcePath());
				copyData(newdata,typed);//.setProperties(newdata.getProperties());
				return typed;
			}	
		}	
		return newdata;
/*		
		if( inField.equals("id"))
		{
			String sourcepath = null;
			String id = null;
	
			if (newdata == null)
			{
				return null;
			}
	
			if (newdata.getSourcePath() == null)
			{
				//sourcepath = getSourcePathCreator().createSourcePath(newdata, newdata.getId() );
				throw new OpenEditException("Sourcepath required for " + getSearchType());
			}
			else
			{
				sourcepath = newdata.getSourcePath();
			}
			id = newdata.getId();
	
			String path = getPathToData() + "/" + sourcepath + "/" + getSearchType() + ".xml";
			XmlArchive archive = getDataArchive().getXmlArchive();
			XmlFile content = archive.getXml(path, getSearchType());
			//log.info( newdata.getProperties() );
			if (!content.isExist())
			{
				//throw new OpenEditException("Missing data file " + path);
				return null;
			}
			Element element = content.getElementById(id);
	
			ElementData realdata = (ElementData) createNewData();
			realdata.setElement(element);
			realdata.setSourcePath(sourcepath);
	
			return realdata;
		}	
		else
		{
			return newdata;
		}
	}
*/	
	}
}
