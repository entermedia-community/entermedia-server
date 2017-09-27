package org.entermediadb.asset.scanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities;

public class MetaDataReader
{
	private static final Log log = LogFactory.getLog(MetaDataReader.class);
	protected List fieldMetadataExtractors;

	public void updateAsset(MediaArchive archive, ContentItem itemFile, Asset target)
	{
		target.setValue("pages",1);
		target.setValue("fileformat",null);
		PropertyDetails details = archive.getAssetSearcher().getPropertyDetails();
		HashMap<String, String> externaldetails = new HashMap<String, String>();
		for(Iterator i = details.iterator(); i.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) i.next();
			if(detail.getExternalId() != null)
			{
				externaldetails.put(detail.getId(), target.get(detail.getId()));
				target.setProperty(detail.getId(), null);
			}
		}
		
		populateAsset(archive, itemFile, target);
		
		for(String detail: externaldetails.keySet())
		{
			if(target.get(detail) == null)
			{
				target.setProperty(detail, externaldetails.get(detail));
			}
		}
	}
	public void populateAsset(MediaArchive inArchive, ContentItem inputFile, Asset inAsset)
	{
		try
		{
			//Make sure this is not a getStub so that S3 can cache it
			//make sure it is fully loaded
			if( inputFile.isStub() )
			{
				inputFile = getPageManager().getRepository().get(inputFile.getPath());
			}	
//			GregorianCalendar cal = new GregorianCalendar();
//			cal.setTimeInMillis(inputFile.lastModified());
//			cal.set(Calendar.MILLISECOND, 0);
			// Asset Modification Date">2005-03-04 08:28:57

			
			String now =  DateStorageUtil.getStorageUtil().formatForStorage(inputFile.lastModified());
			inAsset.setProperty("assetmodificationdate",now);
			// inAsset.setProperty("recordmodificationdate", format.format(
			// new Date() ) );
			inAsset.setProperty("filesize", String.valueOf(inputFile.getLength()));
			inAsset.setName(inputFile.getName());
			String ext = PathUtilities.extractPageType(inputFile.getName());
			if (ext != null)
			{
				ext = ext.toLowerCase();
			}
			inAsset.setProperty("fileformat", ext);
			if( !inputFile.exists() )
			{
				log.info("Original asset missing " + inAsset.getSourcePath());
				return;
			}

			long start = System.currentTimeMillis();
			boolean foundone = false;
			for (Iterator iterator = getMetadataExtractors().iterator(); iterator.hasNext();)
			{
				MetadataExtractor extrac = (MetadataExtractor) iterator.next();
				if( extrac.extractData(inArchive, inputFile, inAsset) )
				{
					foundone = true;
				}
			}
			if( foundone )
			{
				long end = System.currentTimeMillis();
				if( log.isDebugEnabled() )
				{
					log.debug("Got metadata in " + (end - start) + " mili seconds.");
				}
			}
		}
		catch (Exception e)
		{
			log.error("Could not read metadata", e);
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
	
	protected PageManager fieldPageManager;
	
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	
}
