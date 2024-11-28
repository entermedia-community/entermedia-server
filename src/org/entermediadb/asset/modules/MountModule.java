package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.asset.util.UrlRepository;
import org.openedit.OpenEditRuntimeException;
import org.openedit.WebPageRequest;
import org.openedit.WebServer;
import org.openedit.modules.BaseModule;
import org.openedit.repository.Repository;
import org.openedit.repository.filesystem.FileRepository;

public class MountModule extends BaseModule {
	protected WebServer fieldWebServer;

	public List loadMounts(WebPageRequest inReq) {
		List configs = getPageManager().getRepositoryManager()
				.getRepositories();
		inReq.putPageValue("mounts", configs);
		return configs;
	}

	public Repository loadDefaultMountForPath(WebPageRequest inReq) {
		String path = inReq.getRequestParameter("path");

		List configs = getPageManager().getRepositoryManager()
				.getRepositories();
		for (Iterator iterator = configs.iterator(); iterator.hasNext();) {
			Repository config = (Repository) iterator.next();
			if (config.getPath().equals(path)) {
				if (config.getFilterOut() == null) {
					inReq.putPageValue("mount", config);
					return config;
				}
			}
		}
		return null;
	}

	public List loadMountsForPath(WebPageRequest inReq) {
		String path = inReq.getRequestParameter("path");

		List configs = getPageManager().getRepositoryManager()
				.getRepositories();
		List matching = new ArrayList();

		for (Iterator iterator = configs.iterator(); iterator.hasNext();) {
			Repository config = (Repository) iterator.next();
			if (config.getPath().equals(path)) {
				matching.add(config);
			}
		}

		inReq.putPageValue("mounts", matching);
		return matching;
	}

	public Repository loadMount(WebPageRequest inReq) {
		String matches = inReq.findValue("mountid");
		List configs = loadMounts(inReq);
		for (Iterator iterator = configs.iterator(); iterator.hasNext();) {
			Repository config = (Repository) iterator.next();
			if (config.getPath().equals(matches)) {
				inReq.putPageValue("mount", config);
				return config;
			}
		}
		return null;
	}

	public void removeMount(WebPageRequest inReq) {
		List mounts = loadMounts(inReq);
		// Save all the mounts but some of them might not load on startup?
		Repository existing = loadMount(inReq);
		for (Iterator iterator = mounts.iterator(); iterator.hasNext();) {
			Repository config = (Repository) iterator.next();
			if (config.equals(existing)) {
				mounts.remove(config);
				break;
			}
		}
		saveMounts(mounts);
	}

	public void saveMount(WebPageRequest inReq) {
		List mounts = loadMounts(inReq);
		// Save all the mounts but some of them might not load on startup?
		// update data and save all
		String path = inReq.getRequestParameter("path");
		if (path == null) {
			path = "/";
		}
		if (path.length() > 1 && path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}

		Repository repo = loadMount(inReq);
		String repotype = inReq.getRequestParameter("repositorytype");
		if (repo == null) {
			if (repotype == null || repotype.trim().equals("")) // file
			{
				repo = new FileRepository();
			} else if ("versionRepository".equals(repotype)) {
				repo = new FileRepository();
			} else if ("urlRepository".equals(repotype)) {
				repo = new UrlRepository();
			} else if ("sftpRepository".equals(repotype)) {
				repo = new UrlRepository();
			} else {
				throw new OpenEditRuntimeException("Invalid repository type.");
			}
			mounts.add(repo);
		}

		repo.setPath(path);
		repo.setExternalPath(inReq.getRequestParameter("externalpath"));

		String[] fields = inReq.getRequestParameters("field");
		if (fields != null) {
			for (int i = 0; i < fields.length; i++) {
				String field = fields[i];
				String value = inReq.getRequestParameter(field + ".value");
				if (value != null) {
					repo.setProperty(field, value);

				}
			}
		}
		String filterin = inReq.getRequestParameter("filterin");
		repo.setFilterIn(filterin);
		repo.setFilterOut(inReq.getRequestParameter("filterout"));
		repo.setRepositoryType(repotype);

		// save out the file
		saveMounts(mounts);
	}

	protected void saveMounts(List mounts) {
		getWebServer().saveMounts(mounts);
	}

	public void reloadMounts(WebPageRequest inReq) {
		getWebServer().reloadMounts();
	}

	public WebServer getWebServer() {
		return fieldWebServer;
	}

	public void setWebServer(WebServer inWebServer) {
		fieldWebServer = inWebServer;
	}
}