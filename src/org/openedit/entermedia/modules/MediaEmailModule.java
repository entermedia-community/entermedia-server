package org.openedit.entermedia.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.email.EmailSearcher;
import org.entermedia.email.PostMail;
import org.entermedia.email.Recipient;
import org.entermedia.email.TemplateWebEmail;
import org.openedit.data.PropertyDetail;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.repository.ContentItem;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditRuntimeException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.users.Group;
import com.openedit.users.User;

public class MediaEmailModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(MediaEmailModule.class);
	protected PostMail fieldPostMail;
	protected SearcherManager fieldSearcherManager;

	public PostMail getPostMail()
	{
		return fieldPostMail;
	}

	public void setPostMail(PostMail inPostMail)
	{
		fieldPostMail = inPostMail;
	}

	/*
	 * Accepts a list of groups, emails and usernames through the
	 * webPageRequest. To use usernames only, you must also have a value for
	 * "specifydomain" which will be appended to the usernames.
	 */
	public void sendAssetNotification(WebPageRequest inReq) throws Exception
	{
		String[] groups = inReq.getRequestParameters("groupid");
		List grouplist = new ArrayList();
		if (groups != null)
		{
			grouplist = Arrays.asList(groups);
		}
		String othergroups = inReq.findValue("groups");
		if (othergroups != null)
		{
			String[] ids = othergroups.split(",");
			List temp = Arrays.asList(ids);
			grouplist.addAll(temp);
		}
		String addresses = inReq.getRequestParameter("email");

		TemplateWebEmail email = getPostMail().getTemplateWebEmail();
		email.loadSettings(inReq);

		List recipients = new ArrayList();
		if (grouplist != null)
		{
			StringBuffer gbuff = new StringBuffer();
			for (Iterator groupiter = grouplist.iterator(); groupiter.hasNext();)
			{
				String id = (String) groupiter.next();

				Group group = getUserManager().getGroup(id);
				if (group != null)
				{
					gbuff.append(id);
					gbuff.append(",");

					HitTracker users = getUserManager().getUsersInGroup(group);
					for (Iterator iterator = users.iterator(); iterator.hasNext();)
					{
						User user = (User) iterator.next();
						String target = user.getEmail();
						if (email != null)
						{
							Recipient recipient = new Recipient();
							recipient.setEmailAddress(target);
							recipient.setFirstName(user.getFirstName());
							recipient.setLastName(user.getLastName());

							recipients.add(recipient);
						}
					}
				}
			}
			email.setProperty("groups", gbuff.toString());
		}
		if (addresses != null)
		{
			String[] to = addresses.split("[,;]|\\s");
			for( String address: to )
			{
				if( address.contains("@") )
				{
					Recipient rec = new Recipient();
					rec.setEmailAddress(address.trim());
					recipients.add(rec);
				}
				else // This is a username
				{
					User user = getUserManager().getUser(address.trim());
					if( user != null && user.getEmail() != null && user.getEmail().contains("@") )
					{
						Recipient rec = new Recipient();
						rec.setFirstName(user.getFirstName());
						rec.setLastName(user.getLastName());
						rec.setEmailAddress(user.getEmail());
						recipients.add(rec);
					}
				}
			}
			email.setRecipients(recipients);
//			we don't know if the address string contains comma seperated or semicolon seperated values or both
			email.setProperty("emails", addresses);
		}
		MediaArchive mediaArchive = getMediaArchive(inReq);
		String assetSourcePath = inReq.findValue("sourcepath");
		Asset asset = mediaArchive.getAssetBySourcePath(assetSourcePath);
		
		String[] paths = inReq.getRequestParameters("file");
		List attachments = new ArrayList();
		if (paths != null)
		{
			for (int i = 0; i < paths.length; i++)
			{
				String path = paths[i];
				ContentItem item = getPageManager().getRepository().getStub(path);
				File target = new File(item.getAbsolutePath());
				if (target.exists())
				{
					attachments.add(target.getAbsolutePath());
				}
				else
				{
					log.info("file: " + target.getAbsolutePath() + " did not exist");
				}
			}
			if (attachments.size() > 0)
			{
				email.setFileAttachments(attachments);
			}
		}

		email.setProperty("assetid", asset.getId());
		email.setProperty("senderuserid", inReq.getUser().getId());
		email.setProperty("senderfirstname", inReq.getUser().getFirstName());
		email.setProperty("senderlastname", inReq.getUser().getLastName());

		User user = inReq.getUser();
		if (user.getEmail() != null)
		{
			email.setFrom(user.getEmail());
			email.setFromName(user.toString());
		}

		email.setRecipients(recipients);
		try
		{
			email.send();
			updateNotificationDate(asset, inReq);
			EmailSearcher searcher = (EmailSearcher) getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "email");
//			PropertyDetail sentdate = searcher.getDetail("sent-date");
//			if( sentdate != null && sentdate.getDateFormat() != null )
//			{
//				email.setDateFormat(sentdate.getDateFormat());
//			}
			searcher.saveData(email, inReq.getUser());
		}
		catch(Exception e)
		{
			inReq.putPageValue("emailerror", e.getMessage());
		}

	}

	protected void updateNotificationDate(Asset inAsset, WebPageRequest inReq)
	{
		String datefield = inReq.findValue("datefield");
		MediaArchive mediaArchive = getMediaArchive(inReq);
		if (datefield != null)
		{
			PropertyDetail detail = mediaArchive.getAssetSearcher().getDetail(datefield);
			if (!detail.isDataType("date"))
			{
				throw new OpenEditRuntimeException("Date Field isn't a date");
			}
			String date = DateStorageUtil.getStorageUtil().formatForStorage(new Date());
			inAsset.setProperty(datefield, date);
			mediaArchive.saveAsset(inAsset,inReq.getUser());
		}
	}

}
