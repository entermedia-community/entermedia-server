package org.entermediadb.sitemanager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.entermediadb.asset.MediaArchive;
import org.openedit.CatalogEnabled;
import org.openedit.OpenEditException;

public class DiskManager implements CatalogEnabled
{
	protected String fieldCatalogId;

	public List<DiskPartition> getPartitionsStats(MediaArchive archive)
	{
		List<DiskPartition> partitions = new ArrayList<DiskPartition>();

		try
		{
			String filePath = archive.getCatalogSettingValue("diskpartitions");

			if (filePath == null || (filePath != null && filePath.isEmpty()))
			{
				throw new OpenEditException("No partitions declared in catalogsettings");
			}

			//check for duplicates
			String[] fileNames = filePath.split(",");


			for (String file : fileNames)
			{
				//create new JSON and put it in a GET route
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

//	private void toJSON(JSONObject inJsonObject, String partition, Long totalCapacity, Long freePartitionSpace, Long usablePartitionSpace)
//	{
//		JSONArray partitions = null;
//
//		if (inJsonObject == null)
//		{
//			throw new OpenEditException("JSON Object not initialized");
//		}
//		partitions = (JSONArray) inJsonObject.get("partitions");
//
//		JSONObject obj = new JSONObject();
//		obj.put("totalcapacity", totalCapacity);
//		obj.put("freepartitionspace", freePartitionSpace);
//		obj.put("usablepartitionspace", usablePartitionSpace);
//		obj.put("name", partition);
//		partitions.add(obj);
//	}
//
//	private void writeToFile(JSONObject inJsonObject)
//	{
//	    BufferedWriter writer;
//		try
//		{
//			writer = new BufferedWriter(new FileWriter("/root/disks.json"));
//		    writer.write(inJsonObject.toJSONString());
//		     
//		    writer.close();
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//	}

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
