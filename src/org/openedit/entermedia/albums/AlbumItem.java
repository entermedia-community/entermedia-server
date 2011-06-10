/*
 * Created on Jul 2, 2006
 */
package org.openedit.entermedia.albums;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.AssetArchive;

import com.openedit.comments.Comment;

public class AlbumItem implements Data
{
	protected Asset fieldAsset;
	protected List fieldComments;
	protected Map fieldProperties;
	protected String fieldCatalogId;
	protected AssetArchive fieldArchive;
	
	public Asset getAsset()
	{
		if( fieldAsset == null)
		{
			fieldAsset = getArchive().getAsset(getAssetId());
		}
		return fieldAsset;
	}

	public void setAsset(Asset inAsset)
	{
		fieldAsset = inAsset;
		setCatalogId(fieldAsset.getCatalogId());
		setAssetId(fieldAsset.getId());
	}

	public List getComments()
	{
		if (fieldComments == null)
		{
			fieldComments = new ArrayList();

		}

		return fieldComments;
	}

	public void setComments(List inComments)
	{
		fieldComments = inComments;
	}

	public void addComment(Comment comment)
	{
		getComments().add(comment);
	}

	public void removeComment(Comment comment)
	{
		getComments().remove(comment);
	}

	public void clearProperties()
	{
		fieldProperties = null;
	}

	public Map getProperties()
	{
		if (fieldProperties == null)
		{
			fieldProperties = ListOrderedMap.decorate(new HashMap());
		}
		return fieldProperties;
	}

	public String getProperty(String inKey)
	{
		String value = (String) getProperties().get(inKey);
		return value;
	}

	public void setProperties(Map inAttributes)
	{
		fieldProperties = inAttributes;
	}

	public void putAttribute(String inKey, String inValue)
	{
		putProperty(inKey, inValue);
	}

	public void putProperty(String inKey, String inValue)
	{
		if (inValue != null)
		{
			getProperties().put(inKey, inValue);
		}
		else
		{
			getProperties().remove(inKey);
		}
	}

	public void addProperty(String inKey, String inValue)
	{
		if (inValue == null || inValue.length() == 0)
		{
			getProperties().remove(inKey);
		}
		else
		{
			getProperties().put(inKey, inValue);
		}
	}

	public void removeProperty(String inKey)
	{
		if (inKey != null && inKey.length() > 0)
		{
			getProperties().remove(inKey);
		}
	}

	public void setWidth(int inWidth)
	{
		addProperty("width", String.valueOf(inWidth));

	}

	public void setWatermark(boolean inB)
	{
		addProperty("watermark", String.valueOf(inB));

	}

	public String getWidth()
	{
		return getProperty("width");
	}

	public boolean isWatermark()
	{
		return Boolean.parseBoolean(getProperty("watermark"));
	}

	public String toString()
	{
		Asset asset = getAsset();
		if(asset == null)
		{
			return "Asset has been removed";
		}
		return asset.getName();

	}

	public AssetArchive getArchive()
	{
		return fieldArchive;
	}

	public void setArchive(AssetArchive inArchive)
	{
		fieldArchive = inArchive;
	}

	public String get(String inId)
	{
		if( "catalogid".equals(inId) )
		{
			return getCatalogId();
		}
		if( getProperty(inId) != null)
		{
			return getProperty(inId);
		}
		Asset asset = getAsset();
		if(asset == null)
		{
			return null;
		}
		return asset.get(inId);
	}

	public String getId()
	{
		if (fieldAsset != null && getAlbumId() != null)
		{
			return get("albumid") + "_" + fieldAsset.getCatalogId() + "_" + fieldAsset.getId();
		}
		return null;
	}

	public String getName()
	{
		Asset asset = getAsset();
		if(asset == null)
		{
			return null;
		}
		return asset.getName();
	}

	public String getSourcePath()
	{
		Asset asset = getAsset();
		if(asset == null)
		{
			return null;
		}
		return asset.getSourcePath();
	}

	public void setName(String inName)
	{
		getAsset().setName(inName);
		
	}

	public void setProperty(String inId, String inValue)
	{
		getProperties().put(inId, inValue);
	}

	public void setSourcePath(String inSourcepath)
	{
		
	}

	public String getCatalogId()
	{
		if (fieldAsset != null)
		{
			return getAsset().getCatalogId();
		}
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public void setId(String inNewid)
	{
		
	}

	public String getAssetId() {
		return get("assetid");
	}

	public void setAssetId(String inAssetId) {
		setProperty("assetid", inAssetId);
	}

	public String getAlbumId() {
		return get("albumid");
	}

	public void setAlbumId(String inAlbumId) {
		setProperty("albumid", inAlbumId);
	}
}
