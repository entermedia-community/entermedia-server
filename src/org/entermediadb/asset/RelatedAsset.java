package org.entermediadb.asset;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.openedit.Data;
import org.openedit.data.ValuesMap;
/**
 * What is this class used for?
 *
 */
public class RelatedAsset implements Data
{
	protected ValuesMap fieldProperties;
	public String getName(String inLocale) {
		return getName();
	}
	
	public Collection getObjects(String inField)
	{
		Collection values = (Collection)getValue(inField);
		return values;
	}
	
	
	public String getType()
	{
		return get("type");
	}

	public void setType(String inType)
	{
		setProperty("type", inType);
	}

	public void setAssetId(String inAssetId)
	{
		setProperty("assetid", inAssetId);
	}

	public String getAssetId()
	{
		return get("assetid");
	}

	public boolean equals(Object inObject)
	{
		if (inObject instanceof RelatedAsset)
		{
			RelatedAsset p = (RelatedAsset) inObject;

			if (getAssetId() != null && getAssetId().equals(p.getAssetId()))
			{
				if (getType().equals(p.getType()))
				{
					if (getRelatedToCatalogId().equals(p.getRelatedToCatalogId()))
					{
						return getRelatedToAssetId().equals(p.getRelatedToAssetId());
					}
				}
			}
		}
		return false;
	}

	public String getRelatedToAssetId()
	{
		return get("relatedtoassetid");
	}

	public void setRelatedToAssetId(String inRelatedToAssetId)
	{
		setProperty("relatedtoassetid", inRelatedToAssetId);
	}

	public String get(String inId)
	{
		return (String) getProperties().get(inId);
	}

	public String getId()
	{
		return get("id");
	}

	public String getName()
	{
		return getType();
	}

	public void setId(String inNewid)
	{
		setProperty("id", inNewid);
	}
	
	public void setName(String inName)
	{
		setType(inName);
	}

	public void setProperty(String inId, String inValue)
	{
		getProperties().put(inId, inValue);
	}

	public ValuesMap getProperties()
	{
		if (fieldProperties == null)
		{
			fieldProperties = new ValuesMap();
		}
		return fieldProperties;
	}

	public String toString()
	{
		return getAssetId() + " related to " + getRelatedToAssetId() + "(" + getType() + ")";
	}

	public void setRelatedToCatalogId(String inId)
	{
		setProperty("relatedtocatalogid", inId);

	}

	public String getRelatedToCatalogId()
	{
		return get("relatedtocatalogid");

	}

	public String getSourcePath()
	{
		return get("sourcepath");
	}

	public void setSourcePath(String inSourcepath)
	{
		setProperty("sourcepath", inSourcepath);
	}
	public void setProperties(Map inProperties)
	{
		getProperties().putAll(inProperties);
	}
	
	public void setValues(String inKey, Collection<String> inValues)
	{
		getProperties().put(inKey, inValues);

	}

	@Override
	public Object getValue(String inKey)
	{
		return getProperties().getObject(inKey);
	}
	@Override
	public void setValue(String inKey, Object inValue)
	{
		getProperties().put(inKey, inValue);
	}
	public String get(String inKey, String inLocale)
	{
		// TODO Auto-generated method stub
		return get(inKey + "." + inLocale);
	}
	
	@Override
	public Set keySet()
	{
		return getProperties().keySet();
	}

}

