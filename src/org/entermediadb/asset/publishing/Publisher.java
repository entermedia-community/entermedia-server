package org.entermediadb.asset.publishing;

import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;

public interface Publisher
{
	//public void publish(MediaArchive mediaArchive,Data inOrder, Data inDestination, Asset asset);
	public PublishResult publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest,  Data inDestination, List inPresets);

	public PublishResult publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest,  Data inDestination, Data inPreset);
	
}
