package org.entermediadb.asset.sources;

import org.entermediadb.asset.Asset;

public class DefaultAssetSource extends OriginalsAssetSource
{

	@Override
	public int importAssets(String inSubChangePath)
	{
		//Do nothing
		return -1;
	}
	
	//This should never get called
	@Override
	public boolean handles(Asset inAsset)
	{
		return true;
	}
	
}
