package org.entermediadb.asset.scanner;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.repository.ContentItem;

public abstract class MetadataExtractor
{
	public abstract boolean extractData(MediaArchive inArchive, ContentItem inFile, Asset inAsset);
}
