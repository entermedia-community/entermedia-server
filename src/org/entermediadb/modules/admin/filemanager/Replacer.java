/*
 * Created on Nov 17, 2004
 */
package org.entermediadb.modules.admin.filemanager;

import java.io.File;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openedit.OpenEditRuntimeException;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.StringItem;
import org.openedit.util.PathUtilities;
import org.openedit.util.URLUtilities;

/**
 * In eclipse userdata=".*?" finds all attributes named userdata 
 * @author dbrown
 *
 */
public class Replacer
{
	protected File fieldRootDirectory;
	protected String fieldFileTypes = "*.html,*.htm";
	protected String fieldFindText;
	protected String fieldReplaceText;
	protected String fieldSearchPath;
	protected int fieldNumFilesChanged = 0;
	protected PageManager fieldPageManager;

	public Replacer()
	{
	}

	protected boolean fileMatchesFileTypes( File inFile )
	{
		String fileName = inFile.getName();
		String[] fileTypes = getFileTypes().split( "," );
		for (int n = 0; n < fileTypes.length; n++)
		{
			if ( PathUtilities.match(fileName, fileTypes[n] ) )
			{
				return true;
			}
		}
		return false;
	}

	protected boolean isFileIncludedInDirectory( File inFile, File inDirectory )
	{
		String filePath = inFile.getAbsolutePath();
		String dirPath = inDirectory.getAbsolutePath();
		return filePath.startsWith( dirPath );
	}

	protected boolean isDirectoryIncluded( File inFile )
	{
		if( inFile.getAbsolutePath().indexOf(".versions") > -1 )
		{
			return false;
		}
		File siteDir = getRootDirectory();
		if ( !isFileIncludedInDirectory( inFile, siteDir ) )
		{
			return false;
		}
		File openEditDir = new File( siteDir, "openedit" );
		if ( isFileIncludedInDirectory( inFile, openEditDir ) )
		{
			return false;
		}
		openEditDir = new File( siteDir, "base" );
		if ( isFileIncludedInDirectory( inFile, openEditDir ) )
		{
			return false;
		}
		File webInfDir = new File( siteDir, "WEB-INF" );
		if ( isFileIncludedInDirectory( inFile, webInfDir ) )
		{
			return false;
		}
		File logsDir = new File( siteDir, "logs" );
		if ( isFileIncludedInDirectory( inFile, logsDir ) )
		{
			return false;
		}
		return true;
	}

	protected void replaceTextInFile( File inFile ) throws Exception
	{
		Writer writer = null;
		try
		{
			
			String path = URLUtilities.getPathWithoutContext(getRootDirectory().getAbsolutePath(),inFile.getAbsolutePath(), "index.html"); //This is an off API to use for this. TODO: Replace with PathUtilities
			Page page = getPageManager().getPage(path);
			
			//is this in the search path
			if ( getSearchPath() != null )
			{
				//make sure we are within the search path
				//inFile = "/sub/index.html" searchpath="/" or "/sub" or "/sub/index.html"
				//then it passes
				if ( !path.toLowerCase().startsWith( getSearchPath().toLowerCase() ) )
				{
					return;
				}
			}
			
			if ( page.exists() && !page.isBinary() )
			{
				String content = page.getContent();
				Pattern pat = Pattern.compile(getFindText() );// (?s)(?m),  Pattern.DOTALL | Pattern.MULTILINE);
				Matcher mat = pat.matcher(content);
				String newcontent = mat.replaceAll(getReplaceText());
				
				if ( !content.equals( newcontent) )
				{
					ContentItem revision = new StringItem(page.getPath(), newcontent,page.getCharacterEncoding() );
					revision.setType( ContentItem.TYPE_EDITED );
					revision.setMessage( "Replaced text \"" + getFindText() +"\" with \"" + getReplaceText() +"\".");
					page.setContentItem(revision);
					getPageManager().putPage( page );
					setNumFilesChanged( getNumFilesChanged() + 1 );
				}
			}
		}
		finally
		{
			if ( writer != null )
			{
				writer.close();
			}
		}
	}

	protected void replaceAllInDirectory( File inDirectory ) throws Exception
	{
		if ( inDirectory.isDirectory())
		{
			if ( isDirectoryIncluded( inDirectory ) )
			{
				File[] filesInDirectory = inDirectory.listFiles();
				if( filesInDirectory != null)
				{
					for ( int n = 0; n < filesInDirectory.length; n++ )
					{
						replaceAllInDirectory( filesInDirectory[n] );
					}
				}
			}
		}
		else if ( fileMatchesFileTypes( inDirectory ) )
		{
			replaceTextInFile( inDirectory );
		}
	}
	public void replaceAll() throws Exception
	{
		setNumFilesChanged(0);
		if ( getSearchPath() == null)
		{
			throw new OpenEditRuntimeException("No search path set");
		}
		
		File path = new File( getRootDirectory(), getSearchPath());
		
		if ( isDirectoryIncluded( path ) )
		{
			replaceAllInDirectory( path );
		}
	}

	public String getFileTypes()
	{
		return fieldFileTypes;
	}
	public void setFileTypes(String inFileTypes)
	{
		fieldFileTypes = inFileTypes;
	}
	public String getFindText()
	{
		return fieldFindText;
	}
	public void setFindText(String inFindText)
	{
		fieldFindText = inFindText;
	}
	public String getReplaceText()
	{
		return fieldReplaceText;
	}
	public void setReplaceText(String inReplaceText)
	{
		fieldReplaceText = inReplaceText;
	}
	public File getRootDirectory()
	{
		return fieldRootDirectory;
	}
	public void setRootDirectory(File inRootDirectory)
	{
		fieldRootDirectory = inRootDirectory;
	}
	public int getNumFilesChanged()
	{
		return fieldNumFilesChanged;
	}
	public void setNumFilesChanged(int inNumFilesChanged)
	{
		fieldNumFilesChanged = inNumFilesChanged;
	}
	public String getSearchPath()
	{
		return fieldSearchPath;
	}
	public void setSearchPath(String inSearchPath)
	{
		fieldSearchPath = inSearchPath;
	}
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager( PageManager pageManager )
	{
		fieldPageManager = pageManager;
	}
}
