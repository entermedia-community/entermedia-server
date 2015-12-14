/*
 * Created on May 17, 2006
 */
package org.entermediadb.modules.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.OpenEditException;
import org.openedit.page.manage.PageManager;
import org.openedit.util.FileUtils;
import org.openedit.util.PageZipUtil;
import org.openedit.util.ZipUtil;

public class Backup
{
	protected PageManager fieldPageManager;
	protected SimpleDateFormat fieldFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
	protected File fieldRoot;
	protected String fieldIncludePath = "/";
	protected FileUtils fieldUtils = new FileUtils();
	protected List fieldExcludes = new ArrayList();
	private static final Log log = LogFactory.getLog(Backup.class);
	
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	
	protected File backupCurrentSite(String inName) throws OpenEditException
	{
		PageZipUtil zip = new PageZipUtil(getPageManager());
		zip.setRoot(getRoot());
		for (Iterator iter = getExcludes().iterator(); iter.hasNext();)
		{
			String exclude = (String) iter.next();
			zip.addExclude(exclude);
		}
//		zip.addExclude("*/WEB-INF/*");
//		zip.addExclude("*/.versions/*");
//		zip.addExclude("*/WEB-INF/trash/*");
//		zip.addExclude("*/WEB-INF/tmp/*");
//		zip.addExclude("*/WEB-INF/log*");
		
		inName = inName.replace(" ", "_");
		inName = inName.replace("/", "_");
		inName = inName.replace("\\", "_");

		String id = fieldFormat.format(new Date() ) + "_" + inName;
		String outpath = "/WEB-INF/versions/" + id + ".zip";
		zip.addExclude(outpath);
		File out = new File( getRoot() , outpath );
		log.info("Backing up " + out);
		try
		{
			out.getParentFile().mkdirs();
			FileOutputStream stream = new FileOutputStream(out);
			try
			{
				zip.zipFile(getIncludePath(), stream );
			}
			finally
			{
				FileUtils.safeClose(stream);
			}
			return out;
		}
		catch ( IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}
	public File getRoot()
	{
		return fieldRoot;
	}
	public void setRoot(File inRoot)
	{
		fieldRoot = inRoot;
	}
	public List listSiteVersions()
	{
		File verdir = new File( getRoot() , "WEB-INF/versions");
		verdir.mkdirs();
		
		File[] children = verdir.listFiles(new FilenameFilter() {
			public boolean accept(File inDir, String inName)
			{
				return inName.endsWith(".zip");
			}
		});
		List list = new ArrayList();
		if ( children != null)
		{
			for (int i = 0; i < children.length; i++)
			{
				File child = children[i];
				list.add(child);
			}
		}
		Collections.sort(list);
		return list;
	}
	public File loadVersion(String inName)
	{
		List list = listSiteVersions();
		for (Iterator iter = list.iterator(); iter.hasNext();)
		{
			File version= (File) iter.next();
			if ( version.getName().equals( inName ))
			{
				return version;
			}
		}
		return null;
	}
	protected void restoreBackup(File inVersion) throws OpenEditException
	{
		log.info("Restoring " + inVersion.getName() );
		File tmp = null;
		try
		{
			tmp = new File( getRoot(),"WEB-INF/trash/new" +  fieldFormat.format(new Date() ));
			tmp.mkdirs();
			
			ZipUtil utils = new ZipUtil();
			//unzip the zip file in a tmp directory
			utils.unzip(inVersion,tmp);
		}
		catch (IOException ex )
		{
			throw new OpenEditException("No harm done", ex);
		}
		File old = new File( getRoot(),"WEB-INF/trash/old" +  fieldFormat.format(new Date() ));

		try
		{
			replaceDirectories(tmp,getRoot(),old);
		}
		catch (IOException ex )
		{
			throw new OpenEditException( ex );
		}


		
		//		try
//		{
//		File tmpold = null;
//		}
//		finally
//		{
//			if( !tmp.renameTo(getRoot()) ) 
//			{
//				//copy it
//				try
//				{
//					log.error("Could not rename");
//					//org.apache.commons.io.FileUtils.copyFile(tmp, getRoot(), true);
//					new FileUtils().copyFiles(tmp, getRoot() );
//				}
//				catch ( IOException ex)
//				{
//					throw new OpenEditException(ex);
//				}
//			}
//			//bring back the versions
//			File versions = new File( tmpold , "WEB-INF/versions");
//			File newversions = new File( getRoot(), "WEB-INF/versions");
//			versions.renameTo(newversions);
//		}
	}
	/**
	 * This method does a replacement of top level directories. 
	 * One exception is the WEB-INF directory that it will go into
	 * @param inNewDirs
	 * @param inRoot
	 * @param inSubPath
	 * @param inOldDirectory
	 * @throws IOException
	 */
	protected void replaceDirectories(File inNewDirs, File inRoot, File inOldDirectory ) throws IOException
	{		
		//move the existing content to tmp2
//		tmpold = File.createTempFile("upgradeold", "");
//		tmpold.delete();
//		tmpold.mkdirs();
		File[] children = inNewDirs.listFiles();
		for (int i = 0; i < children.length; i++)
		{
			File child = children[i];
			File existing = new File( inRoot, child.getName() );
			if( existing.exists() )
			{
				//Then move it into away
				
				if( child.getName().equals("WEB-INF"))
				{
					//replaceDirectories(child, existing, inOldDirectory);
					continue;
				}
				else
				{
					if( existing.isDirectory() )
					{
						fieldUtils.move(existing, new File( inOldDirectory, existing.getName() ));				
					}	
					else
					{
						//this is an existing file in the inNewDirs directory
						File backup = new File( inOldDirectory, child.getName() );
						if( !existing.renameTo(backup) )
						{
							throw new IOException("Could not move " + existing.getPath() + " to " + backup.getPath());
						}
					}
				}
			}
			//Now replace it
			fieldUtils.move(child, new File( getRoot(), child.getName() ) );
			//child.renameTo(new File( getRoot(), child.getName() ));

		}
		//TODO: Do this in smaller parts so we can exclude WEB-INF/logs/
//		if ( !getRoot().renameTo(tmpold) )
//		{
//			log.info("Had to copy manually");
//			FileUtils fu = new FileUtils();
//			fu.copyFiles(getRoot(), tmpold );
//			fu.deleteAll(getRoot());
//		}
//		File[] all = tmpold.listFiles();
//		if ( all.length == 0)
//		{
//			throw new OpenEditException("Problem: Could not move entire existing site. Some files moved to: " + tmpold.getPath());
//		}
		
	}
	public String getIncludePath()
	{
		return fieldIncludePath;
	}
	public void setIncludePath(String inIncludePath)
	{
		fieldIncludePath = inIncludePath;
	}
	
	public List getExcludes() 
	{
		if (fieldExcludes == null)
		{
			fieldExcludes = new ArrayList();			
		}
		return fieldExcludes;
	}
	public void setExcludes(List inExcludes) 
	{
		this.fieldExcludes = inExcludes;
	}
	public void addExclude(String inExclude)
	{
		getExcludes().add(inExclude);
	}
	
	
	
}



