package org.entermediadb.asset.scanner;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.OpenEditException;
import org.openedit.repository.ContentItem;

public class Md5MetadataExtractor extends MetadataExtractor
{
	private static final Log log = LogFactory.getLog(Md5MetadataExtractor.class);

	public boolean extractData(MediaArchive inArchive, ContentItem inFile, Asset inAsset)
	{
		try
		{
			String catalogSettingValue = inArchive.getCatalogSettingValue("extractmd5");
			if( Boolean.parseBoolean(catalogSettingValue) )
			{
				String md5 = DigestUtils.md5Hex( inFile.getInputStream() );
				inAsset.setValue("md5hex", md5);
			}	
		}
		catch( Throwable ex)
		{
			throw new OpenEditException("Could not read in " + inAsset.getSourcePath(),ex);
		}
		return false;
		
	}

}
