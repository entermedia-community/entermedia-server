/*
 * Created on Jul 1, 2006
 */
package org.openedit.entermedia.albums;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.EnterMedia;
import org.openedit.entermedia.MediaArchive;
import org.openedit.util.DateStorageUtil;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.users.User;

public class Album implements Data
{
	protected String fieldId;
	protected String fieldName;
	protected User fieldUser;
	protected Map fieldProperties;
	protected boolean fieldSelection = false;
	protected int fieldModificationCount = 0;
	protected Date fieldLastModified;
	protected AlbumSearcher fieldAlbumSearcher;
	protected String fieldDataSource;
	protected Collection<User> fieldParticipants;
	protected Collection<String> fieldEmailParticipants;
	
	public String getDataSource()
	{
		return fieldDataSource;
	}

	public void setDataSource(String inDataSource)
	{
		fieldDataSource = inDataSource;
	}

	public AlbumSearcher getAlbumSearcher()
	{
		return fieldAlbumSearcher;
	}

	public void setAlbumSearcher(AlbumSearcher inAlbumSearcher)
	{
		fieldAlbumSearcher = inAlbumSearcher;
	}

	public Album()
	{

	}
	
	
	protected void modified()
	{
		fieldModificationCount++;
		setLastModified(new Date());
	}
	
	public String getIndex()
	{
		return fieldId + String.valueOf(fieldModificationCount);
	}

	public String getId()
	{
		return fieldId;
	}

	public void setId(String inId)
	{
		fieldId = inId;
	}

	public String getName()
	{
		return fieldName;
	}

	public void setName(String inName)
	{
		fieldName = inName;
	}

	public HitTracker getAlbumItems(WebPageRequest inReq)
	{
		HitTracker tracker = getAlbumSearcher().getAlbumItems(getId(), getUserName(), inReq);
		return tracker;
	}
	
	public boolean containsAsset(String inCatalogId, String inId, WebPageRequest inReq)
	{
		if( inId == null)
		{
			return false;
		}
		HitTracker tracker = getAssets(inCatalogId, inReq);
		return tracker.containsById(inId);
	}

	public HitTracker getAssets(String inCatalogId, WebPageRequest inReq) 
	{
		HitTracker tracker = getAlbumSearcher().getAssets(inCatalogId, getId(), getUserName(), "albumitems", inReq);
		return tracker;
	}

	public boolean containsAsset(Asset inAsset, WebPageRequest inReq)
	{
		if( inAsset == null)
		{
			return false;
		}
		return containsAsset(inAsset.getCatalogId(), inAsset.getId(), inReq);
	}

	public User getUser()
	{
		return fieldUser;
	}

	public String getUserName()
	{
		if (getUser() != null)
		{
			return getUser().getUserName();
		}
		return null;
	}

	public void setUser(User inUser)
	{
		fieldUser = inUser;

		if (inUser != null) // this is for the index
		{
			putProperty("owner", inUser.getUserName());
		}
		else
		{
			removeProperty("owner");
		}
		addParticipant(inUser);
		
	}

	public Map getProperties()
	{
		if (fieldProperties == null)
		{
			fieldProperties = ListOrderedMap.decorate(new HashMap());
		}
		return fieldProperties;
	}

	public String getProperty(String inKey)
	{
//		if (inKey.equals("size"))
//		{
//			return String.valueOf(size());
//		}

		if (inKey.equals("user") && getUser() != null)
		{
			return getUser().getUserName();
		}
		String value = (String) getProperties().get(inKey);
		return value;
	}

	public void setProperties(Map inAttributes)
	{
		fieldProperties = inAttributes;
	}

	public void putProperty(String inKey, String inValue)
	{
		if (inValue != null)
		{
			getProperties().put(inKey, inValue);
		}
		else
		{
			getProperties().remove(inKey);
		}
	}

	public void addProperty(String inKey, String inValue)
	{
		if (inValue == null || inValue.length() == 0)
		{
			getProperties().remove(inKey);
		}
		else
		{
			getProperties().put(inKey, inValue);
		}
	}

	public void removeProperty(String inKey)
	{
		if (inKey != null && inKey.length() > 0)
		{
			getProperties().remove(inKey);
		}
	}

	public String get(String inId)
	{
		if( "sourcepath".equals(inId))
		{
			return getSourcePath();
		}
		if( "lastmodified".equals(inId) && getLastModified() != null)
		{			
			return DateStorageUtil.getStorageUtil().formatForStorage(getLastModified());
		}
		return getProperty(inId);
	}

	public void setProperty(String inId, String inValue)
	{
		addProperty(inId, inValue);
	}

	public List keys()
	{
		return new ArrayList(getProperties().keySet());
	}

//	public Document getPrimaryItem()
//	{
//		if (getAlbumItems().size() > 0)
//		{
//			Document item = (Document) getAlbumItems().get(0);
//			return item;
//		}
//		return null;
//	}

	public boolean isPrivate()
	{
		String priv = getProperty("private");
		if(priv != null)
		{
			return Boolean.parseBoolean(priv);
		}
		return true;
	}

	public void setPrivate(boolean inPrivate)
	{
		putProperty("private", String.valueOf(inPrivate));
	}

	public boolean isSelection() {
		return fieldSelection;
	}

	public void setSelection(boolean selection) {
		fieldSelection = selection;
	}
	
	//The home folder of the album
	//users/admin/albums/103
	public String getSourcePath()
	{
		String	custom = "users/" + getUserName() + "/albums/" + getId();
		return custom;
	}

	public void setSourcePath(String inSourcepath)
	{
		//NA
	}
	
	public Data toData(Object inHit)
	{
		AlbumItem item = (AlbumItem)inHit;
		return item;
	}
	
	public String getSessionId()
	{
		return getUserName() + getId();
	}
	
	public void setLastModified(Date inDate)
	{
		fieldLastModified = inDate;
	}
	public Date getLastModified()
	{
		return fieldLastModified;
	}
	public String getSessionId(WebPageRequest inReq)
	{
		return getAlbumItems(inReq).getSessionId();
	}
	public int size(WebPageRequest inReq)
	{
		return getAlbumItems(inReq).size();
	}

	public int size(String inCatalogId, WebPageRequest inReq)
	{
		HitTracker tracker = getAssets(inCatalogId, inReq);
		return tracker != null ? tracker.size():0;
	}

	public String loadThumbData(WebPageRequest inReq)
	{
		HitTracker items = getAlbumItems(inReq);
		if(items.size() > 0 )
		{
			Object hit = items.get(0);
			EnterMedia entermedia = (EnterMedia)inReq.getPageValue("entermedia");
			MediaArchive mediaarchive = entermedia.getMediaArchive(items.getValue(hit, "catalogid") );
			
			String cataloghome = "/" + mediaarchive.getCatalogId();
			
			String type = mediaarchive.getMediaRenderType( items.getValue(hit, "fileformat"));
			inReq.putPageValue("type", type);
			inReq.putPageValue("cell", hit);		
			inReq.putPageValue("cataloghome", cataloghome);
			inReq.putPageValue("catalogid", mediaarchive.getCatalogId());
			inReq.putPageValue("mediaarchive", mediaarchive);
			return cataloghome + "/results/thumbnails/media/" + type + ".html";
		}
		return null;
	}
	
	public Collection<User> getParticipants()
	{
		if (fieldParticipants == null)
		{
			fieldParticipants = new HashSet();
		}
		return fieldParticipants;
	}

	public void addParticipant(User inParticipant)
	{
		if( inParticipant == null)
		{
			//cant add null users
			return;
		}
		getParticipants().add(inParticipant);
	}

	public Collection<String> getEmailParticipants()
	{
		if(fieldEmailParticipants == null)
		{
			fieldEmailParticipants = new HashSet();
		}
		return fieldEmailParticipants;
	}

	public void addEmailParticipant(String inEmailParticipant)
	{
		if(inEmailParticipant == null)
		{
			return;
		}
		getEmailParticipants().add(inEmailParticipant);
	}

	public boolean removeEmailParticipant(String inEmailParticipant)
	{
		if(inEmailParticipant == null)
		{
			return false;
		}
		return getEmailParticipants().remove(inEmailParticipant);
	}

	public void removeAsset(Asset inAsset, WebPageRequest inReq) 
	{
		getAlbumSearcher().removeAssetFromAlbum(inAsset, this, inReq);	
	}
	public void removeAssets(Collection inAssets, WebPageRequest inReq) 
	{
		getAlbumSearcher().removeAssetsFromAlbum(inAssets, this, inReq);	
	}
	
}
