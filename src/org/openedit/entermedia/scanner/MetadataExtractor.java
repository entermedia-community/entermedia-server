package org.openedit.entermedia.scanner;

import java.io.File;

import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

public abstract class MetadataExtractor
{
	public abstract boolean extractData(MediaArchive inArchive, File inFile, Asset inAsset);
}
