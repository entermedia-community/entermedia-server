package org.entermediadb.modules.notification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.openedit.Data;
import org.openedit.MultiValued;

public class UserMetadata
{
	String fieldUserId;

	public String getUserId()
	{
		return fieldUserId;
	}

	public void setUserId(String inUserId)
	{
		fieldUserId = inUserId;
	}

	Map fieldAssetMetadata = new HashMap();
	Map fieldAssets = new HashMap();

	public Map getAssetMetadata()
	{
		return fieldAssetMetadata;
	}

	public void setAssetMetadata(Map inAssetMetadata)
	{
		fieldAssetMetadata = inAssetMetadata;
	}

	public Collection getAssets()
	{
		return fieldAssets.values();
	}

	public void addAssetMetadata(Asset inAsset, Data inMetadata)
	{
		Asset existing = (Asset) fieldAssets.get(inAsset.getId());
		if (existing == null)
		{
			existing = inAsset;
			fieldAssets.put(inAsset.getId(), inAsset);
		}
		Collection existingmetadatas = (Collection) fieldAssetMetadata.get(existing.getId());
		if (existingmetadatas == null)
		{
			existingmetadatas = new ArrayList();
			fieldAssetMetadata.put(inAsset.getId(), existingmetadatas);
		}
		for (Iterator iterator = existingmetadatas.iterator(); iterator.hasNext();)
		{
			Data message = (Data) iterator.next();
			if (message.getId().equals(inMetadata.getId()))
			{
				return;
			}
		}
		existingmetadatas.add(inMetadata);
	}

	public Collection getMetadatas(Data inAsset)
	{
		return getMetadatas(inAsset.getId());
	}

	public Collection getMetadatas(String inAssetId)
	{
		Collection existingmetadatas = (Collection) fieldAssetMetadata.get(inAssetId);
		existingmetadatas = sort(existingmetadatas);
		return existingmetadatas;
	}

	protected Collection sort(Collection messages)
	{
		ArrayList sorted = new ArrayList(messages);
		sorted.sort(new Comparator<MultiValued>()
		{
			@Override
			public int compare(MultiValued inO1, MultiValued inO2)
			{
				//sort by date
				Date date1 = (Date)inO1.getDate("date");
				Date date2 = (Date)inO2.getDate("date");
				if( date1 != null && date2 != null)
				{
					return date1.compareTo(date2);
				}
				return 0;
			}
		});
		return sorted;
	}
}
