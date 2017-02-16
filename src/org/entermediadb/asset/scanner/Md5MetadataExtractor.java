package org.entermediadb.asset.scanner;

import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.repository.ContentItem;
import org.openedit.util.FileUtils;


public class Md5MetadataExtractor extends MetadataExtractor
{
	private static final Log log = LogFactory.getLog(Md5MetadataExtractor.class);

	public boolean extractData(MediaArchive inArchive, ContentItem inFile, Asset inAsset)
	{
		InputStream in = inFile.getInputStream();
		try
		{
			//com.google.common.hash.Hashing;
			String catalogSettingValue = inArchive.getCatalogSettingValue("extractmd5");
			if( Boolean.parseBoolean(catalogSettingValue) )
			{
				//Can we do the first 20% of the file?
				
				String md5 = DigestUtils.md5Hex( in );
				inAsset.setValue("md5hex", md5);
				Searcher assetsearcher = inArchive.getAssetSearcher();
				HitTracker assets = assetsearcher.fieldSearch("md5hex", md5);
				if(assets.size() >0){
					inAsset.setValue("duplicate", true);
					for (Iterator iterator = assets.iterator(); iterator.hasNext();)
					{
						Data hit = (Data) iterator.next();
						Asset asset = (Asset) assetsearcher.loadData(hit);
						asset.setValue("duplicate", true);
						assetsearcher.saveData(asset, null);
					}
				}
			}
		}
		catch( Throwable ex)
		{
			throw new OpenEditException("Error on: " + inAsset.getSourcePath(),ex);
		}
		finally{
			FileUtils.safeClose(in);
		}
		return false;
		
	}

}
