package users

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.profile.UserProfile

public void init()
{
		MediaArchive mediaArchive = context.getPageValue("mediaarchive");//Search for all files looking for videos
		
		Searcher searcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "userprofile");
		
		//Loop over every user profile and move the userid colum into the id column
		HitTracker profiles = searcher.getAllHits();
		profiles.enableBulkOperations();
		int ok = 0;
		profiles.each
		{
			Data hit =  it;
			UserProfile userprofile = (UserProfile) searcher.searchById(hit.id);
			if( userprofile != null )
			{
				userprofile.setCatalogId(mediaArchive.getCatalogId());
				//Check sourcepath
				String oldid = userprofile.get("userid");
				if( oldid != null)
				{
					searcher.delete(userprofile, null);
					userprofile.setSourcePath(oldid);
					userprofile.setId(oldid);
					userprofile.setProperty("userid", null);
					searcher.saveData(userprofile, context.getUser());
					log.info("moved " + oldid );
				}
				else
				{
					ok++;
				}
			}
		}
		log.info("verified  ${ok}");
		
}

init();