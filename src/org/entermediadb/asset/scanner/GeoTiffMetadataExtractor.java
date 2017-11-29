package org.entermediadb.asset.scanner;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetails;
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;
import org.openedit.util.PathUtilities;


public class GeoTiffMetadataExtractor extends MetadataExtractor
{
	private static final Log log = LogFactory.getLog(GeoTiffMetadataExtractor.class);
	protected Exec fieldExec;

	public Exec getExec() {
		return fieldExec;
	}

	public void setExec(Exec fieldExec) {
		this.fieldExec = fieldExec;
	}

	public boolean extractData(MediaArchive inArchive, ContentItem inFile, Asset inAsset)
	{
		try
		{
			//com.google.common.hash.Hashing;
			String catalogSettingValue = inArchive.getCatalogSettingValue("extractgeotiff");
			if( Boolean.parseBoolean(catalogSettingValue) )
			{
			
				String type = PathUtilities.extractPageType(inFile.getPath());
				if (type == null || "data".equals(type.toLowerCase()))
				{
					type = inAsset.get("fileformat");
				}
				if (!("tiff".equalsIgnoreCase(type) || "tif".equalsIgnoreCase(type)))
				{
				return false;
				}
				
				PropertyDetails details = inArchive.getAssetPropertyDetails();
				ArrayList<String> base = new ArrayList<String>();

				
				
				base.add(inFile.getAbsolutePath());
				ArrayList<String> comm = new ArrayList(base);
				comm.add("-n");
				
				//--
				long start = System.currentTimeMillis();
				//log.info("Runnning identify");
				//--
				ExecResult result = getExec().runExec("gdalinfo", comm, true);
				//--
				long end = System.currentTimeMillis();
				double total = (end - start) / 1000.0;
				log.info("Exiftool Done in:"+total);
				
				
				
				String data = result.getStandardOut();
				
				inAsset.setValue("geotiff", data);
				
			
				return true;
			}
		}
		catch( Throwable ex)
		{
			throw new OpenEditException("Error on: " + inAsset.getSourcePath(),ex);
		}
		
		return false;
		
	}

}
