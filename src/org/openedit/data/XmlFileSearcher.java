package org.openedit.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.dom4j.Element;
import org.entermedia.locks.Lock;
import org.openedit.Data;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.entermedia.SourcePathCreator;
import org.openedit.repository.ContentItem;
import org.openedit.xml.ElementData;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.util.IntCounter;
import com.openedit.util.PathProcessor;

public class XmlFileSearcher extends BaseLuceneSearcher
{
	protected Log log = LogFactory.getLog(XmlFileSearcher.class);
	protected XmlArchive fieldXmlArchive;
	protected XmlDataArchive fieldXmlDataArchive;
	protected IntCounter fieldIntCounter;
	protected PageManager fieldPageManager;
	protected String fieldPrefix;
	protected SourcePathCreator fieldSourcePathCreator;
	
	public SourcePathCreator getSourcePathCreator()
	{
		return fieldSourcePathCreator;
	}
	public void setSourcePathCreator(SourcePathCreator inSourcePathCreator)
	{
		fieldSourcePathCreator = inSourcePathCreator;
	}
	protected XmlDataArchive getXmlDataArchive()
	{
		if (fieldXmlDataArchive == null)
		{
			fieldXmlDataArchive = new XmlDataArchive();
			fieldXmlDataArchive.setXmlArchive(getXmlArchive());
			fieldXmlDataArchive.setDataFileName(getDataFileName());
			fieldXmlDataArchive.setElementName(getSearchType());
			fieldXmlDataArchive.setPathToData(getPathToData());
		}
		return fieldXmlDataArchive;
	}
	public void setXmlDataArchive(XmlDataArchive inArchive)
	{
		fieldXmlDataArchive = inArchive;
	}
	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}
/*
	public Data loadData(String inSourcePath, String inId)
	{
		//TODO: Load from cache?
		
		Data data = getXmlDataArchive().loadData(inSourcePath,inId);
		return data;
	}
*/	
	public Data createNewData()
	{	
		if( getNewDataName() == null)
		{
			ElementData data = new ElementData();
			return data;
		}
		return (Data)getModuleManager().getBean(getNewDataName());
	}
	public Object searchByField(String inId, String inValue)
	{
		if (fieldCacheManager != null && "id".equals(inId))
		{
			Object cached = getCacheManager().get(getIndexPath(), inValue);
			if (cached != null)
			{
				return cached;
			}
		}
		if(inValue == null || inValue.trim().length() == 0){
			return null;
		}
		SearchQuery query = createSearchQuery();
		query.addMatches(inId, inValue);
		
		HitTracker hits = search(query);
		hits.setHitsPerPage(1);
		Data first = (Data)hits.first();
		if( first == null)
		{
			return null;
		}
		if( "id".equals(inId) )
		{
			String sourcepath = first.getSourcePath();
			if (sourcepath != null)
			{
				ElementData elementd = (ElementData)getXmlDataArchive().loadData(sourcepath, first.getId());
				if( getNewDataName() != null )
				{
					first = createNewData();
					copyData(elementd,first);
				}
				else
				{
					first = elementd;
				}
				if (fieldCacheManager != null)
				{
					getCacheManager().put(getIndexPath(), inValue, first);
				}
			}
			else
			{
				throw new OpenEditException("Database missing sourcepath for asset " + getSearchType() +" / " + inId);
			}
		}
		return first;
	}
	
	protected void copyData(Data inElementd, Data inNewdata)
	{
		inNewdata.setId(inElementd.getId());
		inNewdata.setName(inElementd.getName());
		inNewdata.setProperties(inElementd.getProperties());
	}
	public void saveAllData(Collection inAll, User inUser)
	{
		//check that all have ids
		for (Object object: inAll)
		{
			Data data = (Data)object;
			if(data.getId() == null)
			{
				data.setId(nextId());
			}
			if( data.getSourcePath() == null) //TODO: Move this to the saving code?
			{
				String sourcepath = getSourcePathCreator().createSourcePath(data, data.getId() );
				data.setSourcePath(sourcepath);
			}
		}
		updateIndex(inAll);
		getXmlDataArchive().saveAllData(inAll, getCatalogId(), getPrefix() + "/", inUser );
		//getLiveSearcher(); //should flush the index
	}

	public void updateIndex(IndexWriter inWriter, Data inData) throws OpenEditException
	{
		Document doc = new Document();
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetailsCached(getSearchType());
		if( details == null)
		{
			throw new OpenEditException("No " + getSearchType() + "properties.xml file available");
		}
		try
		{
			updateIndex(inData, doc, details);
			Term term = new Term("id", inData.getId());
			inWriter.updateDocument(term, doc, getAnalyzer());
			if (fieldCacheManager != null)
			{
				getCacheManager().remove(getIndexPath(), inData.getId());
			}
			clearIndex();
		}
		catch (IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}
	/**
	 * @deprecated use updateIndex(Data)
	 * @param inWriter
	 * @param inElement
	 * @param inSourcePath
	 */
	protected void updateIndex(IndexWriter inWriter, Element inElement, String inSourcePath)
	{
		ElementData data = (ElementData)createNewData();
		data.setElement(inElement);
		data.setSourcePath(inSourcePath);
		updateIndex(inWriter, data);
	}
	protected void updateIndex(Data inData, Document doc, PropertyDetails inDetails)
	{
		if( inData.getSourcePath() == null )
		{
			throw new OpenEditException("Cant save data without a sourcepath parameter");
		}

		super.updateIndex(inData, doc, inDetails);
		doc.add(new Field("sourcepath", inData.getSourcePath(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
	}


	public void saveData(Data data, User inUser)
	{
		if(data.getId() == null)
		{
			data.setId(nextId());
		}
		if( data.getSourcePath() == null)
		{
			String sourcepath = getSourcePathCreator().createSourcePath(data, data.getId() );
			data.setSourcePath(sourcepath);
		}
		
		Lock lock = null;
		try
		{
			lock = getLockManager().lock(getCatalogId(), getPrefix() + "/" + data.getSourcePath(),"admin");
			getXmlDataArchive().saveData(data,inUser, lock);
		
//		String nullcheck = data.get("publishqueueid");
//		log.info( getSearchType() + " " + data.getId() + " " + nullcheck );
		updateIndex(data);
		} 	finally
		{
			getLockManager().release(getCatalogId(), lock);
		}
	}

	protected IntCounter getIntCounter()
	{
		if (fieldIntCounter == null)
		{
			synchronized (this)
			{
				
				if (fieldIntCounter == null)
				{
					fieldIntCounter = new IntCounter();
					fieldIntCounter.setLabelName("idCount");
					
					String path = null;
						
		//			if( fieldIndexRootFolder != null)
		//			{
		//				path = "/WEB-INF/data/" + getCatalogId() +"/"+ getIndexRootFolder() + "/" + getSearchType() + "/idcounter.properties";
		//			}
		//			else
		//			{
						path = "/WEB-INF/data/" + getCatalogId() +"/"+ getSearchType() + "s/idcounter.properties";
		//			}
					Page prop = getPageManager().getPage(path);
					File file = new File(prop.getContentItem().getAbsolutePath());
					file.getParentFile().mkdirs();
					fieldIntCounter.setCounterFile(file);
				}
			}
		}
		return fieldIntCounter;
	}
	
	public synchronized String nextId()
	{
		return String.valueOf(getIntCounter().incrementCount());
	}
	
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager pageManager)
	{
		fieldPageManager = pageManager;
	}
	
	public String getPathToData()
	{
		return "/WEB-INF/data/" + getCatalogId() + "/" + getPrefix();
	}
	
	public String getDataFileName()
	{
		return getSearchType() + ".xml";
	}
	

	protected void reIndexAll(final IndexWriter inWriter, final TaxonomyWriter inTaxonomyWriter) throws OpenEditException
	{
		final List buffer = new ArrayList(100);
		PathProcessor processor = new PathProcessor()
		{
			public void processFile(ContentItem inContent, User inUser)
			{
				if (!inContent.getName().equals(getSearchType() + ".xml"))
				{
					return;
				}
				String sourcepath = inContent.getPath();
				sourcepath = sourcepath.substring(getPathToData().length() + 1,
						sourcepath.length() - getDataFileName().length() - 1);
				String path = inContent.getPath();
				XmlFile content = getXmlArchive().getXml(path, getSearchType());
				for (Iterator iterator = content.getElements().iterator(); iterator.hasNext();)
				{
					Element element = (Element) iterator.next();
					ElementData data = (ElementData)createNewData();
					data.setElement(element);
					data.setSourcePath(sourcepath);
					buffer.add(data);
					if( buffer.size() > 99)
					{
						updateIndex(inWriter, inTaxonomyWriter, buffer);
					}
				}
			}
		};
		processor.setRecursive(true);
		processor.setRootPath(getPathToData());
		processor.setPageManager(getPageManager());
		processor.setIncludeExtensions("xml");
		processor.process();
		updateIndex(inWriter, inTaxonomyWriter, buffer);
	}
	

	public void delete(Data inData, User inUser)
	{
		// Delete from xml
		if( inData == null || inData.getSourcePath() == null || inData.getId() == null )
		{
			throw new OpenEditException("Cannot delete null data.");
		}
		getXmlDataArchive().delete(inData,inUser);
		// Remove from Index
		deleteRecord(inData);
	}
	public void deleteAll(User inUser)
	{
		PathProcessor processor = new PathProcessor()
		{
			public void processFile(ContentItem inContent, User inUser)
			{
				if (inContent.getName().equals(getDataFileName()))
				{
					getPageManager().getRepository().remove(inContent);
				}
			}
		};
		processor.setRecursive(true);
		processor.setRootPath(getPathToData());
		processor.setPageManager(getPageManager());
		processor.setIncludeExtensions("xml");
		processor.process();
		reIndexAll();
	}
	
	public String getPrefix()
	{
		if(fieldPrefix == null)
		{
			fieldPrefix = getPropertyDetails().getPrefix();
			
			if( fieldPrefix == null)
			{
				fieldPrefix = getSearchType();
			}
		}
		return fieldPrefix;
	}

	public void setPrefix(String prefix)
	{
		fieldPrefix = prefix;
	}
	
	public void clearIndex()
	{
		super.clearIndex();
		getXmlDataArchive().clearCache();  //Is this needed? We should just clear one item at a time from the c
	}

	public HitTracker getAllHits(WebPageRequest inReq)
	{
		SearchQuery q = createSearchQuery();
		q.addMatches("id","*");  //Had to do this since description is not always fully populated. TODO: use Lucene API to get All
		addShowOnly(inReq, q);
		if (inReq != null)
		{
			String sort = inReq.getRequestParameter("sortby");
			if (sort == null)
			{
				sort = inReq.findValue("sortby");
			}
			if (sort != null)
			{
				q.setSortBy(sort);
			}
			return cachedSearch(inReq, q);
		} else{
			return search(q);
		}
		
	}

	
}
