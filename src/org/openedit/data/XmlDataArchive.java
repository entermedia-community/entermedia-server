package org.openedit.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.entermedia.cache.CacheManager;
import org.openedit.Data;
import org.openedit.xml.ElementData;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.OpenEditException;
import com.openedit.users.User;

public class XmlDataArchive implements DataArchive 
{
	protected XmlArchive fieldXmlArchive;

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
			element.setAttributes(new ArrayList());
			populateElement(element, inData);
		}
	}
	public void saveData(Data inData, User inUser)
	{
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
		XmlFile xml = getXmlArchive().getXml(path, getElementName());
		addRow(inData, xml);
		getXmlArchive().saveXml(xml, null);
	}
	
	//This is optimized for ordered data
	public void saveAllData(Collection inAll, User inUser)
	{
		XmlFile xml = null;//
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
				}
				xml = getXmlArchive().getXml(path, getElementName());
			}
			addRow(data, xml);
		}
		if( xml != null)
		{
			getXmlArchive().saveXml(xml, null);
		}
	}

	//Not recommeneded, use populateElementData
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
	
	public Data loadData(DataFactory inFactory, String inSourcePath, String inId)
	{
		//This is used a bunch when loading and editing the same xml file
		String path = getPathToXml(inSourcePath);

		XmlFile xml = getXmlArchive().getXml(path, getElementName());
		Element elem = xml.getElementById(inId);
		if(elem == null)
		{
			return null;
		}
		ElementData data = (ElementData)inFactory.createNewData();
		data.setElement(elem);
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
		XmlFile xml = getXmlArchive().getXml(path, getElementName());
		Element element = xml.getElementById(inData.getId());
		if( element != null )
		{
			xml.deleteElement(element);
		}
		getXmlArchive().saveXml(xml, inUser);
	}
//	public XmlFile getXml(String inPath, String inSearchType)
//	{
//		return getXmlArchive().getXml(inPath,inSearchType);
//	}

}
