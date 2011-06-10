package org.openedit.entermedia.fetch;

import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.users.User;

public interface UrlMetadataImporter
{
	//gets the metadata save to this asset. Does not download or save anything
	Asset importFromUrl(MediaArchive inArchive, String inUrl, User inUser);

	//Download the original media it needs
	void fetchMediaForAsset(MediaArchive inArchive, Asset inAsset, User inUser);
}
