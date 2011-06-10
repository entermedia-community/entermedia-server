/*
 * Created on Jul 2, 2006
 */
package org.openedit.entermedia.albums;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.entermedia.EnterMedia;
import org.openedit.repository.ContentItem;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;
import com.openedit.util.IntCounter;

public class AlbumArchive
{
	private static final Log log = LogFactory.getLog(AlbumArchive.class);

	protected PageManager fieldPageManager;
	protected XmlArchive fieldXmlArchive;
	protected UserManager fieldUserManager;
	protected Map fieldCache;
	protected String fieldApplicationId;
	protected EnterMedia fieldEnterMedia;
	protected IntCounter fieldIntCounter;
	protected ModuleManager fieldModuleManager;
	
	public IntCounter getIntCounter()
	{
		if (fieldIntCounter == null)
		{
			fieldIntCounter = new IntCounter();

			ContentItem item = getPageManager().getRepository().get("/WEB-INF/data/" + getEnterMedia().getApplicationId() + "/albums/idcounter.properties");

			File file = new File(item.getAbsolutePath());
			fieldIntCounter.setCounterFile(file);
		}

		return fieldIntCounter;
	}

	public AlbumSelectionList listAlbums(String inUser)
	{
		User user = getUserManager().getUser(inUser);
		return listAlbums(user);
	}

	public AlbumSelectionList listAlbums(User inUser)
	{
		AlbumSelectionList albums = new AlbumSelectionList();
		albums.setUser(inUser);
		
		// User albums
		String homepath = "/WEB-INF/data/" + getEnterMedia().getApplicationId() + "/users/" + inUser.getUserName() + "/albums/";
		List folders = getPageManager().getChildrenPaths(homepath);
		for (Iterator iterator = folders.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			Page folder = getPageManager().getPage(path);
			if (folder.isFolder())
			{
				String dataPath = path + "/data.xml";
				Page data = getPageManager().getPage(dataPath);
				if (data.exists())
				{
					Album album = loadAlbum(dataPath);
					if (album != null && album.getId() != null)
					{
						albums.addAlbum(album);
					}
					else
					{
						log.error("Couldn't load album: " + path);
					}
				}
			}
		}
		
		// Selection albums
		int selections = 3;
		String number = getPageManager().getPage("/" + getEnterMedia().getApplicationId() + "/").getProperty("selectionalbums");
		if (number != null)
		{
			selections = Integer.parseInt(number);
		}
		
		if(albums.getSelectionAlbums().size() < selections)
		{
			for(int i = 0; i < selections; i++)
			{
				if (albums.getSelectionAlbum(String.valueOf(i+1)) == null)
				{
					save(createSelection(albums, String.valueOf(i+1)), albums.getUser());
				}
			}
		}
		albums.sortSelections();
		
		return albums;
	}

	protected Album createSelection(AlbumSelectionList inList, String inId)
	{
		Album album = new Album();
		album.setAlbumSearcher(getEnterMedia().getAlbumSearcher());
		album.setId(inId);
		album.setName(inId);
		album.setUser(inList.getUser());
		album.setSelection(true);
		inList.addAlbum(album);
		return album;
	}

	
	public void save(Album inAlbum, User inSavedBy) throws OpenEditException
	{
		Element root = DocumentHelper.createElement("album");
		root.addAttribute("id", inAlbum.getId());
		saveProperties(root, inAlbum.getProperties());
		root.addAttribute("selection", String.valueOf(inAlbum.isSelection()));
		if (inAlbum.getUser() == null)
		{
			inAlbum.setUser(inSavedBy);
		}
		root.addAttribute("user", inAlbum.getUser().getUserName());
		if (inSavedBy.getUserName().equals(inAlbum.getUser().getUserName()))
		{
			root.addAttribute("name", inAlbum.getName());
		}
		
		Element participants = root.addElement("participants");
		for (Iterator iterator = inAlbum.getParticipants().iterator(); iterator.hasNext();)
		{
			User user = (User) iterator.next();
			Element child = participants.addElement("participant");
			child.addAttribute("userid", user.getId());
		}
		
		Element emailparticipants = root.addElement("emailparticipants");
		for (Iterator iterator = inAlbum.getEmailParticipants().iterator(); iterator.hasNext();)
		{
			String email = (String) iterator.next();
			Element child = emailparticipants.addElement("emailparticipant");
			child.addAttribute("email", email);
		}
		
		XmlFile file = new XmlFile();
		file.setRoot(root);
		String dataPath = buildPath(inAlbum.getId(), inAlbum.getUser().getUserName());
		file.setPath(dataPath);

		getXmlArchive().saveXml(file, inSavedBy);

/*
		PageSettings home = getPageManager().getPageSettingsManager().getPageSettings(folderPath + "/_site.xconf");
		// Add the user home
		String fallback =  home.getPropertyValue("albumid", null);
		if( fallback == null || !fallback.equals(inAlbum.getId()))
		{
			PageSettings userhome = home.getParent().getParent();
			PageProperty un = new PageProperty("username");
			un.setValue(inAlbum.getUser().getUserName());
			userhome.putProperty(un);
			PageProperty homefb = new PageProperty("fallbackdirectory");
			homefb.setValue("/${applicationid}/tools/user");
			userhome.putProperty(homefb);
			getPageManager().getPageSettingsManager().saveSetting(userhome);
	
			PageProperty fb = new PageProperty("albumid");
			home.putProperty(fb);
			fb.setValue(inAlbum.getId());
			getPageManager().getPageSettingsManager().saveSetting(home);
			getPageManager().getPageSettingsManager().clearCache(home.getPath());
			getPageManager().clearCache(folderPath);
		}
		*/
	}

	private String buildPath(String inId, String inUserName)
	{
		return "/WEB-INF/data/" + getApplicationId() + "/users/" + inUserName + "/albums/" + inId + ".xml";
	}

	public String buildPath(Album inAlbum)
	{
		return buildPath(inAlbum.getId(), inAlbum.getUserName());
	}

	private void saveProperties(Element parent, Map properties)
	{
		for (Iterator propiter = properties.keySet().iterator(); propiter.hasNext();)
		{

			String key = (String) propiter.next();
			String value = (String) properties.get(key);
			if (value != null)
			{
				Element prop = parent.addElement("property");
				prop.addAttribute("id", key);
				prop.addCDATA(value);
			}
		}
	}

	public Album loadAlbumInPath(Page inPath)
	{
		String albumid = inPath.getProperty("albumid");
		String ownerid = inPath.getProperty("username");
		return loadAlbum(albumid, ownerid);
	}
	public Album loadAlbum(String inAlbumId, String inUserName)
	{
		String path = buildPath(inAlbumId, inUserName);
		return loadAlbum(path);
	}

	public Album loadAlbum(String path) throws OpenEditException
	{
		XmlFile file = getXmlArchive().loadXmlFile(path);
		Album album = (Album) getCache().get(path);

		if (file == null)
		{
			file = getXmlArchive().getXml(path); // might have beed edited
		}
		else if (album != null)
		{
			return album;
		}
		if (album == null)
		{
			album = new Album();
			album.setAlbumSearcher(getEnterMedia().getAlbumSearcher());
		}
		
		if (file.isExist())
		{
			populateAlbum(album, file);
		}
		else
		{
			return null;
		}
		getCache().put(path, album);
		return album;
	}

	protected void populateAlbum(Album inAlbum, XmlFile file)
	{
		Element child = file.getRoot();
		String colid = child.attributeValue("id");
		inAlbum.setId(colid);
		inAlbum.setName(child.attributeValue("name"));
		String owner = child.attributeValue("user");
		User user = getUserManager().getUser(owner);
		inAlbum.setUser(user);
		inAlbum.setLastModified(new Date( file.getLastModified() ));
		String selection = child.attributeValue("selection");
		if (selection != null)
		{
			inAlbum.setSelection(Boolean.parseBoolean(selection));
		}
		else  //Remove by 8/3/2009
		{
			inAlbum.setSelection("1".equals(colid) || "2".equals(colid) || "3".equals(colid));
		}

		for (Iterator propiter = child.elementIterator("property"); propiter.hasNext();)
		{
			Element property = (Element) propiter.next();
			String propid = property.attributeValue("id");
			String propvalue = property.getText();
			inAlbum.addProperty(propid, propvalue);
		}
		Element participants = child.element("participants");
		if( participants != null)
		{
			for (Iterator piter = participants.elementIterator("participant"); piter.hasNext();)
			{
				Element property = (Element) piter.next();
				String propid = property.attributeValue("userid");
				inAlbum.addParticipant(getUserManager().getUser(propid));
			}
		}
		Element emailparticipants = child.element("emailparticipants");
		if( emailparticipants != null)
		{
			for (Iterator piter = emailparticipants.elementIterator("emailparticipant"); piter.hasNext();)
			{
				Element property = (Element) piter.next();
				String propid = property.attributeValue("email");
				inAlbum.addEmailParticipant(propid);
			}
		}
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public XmlArchive getXmlArchive()
	{
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive)
	{
		fieldXmlArchive = inXmlArchive;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager; // );

	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	protected Map getCache()
	{
		if (fieldCache == null)
		{
			fieldCache = new HashMap();
		}

		return fieldCache;
	}

	public String getApplicationId()
	{
		return fieldApplicationId;
	}

	public void setApplicationId(String inApplicationId)
	{
		fieldApplicationId = inApplicationId;
	}

	public EnterMedia getEnterMedia()
	{
		if (fieldEnterMedia == null)
		{
			fieldEnterMedia = (EnterMedia) getModuleManager().getBean(getApplicationId(), "enterMedia");
		}
		return fieldEnterMedia;
	}

	public void setEnterMedia(EnterMedia inMatt)
	{
		fieldEnterMedia = inMatt;
	}
	public Album createAlbum()
	{
		int count = getIntCounter().incrementCount();
		String id = String.valueOf(count);
		return createAlbum(id);
	}
	
	public Album createAlbum(String inId)
	{
		Album album = new Album();
		album.setId(inId);
		album.setAlbumSearcher(getEnterMedia().getAlbumSearcher());
		album.setLastModified(new Date());
		return album;
	}

	public void deleteAlbum(String inId, User inUser)
	{
		Album album = loadAlbum(inId, inUser.getUserName());
		if(album == null)
		{
			return;
		}
				
		String path = buildPath(inId, inUser.getUserName());
		Page page = getPageManager().getPage(path);
		getPageManager().removePage(page);
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
}
