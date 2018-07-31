package org.entermediadb.asset.sources;

import java.io.InputStream;
import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.repository.ContentItem;

public interface AssetSource
{
	InputStream getOriginalDocumentStream(Asset inAsset) throws OpenEditException;

	ContentItem getOriginalContent(Asset inAsset);

	boolean handles(Asset inAsset);

	boolean removeOriginal(Asset inAsset);

	void setConfig(Data inConfig);

	void setMediaArchive(MediaArchive inMediaArchive);

	Asset addNewAsset(Asset inAsset, List<ContentItem> inTemppages);

	Asset replaceOriginal(Asset inAsset, List<ContentItem> inTemppages);

	Asset assetAdded(Asset inAsset, ContentItem inContentItem);

	Data getConfig();

	void detach();

	void saveConfig();

	int importAssets(String inBasepath);

	public void checkForDeleted();	

}