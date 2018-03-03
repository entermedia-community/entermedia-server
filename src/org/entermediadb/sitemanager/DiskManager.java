package org.entermediadb.sitemanager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.OpenEditException;

public class DiskManager implements CatalogEnabled
{
	protected String fieldCatalogId;

	public List<DiskPartition> getPartitionsStats(MediaArchive archive)
	{
		List<DiskPartition> partitions = new ArrayList<DiskPartition>();

		try
		{
			Collection<String> fileNames = archive.getCatalogSettingValues("diskpartitions");
			if( fileNames == null )
			{
				fileNames = new ArrayList();
			}
			fileNames.add("/");
			fileNames.add( archive.getRootDirectory().getAbsolutePath() );
			
			Collection all = archive.getSearcher("hotfolder").getAllHits();
			for (Iterator iterator = all.iterator(); iterator.hasNext();)
			{
				Data hotfolder = (Data) iterator.next();
				String path = hotfolder.get("externalpath");
				if( path != null)
				{
					fileNames.add(path);
				}
			}

			for (String file : fileNames)
			{
				File partition = new File(file);
				Long totalCapacity = (long) (partition.getTotalSpace() / 1000000.00);
				Long freePartitionSpace = (long) (partition.getFreeSpace() / 1000000.00);
				Long usablePartitionSpace = (long) (partition.getUsableSpace() / 1000000.00);
				
				partitions.add(new DiskPartition(file, totalCapacity, freePartitionSpace, usablePartitionSpace));
			}
			return partitions;
	
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return new ArrayList<DiskPartition>();
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	@Override
	public void setCatalogId(String inId)
	{
		this.fieldCatalogId = inId;
	}


}
