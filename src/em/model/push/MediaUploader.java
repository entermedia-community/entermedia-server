package em.model.push;

import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.users.User;

public interface MediaUploader
{

	public boolean uploadOriginal(MediaArchive archive, Asset inAsset, Data inPublishDestination, User inUser );	
	public boolean uploadGenerated(MediaArchive archive, Asset inAsset, Data inPublishDestination, User inUser );
	

		
}
