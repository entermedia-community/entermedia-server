package org.openedit.entermedia.friends;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.entermedia.email.PostMail;
import org.entermedia.email.Recipient;
import org.entermedia.email.TemplateWebEmail;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.albums.Album;
import org.openedit.util.DateStorageUtil;

import com.openedit.WebPageRequest;
import com.openedit.comments.CommentArchive;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;
import com.openedit.users.UserManager;

public class FriendManager
{
	protected PostMail fieldPostMail;
	protected SearcherManager fieldSearcherManager;
	protected String fieldCatalogId;
	protected UserManager fieldUserManager;
	protected CommentArchive fieldCommentArchive;
	
	public CommentArchive getCommentArchive()
	{
		return fieldCommentArchive;
	}

	public void setCommentArchive(CommentArchive inCommentArchive)
	{
		fieldCommentArchive = inCommentArchive;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	protected String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}
	public Searcher getFriendSearcher()
	{
		return getSearcherManager().getSearcher("system", "friend");
	}
	public Searcher getInvitationSearcher()
	{
		return getSearcherManager().getSearcher(getCatalogId(), "invitation");
	}

	public Data getFriend(String inUserId, String inFriendId)
	{
		SearchQuery q = getFriendSearcher().createSearchQuery();
		q.addExact("ownerid", inUserId);
		q.addExact("friendid", inFriendId);
		HitTracker hits = getFriendSearcher().search(q);
		if( hits.size() > 0)
		{
			return hits.get(0);
		}
		return null;
	}
	public HitTracker getFriends(String inUserId)
	{
		HitTracker hits = getFriendSearcher().fieldSearch("ownerid", inUserId);
		return hits;
	}

	public void makeFriends(String inOwner, String inTarget, User inUser)
	{
		addFriend( inOwner, inTarget, inUser);
		addFriend( inTarget, inOwner, inUser);
	}
	
	protected void addFriend(String inOwner, String inTarget, User inUser)
	{
		if (!isFriend(inOwner, inTarget))
		{
			// create a new subscriber data object
			Data friend = new BaseData();
			//we should look up the user with id = targetid
			String currentusername = inOwner;
			friend.setProperty("ownerid", currentusername);
			friend.setProperty("friendid", inTarget);
			friend.setProperty("notificationcomments", "true");
			friend.setProperty("notificationassetsadded", "true");
//			friend.setProperty("email", user.getEmail());
//			friend.setProperty("firstname", user.getFirstName());
//			friend.setProperty("lastname", user.getLastName());
			//save to correct place
			friend.setSourcePath(currentusername);
			getFriendSearcher().saveData(friend, inUser);
		}

	}
//	public void sendFriendConfirmation(WebPageRequest inReq, User inTarget)
//	{
//		//create the recipient list
//		List<Recipient> recipients = new ArrayList<Recipient>();
//		Recipient recipient = new Recipient();
//		recipient.setEmailAddress(inTarget.getEmail());
//		recipient.setFirstName(inTarget.getFirstName());
//		recipient.setLastName(inTarget.getLastName());
//		recipients.add(recipient);
//		//call send mail method
//		mailFriends(inReq, recipients, null);
//	}
//	public void setReceiveMail(WebPageRequest inReq, String value)
//	{
//		String targetid = inReq.getRequestParameter("targetid");
//		if(targetid == null)
//		{
//			//use current logged in user
//			targetid = inReq.getUserName();
//		}
//		String ownerid = getFriendSearchId(inReq);
//		Data friend = getFriend(targetid, ownerid, inReq);
//		//there could be more than one result returned but that is highly unlikely
//		if(friend != null)
//		{
//			friend.setProperty("receivemail", value);
//			getFriendSearcher(inReq).saveData(friend, inReq.getUser());
//		}
//	}

	public HitTracker getOpenInvitations(String inUserId)
	{
		Searcher inviationSearcher = getInvitationSearcher();
		SearchQuery query = inviationSearcher.createSearchQuery();
		query.addExact("ownerid", inUserId);
		
		HitTracker hits = inviationSearcher.search(query);
		return hits;
	}
/*
	public void autoFriend(WebPageRequest inReq)
	{
		String inviteid = inReq.getRequestParameter("invitationid");
		User owner = (User)inReq.getPageValue("owner");
		if(inviteid != null && !isFriend(inReq) && owner.getId() != inReq.getUser().getId())
		{
			//verify invitation id exists
			String appid = inReq.getContentProperty("applicationid");
			Searcher inviationSearcher = getSearcherManager().getSearcher(appid, "invitation");
			SearchQuery query = inviationSearcher.createSearchQuery();
			query.addExact("id", inviteid);
			query.addExact("owner", inReq.getUserName());
			
			HitTracker hits = inviationSearcher.search(query);
			if( hits.size() == 0)
			{
				throw new OpenEditException("Invitation ID not valid");
			}
			
			inReq.setRequestParameter("targetid", inReq.getUserName());
			addFriend(inReq, owner.getId(), true);
			inReq.setRequestParameter("targetid", owner.getId());
			addFriend(inReq, inReq.getUserName(), true);
			inReq.putPageValue("autofriend", owner);
		}
	}
*/
	
	public Data getFriendByFriendId(String inId)
	{
		Searcher searcher = getFriendSearcher();
		return (Data) searcher.searchById(inId);
	}
	
	//TODO: Cache last 1000 requests. Clear cache when friends are edited 
	public boolean isFriend(String inOwnerid, String inTargetId)
	{
		if(inOwnerid.equals(inTargetId))
		{
			return true;
		}
		else
		{
			if(getFriend(inOwnerid, inTargetId) != null)
			{
				return true;
			}
		}
		return false;
	}

	public void breakFriends(String inOwner, String inTargetid, User inUser)
	{
		Data owner = getFriend(inOwner, inTargetid);
		if(owner != null)
		{
			getFriendSearcher().delete(owner, inUser);
		}
		Data friend = getFriend(inTargetid, inOwner);
		if(friend != null)
		{
			getFriendSearcher().delete(friend, inUser);
		}
	}

	public PostMail getPostMail()
	{
		return fieldPostMail;
	}

	public void setPostMail(PostMail inPostMail)
	{
		fieldPostMail = inPostMail;
	}
	public String nextInviteId()
	{
		return getInvitationSearcher().nextId();
	}
	public void invite(User inUser, String inAddresses, WebPageRequest inReq)
	{
		List recipients = getPostMail().getTemplateWebEmail().setRecipientsFromUnknown(inAddresses);
		for (Iterator iterator = recipients.iterator(); iterator.hasNext();)
		{
			Recipient r = (Recipient) iterator.next();
			String id = nextInviteId();
			inReq.putPageValue("invitationid", id);
			String subject = inReq.findValue("subject");
			String body = inReq.getRequestParameter("body");
			//body may be null because a email body may be set in the xconf
			if(body != null)
			{
				body = body.replace("${invitationid}", id);
			}
			else
			{
				//they are specifiying an emailbody so we need to edit the subject because it's set in the xconf
				subject = inReq.getUser().getShortDescription() + " " + subject;
			}
			Data invite = saveInvite(inUser, id, r.getEmailAddress(), subject, body);
			
			sendInvitation(invite, body, subject, inReq);
		}
	}
	
	public Data saveInvite(User inOwner, String inInviteId, String inEmail, String inSubject, String inBody)
	{
		//need to build recipient list this shouldn't be too bad
		//Now we need to add data for future lookups
		
		//Now save the Invite ID
		Searcher inviationSearcher  = getInvitationSearcher();
		Data invite = inviationSearcher.createNewData();

		invite.setSourcePath(inOwner.getUserName());
		invite.setId(inInviteId);
		
		invite.setProperty("email", inEmail);
		invite.setProperty("ownerid", inOwner.getUserName());
		invite.setProperty("subject", inSubject);
		invite.setProperty("body", inBody);

		String now = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
		invite.setProperty("datesent", now);

		inviationSearcher.saveData(invite, inOwner);
		return invite;
	}

	public void sendInvitation(Data inInvite,String body, String inSubject, WebPageRequest inReq)
	{
		TemplateWebEmail email = getPostMail().getTemplateWebEmail();
		email.loadSettings(inReq); //uses systemfromemail
		
		User sender = inReq.getUser();
		email.setProperty("senderuserid", sender.getId());
		email.setProperty("senderfirstname", sender.getFirstName());
		email.setProperty("senderlastname", sender.getLastName());
		if(body != null)
		{
			email.setMessage(body);
		}
		if(inSubject != null)
		{
			email.setSubject(inSubject);
		}
		email.setTo(inInvite.get("email"));
		email.send();
	}

	
	
	public boolean acceptInvitation(String inOwner, String inInviteId, User inTestUser)
	{
		//look up the invite id
		Searcher invitationSearcher = getInvitationSearcher();
		Data invitation = (Data)invitationSearcher.searchById(inInviteId);
		if(invitation != null && invitation.get("ownerid") != null && invitation.get("ownerid").equals(inOwner))
		{
			makeFriends(inOwner, inTestUser.getId(), inTestUser);
			//remove the row from the invitations xml file the invitation has been taken care of
			invitationSearcher.delete(invitation, inTestUser);
			return true;
		}
		return false;
	}

	protected boolean isAllowed(Data inFriend, HitTracker inAllowedtoalbum)
	{

		for (Iterator iterator2 = inAllowedtoalbum.iterator(); iterator2.hasNext();)
		{
			Data allowed = (Data) iterator2.next();
			if( inFriend.get("friendid").equals(allowed.get("friendid")) )
			{
				return true;
			}
		}
		return false;
	}

	public void sendAlbumCommentNotification(Album inAlbum, WebPageRequest inReq)
	{		
		String inCommenterId = inReq.getUserName();
		HitTracker commenterfriends = getFriends( inCommenterId );
		
		HitTracker allowedtoalbum = getFriends( inAlbum.getUserName() );

		List commonfriends = new ArrayList();
		for (Iterator iterator = commenterfriends.iterator(); iterator.hasNext();)
		{
			Data friend = (Data) iterator.next();
			if(isAlbumOwner(friend, inAlbum) || (isNotifyChecked(friend) && isAllowed(friend, allowedtoalbum)))
			{
				commonfriends.add(friend);
			}
		}
		inReq.putPageValue("album",inAlbum);
		inReq.putPageValue("comment",inReq.getRequestParameter("commenttext"));
		inReq.putPageValue("commenter",inReq.getUser());
		TemplateWebEmail email = getPostMail().getTemplateWebEmail();
		email.loadSettings(inReq); //uses systemfromemail
		
		
		for (Iterator iterator = commonfriends.iterator(); iterator.hasNext();)
		{
			Data friend = (Data) iterator.next();
			User user = getUserManager().getUser(friend.get("friendid"));
			String emaila = user.getEmail();
			if( emaila != null)
			{
				email.setTo(emaila);
				email.send();
			}			
		}
		
	}

	protected boolean isAlbumOwner(Data inFriend, Album inAlbum)
	{
		if(inFriend.get("friendid").equals(inAlbum.getUserName()))
		{
			return true;
		}
		return false;
	}

	protected boolean isNotifyChecked(Data inFriend)
	{
		Data friend = getFriend(inFriend.get("friendid"), inFriend.get("ownerid"));
		String val = friend.get("notificationcomments");
		return val != null && Boolean.parseBoolean(val);
	}

	public void sendAssetCommentNotification(Asset inAsset, WebPageRequest inReq)
	{
		String commentpath = inReq.getRequestParameter("commentpath");
		Set users = getCommentArchive().loadUsersWhoCommented(commentpath);
		
		Collection friends = getFriends(inReq.getUserName());
		for (Iterator iterator = friends.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			if( isNotifyChecked(data) )
			{
				User frienduser = getUserManager().getUser(data.get("friendid"));
				users.add(frienduser);
			}
		}
		
		inReq.putPageValue("asset",inAsset);
		inReq.putPageValue("comment",inReq.getRequestParameter("commenttext"));
		inReq.putPageValue("commenter",inReq.getUser());
		TemplateWebEmail email = getPostMail().getTemplateWebEmail();
		email.loadSettings(inReq); //uses systemfromemail
		
		for (Iterator iterator = users.iterator(); iterator.hasNext();)
		{
			User user = (User) iterator.next();
			String emaila = user.getEmail();
			if( emaila != null)
			{
				email.setTo(emaila);
				email.send();
			}			
		}
				
	}
	
	public void toggleCommentNotification(String inDataId, User inUser)
	{
		Data dataRow = (Data)getFriendSearcher().searchById(inDataId);
		if (dataRow != null)
		{
			if (Boolean.parseBoolean(dataRow.get("notificationcomments")))
			{
				dataRow.setProperty("notificationcomments", "false");
			}
			else
			{
				dataRow.setProperty("notificationcomments", "true");
			}
			getFriendSearcher().saveData(dataRow, inUser);
		}
	}

}
