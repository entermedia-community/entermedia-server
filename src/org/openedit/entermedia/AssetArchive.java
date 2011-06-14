/*
 * Created on Apr 23, 2004
 */
package org.openedit.entermedia;

import org.openedit.data.DataArchive;

import com.openedit.users.User;

/**
 * @author cburkey
 * 
 */
public interface AssetArchive extends DataArchive
{

	public Asset getAssetBySourcePath(String inSourcePath, boolean inAutoCreate);
	public Asset getAssetBySourcePath(String inSourcePath);
	public Asset getAsset(String inId);

	/**
	 * Clears the assets cache. Forces loading assets from persistent storage next time.
	 */
	void clearAssets();
	public void clearAsset(Asset inAsset);
	
	void saveAsset(Asset inAsset, User inUser);
	void saveAsset(Asset inAsset);
	
	void deleteAsset(Asset inItem);
	
	String nextAssetNumber();
	
	void setCatalogId(String inId);
	String getCatalogId();
	
	public String buildXmlPath(Asset inAsset);

}
