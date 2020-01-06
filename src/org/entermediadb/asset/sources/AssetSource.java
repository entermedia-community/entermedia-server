package org.entermediadb.asset.sources;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;

public interface AssetSource
{
	InputStream getOriginalDocumentStream(Asset inAsset) throws OpenEditException;

	ContentItem getOriginalContent(Asset inAsset);

	boolean handles(Asset inAsset);

	boolean removeOriginal(Asset inAsset);

	void setConfig(MultiValued inConfig);

	void setMediaArchive(MediaArchive inMediaArchive);

	Asset addNewAsset(Asset inAsset, List<ContentItem> inTemppages);
	//public Asset createAsset(final String currentcollection, final boolean createCategories, final Map metadata, final Map pages, final User user);
	Asset createAsset(Asset inAsset, ContentItem inUploaded, Map inMetadata, String inSourcepath, boolean inCreateCategories, User inUser);	

	Asset replaceOriginal(Asset inAsset, List<ContentItem> inTemppages);

	Asset assetOrginalSaved(Asset inAsset);

	Data getConfig();

	void detach();

	void saveConfig();

	int importAssets(String inBasepath);

	public void checkForDeleted();

	void assetUploaded(Asset inAsset);

	String getName();

	boolean isEnabled();

	boolean isHotFolder();

	String getId();


}