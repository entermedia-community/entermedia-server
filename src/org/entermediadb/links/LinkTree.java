/*
 * Created on Dec 22, 2004
 */
package org.entermediadb.links;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openedit.page.Page;
import org.openedit.util.PathUtilities;

/**
 * This keeps track of a list of Link objects: ie. <a href="sdsdfdf">sddsfsd</a>
 * @author cburkey
 *
 */
public class LinkTree implements Serializable
{
	private static final long serialVersionUID = 766014286370105378L;
	protected Link fieldRootLink;
	protected Link fieldSelectedLink;
	protected long fieldNextId;
	protected long fieldLastModified;
	protected Page fieldPage;
	protected String fieldId;
	protected LinkedList fieldCrumbs;
	
	public LinkTree()
	{
		String a = "";
		
	}
	
	/**
	 * @param inString
	 * @return
	 */
	public Link getLink(String inId)
	{
		if (getRootLink() != null && getRootLink().getId() != null && getRootLink().getId().equals(inId))
		{
			return getRootLink();
		}

		if( getRootLink() == null)
		{
			return null;
		}
		return getRootLink().getDecendant(inId);
	}
	public List getLinkChildren(String inId)
	{
		Link link = getLink(inId);
		if ( link != null)
		{
			return link.getChildren();
		}
		return null;
	}
	/**
	 * @return
	 */
	public List renderAsList()
	{
		if ( getRootLink() != null)
		{
			return getRootLink().list();
		}
		return null;
	}

	public Link getRootLink()
	{
		return fieldRootLink;
	}
	public void setRootLink(Link inRootLink)
	{
		fieldRootLink = inRootLink;
	}
	public Link getSelectedLink()
	{
		return findSelectedLink(getRootLink());
	}
	public List findSelectedParents(int inParentLevel)
	{
		//get the selected link
		List parents = new ArrayList();
		Link parent = getSelectedLink(); //Most specific on bottom
		while ( parent != null )
		{
			parents.add(0,parent);
			parent = parent.getParentLink();
		}
		parents.addAll(getCrumbs()); //Crumbs become more specific
		if ( parents.size() > inParentLevel)
		{
			return parents.subList(inParentLevel, parents.size());
		}
		return null;
	}
	public Link findSelectedParentLink(int inParentLevel)
	{
		//get the selected link
		List parents = new ArrayList();
		Link parent = getSelectedLink();
		while ( parent != null )
		{
			parents.add(0,parent);
			parent = parent.getParentLink();
		}
		if ( parents.size() > inParentLevel)
		{
			Link selected = (Link)parents.get(inParentLevel);
			return selected;
		}
		return getRootLink();
	}
	
	/**
	 * @param inRootLink
	 * @return
	 */
	private Link findSelectedLink(Link inRootLink)
	{
		if ( inRootLink == null)
		{
			return getRootLink();
		}
		if ( inRootLink.isSelected())
		{
			return inRootLink;
		}
		for (Iterator iter = inRootLink.getChildren().iterator(); iter.hasNext();)
		{
			Link element = (Link) iter.next();
			Link link = findSelectedLink(element);
			if  ( link != null)
			{
				return link;
			}
		}
		return null;
	}
	/**
	 * @param inLink
	 */
	public void removeLink(Link inLink)
	{
		if(inLink == null){
			return;
		}
		Link parent = inLink.getParentLink();
		if ( parent != null)
		{
			parent.removeChild( inLink );
		}
		else
		{
			setRootLink(null);
		}
		if( inLink.hasChildren())
		{
			for (Iterator iter = new ArrayList(inLink.getChildren()).iterator(); iter.hasNext();)
			{
				Link link = (Link) iter.next();
				removeLink(link);
			}
		}
	}
	/**
	 * @param inLink
	 */
	public void moveUp(Link inLink)
	{
		Link parent = inLink.getParentLink();
		if ( parent != null)
		{
			parent.moveUp(inLink);
		}
	}
	/**
	 * @param inLink
	 */
	public void moveDown(Link inLink)
	{
		Link parent = inLink.getParentLink();
		if ( parent != null)
		{
			parent.moveDown(inLink);
		}
	}
	/**
	 * @param inLink
	 */
	public void moveRight(Link inLink)
	{
		//put it as a child of my upper brother
		Link parent = inLink.getParentLink();
		if( parent != null)
		{
			Link brother = parent.getChildAbove(inLink);
			if ( brother != null)
			{
				parent.removeChild(inLink);
				brother.addChild(inLink);
			}
		}
	}
	/**
	 * @param inLink
	 */
	public void moveLeft(Link inLink)
	{
		//pull it up one
		Link parent1 = inLink.getParentLink();
		if ( parent1 != null)
		{
			Link parent2 = parent1.getParentLink();
			if ( parent2 != null)
			{
				
				parent1.removeChild(inLink);
				parent2.addChildNearLocation(inLink, parent1);
			}
		}
	}
	/**
	 * @return
	 */
	public String nextId()
	{
		if( fieldNextId == 0)
		{
			fieldNextId = System.currentTimeMillis();
		}
		return String.valueOf(fieldNextId++);
	}

	public void changeLinkId(Link inLink, String inNewId)
	{
		inLink.setId(inNewId);
	}
	/**
	 * @param inParentId
	 * @param inLink
	 */
	public Link addLink(String inParentId, Link inLink)
	{
		if( getRootLink() == null || getRootLink().getId().equals(inLink.getId()))
			//if we don't have a root or we're re-reading the root, then make this the root
		{
			setRootLink( inLink );
			return inLink;
		}
		
		Link oldLink = getLink(inLink.getId());
		if ( oldLink != null && oldLink.getParentLink() != null)
		{
			oldLink.getParentLink().removeChild(oldLink);
		}

		//find the parent node if none then use the root node
		Link parentLink = getLink(inParentId);
		if ( parentLink == null )
		{
			parentLink = getRootLink();			
		}
		parentLink.addChild(inLink);
		return inLink;
	}
	/**
	 * @param inPath
	 * @return
	 */
	public Link findSelectedLinkByUrl(String inPath)
	{
		if ( getRootLink() == null)
		{
			return null;
		}
		else if ( getRootLink().getUrl() != null &&  getRootLink().getUrl().equals( inPath ) )
		{
			return getRootLink();
		}
		Link selected = getSelectedLink();
		if ( selected != null)
		{
			//look in all the parents first since this is faster
			while( selected != null)
			{
				//loop up the tree and check each parent link. 
				//This keeps us near the place we where before
				Link found =  findLinkByUrl( inPath, selected );
				if ( found != null)
				{
					return found;
				}
				selected = selected.getParentLink();
			}
		}
		else
		{
			return findLinkByUrl( inPath, getRootLink() );
		}
		return null;
	}

	public Link findLinkByUrl( String inPath, Link inLink )
	{
		if ( inPath == null)
		{
			return null;
		}
		if ( inLink == null)
		{
			return null;
		}
		if ( inLink.getUrl() != null )
		{
			if (PathUtilities.match(inPath, inLink.getUrl()))
			{
				return inLink;
			}
		}
		if ( inLink.hasChildren() )
		{
			for (Iterator iter = inLink.getChildren().iterator(); iter.hasNext();)
			{
				Link link = (Link) iter.next();
				Link found = findLinkByUrl( inPath, link);
				if ( found != null )
				{
					return found;
				}
			}
		}
		return null;
	}

	
	public String checkUnique( String inOriginal )
	{
		return checkUnique( inOriginal, inOriginal, 0);
	}
	/**
	 * @param inLinks
	 * @param inId
	 * @return
	 */
	private String checkUnique( String inOriginal, String inId, int count)
	{
		Link id = getLink(inId);
		if ( id != null)
		{
			count ++;
			return checkUnique(inOriginal,inOriginal + count,count);
		}
		if ( count > 0)
		{
			return inOriginal + count;
		}
		return inOriginal;
	}
	/**
	 * @param inSelectedLink
	 */
	public void setSelectedLink(String inSelectedLink)
	{
		Link link = getLink(inSelectedLink);
		setSelectedLink( link );
	}
	public void setSelectedLink(Link link)
	{
		if ( link == null)
		{
			if( getCrumbs().size() > 0)
			{
				clearSelection( getSelectedLink() );
			}
			return;
		}
		else
		{
			clearCrumbs(); //We have hit a known place that can use normall crumbs
			clearSelection( getRootLink() );
		}
		//Look for any children that may also have the same url. i.e. about.html could be in the about category
		if ( link.hasChildren())
		{
			for (Iterator iter = link.getChildren().iterator(); iter.hasNext();)
			{
				Link child = (Link) iter.next();
				Link hit = findLinkByUrl(link.getUrl(),child);
				if ( hit != null )
				{
					hit.setSelected(true);
				}			
			}		
		}
		link.setSelected(true);
	}
	protected void clearSelection(Link inRootLink)
	{
		if( inRootLink == null)
		{
			return;
		}
		inRootLink.setSelected(false);
		if ( inRootLink.hasChildren() )
		{
			for (Iterator iter = inRootLink.getChildren().iterator(); iter.hasNext();)
			{
				Link link = (Link) iter.next();
				clearSelection(link);
			}
		}
	}
	/**
	 * @param inString
	 * @param inLink
	 * @param inI
	 */
	public Link insertLink(String inParentId, Link inLink)
	{
		//duplicate code above
		Link oldLink = getLink(inLink.getId());
		if ( oldLink != null && oldLink.getParentLink() != null)
		{
			oldLink.getParentLink().removeChild(oldLink);
		}

		if( getRootLink() == null )
		{
			setRootLink( inLink );
			return inLink;
		}

		//find the parent node if none then use the root node
		Link parentLink = getLink(inParentId);
		if ( parentLink == null )
		{
			parentLink = getRootLink();			
		}
		parentLink.insertChild(inLink);
		return inLink;

	}
    /**
     * @return
     */
    public long getLastModified()
    {
        return fieldLastModified;
    }
    public void setLastModified(long inLastModified)
    {
        fieldLastModified = inLastModified;
    }
	public String findRedirect(String inPath)
	{
		
		Link link = findLinkByUrl(inPath, getRootLink() );
		if ( link != null)
		{
			String redirectPath = link.getRedirectPath();
			if (redirectPath != null)
			{
				//path  /abc/345.html -> http://xyz/abc/345.html
				int indestpath = redirectPath.indexOf("*"); //http://xyz/*
				int inpath = link.getUrl().indexOf("*"); // /abc/*
			    if (indestpath > -1 && inpath > -1 )
				{
			    	//this is a dynamic redirect path
			    	//http://xyz/
			    	//take off a part of the path before the *?
			    	String begin = redirectPath.substring(0,indestpath);
			    	String ending = inPath.substring(inpath, inPath.length());

					redirectPath = begin + ending;
			    }
				return redirectPath;
			}
		}
		return null;
	}
	
	public String getId()
	{
		return fieldId;
	}
	public void setId(String inId)
	{
		fieldId = inId;
	}
	public boolean isDraft() {
		return getPage().isDraft();
	}
	public void clearCrumbs()
	{
		getCrumbs().clear();
	}
	public LinkedList getCrumbs()
	{
		if (fieldCrumbs == null)
		{
			fieldCrumbs = new LinkedList();
		}
		return fieldCrumbs;
	}
	public void setCrumbs(LinkedList inCrumbs)
	{
		fieldCrumbs = inCrumbs;
	}
	
	protected Link makeCrumb(String inPath, String inText)
	{
		Link link = new Link();
		link.setPath(inPath);
		link.setText(inText);
		link.setId(inText);
		link.setSelected(true);
		return link;
	}
	
	public void addCrumb(String inPath, String inText)
	{
		Link link = makeCrumb(inPath, inText);
		getCrumbs().add(link);
	}
	
	public void prependCrumb(String inPath, String inText)
	{
		Link link = makeCrumb(inPath, inText);
		getCrumbs().addFirst(link);
	}
	
	public Page getPage()
	{
		return fieldPage;
	}
	public void setPage(Page inPage)
	{
		fieldPage = inPage;
	}
	public String getPath()
	{
		return getPage().getPath();
	}
}
