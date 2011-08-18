/*
 * Created on Jul 1, 2006
 */
package org.openedit.entermedia.modules;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.email.PostMail;
import org.entermedia.email.Recipient;
import org.entermedia.email.TemplateWebEmail;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.EnterMedia;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.albums.Album;
import org.openedit.entermedia.albums.AlbumItem;
import org.openedit.entermedia.albums.AlbumSearcher;
import org.openedit.entermedia.albums.AlbumSelectionList;
import org.openedit.entermedia.friends.FriendManager;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventListener;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.Permission;
import com.openedit.users.User;
import com.openedit.util.FileUtils;
import com.openedit.util.strainer.BooleanFilter;
import com.openedit.util.strainer.UserFilter;

public class AlbumModule extends BaseMediaModule {
	private static final Log log = LogFactory.getLog(AlbumModule.class);
	protected PostMail fieldPostMail;
	protected WebEventListener fieldWebEventListener;

	/**
	 * @deprecated use {@link #loadAlbum(WebPageRequest)} inistead.
	 */
	public Album getAlbum(WebPageRequest inReq) throws Exception {
		return loadAlbum(inReq);
	}

	public void toggleItem(WebPageRequest inReq) throws Exception {
		Album album = loadAlbum(inReq);
		if (album == null) {
			return;
		}
		Asset asset = getAsset(inReq);

		if (album.containsAsset(asset.getCatalogId(), asset.getId(), inReq)) {
			removeItem(inReq);
		} else {
			addItem(inReq);
		}
		inReq.putPageValue("album", album);
	}

	public void addSelections(WebPageRequest inReq) throws Exception {
		Album album = loadAlbum(inReq);
		if (album == null) {
			return;
		}

		String catalogid = inReq.findValue("catalogid");
		String hitssessionid = inReq.getRequestParameter("hitssessionid");

		HitTracker assets = (HitTracker) inReq.getSessionValue(hitssessionid);
		for (Iterator iterator = assets.getSelectedHits().iterator(); iterator.hasNext();) {
			Data hit = (Data) iterator.next();
			Asset asset = getMediaArchive(catalogid).getAsset(hit.getId());

			if (!album.containsAsset(asset.getCatalogId(), asset.getId(), inReq)) {
				getAlbumSearcher(inReq).addAssetToAlbum(asset, album, inReq);
				inReq.putPageValue("addedtoalbum", album);
				WebEvent event = createEvent(album, "additem", inReq, asset);
				getWebEventListener().eventFired(event);
			}

			
		}
		inReq.putPageValue("album", album);
	}

	
	public void removeSelections(WebPageRequest inReq) throws Exception {
		Album album = loadAlbum(inReq);
		if (album == null) {
			return;
		}

		String catalogid = inReq.findValue("catalogid");
		String hitssessionid = inReq.getRequestParameter("hitssessionid");

		HitTracker assets = (HitTracker) inReq.getSessionValue(hitssessionid);
		for (Iterator iterator = assets.getSelectedHits().iterator(); iterator.hasNext();) {
			Data hit = (Data) iterator.next();
			Asset asset = getMediaArchive(catalogid).getAsset(hit.getId());

			if (album.containsAsset(asset.getCatalogId(), asset.getId(), inReq)) {
				getAlbumSearcher(inReq).removeAssetFromAlbum(asset, album, inReq);
				inReq.putPageValue("removedfromalbum", album);
				WebEvent event = createEvent(album, "removeitem", inReq, asset);
				getWebEventListener().eventFired(event);
			}

			
		}
		inReq.putPageValue("album", album);
	}

	
	
	private WebEvent createEvent(Album inAlbum, String inOperation, WebPageRequest inReq) {
		String catalogid = inReq.findValue("applicationid");
		WebEvent event = new WebEvent();
		event.setCatalogId(catalogid);

		String sourcepath = inReq.findValue("applicationid") + "/" + inAlbum.getSourcePath();
		event.addDetail("sourcepath", sourcepath);
		event.setSearchType("albumedit");
		event.setSource(this);
		event.setOperation(inOperation);
		event.addDetail("type", inOperation);
		event.addDetail("albumid", inAlbum.getId());
		event.addDetail("ownerid", inAlbum.getUserName());
		event.setDate(new Date());
		event.setUser(inReq.getUser());
		return event;
	}

	private WebEvent createEvent(Album inAlbum, String inOperation, WebPageRequest inReq, Asset inAsset) {
		WebEvent event = createEvent(inAlbum, inOperation, inReq);
		event.addDetail("assetid", inAsset.getId());
		event.addDetail("assetcatalogid", inAsset.getCatalogId());
		event.addDetail("assetname", inAsset.getName());
		return event;
	}

	private WebEvent createEvent(Album inAlbum, String inOperation, WebPageRequest inReq, List<Asset> inAssets) {
		WebEvent event = createEvent(inAlbum, inOperation, inReq);
		StringBuffer ids = new StringBuffer(), catalogs = new StringBuffer();
		for (int i = 0; i < inAssets.size(); i++) {
			ids.append(inAssets.get(i).getId() + ";");
			catalogs.append(inAssets.get(i).getCatalogId());
		}
		event.addDetail("assetids", ids.toString());
		event.addDetail("assetcatalogs", catalogs.toString());
		return event;
	}

	public void addItem(WebPageRequest inReq) throws Exception {
		String assetid = inReq.getRequestParameter("assetid");
		String catalogid = inReq.getRequestParameter("catalogid");
		if (catalogid == null) {
			catalogid = inReq.findValue("catalogid");
		}
		String sourcePath = inReq.getRequestParameter("sourcepath");

		MediaArchive archive = getMediaArchive(catalogid);
		Asset asset = null;

		if (assetid != null) {
			asset = archive.getAsset(assetid);
		}

		if (asset == null && sourcePath != null) {
			asset = archive.getAssetArchive().getAssetBySourcePath(sourcePath, true);
			String id = null;
			if (asset.getId() != null) {
				id = asset.getId();
			} else {
				id = archive.getAssetSearcher().nextAssetNumber();
			}
			asset.setId(id);
			archive.saveAsset(asset, inReq.getUser());
		}

		if (asset != null) {
			Album album = loadAlbum(inReq);

			if (!album.containsAsset(asset, inReq)) {
				getAlbumSearcher(inReq).addAssetToAlbum(asset, album, inReq);
				inReq.putPageValue("addedtoalbum", album);
				WebEvent event = createEvent(album, "additem", inReq, asset);
				getWebEventListener().eventFired(event);
			}
		} else {
			log.info("No asset found " + assetid);
		}
	}

	protected Data createItemData(String inAlbumId, Asset inAsset) {
		BaseData data = new BaseData();
		data.setId(inAlbumId + "_" + inAsset.getCatalogId() + "_" + inAsset.getId());
		data.setSourcePath(inAsset.getSourcePath());
		data.setProperty("albumid", inAlbumId);
		return data;
	}

	public String makeRequestString(Map map) {
		String reqString = "";
		for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			String value = (String) map.get(key);
			if (reqString.length() == 0) {
				reqString = "?";
			} else {
				reqString = reqString + "&";
			}
			reqString = reqString + key + "=" + value;
		}
		return reqString;
	}

	public void removeItem(WebPageRequest inReq) throws Exception {
		String sourcePath = inReq.getRequestParameter("sourcePath");
		String assetid = inReq.getRequestParameter("assetid");
		String catalogid = inReq.getRequestParameter("catalogid");
		if (catalogid == null) {
			catalogid = inReq.findValue("catalogid");
		}

		EnterMedia matt = getEnterMedia(inReq);

		Asset asset = null;
		if (sourcePath != null) {
			asset = matt.getAssetBySourcePath(catalogid, sourcePath);
		}
		if (asset == null && assetid != null) {
			asset = matt.getAsset(catalogid, assetid);
		}

		if (asset != null) {
			Album album = loadAlbum(inReq);
			if (album != null) {
				album.removeAsset(asset, inReq);
				inReq.putPageValue("removedfromalbum", album);
				WebEvent event = createEvent(album, "removeitem", inReq, asset);
				getWebEventListener().eventFired(event);
			}
		}
	}

	public void removeAlbum(WebPageRequest inReq) throws Exception {
		deleteAlbum(inReq);
	}

	public AlbumSelectionList getAlbumList(WebPageRequest inReq) throws Exception {
		if (inReq.getUser() == null) {
			return null;
		}
		EnterMedia em = getEnterMedia(inReq);
		String user = inReq.getUserName();
		String sessionid = em.getApplicationId() + user + "albumlist";
		AlbumSelectionList list = (AlbumSelectionList) inReq.getSessionValue(sessionid);

		if (list == null) {
			list = em.getAlbumArchive().listAlbums(inReq.getUser());
			inReq.putSessionValue(sessionid, list);
		}
		inReq.putPageValue("albumlist", list);
		return list;
	}

	public Album loadAlbum(WebPageRequest inReq) throws Exception {
		String albumid = inReq.findValue("albumid");
		Album album = (Album) inReq.getPageValue("album");
		if (album != null && (album.getId().equals(albumid) || albumid == null)) {
			return album;
		}

		// Need this first case for loading up album thumbs on postings home
		// page.
		String userid = (String) inReq.getPageValue("username");
		if (userid == null) {
			userid = inReq.findValue("username");
		}

		if (userid == null && inReq.getUser() != null)
		{
			userid = inReq.getUser().getId();
		}
		if (albumid == null || userid == null) {
			return null;
		}
		AlbumSearcher searcher = getAlbumSearcher(inReq);
		album = searcher.getAlbum(albumid, userid);
		if (album == null && userid != null) {
			String autocreate = inReq.findValue("autocreatealbum");
			if (Boolean.parseBoolean(autocreate)) {
				album = searcher.createAlbum(albumid);
				searcher.saveData(album, inReq.getUser());
			} else {
				log.error("No album found " + albumid + " " + userid);
				return null;
			}
		}
		String applicationid = inReq.findValue("applicationid");
		album.setDataSource(applicationid + "/" + album.getSourcePath());
		inReq.putPageValue("album", album);
		return album;
	}

	public void addNewShare(WebPageRequest inReq) throws Exception {
		EnterMedia em = getEnterMedia(inReq);
		if (inReq.getRequestParameter("createshare") != null) {
			Album album = em.getAlbumArchive().createAlbum();

			album.setUser(inReq.getUser());

			inReq.putPageValue("album", album);
			// add pictures and participants
			addSelectionToAlbum(inReq);
			addParticipants(inReq);
			saveAlbumSettings(inReq);
			em.getAlbumSearcher().saveData(album, album.getUser());
			// fire the event
			WebEvent event = createEvent(album, "albumcreated", inReq);
			event.setProperty("postingbody", inReq.getRequestParameter("postingbody"));
			event.setProperty("subject", inReq.getRequestParameter("postingsubject"));
			event.setProperty("emails", inReq.getRequestParameter("postingto"));
			event.setProperty("albumid", album.getId());
			event.setProperty("albumowner", album.getUser().getId());
			event.setProperty("siteRoot", inReq.getSiteRoot());
			getWebEventListener().eventFired(event);
		}
	}

	public void addNewAlbum(WebPageRequest inReq) throws Exception {
		EnterMedia em = getEnterMedia(inReq);

		Album album = em.getAlbumArchive().createAlbum();
		album.setUser(inReq.getUser());
		inReq.putPageValue("album", album);
		saveAlbumSettings(inReq);
		em.getAlbumSearcher().saveData(album, album.getUser());
		WebEvent event = createEvent(album, "albumcreated", inReq);
		event.setProperty("postingbody", inReq.getRequestParameter("postingbody"));
		event.setProperty("subject", inReq.getRequestParameter("postingsubject"));
		event.setProperty("albumid", album.getId());
		getWebEventListener().eventFired(event);
	}

	public AlbumSearcher getAlbumSearcher(WebPageRequest inReq) {
		EnterMedia em = getEnterMedia(inReq);
		return (AlbumSearcher) em.getAlbumSearcher();
	}

	public void deleteAlbum(WebPageRequest inReq) throws Exception {
		String albumid = inReq.getRequestParameter("albumid");
		AlbumSearcher searcher = getAlbumSearcher(inReq);
		Album album = searcher.getAlbum(albumid, inReq.getUserName());
		if (album == null) {
			return;
		}
		searcher.delete(album, inReq.getUser());
		WebEvent event = createEvent(album, "delete", inReq);
		getWebEventListener().eventFired(event);
	}

	public void updateAlbum(WebPageRequest inReq) throws Exception {
		// add Album properties
		Album album = loadAlbum(inReq);
		if (album == null) {
			throw new OpenEditException("Invalid album");
		}

		// save any history properties
		String[] fields = inReq.getRequestParameters("field");
		if (fields != null) {
			for (int i = 0; i < fields.length; i++) {
				String field = fields[i];
				String value = inReq.getRequestParameter(field + ".value");
				album.setProperty(field, value);
			}
		}
		String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date());
		album.setProperty("date", date);
		album.setProperty("username", inReq.getUserName());

		getEnterMedia(inReq).getAlbumSearcher().saveData(album, album.getUser());
		WebEvent event = createEvent(album, "update", inReq);
		getWebEventListener().eventFired(event);
	}

	public void removeProperty(WebPageRequest inReq) throws Exception {
		String propertyid = inReq.getRequestParameter("propertyid");

		if (propertyid == null) {
			throw new OpenEditException("please provide a propertyid");
		}

		String assetid = inReq.getRequestParameter("assetid");

		if (assetid != null) {
			AlbumSelectionList list = getAlbumList(inReq);
			Album album = list.getSelectedAlbum();
			if (album == null) {
				throw new OpenEditException("invalid catalog");
			}
			getAlbumSearcher(inReq).saveData(album, inReq.getUser());
		}

	}

	public void createAlbumFromSelection(WebPageRequest inReq) throws Exception {
		addNewAlbum(inReq);
		addHitsToAlbum(inReq);
	}

	public void addHitsToAlbum(WebPageRequest inReq) throws Exception {
		HitTracker hits = null;

		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		if (hitssessionid != null) {
			hits = (HitTracker) inReq.getSessionValue(hitssessionid);
		}
		if (hits == null) {
			String hitsname = inReq.findValue("hitsname");
			hits = (HitTracker) inReq.getPageValue(hitsname);
		}
		if (hits != null) {
			MediaArchive archive = getMediaArchive(hits.getCatalogId());
			// Rerun the search in case it has been edited
			HitTracker hitsnew = archive.getAssetSearcher().cachedSearch(inReq, hits.getSearchQuery());
			if (hitsnew != null) {
				hits = hitsnew;
			}
			Iterator results;
			String onepage = inReq.getRequestParameter("onepage");
			if (onepage != null && Boolean.parseBoolean(onepage)) {
				results = hits.getPageOfHits().iterator();
			} else {
				results = hits.iterator();
			}
			EnterMedia matt = getEnterMedia(inReq);
			Album album = loadAlbum(inReq);
			List<Asset> assets = new ArrayList<Asset>();
			if (album != null) {
				while (results.hasNext()) {
					Object hit = results.next();
					String assetid = hits.getValue(hit, "id");
					String catalogid = hits.getValue(hit, "catalogid");
					Asset asset = matt.getAsset(catalogid, assetid);
					if (asset != null) {
						assets.add(asset);
					}
				}
				getAlbumSearcher(inReq).addAssetsToAlbum(assets, album, inReq);
				WebEvent event = createEvent(album, "additems", inReq, assets);
				getWebEventListener().eventFired(event);
				inReq.putPageValue("addedtoalbum", album);
			}
		}
	}

	public void removeHitsFromAlbum(WebPageRequest inReq) throws Exception {
		HitTracker hits = null;
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		if (hitssessionid != null) {
			hits = (HitTracker) inReq.getSessionValue(hitssessionid);
		}
		if (hits == null) {
			String hitsname = inReq.findValue("hitsname");
			hits = (HitTracker) inReq.getPageValue(hitsname);
		}
		if (hits != null) {
			Iterator results;
			String onepage = inReq.getRequestParameter("onepage");
			if (onepage != null && Boolean.parseBoolean(onepage)) {
				results = hits.getPageOfHits().iterator();
			} else {
				results = hits.iterator();
			}
			Album album = loadAlbum(inReq);
			ArrayList<Asset> assets = new ArrayList<Asset>();
			if (album != null) {
				while (results.hasNext()) {
					Object hit = (Object) results.next();
					String assetid = hits.getValue(hit, "id");
					String catalogid = hits.getValue(hit, "catalogid");
					Asset asset = getMediaArchive(catalogid).getAsset(assetid);
					if (asset != null) {
						assets.add(asset);
					}
				}
				getAlbumSearcher(inReq).removeAssetsFromAlbum(assets, album, inReq);
				WebEvent event = createEvent(album, "removeitems", inReq, assets);
				inReq.putPageValue("removedfromalbum", album);
			}
		}
	}

	public Album saveAlbumSettings(WebPageRequest inReq) throws Exception {
		EnterMedia matt = getEnterMedia(inReq);

		Album album = loadAlbum(inReq);
		Page albumhome = getPageManager().getPage("/" + getEnterMedia(inReq).getApplicationId() + "/" + album.getSourcePath() + "/");

		String name = inReq.getRequestParameter("postingsubject");
		album.setName(name);

		String desc = inReq.getRequestParameter("postingbody");
		album.setProperty("details", desc);

		String isprivate = inReq.getRequestParameter("postingprivate");
		if (isprivate == null) {
			isprivate = "true";
		}
		album.setPrivate(Boolean.parseBoolean(isprivate));

		User olduser = album.getUser();
		User newuser = null;
		String newowner = inReq.getRequestParameter("owner.value");
		if (newowner != null) {
			newuser = getUserManager().getUser(newowner);
		}
		if (newuser == null) {
			newuser = olduser;
		}
		album.setUser(newuser);

		String editable = inReq.getRequestParameter("editable.value");
		String readonly = inReq.getRequestParameter("readonly.value"); // this
																		// one
																		// is
																		// for
																		// SF
		boolean isEditable = true;

		if (editable != null) {
			isEditable = Boolean.parseBoolean(editable);
		} else {
			isEditable = false;
		}

		if (readonly != null) {
			isEditable = !Boolean.parseBoolean(readonly);
		}

		Permission edit = albumhome.getPermission("editalbum");
		if (edit == null) {
			edit = new Permission();
			edit.setName("editalbum");
			edit.setPath(albumhome.getPath());
			albumhome.getPageSettings().addPermission(edit);
		}
		if (isEditable) {
			BooleanFilter bfilter = new BooleanFilter();
			bfilter.setTrue(true);
			edit.setRootFilter(bfilter);
		} else {
			UserFilter ufilter = new UserFilter();
			ufilter.setUsername(album.getUserName());
			edit.setRootFilter(ufilter);
		}

		getPageManager().saveSettings(albumhome);
		AlbumSearcher searcher = matt.getAlbumSearcher();
		if (!olduser.getUserName().equals(newuser.getUserName())) {
			// Move files
			Page newhome = getPageManager().getPage("/" + getEnterMedia(inReq).getApplicationId() + "/" + album.getSourcePath() + "/");
			// getPageManager().movePage(albumhome, newhome);
			new FileUtils().move(albumhome.getContentItem().getAbsolutePath(), newhome.getContentItem().getAbsolutePath());

			HitTracker tracker = searcher.getAlbumItems(album.getId(), olduser.getUserName(), inReq);
			for (Object o : tracker) {
				Asset asset = matt.getAsset(tracker.getValue(o, "catalogid"), tracker.getValue(o, "id"));
				searcher.addAssetToAlbum(asset, album, inReq);
			}
			album.setUser(olduser);
			searcher.clearAlbum(album, inReq);
			album.setUser(newuser);
			inReq.putPageValue("ownerchanged", "true");
		}
		searcher.saveData(album, album.getUser());
		WebEvent event = createEvent(album, "savesettings", inReq);
		getWebEventListener().eventFired(event);
		inReq.putPageValue("album", album);
		return album;
	}

	public void addSelectionToAlbum(WebPageRequest inReq) throws Exception {
		String albumid = inReq.findValue("albumid");
		Album album = getAlbumSearcher(inReq).getAlbum(albumid, inReq.getUserName());
		if (album == null) {
			album = (Album) inReq.getPageValue("album");
		}

		String selectionid = inReq.getRequestParameter("selectionid");
		if (selectionid == null) {
			selectionid = inReq.getRequestParameter("postingattachments");
		}

		EnterMedia em = getEnterMedia(inReq);

		Album selection = em.getAlbumArchive().loadAlbum(selectionid, inReq.getUserName());

		if (selection != null) {
			HitTracker items = selection.getAlbumItems(inReq);
			ArrayList<Asset> assets = new ArrayList<Asset>();
			for (Object o : items) {
				Asset asset = em.getAssetBySourcePath(items.getValue(o, "catalogid"), items.getValue(o, "sourcepath"));
				if (asset != null) {
					assets.add(asset);
				}
			}
			log.info("Added " + assets.size());
			em.getAlbumSearcher().addAssetsToAlbum(assets, album, inReq);
			WebEvent event = createEvent(album, "assetsaddedtoalbum", inReq);
			getWebEventListener().eventFired(event);
		}
	}

	public void removeAll(WebPageRequest inReq) throws Exception {
		Album album = loadAlbum(inReq);
		AlbumSearcher searcher = getAlbumSearcher(inReq);
		searcher.clearAlbum(album, inReq);
		WebEvent event = createEvent(album, "clear", inReq);
		getWebEventListener().eventFired(event);
	}

	protected void updateAssetIndexes(WebPageRequest inReq, Collection<AlbumItem> inItems, boolean inAddItems) {
		for (AlbumItem item : inItems) {
			Searcher assetalbums = getSearcherManager().getSearcher(item.getCatalogId(), "assetalbums");
			if (inAddItems) {
				assetalbums.saveData(item, inReq.getUser());
			} else {
				assetalbums.delete(item, inReq.getUser());
			}
			getMediaArchive(item.getCatalogId()).getAssetSearcher().updateIndex(item.getAsset());
		}
	}

	public void shareAlbum(WebPageRequest inReq) throws Exception {
		String addfriends = inReq.getRequestParameter("addfriends.value");
		String addresses = inReq.getRequestParameter("email");

		TemplateWebEmail email = getPostMail().getTemplateWebEmail();
		email.loadSettings(inReq);

		// TODO: Fire a "share with all" webevent that will actually send the
		// email
		List<Recipient> recipients = new ArrayList<Recipient>();
		EnterMedia entermedia = getEnterMedia(inReq);
		FriendManager friendManager = entermedia.getFriendManager();
		if (addfriends != null) {
			// we need to get all of the current users friends
			// need to get the friendsModule bean
			HitTracker friends = friendManager.getFriends(inReq.getUserName());
			for (Object friend : friends) {
				User user = getUserManager().getUser(((Data) friend).get("targetid"));
				String firstname = user.getFirstName();
				String lastname = user.getLastName();
				String emailaddr = user.getEmail();
				if (emailaddr != null && lastname != null && firstname != null) {
					// create a new recipient object
					Recipient recipient = new Recipient();
					recipient.setEmailAddress(emailaddr);
					recipient.setFirstName(firstname);
					recipient.setLastName(lastname);
					recipients.add(recipient);
				}
			}
		}
		if (addresses != null) {
			List<Recipient> inviteRecipients = email.setRecipientsFromUnknown(addresses);
			String sendInvitationsText = inReq.getContentProperty("sendinvitations");
			if (sendInvitationsText == null || Boolean.parseBoolean(sendInvitationsText)) {
				for (Recipient recipient : inviteRecipients) {
					String targetEmail = recipient.getEmailAddress();
					User targetUser = getUserManager().getUserByEmail(targetEmail);
					if (targetUser == null || !friendManager.isFriend(inReq.getUser().getId(), targetUser.getId())) {
						String inviteid = friendManager.nextInviteId();
						String message = email.getMessage();
						message = message.replace("${args}", "?invitationid=" + inviteid);
						email.setMessage(message);
						friendManager.saveInvite(inReq.getUser(), inviteid, recipient.getEmailAddress(), email.getSubject(), message);
					} else {
						email.setMessage(email.getMessage().replace("${args}", ""));
					}
				}
			}
			recipients.addAll(inviteRecipients);
		}

		email.setProperty("senderuserid", inReq.getUser().getId());
		email.setProperty("senderfirstname", inReq.getUser().getFirstName());
		email.setProperty("senderlastname", inReq.getUser().getLastName());

		try {
			for (Recipient recipient : recipients) {
				email.setRecipient(recipient);
				email.send();
			}
		} catch (Exception e) {
			inReq.putPageValue("emailerror", e.getMessage());
		}
	}

	public void listPublicAlbums(WebPageRequest inReq) {
		EnterMedia matt = getEnterMedia(inReq);
		Searcher albumSearcher = matt.getAlbumSearcher();
		SearchQuery publicAlbumsQuery = albumSearcher.createSearchQuery();
		String username = inReq.getUserName();
		publicAlbumsQuery.setAndTogether(false);
		publicAlbumsQuery.addMatches("private", "false");
		publicAlbumsQuery.addMatches("owner", username);

		SearchQuery notSelectionsQuery = albumSearcher.createSearchQuery();
		notSelectionsQuery.addChildQuery(publicAlbumsQuery);
		notSelectionsQuery.addNot("id", "1");
		notSelectionsQuery.addNot("id", "2");
		notSelectionsQuery.addNot("id", "3");

		notSelectionsQuery.addSortBy("lastmodifiedDown");

		HitTracker tracker = albumSearcher.search(notSelectionsQuery);
		inReq.putPageValue("publicalbums", tracker);
	}

	public void reIndexAlbums(WebPageRequest inReq) {
		getEnterMedia(inReq).getAlbumSearcher().reIndexAll();
	}

	public void listMyAlbums(WebPageRequest inReq) {
		EnterMedia matt = getEnterMedia(inReq);
		AlbumSearcher albumSearcher = matt.getAlbumSearcher();
		albumSearcher.searchForAlbums(inReq.getUserName(), false, inReq);
	}

	// used on users home page
	public void listAllAlbums(WebPageRequest inReq) {
		String ownerid = inReq.getUserName();
		User owner = (User) inReq.getPageValue("owner");
		if (owner != null) {
			ownerid = owner.getUserName();
		}
		EnterMedia matt = getEnterMedia(inReq);
		AlbumSearcher albumSearcher = matt.getAlbumSearcher();
		albumSearcher.searchForAlbums(ownerid, true, inReq);

	}

	public void listAlbumsForAsset(WebPageRequest inReq) {
		String assetid = inReq.getRequestParameter("assetid");
		String catalogid = inReq.getRequestParameter("catalogid");
		if (assetid == null || catalogid == null) {
			return;
		}
		EnterMedia matt = getEnterMedia(inReq);
		Searcher albumSearcher = matt.getAlbumSearcher();
		SearchQuery query = albumSearcher.createSearchQuery();
		query.addMatches("asset", catalogid + "_" + assetid);
		HitTracker tracker = albumSearcher.search(query);
		inReq.putPageValue("assetalbums", tracker);
	}

	public PostMail getPostMail() {
		return fieldPostMail;
	}

	public void setPostMail(PostMail postMail) {
		fieldPostMail = postMail;
	}

	public void removePageOfHitsFromAlbum(WebPageRequest inReq) throws Exception {
		HitTracker hits = null;
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		if (hitssessionid != null) {
			hits = (HitTracker) inReq.getSessionValue(hitssessionid);
		}

		if (hits != null) {
			EnterMedia em = getEnterMedia(inReq);
			Album album = loadAlbum(inReq);
			if (album != null) {
				List<Asset> assets = new ArrayList<Asset>();
				for (Iterator iterator = hits.getPageOfHits().iterator(); iterator.hasNext();) {
					Object hit = (Object) iterator.next();
					String assetid = hits.getValue(hit, "id");
					String catalogid = hits.getValue(hit, "catalogid");
					Asset toremove = em.getAsset(catalogid, assetid);
					getAlbumSearcher(inReq).removeAssetFromAlbum(toremove, album, inReq);
					assets.add(toremove);
				}
				WebEvent event = createEvent(album, "removeitems", inReq, assets);
			}
		}
	}

	public Boolean checkAlbumPermissions(WebPageRequest inReq) throws Exception {
		Album album = loadAlbum(inReq);
		if (album == null) {
			return Boolean.FALSE;
		}
		String albumowner = album.getUserName();
		if (albumowner.equals(inReq.getUserName())) {
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	public void removeSelection(WebPageRequest inReq) throws Exception {
		MediaArchive archive = getMediaArchive(inReq);
		String[] assetids = inReq.getRequestParameters("assetselect_" + archive.getCatalogId());
		Album album = loadAlbum(inReq);

		if (album == null || assetids == null) {
			return;
		}

		List<Asset> assets = new ArrayList<Asset>();
		for (String assetid : assetids) {
			Asset toremove = archive.getAsset(assetid);
			getAlbumSearcher(inReq).removeAssetFromAlbum(toremove, album, inReq);
			assets.add(toremove);
		}
		WebEvent event = createEvent(album, "removeitems", inReq, assets);
	}

	public WebEventListener getWebEventListener() {
		return fieldWebEventListener;
	}

	public void setWebEventListener(WebEventListener webEventListener) {
		fieldWebEventListener = webEventListener;
	}

	public void checkParticipants(WebPageRequest inReq) throws Exception {
		Album album = loadAlbum(inReq);

		// check if the users email address is in email participants if it is
		// add them as a participant
		// and remove them from email participants
		if (album.removeEmailParticipant(inReq.getUser().getEmail())) {
			album.addParticipant(inReq.getUser());
			EnterMedia em = getEnterMedia(inReq);
			em.getAlbumSearcher().saveData(album, album.getUser());
		}
	}

	public void addParticipants(WebPageRequest inReq) throws Exception {
		// first we need to load the album
		Album album = loadAlbum(inReq);

		// get the list of participants
		ArrayList<User> participants = new ArrayList<User>();
		ArrayList<String> eparticipants = new ArrayList<String>();
		String sendto = inReq.getRequestParameter("postingsendto");
		if (sendto.equals("allfriends")) {
			// get all of this users friends and add them as participants
			String userid = inReq.getUser().getId();
			HitTracker friends = getEnterMedia(inReq).getFriendManager().getFriends(userid);
			for (Object friend : friends) {
				Data frienddata = (Data) friend;
				User frienduser = getUserManager().getUser(frienddata.get("friendid"));
				if (frienduser != null) {
					participants.add(frienduser);
				}
			}
		} else if (sendto.equals("selectfriends")) {
			String[] selectedfriends = inReq.getRequestParameters("selectedfriends");
			for (String friendid : selectedfriends) {
				User frienduser = getUserManager().getUser(friendid);
				if (frienduser != null) {
					participants.add(frienduser);
				}
			}

		} else if (sendto.equals("email")) {
			String emails = inReq.getRequestParameter("postingto");
			if (emails != null) {
				String[] splitaddresses = null;
				// check if the string contains a semicolon if so split on the
				// semicolon
				if (emails.indexOf(";") > -1) {
					splitaddresses = emails.split(";");
				}
				// no semicolons treat as all comma seperated list
				if (splitaddresses == null) {
					eparticipants.addAll(Arrays.asList(emails.split(",")));
				} else // check each split section for commas if so add split
						// results to results
				{
					for (int i = 0; i < splitaddresses.length; i++) {
						String addresses = splitaddresses[i].trim();
						if (addresses.indexOf(",") > -1) {
							eparticipants.addAll(Arrays.asList(addresses.split(",")));
						} else if (addresses.length() > 0) {
							eparticipants.add(addresses);
						}
					}
				}
			}
		}

		for (User user : participants) {
			album.addParticipant(user);
		}

		for (String email : eparticipants) {
			album.addEmailParticipant(email.trim());
		}
	}

	public void sendNewPostNotification(WebPageRequest inReq) throws Exception {
		// need to get an album
		Album album = loadAlbum(inReq);
		String addresses = inReq.getRequestParameter("emails");

		TemplateWebEmail email = getPostMail().getTemplateWebEmail();
		email.loadSettings(inReq);
		if (addresses != null) {
			sendShareToAddresses(inReq, addresses, email);
		} else {
			sendShareToParticipants(inReq, album, email);
		}
	}

	private void sendShareToParticipants(WebPageRequest inReq, Album album, TemplateWebEmail email) {
		Collection<User> participants = album.getParticipants();
		for (Iterator iterator = participants.iterator(); iterator.hasNext();) {
			User user = (User) iterator.next();
			if (user.getId() != inReq.getUser().getId()) {
				String emaila = user.getEmail();
				if (emaila != null) {
					email.setTo(emaila);
					email.send();
				}
			}
		}
	}

	private void sendShareToAddresses(WebPageRequest inReq, String addresses, TemplateWebEmail email) {
		EnterMedia entermedia = getEnterMedia(inReq);
		FriendManager friendManager = entermedia.getFriendManager();
		List<Recipient> recipients = email.setRecipientsFromUnknown(addresses);
		for (Recipient recipient : recipients) {
			String targetEmail = recipient.getEmailAddress();
			User targetUser = getUserManager().getUserByEmail(targetEmail);
			if (targetUser == null || !friendManager.isFriend(inReq.getUser().getId(), targetUser.getId())) {
				String inviteid = friendManager.nextInviteId();
				inReq.putPageValue("inviteid", inviteid);
				friendManager.saveInvite(inReq.getUser(), inviteid, recipient.getEmailAddress(), email.getSubject(), email.getMessage());
			}
			email.setTo(targetEmail);
			email.send();
		}
	}

}