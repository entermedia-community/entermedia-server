package org.entermediadb.asset.push;

import org.entermediadb.asset.*;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.users.User;

public interface MediaUploader
{

	public boolean uploadOriginal(MediaArchive archive, Asset inAsset, Data inPublishDestination, User inUser );	
	public boolean uploadGenerated(MediaArchive archive, Asset inAsset, Data inPublishDestination, User inUser );
	

		
}
