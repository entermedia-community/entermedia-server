package org.entermediadb.asset.scanner;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.repository.ContentItem;

public abstract class MetadataExtractor
{
	private static final Log log = LogFactory.getLog(MetadataExtractor.class);

	public abstract boolean extractData(MediaArchive inArchive, ContentItem inFile, Asset inAsset);

	public boolean extractAll(MediaArchive inMediaArchive, Collection<ContentItem> inContentitems, Collection<Asset> inAssets)
	{
		boolean foundone = false;
		Iterator<ContentItem> next = inContentitems.iterator();
		for(Asset inAsset:inAssets)
		{
			ContentItem inputFile = next.next();
			if( !inputFile.exists() )
			{
				log.info("Original asset missing " + inAsset.getSourcePath());
				continue;
			}
			if( extractData(inMediaArchive, inputFile, inAsset) )
			{
				foundone = true;
			}
		}
		return foundone;
	}
}
