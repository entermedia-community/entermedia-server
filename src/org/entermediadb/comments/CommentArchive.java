/*
 * This class needs to be re-created to be based on XmlFileSearcher
 * Created on Oct 18, 2006
 */
package org.entermediadb.comments;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.entermediadb.asset.Asset;
import org.openedit.OpenEditException;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.FileUtils;
import org.openedit.util.GenericsUtil;
import org.openedit.util.LocaleManager;
import org.openedit.util.XmlUtil;

public class CommentArchive
{
	private static final Log log = LogFactory.getLog(CommentArchive.class);
	protected UserManager fieldUserManager;
	protected PageManager fieldPageManager;
	protected XmlUtil fieldXmlUtil;
	protected LocaleManager fieldLocaleManager;
	public LocaleManager getLocaleManager()
	{
		if (fieldLocaleManager == null)
		{
			fieldLocaleManager = new LocaleManager();
			
		}

		return fieldLocaleManager;
	}

	public void setLocaleManager(LocaleManager inLocaleManager)
	{
		fieldLocaleManager = inLocaleManager;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public XmlUtil getXmlUtil()
	{
		if (fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}
	
	protected Comment loadComment(Element inCommentElement) throws ParseException
	{
		Comment comment = new Comment();
		comment.setLocaleManager(getLocaleManager());
		String username = inCommentElement.attributeValue("username");
		if( username != null)
		{
			User user = getUserManager().getUser(username);
			comment.setUser(user);
		}
		
		comment.setComment(inCommentElement.getTextTrim());
		comment.setCreationDate(inCommentElement.attributeValue("date"));

		return comment;
	}
	public Set<User> loadUsersWhoCommented(String inCatalogId, String inPath)
	{
		Set<User> usernames = GenericsUtil.createSet();
		for (Iterator iterator = loadComments(inCatalogId, inPath).iterator(); iterator.hasNext();)
		{
			Comment c = (Comment) iterator.next();

			usernames.add(c.getUser());
		}
		return usernames;
	}

	public Collection loadComments(Asset inAsset)
	{
		String path = findPath( inAsset.getCatalogId(), inAsset.getSourcePath() );
		Page page = getPageManager().getPage(path);
		if(!page.exists()){
			 path = "/WEB-INF/data/" +  inAsset.getCatalogId() + "/comments/" + inAsset.getSourcePath() + "/folder.xml";
			 page = getPageManager().getPage(path);
		}
		
		return loadComments(page);
		
	}
	public Collection loadComments(String path)
	{
		Page page = getPageManager().getPage(path);
		return loadComments(page);
	}
	
	public Collection loadComments(String inCatalogId, String inPath)
	{
		String path = findPath(inCatalogId, inPath);
		Page page = getPageManager().getPage(path);
//		if(!page.exists()){
//			 path = "/WEB-INF/data/" +  inCatalogId + "/comments/" + inPath + "/folder.xml";
//			 page = getPageManager().getPage(path);
//		}
		return loadComments(page);
	}

	public String findPath(String inCatalogId, String inPath) {
		String path = "/WEB-INF/data/"+inCatalogId + "/comments/" + inPath + ".xml";
		return path;
	}

	
	protected Collection loadComments(Page inPage) throws OpenEditException
	{
		List<Comment> comments = GenericsUtil.createList();

		if ( inPage.exists() )
		{
			log.debug( "Loading comments for page " + inPage.getPath() );
			Reader reader = inPage.getReader();
			try
			{
				Element root = getXmlUtil().getXml(reader, inPage.getCharacterEncoding());
				for (Object o: root.elements("comment"))
				{
					Element element = (Element) o;
					Comment comment = loadComment(element);
					comments.add(comment);
				}
			}
			catch ( Exception ex)
			{
				throw new OpenEditException(ex);
			}
			finally
			{
				FileUtils.safeClose(reader);
			}
		}
		//log.info("Found " + comments.size());
		return new ListHitTracker(comments);
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}
	
	public void addComment(String inCatalogId, String inSourcePath, Comment inComment)
	{
		String path = findPath(inCatalogId, inSourcePath);
		Page page = getPageManager().getPage(path);
		addComment(page, inComment);
	}
	
	public void removeComment(String inCatalogId, String inSourcePath, Comment inComment)
	{
		String path = findPath(inCatalogId, inSourcePath);
		Page page = getPageManager().getPage(path);
		removeComment(page, inComment);
	}
	
	protected void addComment(Page inPage, Comment inComment)
	{
		List comments = new ArrayList(loadComments(inPage));
		
		if( comments.isEmpty() )
		{
			comments.add(inComment);
		}
		else
		{
			comments.add(0, inComment);
		}
		saveComments(inPage, comments);
		//inPage.get.pu setProperty("comments", comments);
		//getPageManager().
		//inPage.getp
	}
	
	public Comment getLastComment(String inCatalogId,String inSourcePath)
	{
		Collection comments = loadComments(inCatalogId, inSourcePath);
		if(comments.size() > 0)
		{
			return (Comment)comments.toArray()[comments.size() - 1];
		}
		return null;
	}
	
	protected void removeComment(Page inPage, Comment inComment)
	{
		Collection comments = loadComments(inPage);
		Comment toremove = null;
		for (Iterator iterator = comments.iterator(); iterator.hasNext();)
		{
			Comment c = (Comment) iterator.next();
			//Wow terrible code
			if(c.getComment().equals(inComment.getComment()))
			{
				if(c.getDate().equals(inComment.getDate()) && c.getUser().getId().equals(inComment.getUser().getId()))
				{
					toremove = c;
					break;
				}
			}
		}
		if(toremove != null)
		{
			comments.remove(toremove);
		}
		saveComments(inPage, comments);
	}
	
	public void saveComments(String inPath, Collection inComments)
	{
		if(inPath.endsWith("/"))
		{
			inPath = inPath + "folder.xml";
		}
		if (!inPath.endsWith(".xml"))
		{
			inPath = inPath + ".xml";
		}
		Page page = getPageManager().getPage(inPath);
		saveComments(page, inComments);
	}
	
	public void saveComments(Page inPage, Collection inComments) throws OpenEditException
	{
		Element root = DocumentHelper.createDocument().addElement("comments");
		for (Object o: inComments)
		{
			Comment com = (Comment) o; //	<comment author="admin" date="Feb 18, 2005 2:42:29 PM">This is a snide remark</comment>

			Element comment = root.addElement("comment");
			if( com.getUser() != null)
			{
				comment.addAttribute("username",com.getUser().getUserName());
			}
			
			comment.addAttribute("date",com.getCreationDate());
			comment.setText(com.getComment());
		}
		try
		{
			//TODO: Add locking
			File tmp = File.createTempFile("comment", "junk");
			getXmlUtil().saveXml(root.getDocument(), tmp);
			FileItem item = new FileItem();
			item.setFile(tmp);
			item.setPath(inPage.getPath());
			Page tmpPage = new Page(inPage);
			tmpPage.setContentItem(item);

			getPageManager().copyPage(tmpPage, inPage); //Copy over as a tmp file in case there is a problem
			tmp.delete();
		}
		catch (IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}
}
