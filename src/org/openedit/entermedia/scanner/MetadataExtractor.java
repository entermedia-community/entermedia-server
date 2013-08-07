package org.openedit.entermedia.scanner;

import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.repository.ContentItem;

public abstract class MetadataExtractor
{
	public abstract boolean extractData(MediaArchive inArchive, ContentItem inFile, Asset inAsset);
}
