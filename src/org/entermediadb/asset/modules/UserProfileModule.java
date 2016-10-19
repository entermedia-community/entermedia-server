package org.entermediadb.asset.modules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.EnterMedia;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.search.AssetSearcher;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.page.PageProperty;
import org.openedit.page.PageSettings;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;
import org.openedit.xml.XmlArchive;

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
		User user = getUserManager(inReq).getUser(id);
		inReq.putPageValue("owner", user);
		return user;
	}
	
	
	public User loadOwnerProfile(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		User user = loadOwner(inReq);
		Searcher profilesearcher =getSearcherManager().getSearcher(archive.getCatalogId(), "profile");
		Data profile = (Data) profilesearcher.searchById(user.getUserName());
		inReq.putPageValue("profile", profile);
		return user;
	}
	
	
	public User createOwnerHome(WebPageRequest inReq)
	{
		String id = inReq.getContentProperty("username");
		if(id == null)
		{
			id = inReq.getUser().getId();
		}
		User user = getUserManager(inReq).getUser(id);
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
	
	public void changeUserTemplate(WebPageRequest inReq){
		String templateid = inReq.getRequestParameter("template.value");
		String applicationid = inReq.getContentProperty("applicationid");
		String folderPath = "/" + applicationid + "/users/" + inReq.getUserName();
		PageSettings home = getPageManager().getPageSettingsManager().getPageSettings(folderPath + "/_site.xconf");
		
		PageProperty template = new PageProperty("usertheme");
		
		
		template.setValue(templateid);
		home.putProperty(template);

		getPageManager().getPageSettingsManager().saveSetting(home);
		getPageManager().getPageSettingsManager().clearCache(home.getPath());
		getPageManager().clearCache(folderPath);
		
		
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
			getUserManager(inReq).saveUser(user);
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
		String mode = inReq.getUser().get("oe_edit_mode");
		if( mode == null || mode.equals("preview"))
		{
			inReq.getUser().setValue("oe_edit_mode","debug");
		}
		else
		{
			inReq.getUser().setValue("oe_edit_mode","preview");
		}
		//redirectBack(inReq);		
	}
	
	
	public void searchOwnerAssets(WebPageRequest inReq){
		User owner = loadOwner(inReq);
		MediaArchive archive =getMediaArchive(inReq);
		AssetSearcher searcher = archive.getAssetSearcher();
		
	}
}
