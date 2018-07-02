package org.entermediadb.sitemonitor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.Data;

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
				fileNames = new ArrayList<String>();
			}
			fileNames.add("/");
			fileNames.add( archive.getRootDirectory().getAbsolutePath() );
			
			Collection<Data> all = archive.getSearcher("hotfolder").getAllHits();
			for (Iterator<Data> iterator = all.iterator(); iterator.hasNext();)
			{
				Data hotfolder = iterator.next();
				String path = hotfolder.get("externalpath");
				if( path != null)
				{
					fileNames.add(path);
				}
			}

			for (String file : fileNames)
			{
				File partition = new File(file);
				Long totalCapacity = (long) (partition.getTotalSpace() / SiteMonitorModule.MEGABYTE);
				Long freePartitionSpace = (long) (partition.getFreeSpace() / SiteMonitorModule.MEGABYTE);
				Long usablePartitionSpace = (long) (partition.getUsableSpace() / SiteMonitorModule.MEGABYTE);
				
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
