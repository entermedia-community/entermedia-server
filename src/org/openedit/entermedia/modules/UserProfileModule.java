package org.openedit.entermedia.modules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermedia.upload.FileUpload;
import org.entermedia.upload.FileUploadItem;
import org.entermedia.upload.UploadRequest;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.EnterMedia;
import org.openedit.entermedia.MediaArchive;
import org.openedit.xml.XmlArchive;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.page.PageProperty;
import com.openedit.page.PageSettings;
import com.openedit.users.User;
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
