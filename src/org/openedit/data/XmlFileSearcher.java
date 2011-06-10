package org.openedit.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.data.lucene.BaseLuceneSearcher;
import org.openedit.repository.ContentItem;
import org.openedit.xml.ElementData;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.OpenEditException;
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
	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}

	public Data loadData(String inSourcePath, String inId)
	{
		Data data = getXmlDataArchive().loadData(this,inSourcePath,inId);
		return data;
	}
	
	public Data createNewData()
	{
		if( fieldNewDataName == null)
		{
			ElementData data = new ElementData();
			
			return data;
		}
		return (Data)getModuleManager().getBean(getNewDataName());
	}
	public Object searchByField(String inId, String inValue)
	{
		SearchQuery query = createSearchQuery();
		query.addMatches(inId, inValue);
		HitTracker hits = search(query);
		Data first = (Data)hits.first();
		if( first == null)
		{
			return null;
		}
		String sourcepath = first.getSourcePath();
		if (sourcepath != null)
		{
			return loadData(sourcepath, first.getId());
		}
		return null;
	}
	public Object searchById(String inId)
	{
//		return super.searchById(inId);
		return searchByField("id",inId);
	}
	
	public void saveAllData(List inAll, User inUser)
	{
		//check that all have ids
		for (Object object: inAll)
		{
			Data data = (Data)object;
			if(data.getId() == null)
			{
				data.setId(nextId());
			}			
		}
		getXmlDataArchive().saveAllData(inAll, inUser);
		updateIndex(inAll);
		getLiveSearcher(); //should flush the index
	}

	public void updateIndex(IndexWriter inWriter, Data inData) throws OpenEditException
	{
		Document doc = new Document();
		PropertyDetails details = getPropertyDetailsArchive().getPropertyDetails(getSearchType());
		if( details == null)
		{
			throw new OpenEditException("No " + getSearchType() + "properties.xml file available");
		}
		try
		{
			updateIndex(inData, doc, details);
			Term term = new Term("id", inData.getId());
			inWriter.updateDocument(term, doc, getAnalyzer());
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
		super.updateIndex(inData, doc, inDetails);
		if( inData.getSourcePath() == null)
		{
			throw new OpenEditException("XmlFile searcher requires sourcepath be set " + inData.getProperties());
		}
		doc.add(new Field("sourcepath", inData.getSourcePath(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
	}


	public void saveData(Object inData, User inUser)
	{
		if( !(inData instanceof Data) )
		{
			return;
		}
		Data data = (Data) inData;
		if( data.getSourcePath() == null )
		{
			throw new OpenEditException("Cant save data without a sourcepath parameter");
		}
		if(data.getId() == null)
		{
			data.setId(nextId());
		}
		
		updateIndex(data);
		getXmlDataArchive().saveToXml(data);
	}

	protected IntCounter getIntCounter()
	{
		if (fieldIntCounter == null)
		{
			fieldIntCounter = new IntCounter();
			fieldIntCounter.setLabelName("idCount");
			Page prop = getPageManager().getPage("/WEB-INF/data/" + getCatalogId() +"/"+ getSearchType() + "s/idcounter.properties");
			File file = new File(prop.getContentItem().getAbsolutePath());
			file.getParentFile().mkdirs();
			fieldIntCounter.setCounterFile(file);
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
	

	public void reIndexAll(final IndexWriter inWriter) throws OpenEditException
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
						updateIndex(inWriter, buffer);
					}
				}
			}
		};
		processor.setRecursive(true);
		processor.setRootPath(getPathToData());
		processor.setPageManager(getPageManager());
		processor.setFilter("xml");
		processor.process();
		updateIndex(inWriter, buffer);
	}
	
	public String getIndexPath()
	{
		return "/WEB-INF/data/" + getCatalogId() +"/" + getSearchType() + "s/index";
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
	
	public String getPrefix()
	{
		if(fieldPrefix == null)
		{
			fieldPrefix = getPageManager().getPage("/" + getCatalogId()).get("defaultdatafolder");
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
		getXmlDataArchive().clearCache();
	}

}
