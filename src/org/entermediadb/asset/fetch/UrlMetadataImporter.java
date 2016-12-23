package org.entermediadb.asset.fetch;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.users.User;

public interface UrlMetadataImporter
{
	//gets the metadata save to this asset. Does not download or save anything
	Asset importFromUrl(MediaArchive inArchive, String inUrl, User inUser, String inSourcePath, String inFileName, String inId);

	//Download the original media it needs
	void fetchMediaForAsset(MediaArchive inArchive, Asset inAsset, User inUser);
}
