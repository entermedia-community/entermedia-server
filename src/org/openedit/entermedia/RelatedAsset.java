package org.openedit.entermedia;

import java.util.HashMap;
import java.util.Map;

import org.openedit.Data;

public class RelatedAsset implements Data
{
	protected Map fieldProperties;

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

	public Map getProperties()
	{
		if (fieldProperties == null)
		{
			fieldProperties = new HashMap(3);
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
	public void setProperties(Map<String,String> inProperties)
	{
		getProperties().putAll(inProperties);
	}

}
