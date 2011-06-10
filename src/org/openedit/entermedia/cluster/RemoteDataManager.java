package org.openedit.entermedia.cluster;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.util.SyncFileDownloader;
import org.openedit.util.DateStorageUtil;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.openedit.OpenEditException;
import com.openedit.page.manage.PageManager;
import com.openedit.users.User;
import com.openedit.users.UserManager;

public class RemoteDataManager
{
	private static final Log log = LogFactory.getLog(LocalDataManager.class);
	protected LocalDataManager fieldLocalDataManager;
	protected UserManager fieldUserManager;
	protected PageManager fieldPageManager;
	protected File fieldRoot;
	protected SearcherManager fieldSearcherManager;
	protected XmlArchive fieldXmlArchive;
	
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
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public File getRoot()
	{
		return fieldRoot;
	}

	public void setRoot(File inRoot)
	{
		fieldRoot = inRoot;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public Map getSyncFileDownloaders()
	{
		if (fieldSyncFileDownloaders == null)
		{
			fieldSyncFileDownloaders = new HashMap();
		}
		return fieldSyncFileDownloaders;
	}

	public static Log getLog()
	{
		return log;
	}

	protected Map fieldSyncFileDownloaders;
	
	public LocalDataManager getLocalDataManager()
	{
		return fieldLocalDataManager;
	}

	public void setLocalDataManager(LocalDataManager inLocalDataManager)
	{
		fieldLocalDataManager = inLocalDataManager;
	}

	public Data findNextRemoteServer(String inAppId, String inSelfSiteRoot)
	{
		Searcher clusters = getSearcherManager().getSearcher(inAppId, "cluster");
		//String thishost = inReq.getSiteRoot();
		for (Iterator iterator = clusters.getAllHits().iterator(); iterator.hasNext();)
		{
			Data remoteserver = (Data) iterator.next();
			String status =remoteserver.get("status");
			if( "active".equals(status))
			{
				String host = remoteserver.get("siteroot");
				if(!inSelfSiteRoot.equals(host))
				{
					return remoteserver;
				}
			}
		}
		return null;
	}

	public Element importChanges(String inAppId, Data inServer)
	{
		
		//TODO: Look up local change info
		XmlFile xml = getXmlArchive().getXml("/WEB-INF/data/" + inAppId + "/.versions/clusterstatus.xml");
		Element data = xml.getElementById(inServer.getId());
		if( data == null)
		{
			data = xml.addNewElement();
			data.addAttribute("id", inServer.getId());
		}
		String lastchecked = data.attributeValue("lastchecked");
		Date lastCheckDate = null;
		if( lastchecked == null)
		{
			//assume 3 days?
			GregorianCalendar cal = new GregorianCalendar();
			cal.add(Calendar.DAY_OF_YEAR, -3);
			lastchecked = DateStorageUtil.getStorageUtil().formatForStorage(cal.getTime());
		}
		Element changes = listRemoteChanges(inAppId, inServer, lastchecked);
		
		Date now = new Date();
		importChanges(inAppId, inServer,changes);
		String date =  DateStorageUtil.getStorageUtil().formatForStorage(now);
		data.addAttribute("lastchecked", date);
		getXmlArchive().saveXml(xml, null);
		return changes;
	}

	public Element listRemoteChanges(String inAppId,Data inServer, String inLastCheckDate)
	{
		SyncFileDownloader downloader = getSyncFileDownloader(inAppId, inServer);
		
		downloader.setLastChecked(inLastCheckDate);
		Element changes = downloader.listRemoteChanges("/WEB-INF/data/" + inAppId);
		return changes;
	}

	protected SyncFileDownloader getSyncFileDownloader(String inAppId, Data inServer)
	{
		String id = inAppId  + "_" + inServer.getId();
		SyncFileDownloader downloader = (SyncFileDownloader)getSyncFileDownloaders().get(id);
		if( downloader == null)
		{
			downloader = new SyncFileDownloader();
			downloader.setServerUrl(inServer.get("siteroot"));
			downloader.setListXml("/" + inAppId + "/services/rest/listdatachanges.xml");
			downloader.setLoginPath("/" + inAppId + "/services/rest/login.xml");
			downloader.setDownloadPath("/" + inAppId + "/services/rest/downloadfiles.zip");
			downloader.setSyncPath( "/WEB-INF/data/" + inAppId );
			downloader.setRoot(getRoot());
			downloader.setPageManager(getPageManager());
			downloader.setUsername("admin");
			User user = getUserManager().getUser("admin");
			downloader.setPassword(user.getPassword());
			//				for (Iterator iter = inReq.getCurrentAction().getConfig().getChildIterator("exclude"); iter.hasNext();) 
			//				{
			//					Configuration exclude = (Configuration) iter.next();
			//					server.addExclude(exclude.getValue());
			//				}
			getSyncFileDownloaders().put(id,downloader);
			downloader.login();
		}
		return downloader;
	}

	public void importChanges(String inAppId, Data inServer, Element inRootChanges)
	{
		log.info(inRootChanges);
	}
}
