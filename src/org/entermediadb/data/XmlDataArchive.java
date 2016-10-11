package org.entermediadb.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.SearcherManager;
import org.openedit.locks.Lock;
import org.openedit.locks.LockManager;
import org.openedit.users.User;
import org.openedit.xml.ElementData;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

public class XmlDataArchive implements DataArchive 
{
	protected XmlArchive fieldXmlArchive;
	protected SearcherManager fieldSearcherManager;
	protected String fieldCatalogId;
	
	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public String getPathToData()
	{
		return fieldPathToData;
	}

	public void setPathToData(String inPathToData)
	{
		fieldPathToData = inPathToData;
	}

	public String getDataFileName()
	{
		return fieldDataFileName;
	}

	public void setDataFileName(String inDataFileName)
	{
		fieldDataFileName = inDataFileName;
	}

	protected String fieldPathToData;
	protected String fieldDataFileName;
	protected String fieldElementName;
	
	public String getElementName()
	{
		return fieldElementName;
	}

	public void setElementName(String inElementName)
	{
		fieldElementName = inElementName;
	}

	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}

	private void addRow(Data inData, XmlFile xml)
	{
		Element element = xml.getElementById(inData.getId());
		if( element == null )
		{
			//New element
			element = xml.getRoot().addElement(xml.getElementName());
			element.addAttribute("id", inData.getId());
		}
		if(inData instanceof ElementData)
		{
			populateElementData(element, (ElementData)inData);
		}
		else
		{
			saveDataToElement(inData,element);
		}
	}
	

	protected void saveDataToElement(Data inData, Element inElement)
	{
		// TODO Auto-generated method stub
		inElement.clearContent();
		inElement.setAttributes(null);
		
		ElementData data = new ElementData(inElement);
		data.setId(inData.getId());
		data.setName(inData.getName());
		data.setSourcePath(inData.getSourcePath());
		for (Iterator iterator = inData.keySet().iterator(); iterator.hasNext();)
		{
			String key	= (String) iterator.next();
			data.setValue(key, inData.getValue(key));
		}
	}

	//Not recommeneded, use populateElementData
	/*
	protected void populateElement(Element inElement, Data inData)
	{
		for (Iterator iterator = inData.getProperties().keySet().iterator(); iterator.hasNext();)
		{
			String detail = (String) iterator.next();
			if( !detail.equals("id") && !detail.equals("sourcepath") && !detail.startsWith("."))
			{
				String value = inData.get(detail);
				if( value != null)
				{
					inElement.addAttribute(detail, value);
				}
			}
		}
	}
	*/
	protected void populateElementData(Element inElement, ElementData inData)
	{
		List attributes = inData.getAttributes();
		List attributessaved = new ArrayList(attributes.size()); 
		boolean foundname = false;
		for (Iterator iterator = attributes.iterator(); iterator.hasNext();) {
			Attribute attr = (Attribute) iterator.next();
			if( !attr.getName().startsWith(".") )
			{
				attributessaved.add(attr);				
			}
			if( attr.getName().equals("name")  )
			{
				foundname = true;
			}
		}
		inElement.setAttributes(attributessaved);
		
		inElement.clearContent();
		
		//Mixed content is ok
		for (Iterator iterator = inData.getElement().elementIterator(); iterator.hasNext();)
		{
			Element child = (Element) iterator.next();
			inElement.add(child.createCopy());
			if( "name".equals( child.attributeValue("id") ) )
			{
				foundname = true;
			}
		}
		
//		boolean foundname = false;
//		for(Iterator iterator = attributes.iterator(); iterator.hasNext();)
//		{
//			Attribute attr = (Attribute)iterator.next();
//			String id = attr.getName();
//			if(!id.equals("id") && !id.startsWith("."))
//			{
//				if( id.equals("name"))
//				{
//					foundname = true;
//				}
//				inElement.addAttribute(attr.getName(), attr.getValue());
//			}
//		}
		
		//This should not happen any more
		if( !foundname && inData.getName() != null)
		{
			inElement.addCDATA(inData.getName());
		}
	}

	public String getPathToXml( String inSourcePath )
	{
		String path = getPathToData() + "/" + inSourcePath;
		if( !path.endsWith("/"))
		{
			path = path + "/";
		}
		path = path +  getDataFileName();
		return path;
	}
	protected String getCacheName()
	{
		return getPathToData() + getDataFileName();
	}
	
	public Data loadData(String inSourcePath, String inId)
	{
		//This is used a bunch when loading and editing the same xml file
		String path = getPathToXml(inSourcePath);

		XmlFile xml = getXmlArchive().getXml(path, getElementName());
		Element elem = xml.getElementById(inId);
		if(elem == null)
		{
			return null;
		}
		ElementData data = new ElementData(elem);
		data.setSourcePath(inSourcePath);
		return data;
	}
	public void clearCache()
	{
		//getIdCache().clear();
	}
	public void delete(Data inData, User inUser)
	{
		String path = getPathToXml(inData.getSourcePath());
		LockManager lockManager = getSearcherManager().getLockManager(getCatalogId());
		Lock lock = lockManager.lock(path, "xmlDataArchive.delete");
		try
		{
			XmlFile xml = getXmlArchive().getXml(path, getElementName());
			Element element = xml.getElementById(inData.getId());
			if( element != null )
			{
				xml.deleteElement(element);
			}
			getXmlArchive().saveXml(xml, inUser);
		}
		finally
		{
			lockManager.release(lock);
		}	
	}
//	public XmlFile getXml(String inPath, String inSearchType)
//	{
//		return getXmlArchive().getXml(inPath,inSearchType);
//	}

	
	public void saveData(Data inData, User inUser) {
		
		if( inData == null )
		{
			throw new OpenEditException("Cannot save null data.");
		}
		if(  inData.getSourcePath() == null )
		{
			throw new OpenEditException("sourcepath is required ");
		}
		String path = getPathToXml(inData.getSourcePath());
		//TODO: Need to lock this file so another person does not call save
		LockManager lockManager = getSearcherManager().getLockManager(getCatalogId());
		Lock lock = lockManager.lock(path, "xmlDataArchive.saveData");
		try
		{
			XmlFile xml = getXmlArchive().getXml(path, getElementName());
			addRow(inData, xml);
			getXmlArchive().saveXml(xml, null);
		}
		finally
		{
			lockManager.release(lock);
		}
	}
	public void saveAllData(Collection<Data> inAll, User inUser) 
	{
		XmlFile xml = null;//
		Lock lock = null;
		LockManager lockManager = getSearcherManager().getLockManager(getCatalogId());
		try
		{
			for (Iterator iterator = inAll.iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				String path = getPathToXml(data.getSourcePath());
				//open the xml file. May reuse this file for other rows
				
				//TODO: Add Lock Manager so that two threads dont save on top of one another
				if( xml == null || !xml.getPath().equals(path))
				{
					if( xml != null)
					{
						getXmlArchive().saveXml(xml, null);
						lockManager.release(lock);
					}
					lock = lockManager.lock(path, "xmlDataArchive.saveAllData");
					xml = getXmlArchive().getXml(path, getElementName());
				}
				addRow(data, xml);
			}
			if( xml != null)
			{
				getXmlArchive().saveXml(xml, null);
			}
		}
		finally
		{
			if( lock != null)
			{
				lockManager.release(lock);
			}
		}

	}

}
