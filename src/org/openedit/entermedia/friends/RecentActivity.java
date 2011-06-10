package org.openedit.entermedia.friends;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.albums.Album;

import com.openedit.WebPageRequest;
import com.openedit.comments.Comment;
import com.openedit.users.User;

public class RecentActivity extends BaseData
{
	protected Data fieldDetails;
	protected Album fieldAlbum;
	protected Asset fieldAsset;
	protected Comment fieldComment;
	protected User fieldActionUser;
	protected Date fieldDate;
	public String get(String inKey)
	{
		return getDetails().get(inKey);
	}
	public Date getDate()
	{
		return fieldDate;
	}
	public void setDate(Date inDate)
	{
		fieldDate = inDate;
	}
	public User getActionUser()
	{
		return fieldActionUser;
	}
	public void setActionUser(User inActionUser)
	{
		fieldActionUser = inActionUser;
	}
	public Data getDetails()
	{
		return fieldDetails;
	}
	public void setDetails(Data inDetails)
	{
		fieldDetails = inDetails;
	}
	public Album getAlbum()
	{
		return fieldAlbum;
	}
	public void setAlbum(Album inAlbum)
	{
		fieldAlbum = inAlbum;
	}
	public Asset getAsset()
	{
		return fieldAsset;
	}
	public void setAsset(Asset inAsset)
	{
		fieldAsset = inAsset;
	}
	public Comment getComment()
	{
		return fieldComment;
	}
	public void setComment(Comment inComment)
	{
		fieldComment = inComment;
	}
	public String getDateFormated(WebPageRequest inReq)
	{
		//LocaleManager
		DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, new Locale(inReq.getLocale()));
		return format.format(getDate());
	}
}
