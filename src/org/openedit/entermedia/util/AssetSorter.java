package org.openedit.entermedia.util;

import java.util.Comparator;
import java.util.List;

import org.openedit.Data;

public class AssetSorter implements Comparator<Data>
{

	List<String> fieldAssetIds;

	public AssetSorter()
	{
		super();
	}

	public AssetSorter(List<String> fieldAssetIds)
	{
		this.fieldAssetIds = fieldAssetIds;
	}

	public int compare(Data o1, Data o2) {
		int index1 = fieldAssetIds.indexOf(o1.getId());
		int index2 = fieldAssetIds.indexOf(o2.getId());
		return index1==index2?0:index1<index2?-1:1;
	}

	public List<String> getAssetIds()
	{
		return fieldAssetIds;
	}

	public void setFieldAssetIds(List<String> fieldAssetIds)
	{
		this.fieldAssetIds = fieldAssetIds;
	}

}
