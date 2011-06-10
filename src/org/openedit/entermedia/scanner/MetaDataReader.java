package org.openedit.entermedia.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.util.DateStorageUtil;

public class MetaDataReader
{
	private static final Log log = LogFactory.getLog(MetaDataReader.class);
	protected List fieldMetadataExtractors;

	public void populateAsset(MediaArchive inArchive, File inputFile, Asset inAsset)
	{
		try
		{
//			GregorianCalendar cal = new GregorianCalendar();
//			cal.setTimeInMillis(inputFile.lastModified());
//			cal.set(Calendar.MILLISECOND, 0);
			// Asset Modification Date">2005-03-04 08:28:57

			String now =  DateStorageUtil.getStorageUtil().formatForStorage(new Date(inputFile.lastModified()));
			inAsset.setProperty("assetmodificationdate",now);
			// inAsset.setProperty("recordmodificationdate", format.format(
			// new Date() ) );
			inAsset.setProperty("filesize", String.valueOf(inputFile.length()));
			if (inAsset.getName() == null)
			{
				inAsset.setName(inputFile.getName());
			}
			long start = System.currentTimeMillis();
			for (Iterator iterator = getMetadataExtractors().iterator(); iterator.hasNext();)
			{
				MetadataExtractor extrac = (MetadataExtractor) iterator.next();
				extrac.extractData(inArchive, inputFile, inAsset);
			}
			log.info("Got metadata in " + (System.currentTimeMillis() - start) + " mili seconds.");

		}
		catch (Exception e)
		{
			log.error(e);
		}
	}

	public List getMetadataExtractors()
	{
		if (fieldMetadataExtractors == null)
		{
			fieldMetadataExtractors = new ArrayList();
		}

		return fieldMetadataExtractors;
	}

	public void setMetadataExtractors(List inMetadataExtractors)
	{
		fieldMetadataExtractors = inMetadataExtractors;
	}
}
