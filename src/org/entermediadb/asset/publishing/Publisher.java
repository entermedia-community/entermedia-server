package org.entermediadb.asset.publishing;

import java.util.List;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.orders.Order;
import org.openedit.Data;

public interface Publisher
{
	//public void publish(MediaArchive mediaArchive,Data inOrder, Data inDestination, Asset asset);
	public PublishResult publish(MediaArchive mediaArchive,Order inOrder, Data inOrderItem,  Data inDestination, List inPresets, Asset inAsset);

	public PublishResult publish(MediaArchive mediaArchive,Order inOrder, Data inOrderItem,  Data inDestination, Data inPreset, Asset inAsset);
	
}
