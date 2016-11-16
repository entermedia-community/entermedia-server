/*
Copyright (c) 2004 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
 */

/*
 * Created on Feb 17, 2004
 *
 * Updated by: 	Todd Fisher\Christopher Burke
 * Date:		6/27/2005
 * Comments:	updated updateProject to download and save install.xml and run ANT shell script
 */
package org.entermediadb.modules.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.FileUploadItem;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.email.PostMail;
import org.entermediadb.email.Recipient;
import org.entermediadb.email.TemplateWebEmail;
import org.entermediadb.modules.scriptrunner.ScriptModule;
import org.entermediadb.scripts.Script;
import org.entermediadb.scripts.ScriptLogger;
import org.entermediadb.scripts.ScriptManager;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.PlugIn;
import org.openedit.WebPageRequest;
import org.openedit.config.Configuration;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.page.PageSettings;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.FileUtils;
import org.openedit.util.OutputFiller;
import org.openedit.util.PathUtilities;
import org.openedit.util.WindowsUtil;
import org.openedit.util.XmlUtil;
import org.openedit.util.ZipUtil;

/**
 * This module allows users to update their version of OpenEdit.
 * 
 * @author dbrown
 */
public class UpdateModule extends BaseMediaModule {
	protected Map fieldPlugInFinders;
	protected PostMail fieldPostMail;
	protected ScriptManager fieldScriptManager;
	public ScriptManager getScriptManager()
	{
		if( fieldScriptManager == null)
		{
			fieldScriptManager = (ScriptManager)getModuleManager().getBean("scriptManager");
		}
		return fieldScriptManager;
	}
	public void setScriptManager(ScriptManager inScriptManager)
	{
		fieldScriptManager = inScriptManager;
	}

	private static final Log log = LogFactory.getLog(UpdateModule.class);
	protected WindowsUtil fieldWindowsUtil;

	public WindowsUtil getWindowsUtil() {
		return fieldWindowsUtil;
	}

	public void setWindowsUtil(WindowsUtil inWindowsUtil) {
		fieldWindowsUtil = inWindowsUtil;
	}

	// public void listPageValues(WebPageRequest inReq) throws Exception {
	// List allModules = new ArrayList();
	//
	// List sortedNames = new ArrayList(inReq.getPageMap().keySet());
	// Collections.sort(sortedNames);
	//
	// for (int i = 0; i < sortedNames.size(); i++) {
	//
	// String name = (String) sortedNames.get(i);
	// Bean bean = new Bean();
	// bean.setName(name);
	// Object obj = inReq.getPageValue(name);
	//
	// if (obj instanceof String) {
	// bean.setValue((String) obj);
	// }
	//
	// bean.setBeanDefinition(obj.getClass());
	// allModules.add(bean);
	// }
	//
	// inReq.putPageValue("allPageValues", allModules);
	// }

	public List loadSiteList(WebPageRequest inReq) throws Exception {
		Page sites = getPageManager().getPage("/openedit/update/sites.xml");
		Element root = new XmlUtil().getXml(sites.getReader(),
				sites.getCharacterEncoding());
		List all = new ArrayList();
		for (Iterator iter = root.elementIterator("site"); iter.hasNext();) {
			Element child = (Element) iter.next();
			Site site = new Site();
			site.setId(child.attributeValue("id"));
			site.setText(child.attributeValue("text"));
			site.setHref(child.attributeValue("href"));
			all.add(site);
		}

		List dirs = new ArrayList();
		dirs.add("/");
		List names = getPageManager().getChildrenNames("/");
		for (Iterator iterator = names.iterator(); iterator.hasNext();) {
			String path = (String) iterator.next();
			Page dir = getPageManager().getPage(path);
			if (dir.isFolder()) {
				dirs.add(path);
			}
		}
		inReq.putPageValue("dirs", dirs);

		inReq.putPageValue("sites", all);
		return all;
	}

	public Site selectSite(WebPageRequest inReq, Data inSync) throws Exception {

		String url = inSync.get("siteurl");
		if (url == null) {
			return null;
		}
		Site selected = new Site();
		selected.setHref(url);
		selected.setId(url);
		// set password
		if (selected != null) {
			String username = inSync.get("accountname");
			String password = getUserManager(inReq).getUser(username).getPassword();
			selected.setUsername(username);
			selected.setPassword(password);
		}
		inReq.putPageValue("sitetopush", selected);
		return selected;
	}

	// public void pushDirectory(WebPageRequest inReq) throws Exception {
	// Site toUpgrade = selectSite(inReq);
	// inReq.setRequestParameter("name", "pushed to " + toUpgrade.getId());
	// File backup = backUpDirectory(inReq);
	//
	// String url = toUpgrade.getHref();
	// if (!url.startsWith("http://")) {
	// url = "http://" + url;
	// }
	//
	// if (!url.endsWith("/")) {
	// url += "/";
	// }
	// url += "openedit/update/receivepush.html";
	// log.info("posting here:" + url);
	//
	// PostMethod postMethod = new PostMethod(url);
	//
	// Part[] parts = { new StringPart("username", toUpgrade.getUsername()), new
	// StringPart("password", toUpgrade.getPassword()), new
	// StringPart("savedas", backup.getName()),
	// new FilePart("file", backup) };
	//
	// postMethod.setRequestEntity(new MultipartRequestEntity(parts,
	// postMethod.getParams()));
	//
	// HttpClient client = new HttpClient();
	// // client.getHttpConnectionManager().getParams().setConnectionTimeout(0);
	// int statusCode1 = client.executeMethod(postMethod);
	// postMethod.releaseConnection();
	// if (statusCode1 == 200) {
	// inReq.putPageValue("message", "Push is completed.");
	// } else {
	// inReq.putPageValue("message", "Status code: <b>" + statusCode1 +
	// "</b><br>" + postMethod.getResponseBodyAsString());
	// }
	// }

	public File backUpDirectory(WebPageRequest inReq) throws Exception {
		Backup backup = new Backup();
		for (Iterator iter = inReq.getCurrentAction().getConfig()
				.getChildIterator("exclude"); iter.hasNext();) {
			Configuration exclude = (Configuration) iter.next();
			backup.addExclude(exclude.getValue());
		}
		backup.setRoot(getRoot());
		String subdir = inReq.getRequiredParameter("directory");
		backup.setIncludePath(subdir);
		backup.setPageManager(getPageManager());

		// String backupName = inReq.getRequiredParameter("name");
		String backupName = inReq.getUserName() + subdir;
		File results = backup.backupCurrentSite(backupName);
		inReq.putPageValue("result", results);
		return results;
	}

	public List listVersions(WebPageRequest inReq) throws Exception {
		Backup backup = new Backup();
		backup.setRoot(getRoot());
		backup.setPageManager(getPageManager());
		List versions = backup.listSiteVersions();
		inReq.putPageValue("versionlist", versions);
		return versions;
	}

	public void restoreVersion(WebPageRequest inReq) throws Exception {
		String name = inReq.getRequestParameter("versionid");
		if (name != null) {
			// check that we are logged in
			Backup backup = new Backup();
			backup.setRoot(getRoot());
			backup.setPageManager(getPageManager());
			File version = backup.loadVersion(name);
			if (version == null) {
				log.error("No such backup found " + name);
				inReq.putPageValue("error", "No such file");
			} else {
				backup.restoreBackup(version);
			}
		} else {
			log.error("No versionid parameter");
		}

	}

	public void receivePush(WebPageRequest inReq) throws Exception {
		// this is an upload then a restore
		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inReq);
		if (properties == null) {
			log.error("Nothing posted");
			return;
		}
		String username = inReq.getRequestParameter("username");
		String password = inReq.getRequestParameter("password");

		User admin = getUserManager(inReq).getUser(username);
		if (!getUserManager(inReq).authenticate(admin, password)) {
			throw new OpenEditException("Did not authenticate: " + username);
		} else {
			inReq.setUser(admin);
		}
		String id = (String) inReq.getRequestParameter("savedas");
		if (id != null && id.length() > 0) {
			String path = "/WEB-INF/versions/" + id;
			Page page = properties.saveFirstFileAs(path, inReq.getUser());
			if (page.exists() && page.getContentItem().getLength() > 5000) {
				// TODO: Make sure we can unzip it
				// backup
				// backUpDirectory(inReq); //Restore now keeps old copies in the
				// WEB-INF/trash directory
				inReq.setRequestParameter("versionid", id);
				restoreVersion(inReq);
			} else {
				log.error("Page did not save " + path);
			}
		} else {
			log.error("No ID found");
		}
	}

	public void changeLayout(WebPageRequest inReq) throws Exception {
		checkLogin(inReq);
		String zip = inReq.getRequestParameter("ziplocation");
		if (zip == null) {
			return;
		}
		URL url = new URL(zip);
		InputStream input = url.openStream();

		// save to the tmp directory?
		String name = PathUtilities.extractPageName(zip);
		File tmp = File.createTempFile("layoutzip", name);

		OutputFiller filler = new OutputFiller();
		FileOutputStream out = new FileOutputStream(tmp);
		filler.fill(input, out);
		out.close();
		input.close();

		// UNZIP
		ZipUtil ziputil = new ZipUtil();
		File destination = new File(getRoot(), "/WEB-INF/base/layouts/");
		destination.mkdirs();
		ziputil.unzip(tmp, destination);

		// CHECK THE BASE
		// PageSettings settings =
		// getPageManager().getPageSettingsManager().getPageSettings("/layouts/_site.xconf");
		// PageProperty fall = settings.getFieldProperty("fallbackdirectory");
		// if( fall == null)
		// {
		// fall = new PageProperty("fallbackdirectory");
		// fall.setValue("/WEB-INF/base/layouts");
		// settings.putProperty(fall);
		// getPageManager().getPageSettingsManager().saveSetting(settings);
		// }
		// CHANGE LAYOUT
		PageSettings topsettings = getPageManager().getPageSettingsManager()
				.getPageSettings("/_site.xconf");

		topsettings.setInnerLayout("/layouts/" + name + "/layout.html");
		getPageManager().getPageSettingsManager().saveSetting(topsettings);
		inReq.putPageValue("layoutname", name);

		// reload the content page settings
		Page content = getPageManager().getPage(
				inReq.getContentPage().getPath(), true);
		inReq.getContentPage().setPageSettings(content.getPageSettings());
		getPageManager().clearCache();
		// TODO: Create the directory structure for them to change stuff
	}

	public void changeTheme(WebPageRequest inReq) throws Exception {
		checkLogin(inReq);
		String zip = inReq.getRequestParameter("ziplocation");
		if (zip == null) {
			return;
		}
		URL url = new URL(zip);
		InputStream input = url.openStream();

		// save to the tmp directory?
		String name = PathUtilities.extractPageName(zip);
		File tmp = File.createTempFile("layoutzip", name);

		OutputFiller filler = new OutputFiller();
		FileOutputStream out = new FileOutputStream(tmp);
		filler.fill(input, out);
		out.close();
		input.close();

		// UNZIP
		ZipUtil ziputil = new ZipUtil();
		File destination = new File(getRoot(), "/WEB-INF/base/themes/");
		destination.mkdirs();
		ziputil.unzip(tmp, destination);

		// CHANGE LAYOUT
		PageSettings topsettings = getPageManager().getPageSettingsManager()
				.getPageSettings("/_site.xconf");
		// topsettings.
		topsettings.removeProperty("themeprefix");
		PageProperty skin = new PageProperty("themeprefix");
		skin.setValue("/themes/" + name);
		topsettings.putProperty(skin);

		topsettings.setInnerLayout("${themeprefix}/layouts/layout.html");
		getPageManager().getPageSettingsManager().saveSetting(topsettings);
		inReq.putPageValue("layoutname", name);

		// reload the content page settings
		getPageManager().clearCache();
		// Page content =
		// getPageManager().getPage(inReq.getContentPage().getPath(),true);
		// inReq.getContentPage().setPageSettings(content.getPageSettings());

		new FileUtils().deleteAll(tmp);
		// TODO: Create the directory structure for them to change stuff
		inReq.redirect("/openedit/editors/themes/finished.html");
	}

	
	public void saveSync(WebPageRequest inReq) {
		String applicationid = inReq.getRequestParameter("applicationid");
		Searcher syncSearcher = getSearcherManager().getSearcher(applicationid,
				"sync");
		String syncid = inReq.getRequestParameter("syncid");
		Data existingsync = (Data) syncSearcher.searchById(syncid);
		if (existingsync == null) {
			existingsync = syncSearcher.createNewData();
		}
		String username = inReq.getRequestParameter("accountname");
		String siteurl = inReq.getRequestParameter("siteurl");
		String exclude = inReq.getRequestParameter("exclude");
		String syncpath = inReq.getRequestParameter("syncpath");
		String name = inReq.getRequestParameter("name");

		existingsync.setProperty("accountname", username);
		existingsync.setProperty("siteurl", siteurl);
		existingsync.setProperty("exclude", exclude);
		existingsync.setProperty("syncpath", syncpath);
		existingsync.setProperty("name", name);
		syncSearcher.saveData(existingsync, inReq.getUser());
		saveSyncEvents(inReq);
	}

	public void saveSyncEvents(WebPageRequest inReq) {
		String applicationid = inReq.findValue("applicationid");
		Searcher syncSearcher = getSearcherManager().getSearcher(applicationid,
				"sync");
		for (Iterator iterator = syncSearcher.getAllHits().iterator(); iterator
				.hasNext();) {
			Data inSync = (Data) iterator.next();
			String path = "/" + applicationid + "/events/" + inSync.get("id")
					+ ".xconf";
			Page page = getPageManager().getPage(path);
			if (!page.exists()) {
				String temppath = "/WEB-INF/base/entermedia/tools/sync/template.xconf";
				Page template = getPageManager().getPage(temppath);
				List bar = template.getPageSettings().getFieldPathActions();
				page.getPageSettings().setPathActions(bar);
			}
			List actions = page.getPageSettings().getFieldPathActions();
			// PageSettings eventpage = null;
			// PageSettings eventpage =
			// getPageManager().getPageSettingsManager().getPageSettings(path);
			String nameval = page.getProperty("eventname");
			if (nameval == null || !nameval.equals(inSync.get("name"))) {
				PageProperty nameprop = new PageProperty("eventname");
				nameprop.setValue(inSync.get("name"));
				page.getPageSettings().putProperty(nameprop);
			}

			String syncidval = page.getProperty("syncid");
			if (syncidval == null || !syncidval.equals(inSync.get("syncid"))) {
				PageProperty syncprop = new PageProperty("syncid");
				syncprop.setValue(inSync.get("id"));
				page.getPageSettings().putProperty(syncprop);
			}

			/*
			 * List pathactions = eventpage.getPathActions();
			 * if(pathactions.size() != 1) { PageAction action = new
			 * PageAction("UpdateModule.syncToServer"); pathactions.add(action);
			 * eventpage.setPathActions(pathactions); }
			 */
			getPageManager().saveSettings(page);
			getPageManager().clearCache(page);
		}
	}

	public void deleteSync(WebPageRequest inReq) {
		String applicationid = inReq.findValue("applicationid");
		Searcher syncSearcher = getSearcherManager().getSearcher(applicationid,
				"sync");
		String syncid = inReq.getRequestParameter("syncid");
		if (syncid == null) {
			return;
		}
		Data sync = (Data) syncSearcher.searchById(syncid);
		String path = "/" + applicationid + "/events/" + sync.get("id")
				+ ".xconf";
		if (sync != null) {
			syncSearcher.delete(sync, inReq.getUser());
		}
		// delete event
		Page page = getPageManager().getPage(path);
		getPageManager().removePage(page);
	}

	public void loadSync(WebPageRequest inReq) {
		String applicationid = inReq.findValue("applicationid");
		Searcher syncSearcher = getSearcherManager().getSearcher(applicationid,
				"sync");
		String syncid = inReq.findValue("syncid");
		Data sync = (Data) syncSearcher.searchById(syncid);
		inReq.putPageValue("sync", sync);
	}

	public void loadSyncs(WebPageRequest inReq) {
		String applicationid = inReq.findValue("applicationid");
		Searcher syncSearcher = getSearcherManager().getSearcher(applicationid,
				"sync");
		HitTracker synchits = syncSearcher.getAllHits();
		inReq.putPageValue("synclist", synchits);
	}

	
	protected void checkLogin(WebPageRequest inReq) throws Exception {
		if (inReq.getUser() == null) {
			String username = inReq.getRequestParameter("accountname");
			String password = inReq.getRequestParameter("password");

			User admin = getUserManager(inReq).getUser(username);
			if (!getUserManager(inReq).authenticate(admin, password)) {
				throw new OpenEditException("Did not authenticate: " + username);
			} else {
				inReq.setUser(admin);
			}
		}
	}

	/**
	 * This method should be removed. The normall email error handling should
	 * send these out
	 * 
	 * @param message
	 * @param inReq
	 * @throws Exception
	 */

	public void notify(String message, WebPageRequest inReq) throws Exception {
		log.info("sending notifications: " + message);
		TemplateWebEmail mailer = getPostMail().getTemplateWebEmail();
		Page template = getPageManager().getPage(
				"/openedit/update/sync/syncnotification.html");
		mailer.loadSettings(inReq.copy(template));
		mailer.setMailTemplatePage(template);

		String subject = mailer.getWebPageContext().findValue("subject");
		if (subject == null) {
			subject = "Sync notification";
		}
		if (!template.exists()) {
			mailer.setAlternativeMessage("Sync Report \n" + message);
		}
		mailer.setSubject(subject);

		mailer.getWebPageContext().putPageValue("message", message);
		String from = mailer.getWebPageContext().findValue("from");
		mailer.setFrom(from);
		if (mailer.getFrom() == null) {
			mailer.setFrom("support@openedit.org");
		}
		Group notify = getUserManager(inReq).getGroup("notify");
		if (notify != null) {
			Collection list = getUserManager(inReq).getUsersInGroup(notify);
			for (Iterator iter = list.iterator(); iter.hasNext();) {
				User user = (User) iter.next();
				String email = user.getEmail();
				if (email != null) {
					Recipient recipient = new Recipient();
					recipient.setEmailAddress(email);
					recipient.setLastName(user.getLastName());
					recipient.setFirstName(user.getFirstName());
					mailer.setRecipient(recipient);
					mailer.getWebPageContext().putPageValue("sendto", user);
					mailer.send();
				}
			}
		}
	}

	public PostMail getPostMail() {
		return fieldPostMail;
	}

	public void setPostMail(PostMail inPostMail) {
		fieldPostMail = inPostMail;
	}

	public void listPlugins(WebPageRequest inReq) throws Exception {
		List all = getPlugInFinder(inReq).getPlugIns();

		inReq.putPageValue("sortedlist", all);

		HitTracker hits = getSearcherManager().getList("system","installedplugin");
		Set installed = new HashSet();
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			installed.add(hit.getId());
		}
		inReq.putPageValue("installedids", installed);

	}

	public PlugIn loadPlugIn(WebPageRequest inReq) throws Exception {
		String pluginid = inReq.findValue("pluginid");
		if (pluginid != null) {
			PlugIn found = getPlugInFinder(inReq).getPlugIn(pluginid);
			inReq.putPageValue("plugin", found);
			return found;
		}
		return null;
	}

	public void createNewApp(WebPageRequest inReq) throws Exception {
		String appfolder = inReq.findValue("appfolder");

		appfolder = appfolder.replace('\\', '/');

		if (!appfolder.startsWith("/")) {
			appfolder = "/" + appfolder;
		}
		if (!appfolder.endsWith("/")) {
			appfolder = appfolder + "/";
		}
		String prefix = inReq.findValue("appfolderprefix");
		if (prefix == null) {
			prefix = "";
		}
		Page app = getPageManager().getPage(prefix + appfolder + "_site.xconf");
		PageProperty prop = new PageProperty("fallbackdirectory");

		PlugIn plugin = loadPlugIn(inReq);
		if (plugin != null) {
			prop.setValue(plugin.getBasePath().getPath());
		} else {
			prop.setValue(inReq.findValue("fallbackfolder"));
		}

		app.getPageSettings().putProperty(prop);

		PageProperty catid = new PageProperty("catalogid");
		String catalogid = appfolder;
		catalogid = catalogid.replace("/", "");
		catid.setValue(catalogid);
		app.getPageSettings().putProperty(catid);

		getPageManager().saveSettings(app);
		inReq.putPageValue("newpath", appfolder);

	}

	// public void updateProject(WebPageRequest inContext) throws Exception {
	// checkLogin(inContext);
	// String strUrl = inContext.getRequestParameter("installscripturl");
	// if (strUrl != null) {
	// // *** configure file path variable
	// String strOutputFile = "/WEB-INF/install.js";
	//
	// // *** get root path of this object
	// String root = getRoot().getAbsolutePath();
	// if (root.endsWith("/")) {
	// root = root.substring(0, root.length() - 1);
	// }
	//
	// // *** connect to configured web site
	// File out = new File(root, strOutputFile);
	// new Downloader().download(strUrl, out);
	//
	// ScriptModule module = (ScriptModule) getModule("Script");
	// Map variables = new HashMap();
	// variables.put("context", inContext);
	// List logs = new ArrayList();
	// logs.add("Downloading latest upgrade script...");
	// inContext.putPageValue("log", logs);
	// variables.put("log", logs);
	// try {
	// log.info("Upgrading " + strUrl);
	// module.execScript(variables, strOutputFile);
	// } catch (OpenEditException ex) {
	// inContext.putPageValue("exception", ex);
	// log.error(ex);
	// }
	// }
	// // Read in the output file?
	// // redirect the user to a blank page
	// }

	public void updateProjects(WebPageRequest inReq) throws Exception {
		checkLogin(inReq);
		if (inReq.getSessionValue("status") != null
				&& !Boolean.parseBoolean(inReq
						.getRequestParameter("forceupgrade"))) {
			inReq.putPageValue("error", "Upgrade already in progress");
			return;
		}
		Upgrader upgrader = getUpgrader(inReq);
		String[] toupdate = inReq.getRequestParameters("toupdate");
		// *** get root path of this object
		if (upgrader == null
				|| ((upgrader.isCanceled() || upgrader.isComplete()) && toupdate != null)) {
			String root = getRoot().getAbsolutePath();
			if (root.endsWith("/")) {
				root = root.substring(0, root.length() - 1);
			}
			// loop over each project. Keep a log
			upgrader = new Upgrader();
			upgrader.setPlugInFinder(getPlugInFinder(inReq));
			upgrader.setRoot(getRoot());
			if (toupdate == null) {
				inReq.putPageValue("error", "No upgrades specified");
				return;
			}
			upgrader.setToUpgrade(toupdate);
			upgrader.setScriptModule((ScriptModule) getModule("Script"));
			String serverid = inReq.findValue("serverid");
			inReq.putSessionValue("upgrader" + serverid, upgrader);
			inReq.putPageValue("upgrader", upgrader);
			
			//save these before the server crashes
			Searcher searcher = getSearcherManager().getSearcher("system", "installedplugin");
			searcher.deleteAll(null);
			List all = new ArrayList();
			for (int i = 0; i < toupdate.length; i++)
			{
				Data tosave = searcher.createNewData();
				tosave.setId(toupdate[i]);
				tosave.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
				all.add(tosave);
			}
			searcher.saveAllData(all, null);
			
		}
	}

	public void cancelUpdate(WebPageRequest inReq) {

		Upgrader up = getUpgrader(inReq);
		if (up != null) {
			up.cancel();
			inReq.putPageValue("upgrader", up);
		}
		String serverid = inReq.getRequestParameter("serverid");
		if (serverid == null) {
			serverid = "1";
		}
		inReq.removeSessionValue("upgrader" + serverid);

	}

	private Upgrader getUpgrader(WebPageRequest inReq) {
		String serverid = inReq.getRequestParameter("serverid");
		if (serverid == null) {
			serverid = "1";
		}
		Upgrader upgrader = (Upgrader) inReq.getSessionValue("upgrader"
				+ serverid);

		return upgrader;
	}

	public PlugInFinder getPlugInFinder(WebPageRequest inReq) {
		Searcher searcher = getSearcherManager().getSearcher("system",
				"extensionservers");
		String serverid = inReq.getRequestParameter("serverid");
		if (serverid == null) {
			serverid = "1";
		}
		PlugInFinder finder = (PlugInFinder) getPluginFinders().get(serverid);

		if (finder == null) {
			Data data = (Data) searcher.searchById(serverid);
			String path = data.get("listingurl");
			finder = (PlugInFinder) getModuleManager().getBean("plugInFinder");
			finder.setAppServerPath(path);
			finder.setId(serverid);
			getPluginFinders().put(serverid, finder);
		}
		inReq.putPageValue("pluginfinder", finder);
		return finder;
	}

	protected Map getPluginFinders() {
		if (fieldPlugInFinders == null) {
			fieldPlugInFinders = new HashMap();
		}

		return fieldPlugInFinders;
	}

	public void upgradeFromZip(WebPageRequest inReq) {
		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inReq);
		if (properties.getFirstItem() == null) {
			return;
		}
		FileUploadItem item = (FileUploadItem) properties.getFirstItem();
		String name = item.getFieldName();
		String path = "/WEB-INF/temp/" + item.getName();
		properties.saveFileAs(item, path, inReq.getUser());
		Page outputfolder = getPageManager().getPage("/WEB-INF/temp/upgrade/");
		Page inputfile = getPageManager().getPage(path);
		
		try {
			ZipUtil util = new ZipUtil();

			File in = new File(inputfile.getContentItem().getAbsolutePath());
			File out = new File(outputfolder.getContentItem().getAbsolutePath());

			util.unzip(in, out);

			Page scriptpage = getPageManager().getPage("/WEB-INF/temp/upgrade/etc/install.js");
			if(!scriptpage.exists()){
				 scriptpage = getPageManager().getPage("/WEB-INF/temp/upgrade/etc/zipinstall.js");

			}
			if (scriptpage.exists()) {
				Map variables = new HashMap();
				variables.put("context", inReq);
				ScriptLogger logger = new ScriptLogger();
				logger.startCapture();
				variables.put("log", logger);
				Script script = getScriptManager().loadScript("/WEB-INF/temp/upgrade/etc/install.js");
				getScriptManager().execScript(variables, script);
			}

		} catch (Exception e) {
				throw new OpenEditException(e);
		}

		finally {
			getPageManager().removePage(outputfolder);
			getPageManager().removePage(inputfile);
		}

	}

}