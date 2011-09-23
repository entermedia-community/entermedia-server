package org.openedit.entermedia.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.upload.FileUpload;
import org.entermedia.upload.FileUploadItem;
import org.entermedia.upload.UploadRequest;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.EnterMedia;
import org.openedit.entermedia.MediaArchive;
import org.openedit.profile.UserProfile;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.ListHitTracker;
import com.openedit.page.PageProperty;
import com.openedit.page.PageSettings;
import com.openedit.users.User;
import com.openedit.users.UserPreferences;
import com.openedit.util.PathUtilities;

public class UserProfileModule extends BaseMediaModule
{
	protected XmlArchive fieldXmlArchive;
	protected FileUpload fieldFileUpload;
	private static final Log log = LogFactory.getLog(UserProfileModule.class);
	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}

	public User loadOwner(WebPageRequest inReq)
	{
		String id = inReq.getRequestParameter("userid");
		if( id == null)
		{
			id = inReq.getContentProperty("username");
		}
		if( id == null)
		{
			id = PathUtilities.extractDirectoryName( inReq.getPath() );
		}
		User user = getUserManager().getUser(id);
		inReq.putPageValue("owner", user);
		return user;
	}
	
	public User createOwnerHome(WebPageRequest inReq)
	{
		String id = inReq.getContentProperty("username");
		if(id == null)
		{
			id = inReq.getUser().getId();
		}
		User user = getUserManager().getUser(id);
		inReq.putPageValue("owner", user);
		
		String applicationid = inReq.getContentProperty("applicationid");
		
		String folderPath = "/" + applicationid + "/users/" + id;
		PageSettings home = getPageManager().getPageSettingsManager().getPageSettings(folderPath + "/_site.xconf");
		if( !home.exists() || home.getProperties().size() < 2) //should have two in there
		{
			// Add the user home
			PageProperty fb = new PageProperty("fallbackdirectory");
			
			
			
			fb.setValue("/${applicationid}/tools/user");
			home.putProperty(fb);

			PageProperty un = new PageProperty("username");
			un.setValue(id);
			home.putProperty(un);
						
			getPageManager().getPageSettingsManager().saveSetting(home);
			getPageManager().getPageSettingsManager().clearCache(home.getPath());
			getPageManager().clearCache(folderPath);
			//EnterMedia needs to reload fallback structure
			inReq.redirect(inReq.getPathUrlWithoutContext());
		}
		return user;
	}
	/**
	 * This is old and should not be used any more.... Use ProfileModule.loadUserProfile
	 * @param ex
	 * @return
	 * @deprecated
	 * @throws Exception
	 */
	public UserPreferences loadUserPreferences(WebPageRequest ex) throws Exception
	{
		String appid = ex.findValue("applicationid");
		if(appid == null){
			return null;
		}
		String id = appid + "usersettings" + ex.getUserName();
		UserPreferences prefs = (UserPreferences)ex.getSessionValue(id);
		if( prefs == null)
		{
			XmlFile xml = getXmlArchive().getXml("/WEB-INF/data/" + appid + "/users/" + ex.getUserName() + "/settings.xml");
			prefs = new UserPreferences();
			prefs.setUserData(xml);
			prefs.setUser(ex.getUser());
			prefs.setXmlArchive(getXmlArchive());
			List ok = new ArrayList();
			List okUpload = new ArrayList();
			
			Collection catalogs = getSearcherManager().getSearcher(appid, "catalogs").getAllHits();
			
			WebPageRequest uploadcheck = ex.copy();
			WebPageRequest catcheck = ex.copy();
	
			for (Iterator iterator = catalogs.iterator(); iterator.hasNext();)
			{
				Data cat = (Data) iterator.next();
				MediaArchive archive = getMediaArchive(cat.getId());

				archive.loadCategoryPermissions(catcheck);
				Boolean canview = (Boolean)catcheck.getPageValue("canview");
				if( canview != null && canview)
				{
					//if(catcheck.getPageValue("canviewasset") != null && (Boolean)catcheck.getPageValue("canviewasset"))
					ok.add(cat);
				}
				else
				{
					log.info("No access");
				}
				Boolean canupload = (Boolean)catcheck.getPageValue("canupload");
				if(canupload != null && canupload)
				{
					okUpload.add(cat);
				}
			}
			prefs.setCatalogs(new ListHitTracker(ok));
			prefs.setUploadCatalogs(new ListHitTracker(okUpload));
			ex.putSessionValue(id, prefs);
		}
		ex.putPageValue("usersettings", prefs);
		return prefs;
	}
	public void saveResultPreferences(WebPageRequest inReq) throws Exception
	{
		UserPreferences pref = loadUserPreferences(inReq);

		String[] resulttypes = inReq.getRequestParameters("resulttype");
		String[] newsettings = inReq.getRequestParameters("newresultview");
		String[] sortbys = inReq.getRequestParameters("sortby");
		String[] hitsperpage = inReq.getRequestParameters("hitsperpage");
		//View
		String oldresulttype = inReq.getRequestParameter("oldresulttype");
		if( !"default".equals(oldresulttype ))
		{
			pref.removeSearchType(oldresulttype);
		}
		
		for(int i =0; i<resulttypes.length;i++)
		{
			if(newsettings != null)
			{
				pref.setResultViewPreference(resulttypes[i], newsettings[i]);
			}
			if(sortbys != null)
			{
				pref.setSortForSearchType(resulttypes[i], sortbys[i]);
			}
			if(hitsperpage != null)
			{
				int hpp = Integer.parseInt(hitsperpage[i]);
				pref.setHitsPerPageForSearchType(resulttypes[i], hpp);
			}
		}
		pref.save();
		
		String sid = inReq.getRequestParameter("hitssessionid");
		if( sid != null)
		{
			HitTracker hits = (HitTracker)inReq.getSessionValue(sid);
			
			if( hits != null)
			{
				String currentview = hits.getResultType();
				//TODO: maybe these should all be re-loaded in velocity?
				hits.getSearchQuery().setSortBy(pref.getSortForSearchType(currentview));
				hits.setHitsPerPage(pref.getHitsPerPageForSearchType(currentview));
				hits.setIndexId(String.valueOf(System.currentTimeMillis()));
				Searcher searcher = getSearcherManager().getSearcher(hits.getCatalogId(), "asset");
				searcher.cachedSearch(inReq, hits.getSearchQuery());
			}
		}
		
		
		
	}
	

/*		
		public void toggleView(WebPageRequest inReq) throws Exception {
			User user = inReq.getUser();
			// String current = user.get("resulttype");

			String use = inReq.getRequestParameter("resulttype");

			// legacy support for data-mining
			if (use == null) {
				use = user.get("resulttype");
				if ("table".equals(use)) {
					user.setProperty("resulttype", "icon");
				} else {
					user.setProperty("resulttype", "table");
				}
				return;
			}
			// end legacy support section

			String catalogid = inReq.findValue("catalogid");
			HitTracker lht = getSearcherManager().getList(catalogid, "resulttype");
			String hitsperpage = null;
			if (lht != null) {
				ElementData temp = (ElementData) lht.getById(use);
				if (temp != null) {
					hitsperpage = temp.get("hitsperpage");
				}
			}
			if (user != null) {
				user.setProperty("resulttype", use);
				user.setProperty("hitsperpage", hitsperpage);
				getUserManager().saveUser(user);
			}
			inReq.setRequestParameter("hitsperpage", hitsperpage);

			HitTracker ht = loadHits(inReq);
			if (ht == null) {
				return;
			} else {
				ht.setResultsView(use);
				ht.setHitsPerPage(Integer.parseInt(hitsperpage));
				inReq.putSessionValue(ht.getSessionId(), ht);
			}
		}
*/
	
	public void changeHitsPerPage(WebPageRequest inReq) throws Exception
	{
		String count = inReq.getRequestParameter("count");
		if (count != null)
		{
			// MediaArchive archive = getMediaArchive(inReq);
			// archive.setHitsPerPage(Integer.parseInt(count));
			inReq.getUser().setProperty("hitsperpage", count);
		}
	}

	public void receivePortraitUpload(WebPageRequest inReq)
	{
		//need to generate the correct path to save the file
		UploadRequest map = getFileUpload().parseArguments(inReq);
		if (map == null || map.getUploadItems().size() == 0)
		{
			throw new OpenEditException("No upload included");
		}
		EnterMedia entermedia = getEnterMedia(inReq);
		
		String path = "/WEB-INF/data/media/catalogs/public/originals/users/" + inReq.getUser().getId();
		//inReq.setRequestParameter("path", path);
		FileUploadItem item = map.getFirstItem();
		//if(item.getName().toLowerCase().endsWith(".jpg"))
		//{
			map.saveFileAs(item, path + "/" + item.getName(), inReq.getUser());
			MediaArchive archive = getMediaArchive( "media/catalogs/public");
			String sourcePath = "users/" + inReq.getUser().getId();
			Asset asset = archive.getAssetBySourcePath(sourcePath);
			if(asset == null)
			{
				asset = archive.createAsset(sourcePath);
			}
			archive.getAssetImporter().getAssetUtilities().populateCategory(asset, archive, "/WEB-INF/data/media/catalogs/public/originals", path, inReq.getUser());
			asset.setPrimaryFile(item.getName());
			archive.removeGeneratedImages(asset);
			archive.saveAsset(asset, inReq.getUser());
			User user = inReq.getUser();
			user.setProperty("hasportrait", "true");
			getUserManager().saveUser(user);
		//}
		
	}

	public FileUpload getFileUpload()
	{
		return fieldFileUpload;
	}

	public void setFileUpload(FileUpload inFileUpload)
	{
		fieldFileUpload = inFileUpload;
	}

	public void toggleDebug(WebPageRequest inReq) 
	{
		String mode = inReq.getUser().get("oe.edit.mode");
		if( mode == null || mode.equals("preview"))
		{
			inReq.getUser().put("oe.edit.mode","debug");
		}
		else
		{
			inReq.getUser().put("oe.edit.mode","preview");
		}
		//redirectBack(inReq);		
	}
}
