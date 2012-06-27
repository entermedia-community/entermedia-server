package org.openedit.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.xml.ElementData;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.amazonaws.services.simpleemail.model.GetSendQuotaRequest;
import com.openedit.OpenEditException;
import com.openedit.users.User;

public class XmlDataArchive implements DataArchive
{
	protected XmlArchive fieldXmlArchive;
	protected Map fieldIdCache;
	
	protected Map getIdCache()
	{
		if( fieldIdCache == null)
		{
			fieldIdCache = new HashMap();
		}
		return fieldIdCache;
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
		XmlFile xml = getXmlArchive().getXml(path, getElementName());
		addRow(inData, xml);
		getXmlArchive().saveXml(xml, null);
	}
	public void saveAllData(Collection inAll, User inUser)
	{
		XmlFile xml = null;//
		for (Iterator iterator = inAll.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			String path = getPathToXml(data.getSourcePath());
			//open the xml file. May reuse this file for other rows
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


	protected void populateElement(Element inElement, Data inData)
	{
		for (Iterator iterator = inData.getProperties().keySet().iterator(); iterator.hasNext();)
		{
			String detail = (String) iterator.next();
			if( !detail.equals("id") && !detail.equals("sourcepath") && !detail.startsWith("_"))
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
		boolean foundname = false;
		for(Iterator iterator = attributes.iterator(); iterator.hasNext();)
		{
			Attribute attr = (Attribute)iterator.next();
			String id = attr.getName();
			if(!id.equals("id") && !id.startsWith("_"))
			{
				if( id.equals("name"))
				{
					foundname = true;
				}
				inElement.addAttribute(attr.getName(), attr.getValue());
			}
		}
		if( !foundname && inData.getName() != null)
		{
			inElement.setText(inData.getName());
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
	
	public Data loadData(DataFactory inFactory, String inSourcePath, String inId)
	{
		String path = getPathToXml(inSourcePath);
		XmlFile xml = getXmlArchive().getXml(path, getElementName());
		String id = path + inId + xml.getLastModified();
		Data found = (Data)getIdCache().get(id);
		if( found != null)
		{
			return found;
		}

		Element elem = xml.getElementById(inId);
		if(elem == null)
		{
			return null;
		}
		ElementData data = (ElementData)inFactory.createNewData();
		data.setElement(elem);
		data.setSourcePath(inSourcePath);
		if( getIdCache().size() > 100)
		{
			getIdCache().clear();
		}
		getIdCache().put(id,data);
		return data;
	}
	public void clearCache()
	{
		getIdCache().clear();
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
	public XmlFile getXml(String inPath, String inSearchType)
	{
		return getXmlArchive().getXml(inPath,inSearchType);
	}

}
