package org.entermediadb.asset.convert;

public interface ConversionEventListener
{

	public void finishedConversions(AssetConversions inAssetconversions);

	public void ranConversions(AssetConversions assetConversions);
}
