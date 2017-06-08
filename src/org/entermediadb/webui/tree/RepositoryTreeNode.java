/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/

package org.entermediadb.webui.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.OpenEditRuntimeException;
import org.openedit.page.Page;
import org.openedit.page.PageSettings;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.Repository;
import org.openedit.repository.RepositoryException;
import org.openedit.util.PathUtilities;


/**
 * This class represents a node in a {@link RepositoryTreeModel}.
 *
 * @author Matt Avery, mavery@einnovation.com
 */
public class RepositoryTreeNode extends DefaultWebTreeNode implements Comparable
{
	//public static final DefaultWebTreeNode ERROR_NODE = new DefaultWebTreeNode( "Error accessing tree node." );
	protected Repository fieldRepository;
	protected ContentItem fieldContentItem;
	protected PageManager fieldPageManager;
	protected boolean fieldVirtual;
	protected String fieldUrl;
	
	private static final Log log = LogFactory.getLog(RepositoryTreeNode.class);
	/**
	 * Create a new <code>PageTreeNode</code>.
	 *
	 * @param inFile The file that this node represents
	 * @param inPath The path to the file, relative to the site context root
	 * @throws RepositoryException
	 */
	public RepositoryTreeNode( Repository inRepository, ContentItem inContentItem, String inUserPath )
	{
		super( extractName( inContentItem ) );
		
//		String  id = PathUtilities.makeId(inUserPath);
//		id = id.replace('/', '_');
		setId( inUserPath );
		fieldRepository = inRepository;
		fieldContentItem = inContentItem;
		setLeaf(!getContentItem().isFolder());
	}
	
	private static String extractName( ContentItem inItem )
	{
		String path = inItem.getPath();
		if ( "/".equals( path ) )
		{
			return "Root";
		}
		String name = PathUtilities.extractFileName(path);
		return name;
	}

	/**
	 * Get the first child of this node with the given name.
	 *
	 * @param inName The node name
	 *
	 * @return The node, or <code>null</code> if no such child could be found
	 */
	public RepositoryTreeNode getChild(String inName)
	{
		for (Iterator iter = getChildren().iterator(); iter.hasNext();)
		{
			RepositoryTreeNode child = (RepositoryTreeNode) iter.next();

			if ((child.getName() != null) && child.getName().equals(inName))
			{
				return child;
			}
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see DefaultWebTreeNode#getChildren()
	 */
	public List getChildren() 
	{
		if (fieldChildren == null)
		{
			fieldChildren = new ArrayList();
			reloadChildren();
		}

		return fieldChildren;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(Object)
	 */
	public int compareTo(Object o)
	{
		if (o == null)
		{
			return 1;
		}

		if (o instanceof RepositoryTreeNode)
		{
			RepositoryTreeNode node = (RepositoryTreeNode) o;

			return getContentItem().getPath().toLowerCase().compareTo(node.getContentItem().getPath().toLowerCase());
		}
		else
		{
			return 0;
		}
	}

	/**
	 * Find the descendant of this node with the given path.
	 *
	 * @param inPath The path to find
	 *
	 * @return The node at the given path, or <code>null</code> if it could not be found
	 */
	public RepositoryTreeNode findNode(String inPath)
	{
		// Quick initial checks...
		if (!inPath.startsWith("/"))
		{
			inPath = "/" + inPath;
		}

		if (inPath.equals("") || inPath.equals("/"))
		{
			return this;
		}

		int beforeSlashIndex = 0;

		if (inPath.startsWith("/"))
		{
			beforeSlashIndex = 1;
		}

		int nextSlashIndex = inPath.indexOf('/', beforeSlashIndex);

		if (nextSlashIndex < 0)
		{
			nextSlashIndex = inPath.length();
		}

		String childName = inPath.substring(beforeSlashIndex, nextSlashIndex);

		RepositoryTreeNode child = getChild(childName);

		if (child == null)
		{
			return null;
		}
		else
		{
			return child.findNode(inPath.substring(nextSlashIndex));
		}
	}

	/**
	 * Reload the children of this page tree node.
	 */
	public void reloadChildren() 
	{
		getChildren().clear();

		List directories = new ArrayList();
		List files = new ArrayList();

		if (getContentItem().isFolder())
		{
			//TODO: Clean up this class with an archive class or some other class
			Page thisdir = null;
			String thisdirpath = getContentItem().getPath();
			if( !thisdirpath.endsWith("/"))
			{
				thisdirpath = thisdirpath + "/";
			}
			thisdir = getPageManager().getPage(thisdirpath);
			boolean checkpermissions = Boolean.parseBoolean( thisdir.getProperty("oecheckfilepermissions") );
			boolean loadfallback = true;
			String load = thisdir.getProperty("oeshowfallbackfiles");
			if( load != null )
			{
				loadfallback = Boolean.parseBoolean(load);
			}
			Collection actualfiles = getChildPaths(false);
			
			Collection allfiles = new ArrayList();
			allfiles.addAll( actualfiles );
			allfiles.addAll( getChildPaths(true) );

			Set addedfiles = new HashSet();
			for ( Iterator iterator = allfiles.iterator(); iterator.hasNext(); )
			{
//				Object  obj = iterator.next();
//				if ( !(obj instanceof ContentItem))
//				{
//					throw new OpenEditRuntimeException("Must be type " + ContentItem.class + " " + obj.getClass());
//				}
//				childItem = (ContentItem) obj;
				
				String npath = (String)iterator.next();
				String name = PathUtilities.extractFileName(npath);
				if( addedfiles.contains(name))
				{
					continue;
				}
				addedfiles.add(name);
				RepositoryTreeNode child = createNode( thisdirpath + name );
				if( !actualfiles.contains(npath))
				{
					child.setVirtual(true);
				}
				
//				boolean okToAdd = true;
//				if( checkpermissions && getFilter() != null && !child.isLeaf() )
//				{
//					okToAdd = getFilter().passes(npath);
//				}
				child.setParent(this);

				if (child.isLeaf())
				{
					//Make sure it's not a fallback folder
//					String tmp = thisdirpath + name + "/";
//					Page folderpage = getPageManager().getPage(tmp	);
//					if( folderpage.isFolder() )
//					{
//						child.getContentItem().setActualPath(folderpage.getAlternateContentPath());
//						directories.add(child);						
//					}
//					else
//					{
						files.add(child);
//					}	
				}
				else
				{
					directories.add(child);
				}
			}
//			if( loadfallback)
//			{
//				//clear cache not needed since the permission check has clear the cache getPageManager().clearCache(thisdirpath);
//				PageSettings fallback = thisdir.getPageSettings().getFallback();
//				if( fallback != null)
//				{
//					Set existingDirNames = new HashSet();
//					for (Iterator iterator = directories.iterator(); iterator.hasNext();)
//					{
//						RepositoryTreeNode item = (RepositoryTreeNode) iterator.next();
//						existingDirNames.add(item.getName());
//					}
//					Set existingFileNames = new HashSet();
//					for (Iterator iterator = files.iterator(); iterator.hasNext();)
//					{
//						RepositoryTreeNode item = (RepositoryTreeNode) iterator.next();
//						existingFileNames.add(item.getName());
//					}
//					String dirparent = PathUtilities.extractDirectoryPath(fallback.getPath());
//					//ContentItem basedir = getPageManager().getRepository().get(dirparent);
//					RepositoryTreeNode child = createNode( dirparent );
//					for (Iterator iterator = child.getChildPaths().iterator(); iterator.hasNext();)
//					{
//						String inbasepath = (String)iterator.next();
//						//ContentItem basechildItem = (ContentItem) iterator.next();
//						boolean okToAdd = true;
//						if( getFilter() != null )
//						{
//							okToAdd = getFilter().passes(inbasepath);
//						}
//	
//						if (!okToAdd)
//						{
//							continue;
//						}
//	
//						RepositoryTreeNode node = createNode( inbasepath );
//						node.setParent(this);
//	
//						if( node.isLeaf())
//						{
//							files.add(node);
//							node.setVirtual(true);
//							//If on list already then set the full path. Kind of annoying but otherwise can't click on the virtual one
//							if( existingFileNames.contains(node.getName()))
//							{
//								node.setUrl(inbasepath);
//							}
//						}
//						else 
//						{
//							if( !existingDirNames.contains(node.getName()))
//							{
//								directories.add(node);  //Only add if not already in there
//							}
//							node.setVirtual(true);
//						}
//					}
//				}
//			}				
			// Make sure the files appear in lexicographically increasing
			// order, with all the directories appearing before all the files.
			Collections.sort(directories);
			Collections.sort(files);
			getChildren().addAll(directories);
			getChildren().addAll(files);
		}
	}


	protected Collection getChildPaths(boolean includefallback)
	{
		if ( getContentItem().isFolder() )
		{
				Collection paths = getPageManager().getChildrenPaths(getContentItem().getPath(),includefallback);
				return paths;
//			if ( getContentItem() instanceof FileItem)
//			{
//				try
//				{
//					FileItem item = (FileItem)getContentItem();
//					File[] files = item.getFile().listFiles();
//					String between = "/";
//					if( getContentItem().getPath().endsWith("/"))
//					{	
//						between = "";
//					}
//					for (int i = 0; i < files.length; i++)
//					{
//						items.add( getRepository().get( getContentItem().getPath() + between +  files[i].getName() ) );
//					}
//			}
//			else
//			{
//				throw new OpenEditRuntimeException("Tree only works with FileItem class");
		}
		return Collections.EMPTY_LIST;
	}

	/**
	 * Method createPageTreeNode.
	 *
	 * @param childFile
	 * @param path
	 *
	 * @return PageTreeNode
	 */
	protected RepositoryTreeNode createNode(String inPath) 
	{
//		//inPath may be the name of the fallback file. Rebuild the path 
//		String path = null;
//		if( getParent() == null)
//		{
//			path = "/" + PathUtilities.extractFileName(inPath);
//		}
//		else
//		{
//			path = getURL() + "/" + PathUtilities.extractFileName(inPath);
//		}
//		ContentItem childItem = null;
//		try
//		{
//			if( inPath.endsWith("/") )
//			{
//				childItem = getRepository().getStub(path + "/");
//			}
//			else
//			{
//				childItem = getRepository().getStub(path);
//			}
//		}
//		catch (RepositoryException e)
//		{
//			throw new OpenEditRuntimeException(e);
//		}
//		if( childItem.getActualPath() != null && !path.equals(childItem.getActualPath()))
//		{
//			path = path + "_" + childItem.getActualPath(); //To make sure it is unique use virtual path + actual path on disk drive
//		}
		
		Page folderpage = getPageManager().getPage(inPath);
		if(!inPath.endsWith("/") && !folderpage.exists() && !folderpage.isFolder() )
		{
			Page isfolderpage = getPageManager().getPage(inPath + "/");
			if(isfolderpage.isFolder())
			{
				folderpage = isfolderpage;
			}
		}
			
		
		String  id = PathUtilities.makeId(inPath);
		id = id.replace('/', '_');
	
		RepositoryTreeNode node = new RepositoryTreeNode( getRepository(), folderpage.getContentItem(), id);
		node.setFilter(getFilter());
		//node.setParent(this);
		node.setPageManager(getPageManager());
		
		return node;
	}

	public ContentItem getContentItem()
	{
		return fieldContentItem;
	}
	public void setContentItem( ContentItem contentItem )
	{
		fieldContentItem = contentItem;
	}
	public Repository getRepository()
	{
		return fieldRepository;
	}
	public void setRepository( Repository repository )
	{
		fieldRepository = repository;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	
	//This is really the path
	public String getURL()
	{
		if( fieldUrl != null)
		{
			return fieldUrl;
		}
		if ( getParent() != null)
		{
			String p =getParent().getURL();
			if ( p.endsWith("/"))
			{
				return  p + getName();
			}
			else
			{
				return  p + "/" + getName();
			}
		}
		else
		{
			return getName(); //the root does not need a special URL since it is part of the base path
		}
	}
	
	public void setParent(DefaultWebTreeNode inParent)
	{
		super.setParent(inParent);
		setUrl( getURL() );
	}
	protected void setUrl(String inPath)
	{
		fieldUrl = inPath;
	}
	
	public void setVirtual(boolean inVirtual)
	{
		fieldVirtual = inVirtual;
	}
	public String getIconSet()
	{
		if( isVirtual())
		{
			return "linked";
		}
		return super.getIconSet();
	}

	public boolean isVirtual()
	{
		if( fieldVirtual)
		{
			return true;
		}
		if( getParent() != null )
		{
			return ((RepositoryTreeNode)getParent()).isVirtual();
		}
		return fieldVirtual;
	}
	public String toString()
	{
		return getURL();
	}
	
}
