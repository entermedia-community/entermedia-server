package org.openedit.entermedia.publishing;

import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

public interface Publisher
{
	public void publish(MediaArchive mediaArchive,Data inOrder, Data inDestination, Asset asset);

	public void publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest,  Data inDestination, Data inPreset);
	
}
