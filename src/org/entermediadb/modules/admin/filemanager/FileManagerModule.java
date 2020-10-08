/*
 * Created on Dec 22, 2004
 */
package org.entermediadb.modules.admin.filemanager;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.upload.FileUpload;
import org.entermediadb.asset.upload.UploadRequest;
import org.entermediadb.webui.tree.TreeModule;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.modules.BaseModule;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.util.PathUtilities;
import org.openedit.util.URLUtilities;

/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public class FileManagerModule extends BaseModule
{
	private static final Log log = LogFactory.getLog(FileManagerModule.class);
	
	protected FileUpload fieldFileUpload;
	protected TreeModule fieldTreeSupport;

	public FileManagerModule()
	{
	}

	protected TreeModule getTreeSupport()
	{
		return fieldTreeSupport;
	}
	public void setTreeSupport(TreeModule inSupport)
	{
		fieldTreeSupport = inSupport;
	}
	
	/**
	 * Construct one parameter dynamically from the values of other parameters.
	 * Example:
	 * 	<input type="hidden" name="param-part" value="${page.filerootpath}/index.html?path=" />	
	 *	<input type="hidden" name="param-part" value="req.path" />	
	 *	<input name="path"/>
	 *	<input type="hidden" name="param-name" value="newpath" />
	 * @param inReq
	 */
	public void constructParameter(WebPageRequest inReq)
	{
		String pathName = inReq.getRequestParameter("param-name");
		
		if (pathName == null || pathName.length() == 0)
		{
			return;
		}
		
		
		String[] pathParts = inReq.getRequestParameters("param-part");
		StringBuilder path = new StringBuilder();
		
		for (int i = 0; i < pathParts.length; i++)
		{
			String part = pathParts[i];
			if (part.startsWith("req."))
			{
				String name = part.substring(4);
				part = inReq.getRequestParameter(name);
			}
			if (part != null)
			{
				path.append(part);
			}
		}
		inReq.setRequestParameter(pathName, path.toString());
	}
	
	public String makeNewPath(WebPageRequest inReq)
	{
		String path = inReq.getRequestParameter("path");
		String name = inReq.getRequestParameter("newname");
		
		if (path == null)
		{
			return null;
		}
		
		String fullpath = null;
		
		if (name == null)
		{
			fullpath = path;
		}
		else
		{
			if( name.startsWith("/"))
			{
				name = name.substring(1);
			}
			if( !path.endsWith("/"))
			{
				path = path + "/";
			}
			fullpath = path + name;
		}
		return fullpath;
	}
	
	public void makeFolder( WebPageRequest inReq ) throws OpenEditException
	{
		String path = makeNewPath(inReq);
		
		if( !path.endsWith("/"))
		{
			path = path + "/";
		}
		
		Page folder = getPage(path);
		
		if (!folder.exists())
		{
			checkUser(inReq);
			folder.getContentItem().setAuthor(inReq.getUser().getUserName());
			getPageManager().putPage(folder);
		}
		inReq.putPageValue("folderpath", path);
	}
	
	public void makeFile( WebPageRequest inReq ) throws OpenEditException
	{
		String path = makeNewPath(inReq);
		
		Page file = getPage(path);
		
		if (!file.exists())
		{
	        String sourcePath="/system/templates/blankhtml.html";
			Page source = getPageManager().getPage(sourcePath);

			checkUser(inReq);
			file.getContentItem().setAuthor(inReq.getUser().getUserName());
			getPageManager().copyPage(source, file);
		}
	}
	
	public void copyPage( WebPageRequest inReq ) throws OpenEditException
	{
		String destinationPath = inReq.getRequestParameter("destinationPath");

		String sourcePath = inReq.getRequestParameter("sourcePath");
		if( sourcePath == null && !destinationPath.endsWith("/"))
		{
	        sourcePath="/openedit/styles/blankhtml.html";

		}
		if(sourcePath != null && sourcePath.equals(destinationPath))
		{
			return;
		}
		destinationPath = destinationPath.replace('\\','/');
		
		Page destPage = getPage(destinationPath);
		if ( destPage.isFolder() ) //Saving to a folder
		{
			if( !destinationPath.endsWith("/"))
			{
				destinationPath = destinationPath + "/"; 
			}
			destinationPath = destinationPath + PathUtilities.extractFileName(sourcePath);
			destPage = getPage(destinationPath);
		}
		String overwriteStr = inReq.getRequestParameter("overwrite");
		boolean overwrite = ((overwriteStr != null) && overwriteStr.equals("true"));

		if (!overwrite && destPage.exists() )
		{
			inReq.putPageValue("error",	"Content already exists at path " + destinationPath);
		}
		checkUser(inReq);
		destPage.getContentItem().setAuthor(inReq.getUser().getUserName());
		if( sourcePath != null)
		{
			Page page = getPageManager().getPage(sourcePath);
			getPageManager().copyPage(page, destPage);
		}
		else
		{
			getPageManager().putPage(destPage); //This is a folder?
		}
		inReq.setRequestParameter("path", destPage.getPath());
		inReq.setRequestParameter("editPath", destPage.getPath());

	}
	
	public void deletePage( WebPageRequest inReq ) throws OpenEditException
	{
		String path = inReq.getRequestParameter("delete");
		if (path == null)
		{
			return;
		}

		checkUser(inReq);

		Page page = getPage(path);
		
		if (path.indexOf("trash") > -1)
		{
			//just delete it
			page.getContentItem().setAuthor(inReq.getUser().getUserName());
			getPageManager().removePage( page );
		}
		else
		{
			//move page to trash folder
			String trashPath = "/WEB-INF/trash/" + PathUtilities.extractFileName(path);
			Page trashPage = getPage(trashPath);
	
			trashPage.getContentItem().setAuthor(inReq.getUser().getUserName());
			getPageManager().movePage(page, trashPage);
		}	
		inReq.setRequestParameter("path", page.getParentPath());
	}
	//TODO: Move over to our built in permission checker API
	protected void checkUser(WebPageRequest inReq ) throws OpenEditException
	{
		if( inReq.getUser() == null && (!inReq.getUser().hasPermission("oe.administration") ||  inReq.getUser().isInGroup("administrators")))
		{
			throw new OpenEditException("Must be logged in as an administrator");
		}
		
	}
	protected Page getPage( String inPath ) throws OpenEditException
	{
		return getPageManager().getPage( inPath );
	}
	public void uploadFile( WebPageRequest inReq ) throws OpenEditException
	{
		String reload = inReq.getRequestParameter("reload");
		if( Boolean.parseBoolean(reload))
		{
			return; //we are reloading
		}
		UploadRequest map = getFileUpload().uploadFiles( inReq );
		if ( map == null)
		{
			log.info("reloading page");
			return;
		}
		String unzip = inReq.getRequestParameter("unzip");
		
		List files = map.unzipFiles(Boolean.parseBoolean(unzip));
		inReq.putPageValue("uploadrequest", map);
		inReq.putPageValue("unzippedfiles", files);
		inReq.putPageValue("pageManager", getPageManager());
	}
	public FileUpload getFileUpload()
	{
		return fieldFileUpload;
	}
	public void setFileUpload(FileUpload inFileUpload)
	{
		fieldFileUpload = inFileUpload;
	}
	public void movePage( WebPageRequest inReq ) throws OpenEditException
	{
		String[] sourcePath = inReq.getRequestParameters("sourcePath");
		String destinationPath = inReq.getRequestParameter("destinationPath");

		if ((sourcePath == null) || sourcePath.length == 0 || (destinationPath == null))
		{
			return;
		}

		if (sourcePath.equals(destinationPath))
		{
			throw new PageAlreadyExistsException(
				"The destination path is the same as the source path");
		}
		if( inReq.getUser() == null)
		{
			throw new OpenEditException("No such user");
		}
		if( inReq.getUser().hasPermission("oe.administration") || Boolean.parseBoolean(inReq.getPageProperty("allowmove"))  || inReq.getUser().isInGroup("administrators"))
		{
		}
		else
		{
			throw new OpenEditException("No permission to move files");
		}
		//checkUser(inReq);
		Page destpage = getPageManager().getPage(destinationPath);
		for (int i = 0; i < sourcePath.length; i++) 
		{
			Page page = getPageManager().getPage(sourcePath[i]);
			page.getContentItem().setAuthor(inReq.getUser().getUserName());
			destpage.getContentItem().setAuthor(inReq.getUser().getUserName());
			getPageManager().movePage( page, destpage );
		}
		inReq.setRequestParameter("path", destpage.getPath());

			inReq.setRequestParameter("editPath", destpage.getPath());
	}
	
	public void clearCache(WebPageRequest inReq)
	{
		getPageManager().clearCache();
	}

	public void expandTreeNode( WebPageRequest inReq ) throws OpenEditException
	{
		getTreeSupport().expandTreeNode( inReq );
	}
	public void getTree( WebPageRequest inReq ) throws OpenEditException
	{
		getTreeSupport().getTree( inReq );
	}
	public void reloadTree( WebPageRequest inReq ) throws OpenEditException
	{
		getTreeSupport().reloadTree( inReq );
	}
	
	public void jumpToPage( WebPageRequest inReq ) throws OpenEditException
	{
		String destinationPath = inReq.getRequestParameter( "destinationPath" );
		String forcedDestinationPath = inReq.getRequestParameter( "forcedDestinationPath" );
		String redirectPrefix = inReq.getRequestParameter("redirectPrefix");
		
		if (redirectPrefix != null && destinationPath != null)
		{
			destinationPath = redirectPrefix + destinationPath;
		}

		/* 
		   Even if we're dealing with an XML file, we want to point the browser to
		   the base filename with an HTML extension since XML files are rendered to 
		   HTML by Open Edit.
		*/
		int extensionIndex = destinationPath.indexOf(".xml");
		if ( extensionIndex > -1 )
		{
			String baseFileName = destinationPath.substring( 0, extensionIndex );
			destinationPath = baseFileName + ".html";
		}

		if ( forcedDestinationPath != null )
		{
			inReq.redirect( forcedDestinationPath);
		}
		else
		{
			inReq.redirect( destinationPath);
		}

	}

	public void replaceAll( WebPageRequest inReq) throws Exception
	{

		checkUser(inReq);
		String ffindText = inReq.getRequestParameter( "findText" );
		String newText = inReq.getRequestParameter( "newText" );
		String path = inReq.getRequestParameter( "sourcePath" );
		String ext = inReq.getRequestParameter("extensions");
		
		if ( ffindText != null && path != null)
		{
			Replacer replace = new Replacer();
			replace.setPageManager( getPageManager() );
			replace.setFindText(ffindText);
			replace.setFileTypes(ext);
			replace.setRootDirectory(getRoot());
			if ( newText == null )
			{
				newText = "";
			}
			if ( path != null && path.length() > 0)
			{
				replace.setSearchPath(path);
			}
			replace.setReplaceText(newText);
//				Repository repository = getPageManager().getRepository();
//				if ( repository instanceof VersionedRepository )
//				{
//					File rootDir = ((VersionedRepository)repository).getRootDirectory();
//					replace.setRootDirectory( rootDir );
//				}
//				else
//				{
//					throw new OpenEditException( "The 'replace' action only works with file system repositories" );
//				}

			replace.replaceAll();
			inReq.putPageValue("replacer",replace);
			inReq.putPageValue("textFound",URLUtilities.xmlEscape(ffindText));
			inReq.putPageValue("textSaved",URLUtilities.xmlEscape(newText));
		}			
	}
	public void forwardToZip( WebPageRequest inReq) throws Exception
	{
		String path = inReq.getRequestParameter("path");
		if (path.endsWith("/") && !path.equals("/"))
		{
			path = path.substring(0, path.length() - 1);
		}
//		String folder = PathUtilities.extractPagePath(path);
//		folder = PathUtilities.extractFileName(folder);
		String folder = PathUtilities.extractPageName(path);
		if ( folder.length() == 0)
		{
			folder = "root";
		}
		String done = "/openedit/filemanager/zipdir/" + folder + ".zip?path=" + path;
		inReq.redirect(done);
	}
	public PageManager loadPageManager(WebPageRequest inReq) throws Exception 
	{
		PageManager manager = getPageManager();
		inReq.putPageValue("pageManager", manager);
		return manager;
	}
	
	
	public void loadInEclipse(WebPageRequest inReq) throws Exception{
		//https://stackoverflow.com/questions/48545648/opening-files-in-eclipse-via-code
		String path = inReq.getRequestParameter("path");
		if (path.endsWith("/") && !path.equals("/"))
		{
			path = path.substring(0, path.length() - 1);
		}
		String absolutepath = getPageManager().getPage(path).getContentItem().getAbsolutePath();
		String eclipsepath = inReq.findValue("eclipsepath");
		Runtime.getRuntime().exec(new String[] {
			    eclipsepath,
			    "--launcher.openFile",
			    absolutepath,
			    // "path/to/file2.txt",
			    // ...
			});
	}
	
	
	
}
