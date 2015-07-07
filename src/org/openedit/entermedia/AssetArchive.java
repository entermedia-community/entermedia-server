/*
 * Created on Apr 23, 2004
 */
package org.openedit.entermedia;

import java.util.Collection;

import org.entermedia.locks.Lock;
import org.openedit.Data;
import org.openedit.data.DataArchive;
import org.openedit.xml.XmlArchive;

import com.openedit.users.User;

/**
 * @author cburkey
 * 
 */
public interface AssetArchive extends DataArchive
{

	void setXmlArchive(XmlArchive inXmlArchive);

	void setDataFileName(String inDataFileName);

	void setElementName(String inSearchType);

	void setPathToData(String inPathToData);

	XmlArchive getXmlArchive();

	void delete(Data inData, User inUser);
	
	public Asset getAssetBySourcePath(String inSourcePath, boolean inAutoCreate);
	public Asset getAssetBySourcePath(String inSourcePath);

	/**
	 * Clears the assets cache. Forces loading assets from persistent storage next time.
	 */
	void clearAssets();
	public void clearAsset(Asset inAsset);
	
	void saveAsset(Asset inAsset, User inUser);
	void saveAsset(Asset inAsset);
	
	void deleteAsset(Asset inItem);
			
	public String buildXmlPath(Asset inAsset);

}
