package org.entermediadb.asset.sources;

import java.util.Collections;
import java.util.List;

import org.entermediadb.asset.Asset;

public class DefaultAssetSource extends OriginalsAssetSource
{

	@Override
	public List<String> importAssets(String inSubChangePath)
	{
		//Do nothing
		return Collections.EMPTY_LIST;
	}
	
	//This should never get called
	@Override
	public boolean handles(Asset inAsset)
	{
		return true;
	}
	
}
