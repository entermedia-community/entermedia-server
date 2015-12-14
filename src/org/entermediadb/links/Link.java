/*
 * Created on Dec 22, 2004
 */
package org.entermediadb.links;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.openedit.util.PathUtilities;

/**
 * @author cburkey
 * 
 */
public class Link implements Serializable {

	private static final long serialVersionUID = 6696306680268063277L;

	protected String fieldPath;
	protected String fieldText;
	protected String fieldId;
	protected boolean fieldSelected;
	protected Link fieldParentLink;
	protected List fieldChildren;
	protected String fieldUserData;
	protected String fieldRedirectPath;
	protected String fieldAccessKey;
	protected boolean fieldAutoLoadChildren;
	protected String fieldPrefix;
	protected String fieldPostfix;
	protected int fieldRank = 5000;
	protected String fieldConfirmText;
	
	public String getConfirmText()
	{
		return fieldConfirmText;
	}

	public void setConfirmText(String inConfirmText)
	{
		fieldConfirmText = inConfirmText;
	}

	public String getPrefix() {
		return fieldPrefix;
	}

	public void setPrefix(String fieldPrefix) {
		this.fieldPrefix = fieldPrefix;
	}

	public String getPostfix() {
		return fieldPostfix;
	}

	public void setPostfix(String fieldPostfix) {
		this.fieldPostfix = fieldPostfix;
	}

	public String getText() {
		return fieldText;
	}

	public void setText(String inText) {
		fieldText = inText;
	}

	/** I could not decide what to call this TODO: Remove URL */
	public String getPath() {
		return fieldPath;
	}

	public String getHref() {
		return getPath();
	}

	public String getUrl() {
		return getPath();
	}

	public String getPageName() {
		return PathUtilities.extractPageName(getPath());
	}

	public void setPath(String inString) {
		fieldPath = inString;
	}

	public String getId() {
		return fieldId;
	}

	public void setId(String inId) {
		fieldId = inId;
	}

	public int getDepth() {
		int i = 0;
		Link parent = getParentLink();
		while (parent != null) {
			i++;
			parent = parent.getParentLink();
		}
		return i;
	}

	public boolean hasChildren() {
		return fieldChildren != null && fieldChildren.size() > 0;
	}

	/**
	 * Divides the children into rows
	 * 
	 * @param inRowCount
	 * @return
	 */
	public List getChildrenInRows(int inColCount) {
		double rowscount = (double) getChildren().size() / (double) inColCount;

		List rows = new ArrayList();
		for (int i = 0; i < rowscount; i++) {
			int start = i * inColCount;
			int end = i * inColCount + inColCount;
			// start = Math.min
			List sublist = getChildren().subList(start,
					Math.min(getChildren().size(), end));
			rows.add(sublist);
		}
		return rows;
	}

	public List getChildren() {
		if (fieldChildren == null) {
			fieldChildren = new ArrayList();
		}
		return fieldChildren;
	}

	public void setChildren(List inChildren) {
		fieldChildren = inChildren;
	}

	public Link getParentLink() {
		return fieldParentLink;
	}

	public void setParentLink(Link inParentLink) {
		fieldParentLink = inParentLink;
	}

	/**
	 * @param inLink
	 */
	public void addChild(Link inLink) {
		inLink.setParentLink(this);
		getChildren().add(inLink);
	}

	public void insertChild(Link inLink) {
		inLink.setParentLink(this);
		getChildren().add(0, inLink);
	}

	/**
	 * @param inLink
	 */
	public void removeChild(Link inLink) {
		getChildren().remove(inLink);
		inLink.setParentLink(null);
	}

	/**
	 * @param inLink
	 */
	public void moveUp(Link inLink) {
		int index = getChildren().indexOf(inLink);
		if (index != -1 && index != 0) {
			getChildren().remove(index);
			index--;
			getChildren().add(index, inLink);
		}
	}

	/**
	 * @param inLink
	 */
	public void moveDown(Link inLink) {
		int index = getChildren().indexOf(inLink);
		if (index != -1 && index != getChildren().size() - 1) {
			getChildren().remove(index);
			index++;
			getChildren().add(index, inLink);
		}
	}

	/**
	 * @param inLink
	 * @return
	 */
	public Link getChildAbove(Link inLink) {
		int count = getChildren().indexOf(inLink);
		if (count != -1 && count != 0) {
			count--;
			Link brother = (Link) getChildren().get(count);
			return brother;
		}
		return null;
	}

	public Link getChildBelow(Link inLink) {
		int count = getChildren().indexOf(inLink);
		if (count != -1 && count != getChildren().size() - 1) {
			count++;
			Link brother = (Link) getChildren().get(count);
			return brother;
		}
		return null;
	}

	public Link getDecendant(String inId) {
		for (Iterator iter = getChildren().iterator(); iter.hasNext();) {
			Link child = (Link) iter.next();
			if (child.getId().equals(inId)) {
				return child;
			} else {
				Link decendant = child.getDecendant(inId);
				if (decendant != null) {
					return decendant;
				}
			}
		}
		return null;
	}

	/**
	 * @param inLink
	 * @param inParent1
	 */
	public void addChildNearLocation(Link inLink, Link inParent1) {
		int count = getChildren().indexOf(inParent1);
		if (count != -1) {
			getChildren().add(count + 1, inLink);
		} else {
			getChildren().add(inLink);
		}
		inLink.setParentLink(this);
	}

	/**
	 * This is a flat list of links. Useful for generating menus or trees in
	 * velocity
	 * 
	 * @return
	 */
	public List list() {
		ArrayList fieldAllLinks = new ArrayList();
		addLinksToList(this, fieldAllLinks);
		return fieldAllLinks;
	}

	/**
	 * @param inRootLink
	 * @param inAllLinks
	 */
	protected void addLinksToList(Link inRootLink, List inAllLinks) {
		inAllLinks.add(inRootLink);
		if (inRootLink.hasChildren()) {
			for (Iterator iter = inRootLink.getChildren().iterator(); iter
					.hasNext();) {
				Link element = (Link) iter.next();
				addLinksToList(element, inAllLinks);
			}
		}
	}

	public String getUserData() {
		return fieldUserData;
	}

	public void setUserData(String inUserData) {
		fieldUserData = inUserData;
	}

	public boolean isSelected() {
		return fieldSelected;
	}

	public void setSelected(boolean inSelected) {
		fieldSelected = inSelected;
	}

	/**
	 * @return
	 */
	public boolean isChildSelected() {
		if (isSelected()) {
			return true;
		}
		if (hasChildren()) {
			for (Iterator iter = getChildren().iterator(); iter.hasNext();) {
				Link child = (Link) iter.next();
				if (child.isChildSelected()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param inUrl
	 * @return
	 */
	public boolean isChild(Link inUrl) {
		for (Iterator iter = getChildren().iterator(); iter.hasNext();) {
			Link element = (Link) iter.next();
			if (element == inUrl) {
				return true;
			}
			if (element.isChild(inUrl)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return Returns the redirectPath.
	 */
	public String getRedirectPath() {
		return fieldRedirectPath;
	}

	/**
	 * @param inRedirectPath
	 *            The redirectPath to set.
	 */
	public void setRedirectPath(String inRedirectPath) {
		fieldRedirectPath = inRedirectPath;
	}

	public String getAccessKey() {
		return fieldAccessKey;
	}

	public void setAccessKey(String inAccessKey) {
		fieldAccessKey = inAccessKey;
	}

	public boolean isAutoLoadChildren() {
		return fieldAutoLoadChildren;
	}

	public void setAutoLoadChildren(boolean inAutoLoadChildren) {
		fieldAutoLoadChildren = inAutoLoadChildren;
	}

	public String toString() {
		return getHref();
	}

	public void sortChildren()
	{
		Collections.sort(getChildren(), new Comparator() 
		{
			public int compare(Object arg0, Object arg1)
			{
				Link link1 = (Link)arg0;
				Link link2 = (Link)arg1;
				if( link1.getRank() == link2.getRank())
				{
					if( link1.getText() != null && link2.getText() != null)
					{
						return link1.getText().compareTo(link2.getText());
					}
					return 0;
				}
				if( link1.getRank() > link2.getRank()) 
				{
					return 1;
				}
				else
				{
					return -1;
				}
			}
		});
		
	}
	public String getDirectory()
	{
		String path = PathUtilities.extractDirectoryPath(getPath());
		// urlpath is the address the link came in on
		return path;
	}

	public List getParents()
	{
		List parents = new ArrayList();
		Link parent = this;
		while(parent != null )
		{
			parents.add(0, parent);
			parent = parent.getParentLink();
		}
		return parents;
	}

	public int getRank()
	{
		return fieldRank;
	}

	public void setRank(int inRank)
	{
		fieldRank = inRank;
	}
	
}
