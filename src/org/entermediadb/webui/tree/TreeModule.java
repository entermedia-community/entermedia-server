/*
 * Created on Dec 22, 2004
 */
package org.entermediadb.webui.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.modules.BaseModule;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.users.User;
import org.openedit.util.strainer.AndFilter;
import org.openedit.util.strainer.Filter;
import org.openedit.util.strainer.NotFilter;
import org.openedit.util.strainer.PathMatchesFilter;

/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public class TreeModule extends BaseModule
{
	private static final Log log = LogFactory.getLog(TreeModule.class);
	public static final String PARAMETER_KEY = "treeName";
	protected PageManager fieldPageManager;
	protected String fieldTreeName;

	public void toggleTreeNode(WebPageRequest inRequest) throws OpenEditException
	{
		WebTree tree = (WebTree) getTree(inRequest);
		Object childNode = findNode(inRequest, tree);
		if (childNode != null)
		{
			if (tree.getTreeRenderer().hasBeenExpanded(childNode))
			{
				tree.getTreeRenderer().collapseNode(childNode);
			}
			else
			{
				tree.getTreeRenderer().expandNode(childNode);
			}
		}
	}
	public void setCutOffNode(WebPageRequest inRequest) throws OpenEditException
	{
		WebTree tree = (WebTree) getTree(inRequest);
		if (tree != null)
		{
			Object childNode = findNode(inRequest, tree);
			tree.getTreeRenderer().setLastCutoffNode(childNode);
		}
		
	}
	public void expandTreeNode(WebPageRequest inRequest) throws OpenEditException
	{
		WebTree tree = (WebTree) getTree(inRequest);
		if (tree != null)
		{
			Object childNode = findNode(inRequest, tree);

			if (childNode == null)
			{
				log.error("Must specify nodeID or nodePath for expansion");
				return;
			}

			tree.getTreeRenderer().expandNode(childNode);
		}
	}

	public void collapseTreeNode(WebPageRequest inRequest) throws OpenEditException
	{
		WebTree tree = getTree(inRequest);
		if (tree == null)
		{
			return;
		}
		Object childNode = findNode(inRequest, tree);
		// log.info("Collapse" + inRequest.getPathUrl() + " " + childNode);

		if (childNode == null)
		{
			log.error("Most specify nodeID or nodePath for expansion");
			return;
		}

		// WebTree tree = PageTree.getPageTree( getSiteContext() );
		tree.getTreeRenderer().collapseNode(childNode);
	}

	protected Object findNode(WebPageRequest inRequest, WebTree tree)
	{
		String id = inRequest.getRequestParameter("nodeID");
		if (id != null)
		{
			Object childNode = tree.getModel().getChildById(id);
			if (childNode != null)
			{
				return childNode;
			}
		}

		// This assumes that we're using DefaultWebTreeNodes.
		String path = inRequest.getRequestParameter("nodePath");
		if (path != null)
		{
			Object childNode = getNode((DefaultWebTreeNode) tree.getModel().getRoot(), path.split("/"));
			return childNode;
		}
		return null;
	}

	public void reloadTree(WebPageRequest inRequest) throws OpenEditException
	{
		WebTree webTree = getTree(inRequest);
		if( webTree != null)
		{
			inRequest.removeSessionValue(webTree.getId());
			//inRequest.redirect(inRequest.getPath());
		}
		getTree(inRequest);
	}

	/**
	 * This method initializes the WebTree from the action config.
	 * 
	 * @param inRequest
	 * @throws OpenEditException
	 */
	public WebTree  getTree( WebPageRequest inRequest ) throws OpenEditException
	{
		String treeid = inRequest.getRequestParameter("treeid");
		String name = null;
		if( inRequest.getCurrentAction() != null)
		{
			name = inRequest.getCurrentAction().getChildValue("tree-name");
		}
		if( name == null)
		{
			name = inRequest.findValue("tree-name");
		}

		// The root is applicable to our model only
		String root = inRequest.findValue("root"); 
		if ((root == null) || root.equals("/"))
		{
			root = "/";
		}
		
		if( name == null)
		{
			name = inRequest.findValue("WebTreeName"); // legacy
		}
		if( treeid == null)
		{
			treeid = name + "_" + inRequest.getUserName() + root;
		}				
		WebTree webTree = (WebTree) inRequest.getSessionValue(treeid);

		if ( (webTree == null && name != null) || !treeid.equals( webTree.getId() ) ) //might have been serialized
		{
			RepositoryTreeModel model = null;
			
			model = new RepositoryTreeModel(getPageManager().getRepository(), root );
			getPageManager().addPageAccessListener(model);

			AndFilter and = new AndFilter();

			String ignore = inRequest.findValue("excludes");
			if (ignore != null)
			{
				String[] types = ignore.split(",");
				Filter[] not = new Filter[types.length];
				for (int i = 0; i < types.length; i++)
				{
					not[i] = new NotFilter( new PathMatchesFilter(types[i].trim() ) );					
				}
				and.setFilters(not);
			}
			User user = inRequest.getUser();
			if( user != null && user.hasPermission("oe.filemanager.editall") )
			{
				
			}
			else
			{
				//Look for hidden paths at the top level tree
				populateRestrictedPaths(and, inRequest);
			}			
			model.setPageManager(getPageManager());
			model.setFilter(and);
			webTree = new WebTree(model);
			webTree.setName(name);
			webTree.setId(treeid);
			// setup the renderer
			WebTreeNodeTreeRenderer renderero = new WebTreeNodeTreeRenderer(webTree);
			String renderleaves = inRequest.findValue("renderleaves");
			if( renderleaves != null )
			{
				renderero.setRenderLeaves(Boolean.parseBoolean(renderleaves));
			}
			String prefix = inRequest.findValue( "url-prefix" );
			if (prefix != null)
			{
				renderero.setUrlPrefix(prefix);
			}
			String friendly = inRequest.findValue( "friendlyNames" );
			if (friendly != null)
			{
				renderero.setFriendlyNames(Boolean.valueOf(friendly).booleanValue());
			}
			String home = (String) inRequest.getPageValue( "home" );
			renderero.setHome(home);
			String iconhome = (String) inRequest.findValue( "iconhome" );
			renderero.setIconHome(iconhome);

			String iconwidth = (String) inRequest.getPageProperty( "iconwidth" ); //must be saved to page path
			if( iconwidth != null)
			{
				renderero.setIconWidth(Integer.parseInt(iconwidth));
			}

			webTree.setTreeRenderer(renderero);
			
			inRequest.putSessionValue(treeid, webTree);
		}
		if( webTree != null)
		{
			inRequest.putPageValue("pageManager",getPageManager());
			inRequest.putPageValue(name , webTree);
		}
		return webTree;
	}

	private void populateRestrictedPaths(AndFilter inAnd, WebPageRequest inRequest) throws OpenEditException
	{
		List not = new ArrayList();
		
		if( inAnd.getFilters() != null )
		{
			not.addAll(Arrays.asList(inAnd.getFilters()));
		}
		PagePathViewFilter pathfilter = new PagePathViewFilter();
		pathfilter.setPageManager(getPageManager());
		pathfilter.setLoadingWebPageRequest(inRequest);
		not.add(pathfilter);
		inAnd.setFilters((Filter[])not.toArray(new Filter[not.size()]));
	}

	/*
	 * Uses a string path name
	 */
	protected DefaultWebTreeNode getNode(DefaultWebTreeNode inRoot, String[] inPath)
	{
		DefaultWebTreeNode currentPath = inRoot;

		for (int i = 0; i < inPath.length; i++)
		{
			List children = currentPath.getChildren();

			for (int j = 0; j < children.size(); j++)
			{
				DefaultWebTreeNode child = (DefaultWebTreeNode) children.get(j);

				if (inPath[i].equals(child.getName()))
				{
					currentPath = child;
				}
			}
		}

		return currentPath;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager pageManager)
	{
		fieldPageManager = pageManager;
	}
	
	public void selectNodeByPath(WebPageRequest inReq)
	{
		String path = inReq.getRequestParameter("path");
		
		if (path != null)
		{
			WebTree tree = getTree(inReq);
			Object node = tree.selectNodeByUrl(path);
			
			if (node == null && path.endsWith("/") && "true".equals(inReq.findValue("createfolders")))
			{
				Page page = getPageManager().getPage(path);
				getPageManager().getRepository().put(page.getContentItem());
				reloadTree(inReq);
				tree = getTree(inReq);
				node = tree.selectNodeByUrl(path);
			}
			if (node != null)
			{
				String id = tree.getModel().getId(node);
				
				inReq.setRequestParameter("tabid", id);
				inReq.setRequestParameter("tabpath", path);
	//			inReq.setRequestParameter("tabname", tree.get);
			}
		}
		
	}
}
