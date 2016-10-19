/*
 * Created on Dec 22, 2004
 */
package org.entermediadb.links;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.modules.workflow.WorkFlow;
import org.entermediadb.webui.tree.WebTree;
import org.entermediadb.webui.tree.WebTreeNodeTreeRenderer;
import org.openedit.OpenEditException;
import org.openedit.PageAccessListener;
import org.openedit.WebPageRequest;
import org.openedit.modules.BaseModule;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.repository.ReaderItem;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

/**
 * TODO: Make the linktree name a param that can be set externally
 * @author cburkey
 *
 */
public class LinkModule extends BaseModule implements PageAccessListener
{
	protected static final String FILESIZE = "filesize";
	protected FileSize fieldFileSize;
	private static final Log log = LogFactory.getLog(LinkModule.class);
	protected boolean fieldForceReload;
	protected WorkFlow fieldWorkFlow;
	
	public FileSize getFileSize()
	{
		//TODO: Move this to its own module method
		if ( fieldFileSize == null)
		{
			fieldFileSize = new FileSize();
			fieldFileSize.setPageManager(getPageManager());
			fieldFileSize.setRoot(getRoot());
		}
		return fieldFileSize;
	}
	public void setFileSize(FileSize inFileSize)
	{
		fieldFileSize = inFileSize;
	}

	public LinkTree loadLinks(WebPageRequest inReq) throws OpenEditException
	{
		return getLinkTree(inReq);
	}
	
	public void setSelectedLinkByUrl(WebPageRequest inReq) throws OpenEditException
	{
		LinkTree tree = getLinkTree(inReq);
		String path = inReq.getContentPage().getPath();
		
		Link newselection = tree.findSelectedLinkByUrl(path);
		tree.setSelectedLink(newselection);	
	}
	protected void init()
	{
		getPageManager().addPageAccessListener(this);
		
	}
	
	protected LinkTree getLinkTree(WebPageRequest inReq) throws OpenEditException
	{

		String path = inReq.findValue("linkpath");
		String forcereload = inReq.findValue("forcereload");
		if(forcereload != null && Boolean.parseBoolean(forcereload)){
			setForceReload(true);
		}
		if ( path == null)
		{
			path = "/links.xml";
		}
		String name = inReq.findValue("linktreename");
		
		if ( name == null)
		{
	    	name = "linktree";
		}

		Page linkpage = getPageManager().getPage(path,inReq ); //may be draft if it exists
//		String id = null;

//		User user = inReq.getUser();
//		if ( user != null)
//		{
//			if ( user.hasProperty("oe_edit_draftmode" ))
//			{
//				String draftPath = PathUtilities.createDraftPath(path);
//				id = name + draftPath;				
//			}
//		}
//		if( id == null)
//		{
		String draft = "false";
		User user = inReq.getUser();
		if( user != null && user.hasPermission("oe_edit_draftmode") )
		{
			draft = "true";
		}
		String id = path + "_" + draft +"_"+ name +"_" + getLoaderName(inReq);
//		}
		LinkTree tree = (LinkTree)inReq.getSessionValue(id);
		
		if ( tree == null && !linkpage.exists())
		{
			//fake tree
			log.info( path + " not found");
			tree = new LinkTree();
			Link root = new Link();
			root.setPath("/index.html");
			root.setId("index");
			root.setText("Index");
			tree.setRootLink(root);
			tree.setPage(linkpage);
			tree.setId(id);
			//throw new OpenEditException("could not find " + slink);
		}
		else
		{
		    boolean needsUpdate = tree == null;
			String selectedLink = null;
			long newModified = linkpage.getLastModified().getTime();
		    if (  tree != null )
		    {
		        needsUpdate = (tree.getLastModified() != newModified);
				if ( tree.getSelectedLink() != null)
				{
					selectedLink = tree.getSelectedLink().getId();
				}
		    }
		    if( isForceReload() )
		    {
		    	//TODO: Change this to work for multiple browsers. 
		    	//TODO: Being used by htmlLinkLoader
		    	setForceReload(false);
		    	needsUpdate = true;
		    }
		    if ( needsUpdate )
		    {
				if (linkpage.exists())
				{
					XmlLinkLoader loader = getLinkLoader(inReq);
				    tree = loader.loadLinks(linkpage, tree);
				    String reloadagain = linkpage.get("forcereload");
				    if( reloadagain != null)
				    {
				    	setForceReload(Boolean.parseBoolean(reloadagain));
				    }
//				    log.info("loaded " + linkpage.getContentItem().getActualPath() + " into " + id);
//					log.info("Count " + tree.getRootLink().getChildren().size());
					tree.setLastModified(newModified);
				}
				else
				{
					log.info("deleted links.xml file");
				}
				tree.setSelectedLink( selectedLink );
				tree.setId(id);
				inReq.putSessionValue(id, tree);
				tree.setPage(linkpage);
		    }
		}
		//need to put the right tree in here if we are in draft mode
//		log.info(tree.getRootLink().hashCode() + "Returning " + id +" Count " + tree.getRootLink().getChildren().size());
		inReq.putPageValue(name,tree);
		//loadSizer(inReq); call the action if needed
 		return tree;
	}
	
	public String getLoaderName(WebPageRequest inReq)
	{
		String linkLoader = null; 
			
		if (linkLoader == null && inReq.getCurrentAction()!= null )
		{
			linkLoader = inReq.getCurrentAction().getChildValue("linkloader");
		}
		if( linkLoader == null)
		{
			linkLoader = inReq.getPage().get("linkloader");
		}
		if( linkLoader != null && linkLoader.equals("SimpleLinkLoader")) //Old name
		{
			linkLoader = "xmlLinkLoader";
		}
		if( linkLoader == null )
		{
			linkLoader = "htmlLinkLoader";
		}
		return linkLoader;
	}
	
	public XmlLinkLoader getLinkLoader(WebPageRequest inReq) throws OpenEditException
	{
		String linkLoader = getLoaderName(inReq);
		return (XmlLinkLoader) getModuleManager().getBean(linkLoader);
	}
	public void loadSizer(WebPageRequest inReq) 
	{
		inReq.putSessionValue(FILESIZE, getFileSize());
	}
	protected void save(WebPageRequest inReq, String inMessage) throws Exception
	{
		LinkTree tree = getLinkTree(inReq);
		XmlLinkLoader loader = new XmlLinkLoader();

		//TODO: Dont hard code the links.xml location
		String path = tree.getPath();
		path = PathUtilities.createDraftPath(path);

		Page linkpage = getPageManager().getPage(path, inReq);
		
		Writer out = new StringWriter();
		try
		{
			loader.saveLinks(tree, out, linkpage.getCharacterEncoding());
		}
		finally
		{
			out.close();
		}
		ReaderItem item = new ReaderItem(linkpage.getPath(),new StringReader(out.toString()),linkpage.getCharacterEncoding() );
		item.setAuthor(inReq.getUser().getUserName());
		item.setMessage(inMessage);
		item.setType(ContentItem.TYPE_EDITED);
		linkpage.setContentItem(item);
		getPageManager().putPage(linkpage);
		//log.info("Saved: " + tree.getPath() );

	}

	public void saveLink(WebPageRequest inReq) throws Exception
	{
		inReq.getUser().setValue("oe_edit_draftmode", "true");

		LinkTree tree = getLinkTree(inReq);
		Link link = tree.getSelectedLink();

		String name = inReq.getRequestParameter("linktext");
		String url = inReq.getRequestParameter("url");
		String userdata = inReq.getRequestParameter("userdata");
		String newLinkId = inReq.getRequestParameter("newlinkid");
		if (newLinkId != null && !newLinkId.equals(link.getId()))
		{
			getLinkTree(inReq).changeLinkId(link, newLinkId);
		}
		link.setUserData(userdata);
		link.setText(name);
		link.setPath(url);
		
		String redirectpath = inReq.getRequestParameter("redirectPath");
		String accessKey = inReq.getRequestParameter("accesskey");
		link.setRedirectPath(redirectpath);
		String autoloadchildren = inReq.getRequestParameter("autoloadchildren");
		link.setAutoLoadChildren(Boolean.parseBoolean(autoloadchildren));
		link.setAccessKey(accessKey);
		save(inReq, "Saved " + name);

	}

	public void appendLink(WebPageRequest inReq) throws Exception
	{
		inReq.getUser().setValue("oe_edit_draftmode", "true");

		LinkTree tree = getLinkTree(inReq);

		Link newlink =  new Link();//getLinkTree().addLink(userdata,getLinkTree().nextId(),parentId,url,name);
		String userdata = inReq.getRequestParameter("userdata");
		newlink.setUserData(userdata);
		String name = inReq.getRequestParameter("linktext");
		if( name == null )
		{
			name = "New Link";
		}
		newlink.setText(name);

		if ( tree.getRootLink() == null)
		{
			newlink.setId("index");
			tree.setRootLink( newlink);
			log.error("No index link. Creating one.");
			return;
		}
		String parentId = inReq.getRequestParameter("linkid");
		if( parentId == null)
		{
			parentId = "index";
		}
		Link link = tree.getLink(parentId);
		if ( link == null)
		{
			log.error("No link selected");
			return;
		}
		String url = inReq.getRequestParameter("url");
		if( url == null )
		{
			url = PathUtilities.extractDirectoryPath(link.getUrl()) + "/newlink.html"; //inReq.getRequestParameter("url");
		}
		String newid = inReq.getRequestParameter("newlinkid");
		if( newid == null)
		{
			newid = tree.nextId();
		}
		newlink.setId(newid);
		
		newlink.setPath(url);
		tree.addLink(parentId,newlink);
		save(inReq, "Added New link " + url);
		
		//getLinkTree(inReq).setSelectedLink(newlink);
		tree.setSelectedLink(newlink.getId());
	}
	
	public void addNewLink(WebPageRequest inReq) throws Exception
	{
		//Should this method replace appendLink?
		inReq.getUser().setValue("oe_edit_draftmode", "true");

		LinkTree tree = getLinkTree(inReq);

		Link newlink =  new Link();
		newlink.setText("New Link");
		newlink.setId(tree.nextId());
		newlink.setPath("/newlink.html");
		
		Link selected = tree.getSelectedLink();
		if (selected != null)
		{
			tree.addLink(selected.getId(), newlink);
		}
		else
		{
			tree.addLink(null, newlink);
		}

		save(inReq, "Added New link");
		tree.setSelectedLink(newlink);
	}
	
	public void copyLink(WebPageRequest inReq) throws Exception
	{
		inReq.getUser().setValue("oe_edit_draftmode", "true");

		LinkTree tree = getLinkTree(inReq);
		Link link = tree.getSelectedLink();
		if ( link == null)
		{
			log.error("No link selected");
			return;
		}
		String name = inReq.getRequestParameter("linktext");
		String url = inReq.getRequestParameter("url");
		String userdata = inReq.getRequestParameter("userdata");
		String newLinkId = inReq.getRequestParameter("newlinkid");

		if (name == null)
		{
			name = link.getText();
		}
		if (url == null)
		{
			url = link.getUrl();
		}
		if (userdata == null)
		{
			userdata = link.getUserData();
		}
		if (newLinkId == null)
		{
			newLinkId = tree.nextId();
		}
		
		link = link.getParentLink();


		Link newlink =  new Link();//getLinkTree().addLink(userdata,getLinkTree().nextId(),parentId,url,name);
		newlink.setUserData(userdata);
		if ( tree.getLink(newLinkId) != null )
		{
			newlink.setId(tree.nextId()); //maybe we should loop this until we're sure this new id is unique
		}
		else
		{
			newlink.setId(newLinkId);
		}
		newlink.setPath(url);
		newlink.setText(name);

		tree.addLink(link.getId(),newlink);
		save(inReq,"Copy link");
		
		//getLinkTree(inReq).setSelectedLink(newlink);
		tree.setSelectedLink(newlink.getId());
		
	}

	public void selectLink(WebPageRequest inReq) throws Exception
	{
		String linkId = inReq.getRequestParameter("linkid");
		if( linkId != null)
		{
			if(inReq.getUser() != null){
				inReq.getUser().setValue("oe_edit_draftmode", "true");
			}
			getLinkTree(inReq).setSelectedLink(linkId);
		}
	}

	public void removeLink(WebPageRequest inReq) throws Exception
	{
		inReq.getUser().setValue("oe_edit_draftmode", "true");

			Link link = getLinkTree(inReq).getSelectedLink();
			getLinkTree(inReq).removeLink(link);
			save(inReq, "Removed Link");
	}
	public void moveUp(WebPageRequest inReq) throws Exception
	{
		inReq.getUser().setValue("oe_edit_draftmode", "true");

		Link link = getLinkTree(inReq).getSelectedLink();
		getLinkTree(inReq).moveUp(link);
		save(inReq,"Moved up");
	}
	public void moveDown(WebPageRequest inReq) throws Exception
	{
		inReq.getUser().setValue("oe_edit_draftmode", "true");

		Link link = getLinkTree(inReq).getSelectedLink();
		getLinkTree(inReq).moveDown(link);
		save(inReq,"Moved down");
	}
	public void moveRight(WebPageRequest inReq) throws Exception
	{
		inReq.getUser().setValue("oe_edit_draftmode", "true");

		Link link = getLinkTree(inReq).getSelectedLink();
		getLinkTree(inReq).moveRight(link);
		save(inReq,"Moved Right");
	}
	public void moveLeft(WebPageRequest inReq) throws Exception
	{
		inReq.getUser().setValue("oe_edit_draftmode", "true");

		Link link = getLinkTree(inReq).getSelectedLink();
		getLinkTree(inReq).moveLeft(link);
		save(inReq,"Moved left");
	}	
	public void moveLink(WebPageRequest inReq) throws Exception
	{
		String dir = inReq.getRequestParameter("direction");
		if (dir != null)
		{
			if ("up".equals(dir))
				moveUp(inReq);
			else if ("down".equals(dir))
				moveDown(inReq);
			else if ("left".equals(dir))
				moveLeft(inReq);
			else if ("right".equals(dir))
				moveRight(inReq);
		}
	}
	/**
	 * @param inReq
	 * @param pageManager
	 * @throws OpenEditException
	 */
	public void checkLinksRedirect(WebPageRequest inReq) throws OpenEditException 
	{
		if ( !inReq.getContentPage().isHtml() )
		{
			return;
		}		
		LinkTree tree = loadLinks(inReq);
		if ( tree != null)
		{
			//look in the /links.xml file, if a matching link has a redirectPath defined, then redirect
			String redirect = tree.findRedirect( inReq.getPath() );
			if ( redirect != null)
			{
				if(redirect.toLowerCase().startsWith("http"))
				{
					inReq.redirectPermanently(redirect);					
				}
				else
				{
					PageManager pageManager = getPageManager();

					Page redirectPage = pageManager.getPage(redirect);
					if (redirectPage.exists())
					{
						inReq.redirectPermanently(redirect);
					}
					
				}
			}

		}
	}
	public void pageAdded(Page inPage)
	{
		setForceReload(true);
	}
	public void pageModified(Page inPage)
	{
		setForceReload(true);
	}
	public void pageRemoved(Page inPage)
	{
		setForceReload(true);
	}
	public void pageRequested(Page inPage)
	{
	}
	public boolean isForceReload()
	{
		return fieldForceReload;
	}
	public void setForceReload(boolean inForceReload)
	{
		fieldForceReload = inForceReload;
	}
	
	public WebTree getWebTree( WebPageRequest inRequest ) throws OpenEditException
	{
		String name = inRequest.findValue("tree-name");
				
		String treeid = inRequest.getRequestParameter("treeid");  
		if( treeid == null)
		{
			treeid = name + inRequest.getUserName();
		}
		WebTree webTree = (WebTree) inRequest.getSessionValue( treeid );

		LinkTree linktree = getLinkTree(inRequest);
		if ( webTree == null )
		{
			log.info("No web link tree in Session, creating new " + name);

			webTree = new WebTree();
			webTree.setName(name);
			webTree.setId( treeid);
			WebTreeNodeTreeRenderer renderer = new WebTreeNodeTreeRenderer( webTree );
			renderer.setFoldersLinked( true );
			String prefix = inRequest.findValue( "url-prefix" );
			prefix = inRequest.getPage().getPageSettings().replaceProperty(prefix);
			renderer.setUrlPrefix(prefix );
			String postfix = inRequest.findValue( "url-postfix");
			renderer.setUrlPostfix(postfix );
			webTree.setTreeRenderer(renderer);
			String home = (String) inRequest.getPageValue( "home" );
			renderer.setHome(home);
			
			//expand just the top level
/*			for (Iterator iter = main.getChildren().iterator(); iter.hasNext();)
			{
				Category child = (Category) iter.next();
				renderer.expandNode(child);
			}*/
			LinkTreeModel model = new LinkTreeModel(linktree);
			webTree.setModel(model);
			renderer.expandAll(model.getRoot());
			inRequest.putSessionValue(treeid, webTree);
		}
		else
		{
			LinkTreeModel model = new LinkTreeModel(linktree);
			webTree.setModel(model);
		}

		inRequest.putPageValue(name, webTree);
		return webTree;
	}
	public void approveDraft(WebPageRequest inReq) throws OpenEditException
	{
		LinkTree tree = loadLinks(inReq);
		if( tree.isDraft() )
		{
			getWorkFlow().approve(tree.getPage().getPath(), inReq.getUser() );
		}
	}
	public WorkFlow getWorkFlow()
	{
		return fieldWorkFlow;
	}
	public void setWorkFlow(WorkFlow inWorkFlow)
	{
		fieldWorkFlow = inWorkFlow;
	}

}
