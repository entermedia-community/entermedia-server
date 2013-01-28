package com.openedit.modules.update;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.openedit.entermedia.util.SyncFileDownloader;
import org.openedit.repository.ContentItem;

import com.openedit.OpenEditException;
import com.openedit.util.PathUtilities;

public class SyncToServer extends SyncFileDownloader
{
	public static final Log log = LogFactory.getLog(SyncToServer.class);

	public void syncFromServer()
	{
		String url = getServerUrl();
		//log.info( "downloading from here:" + url );

		if( !login() )
		{
			throw new OpenEditException("Could not log in");
		}
		url += getListXml();
		String path = " relative path in " + getSyncPath();
		log.info("Saving to a " + path);
		processPath(getSyncPath());
		log.info("Completed sync");
	}

	protected void processPath(String inRemotePath)
	{
		Element remotelist = listRemoteChanges(inRemotePath);
			// Make three lists. Remote, local, remote downloads
			// loop over files look for dates or time changes
		if (log.isDebugEnabled())
		{
			log.debug("Found these changes on the remote server:  " + inRemotePath);
		}
		//This should call recursively for each folder
		syncWithTheseRemoteChanges(remotelist, inRemotePath);
	}

	// This bug is only somewhat related:
	// http://bugs.sun.com/view_bug.do?bug_id=4860999
	//
	// The workaround recommends comparing times in whole seconds. 
	protected void syncWithTheseRemoteChanges(Element remotedata, String inRemotePath) 
	{
		Map remote = buildRemoteFileMap(remotedata);

		// Set local
		Map local = buildLocalFileMap(inRemotePath);

		//remove local files and folders that we already have equal
		synchronizeRemoteAndLocalMaps(remote, local);

		if (remote.size() == 0)
		{
			log.info("No files to download for directory " + inRemotePath);
		}
		else
		{
			log.info("Downloading " + remote.size() + " changed files for: " + inRemotePath);

			//We downloaded all files and the top levels of any new directories
			//This will not download recursively
			try
			{
				downloadAllFiles(remote);
			}
			catch ( Exception ex )
			{
				throw new OpenEditException(ex);
			}
		}

		//Check for extra files and folders that we have extra
		deleteExtraFiles(local);

		//Now check all the local and remote folders 

		// loop over subdirectories calling this method
		for (Iterator iterator = remotedata.elementIterator(DIR); iterator.hasNext();)
		{
			Element dir = (Element) iterator.next();
			String next = dir.attributeValue(PATH);
			processPath(next);
		}
	}

	/*
	 * TODO: Come up with a better method name? This method compares the local
	 * file map to the remote file map based on path and timestamp. Entries
	 * remaining in the remote map represent files that need to be downloaded.
	 * Entries remaining in the local map represent files that can be deleted
	 * locally if a true "sync" is desired.
	 */
	protected void synchronizeRemoteAndLocalMaps(Map remote, Map local)
	{

		List allremote = new ArrayList(remote.values());
		for (Iterator iterator = allremote.iterator(); iterator.hasNext();)
		{
			Element remotefile = (Element) iterator.next();
			String path = remotefile.attributeValue(PATH);
			Element localfile = (Element) local.get(path);
			if (isEquals(remotefile, localfile))
			{
				remote.remove(remotefile.attributeValue(PATH)); // remove it from the lis of files to download
				if (log.isDebugEnabled())
				{
					if (isDirectory(remotefile))
					{
						log.debug("Local directory exists: " + path);
					}
					else
					{
						log.debug("File up to date, skipping: " + path);
					}
				}
			}
			else
			{
				if (log.isDebugEnabled())
				{
					log.debug("File has changed:  " + path);
				}
			}
			local.remove(path); // If diferent then remote will replace local
			// anyways
		}
	}

	protected Map buildRemoteFileMap(Element root)
	{
		Map remote = ListOrderedMap.decorate(new HashMap());
		for (Iterator iterator = root.elementIterator(); iterator.hasNext();)
		{
			Element file = (Element) iterator.next();
			String path = file.attributeValue(PATH);
			if (pathPassesExcludes(file.getName(), path))
			{
				remote.put(path, file);
			}
		}
		return remote;
	}

	protected void deleteExtraFiles(Map inLocal) throws OpenEditException
	{
		for (Iterator iterator = inLocal.values().iterator(); iterator.hasNext();)
		{
			Element element = (Element) iterator.next();

			File file = getFile(element.attributeValue(PATH));
			log.info("deleting " + file.getAbsolutePath());
			getFileUtils().deleteAll(file);
			if (file.exists())
			{
				log.info("was unable to delete file");
				getWindowsUtil().delete(file);
			}

		}
	}

	public Map buildLocalFileMap(String inRemotePath) throws OpenEditException
	{
		log.info("Sending file list for " + inRemotePath);
		Map local = ListOrderedMap.decorate(new HashMap());
		Collection children = null;
		//children = resolveLocalFile( localPath ).listFiles( exclusionFilter );
		children = getPageManager().getRepository().getChildrenNames(inRemotePath);
		if (children != null)
		{
			for (Iterator iterator = children.iterator(); iterator.hasNext();)
			{
				String path = (String) iterator.next();
				ContentItem child = getPageManager().getRepository().getStub(path);
				String type = child.isFolder() ? DIR : FILE;
				if (!pathPassesExcludes(type, path))
				{
					continue;
				}
				Element e = null;
				if (child.isFolder())
				{
					e = DocumentHelper.createElement(DIR);
				}
				else
				{
					e = DocumentHelper.createElement(FILE);
					e.addAttribute(DATE, "" + child.getLastModified() / 1000L);
				}
				e.addAttribute(PATH, path);
				local.put(path, e);
			}
		}
		log.info("Found " + local.size() + " files for " + inRemotePath);
		return local;
	}

	protected boolean isEquals(Element inRemotefile, Element inLocalfile)
	{
		if (inLocalfile == null && inRemotefile == null)
		{
			return true;
		}
		if (inLocalfile != null)
		{
			if (FILE.equalsIgnoreCase(inRemotefile.getName()))
			{
				String date = inRemotefile.attributeValue(DATE);
				String date2 = inLocalfile.attributeValue(DATE);
				return date.equals(date2);
			}
			else if (isDirectory(inRemotefile))
			{
				String remotePath = inRemotefile.attributeValue(PATH);
				String localPath = inLocalfile.attributeValue(PATH);
				//Is this needed? Seems like they are equals by def
				return remotePath.equals(localPath);
			}
		}

		return false;
	}

	protected boolean pathPassesExcludes(String inType, String path)
	{
		if (DIR.equals(inType) && !path.endsWith("/"))
		{
			path = path + "/";
		}
		for (Iterator iterator = getZipUtils().getExcludes().iterator(); iterator.hasNext();)
		{
			String excludePattern = (String) iterator.next();

			//the path might be a folder

			if (PathUtilities.match(path, excludePattern))
			{
				return false;
			}
			//if the pattern ends with /*

		}
		return true;
	}
}
