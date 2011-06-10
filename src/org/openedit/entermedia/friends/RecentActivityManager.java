package org.openedit.entermedia.friends;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.EnterMedia;
import org.openedit.entermedia.albums.Album;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.ListHitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;
import com.openedit.users.UserManager;

public class RecentActivityManager
{
	protected UserManager fieldUserManager;
	
	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	/**
	 * This should cache the results if possible
	 * @param inEnterMedia
	 * @param inUser
	 * @param inReq
	 * @return
	 */
	public HitTracker getActivityForUser(EnterMedia inEnterMedia, User inUser, WebPageRequest inReq)
	{
		Searcher logsearcher = inEnterMedia.getSearcherManager().getSearcher(inEnterMedia.getApplicationId(), "recentactivityLog");
		SearchQuery query = logsearcher.createSearchQuery();
		//	we dont really need only recent stuff. Just need to cut off the results after 20 hits
		//	Calendar cal = Calendar.getInstance();
		//	cal.add(Calendar.DATE, -7);
		//	query.addAfter("date", cal.getTime());

		HitTracker hits = inEnterMedia.getFriendManager().getFriends(inUser.getUserName());
		for (Object friend : hits)
		{
			query.addMatches("user", hits.getValue(friend, "friendid"));
		}
		query.addMatches("user", inUser.getUserName());
		query.addSortBy("dateDown");
		HitTracker loghits = logsearcher.cachedSearch(inReq, query);
		if( loghits == null)
		{
			return null;
		}
		HashMap<String, String> dups = new HashMap<String, String>();
		HitTracker result = new ListHitTracker();
		for (Iterator iterator = loghits.iterator(); iterator.hasNext();)
		{
			Data activity = (Data) iterator.next();
			String operation = activity.get("operation");
			if(operation == null)
			{
				continue;
			}
			if (operation.equals("userlikes") || operation.equals("assetsuploaded") || operation.equals("assetsimported"))
			{
				addAssetEvent(dups, result, activity, operation, loghits, inEnterMedia);
			}
			else if (operation.equals("albumcommentadded"))
			{
				addAlbumEvent(dups, result, activity, operation, loghits,  inEnterMedia);
			}
			else if (operation.equals("assetcommentadded"))
			{
				addAssetEvent(dups, result, activity, operation, loghits, inEnterMedia);
			}
			else if (operation.equals("nowfriends"))
			{
				addFriendEvent(dups, result, activity, operation, loghits,  inEnterMedia);
			}
			//		if( operation.equals("albumcreated"))
			//		{
			//			addEvent(dups, result, activity, operation);
			//		}
			//		
			if (result.size() > 20)
			{
				break;
			}
		}
		return result;
	}

	private void addAlbumEvent(HashMap<String, String> dups, HitTracker result, Data activity, String operation, HitTracker inHits, EnterMedia inEnterMedia)
	{
		//just want the most recent operation for this sourcepath
		String path = activity.get("albumid");
		if (path == null)
		{
			return;
		}
		//this is slow... Need a nicer way to get this info
		if (dups.containsKey(path))
		{
			return;
		}
		result.add(createAlbumActivity(activity, inHits,  inEnterMedia));
		dups.put(path, operation);
	}

	private void addAssetEvent(HashMap<String, String> dups, HitTracker result, Data activity, String operation, HitTracker inHits, EnterMedia inEnterMedia)
	{
		//just want the most recent operation for this sourcepath
		String path =  activity.getSourcePath() + operation;
		if( path == null)
		{
			return;
		}
		//this is slow... Need a nicer way to get this info
		if( dups.containsKey(path))
		{
			return;
		}
		result.add(createAssetActivity(activity, inHits, inEnterMedia));
		dups.put(path, operation);
	}


	private void addFriendEvent(HashMap<String, String> dups, HitTracker result, Data activity, String operation, HitTracker inHits, EnterMedia inEnterMedia)
	{
		//just want the most recent operation for this sourcepath
		String path = activity.get("commentpath");
		if (dups.containsKey(path))
		{
			return;
		}
		dups.put(path, operation);
		result.add(createFriendActivity(activity,inHits, inEnterMedia));
	}

	private RecentActivity createAssetActivity(Data inActivity,HitTracker inHits, EnterMedia inEnterMedia)
	{
		RecentActivity activity = createActivity(inActivity, inHits);

		Asset asset = inEnterMedia.getAssetBySourcePath(inActivity.get("catalogid"), inActivity.getSourcePath());
		activity.setAsset(asset);
		
		//load up the comment info?
		return activity;
	}

	protected RecentActivity createActivity(Data inActivity, HitTracker inHits)
	{
		RecentActivity activity = new RecentActivity();
		activity.setDetails(inActivity);
		
		User user = getUserManager().getUser(inActivity.get("user"));
		activity.setActionUser(user);
		
		Date date = inHits.getDateValue(inActivity,"date");
		activity.setDate(date);
		return activity;
	}
	private RecentActivity createAlbumActivity(Data inActivity, HitTracker tracker, EnterMedia inEnterMedia)
	{
		RecentActivity activity = createActivity(inActivity, tracker);
		
//		String operation = inActivity.get("operation");
//		if( "albumcommentadded".equals(operation) )
//		{
//			Comment comment = new Comment();
//			comment.setUser(inUser)
//			comment.setComment(inActivity.get("commenttext"));
//			activity.setComment(comment);
//		}
		
		String albumid = inActivity.get("albumid");
		String owner = inActivity.get("owner");
		
		Album album = inEnterMedia.getAlbumSearcher().getAlbum(albumid, owner);
		activity.setAlbum(album);
		
		//load up the comment info?
		return activity;
	}
	private RecentActivity createFriendActivity(Data inActivity, HitTracker inHits, EnterMedia inEnterMedia)
	{
		RecentActivity activity = createActivity(inActivity, inHits);
		return activity;
	}


}
