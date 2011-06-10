package org.openedit.entermedia.xmp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.ModuleManager;
import com.openedit.page.Page;

public class MetadataWriter
{
	private Log log = LogFactory.getLog(MetadataWriter.class);
	protected String fieldCatalogId;
	protected List<Asset> fieldAssetQueue;
	protected XmpWriter fieldXmpWriter;
	protected ModuleManager fieldModuleManager;
	
	synchronized public void addAssetForWriting(Asset inAsset)
	{
		if(getMediaArchive().isTagSync(inAsset.getFileFormat()))
		{
			getAssetQueue().add(inAsset);
		}
		else
		{
			log.info("Skipping keyword writing for asset " + inAsset.getName() + "(" + inAsset.getId() + ")");
		}
		
	}
	
	public List<Asset> getAssetQueue()
	{
		if(fieldAssetQueue == null)
		{
			fieldAssetQueue = new ArrayList<Asset>();
		}
		return fieldAssetQueue;
	}
	
	synchronized public void writeAssets()
	{
		ArrayList<Asset> done = new ArrayList<Asset>();
		MediaArchive archive = getMediaArchive();
		ArrayList<Asset> copy = new ArrayList<Asset>();
		copy.addAll( getAssetQueue() );
		for(Asset asset: copy)
		{
			List<String> keywords = asset.getKeywords();
			Page original = archive.getOriginalDocument(asset);
			try
			{
				if( getXmpWriter().saveMetadata(archive, asset) )
				{
					done.add(asset);
				}
				else
				{
					log.warn("Could not write metadata to file: " + original.getContentItem().getAbsolutePath());
				}
			}
			catch(Exception e)
			{
				log.warn("Could not write metadata to file: " + original.getContentItem().getAbsolutePath() + " (" + e.getMessage() + ")");
			}
			finally
			{
				getAssetQueue().remove(asset);
			}
		}
	}
	
	public MediaArchive getMediaArchive()
	{
		return (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String catalogId)
	{
		fieldCatalogId = catalogId;
	}

	public XmpWriter getXmpWriter()
	{
		return fieldXmpWriter;
	}

	public void setXmpWriter(XmpWriter xmpWriter)
	{
		fieldXmpWriter = xmpWriter;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager moduleManager)
	{
		fieldModuleManager = moduleManager;
	}
}
