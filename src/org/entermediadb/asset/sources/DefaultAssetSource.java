package org.entermediadb.asset.sources;

import org.entermediadb.asset.Asset;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;

public class DefaultAssetSource extends OriginalsAssetSource
{

	@Override
	public int importAssets(String inSubChangePath)
	{
		//Do nothing
		return -1;
	}
	
	@Override
	public MultiValued getConfig()
	{
		if( fieldConfig == null)
		{
			fieldConfig = new BaseData();
			fieldConfig.setId(getClass().getName());
		}
		return fieldConfig;
	}
	//This should never get called
	@Override
	public boolean handles(Asset inAsset)
	{
		return true;
	}
	
}
