package org.openedit.entermedia.modules;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.links.CatalogTreeRenderer;
import org.openedit.entermedia.links.CatalogWebTreeModel;
import org.openedit.entermedia.search.SearchFilter;
import org.openedit.links.Link;
import org.openedit.links.LinkTree;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.util.PathUtilities;
import com.openedit.util.RequestUtils;
import com.openedit.util.ZipUtil;
import com.openedit.webui.tree.WebTree;

public class CategoryModule extends BaseMediaModule
{
	
	private static final Log log = LogFactory.getLog(CategoryModule.class);
	private RequestUtils fieldRequestUtils;
	public RequestUtils getRequestUtils() {
		return fieldRequestUtils;
	}
	public void setRequestUtils(RequestUtils inRequestUtils) {
		fieldRequestUtils = inRequestUtils;
	}
	/**
	 * Installs a {@link WebTree} that shows the catalog tree from a specified
	 * root catalog on down.
	 * 
	 * @param inRequest  The web page request
	 */
	public WebTree getCatalogTree( WebPageRequest inRequest ) throws OpenEditException
	{
		MediaArchive archive = getMediaArchive(inRequest);
		if (archive == null )
		{
			return null;
		}
		String name = null;
		if( inRequest.getCurrentAction() != null)
		{
			name = inRequest.getCurrentAction().getChildValue("tree-name");
		}
		if( name == null)
		{
			name = inRequest.findValue("tree-name");
		}
		String appid = inRequest.findValue("applicationid");
		
		String treeid = inRequest.getRequestParameter("treeid");
		if( treeid == null)
		{
			treeid = name + "_" + appid + "_" + archive.getCatalogId() + "_" + inRequest.getUserName();
		}		
		WebTree webTree = (WebTree) inRequest.getPageValue( treeid );

		String reload = inRequest.getRequestParameter("reload");
		Object needsreload = inRequest.getSessionValue("reloadcategorytree");
		if(needsreload != null){
			webTree = null;
			inRequest.removeSessionValue("reloadcategorytree");
		}
		if( Boolean.parseBoolean(reload))
		{
			webTree = null;
		}
		if ( webTree == null )
		{
			if( name == null)
			{
				return null;
			}
			log.info("No Category in Session, creating new " + treeid);
			String root = inRequest.findValue(name + "root");
			if( root  == null )
			{
				root = inRequest.findValue("root");
			}

			Category main = archive.getCategoryArchive().getCategory( root );
			if ( main == null)
			{
				log.error("No such category named " + root);
				main = archive.getCategoryArchive().getRootCategory();
				
			}
			CatalogWebTreeModel model = new CatalogWebTreeModel( );
			model.setCatalogId(archive.getCatalogId());
			model.setRoot(main);
			SearchFilter filter = archive.getSearchFilterArchive().getSearchFilter(inRequest,false, false,archive.getCatalogId());
			model.setSearchFilter(filter);
			model.setCatalogArchive(archive.getCategoryArchive());
			model.setUser(inRequest.getUser());
			model.setRequestUtils(getRequestUtils());
			webTree = new WebTree(model);
			webTree.setName(name);
			webTree.setId(treeid);

			CatalogTreeRenderer renderer = new CatalogTreeRenderer( webTree );
			renderer.setFoldersLinked( true );
			String prefix = inRequest.findValue( "url-prefix" );
			prefix = inRequest.getPage().getPageSettings().replaceProperty(prefix);
			renderer.setUrlPrefix(prefix );
			String postfix = inRequest.findValue( "url-postfix" );
			renderer.setUrlPostfix(postfix );
			webTree.setTreeRenderer(renderer);
			String home = (String) inRequest.getPageValue( "home" );
			renderer.setHome(home);
			String iconHome = (String) inRequest.findValue( "iconhome" );
			renderer.setIconHome(iconHome);
			String allowselections = inRequest.findValue( "allowselections" );
			renderer.setAllowSelections(Boolean.parseBoolean(allowselections));

			String editable = inRequest.findValue( "editabletree" );
			renderer.setEditable(Boolean.parseBoolean(editable));

			String iconwidth = (String) inRequest.getPageProperty( "iconwidth" ); //must be saved to page path
			if( iconwidth != null)
			{
				renderer.setIconWidth(Integer.parseInt(iconwidth));
			}

			inRequest.putSessionValue(treeid, webTree);
			inRequest.putPageValue(webTree.getName(), webTree);
		}
		else
		{
			inRequest.putPageValue(webTree.getName(), webTree);
		}
		return webTree;
	}
	public void selectNodes(WebPageRequest inReq)
	{
		WebTree tree =  getCatalogTree(inReq);
		Collection nodes = (Collection)inReq.getPageValue("selectednodes");
		if( nodes != null )
		{		
			tree.getTreeRenderer().selectNodes(nodes);
		}
	}
	public void expandNode(WebPageRequest inReq){
		WebTree tree =  getCatalogTree(inReq);
		String catid = inReq.getRequestParameter("nodeID");
		Object target = tree.getModel().getChildById(catid);
		tree.getTreeRenderer().expandNode(target);

	}
	
	public void collapseNode(WebPageRequest inReq){
		WebTree tree =  getCatalogTree(inReq);
		String catid = inReq.getRequestParameter("nodeID");
		Object target = tree.getModel().getChildById(catid);
		tree.getTreeRenderer().collapseNode(target);

	}
	
	public void reloadTree(WebPageRequest inReq) throws OpenEditException
	{
		WebTree tree = getCatalogTree(inReq);
		getMediaArchive(inReq).getCategoryArchive().clearCategories();
		if(tree != null){
			inReq.removeSessionValue(tree.getId());
		}
		getCatalogTree(inReq);
	}

	public void exportAllAssets(WebPageRequest inReq) throws OpenEditException
	{
		MediaArchive archive = getMediaArchive(inReq);
		StringWriter assets = new StringWriter(); //TODO: This is a memory hog
		archive.getAssetExport().exportAllAssets(archive, assets);
		
		ZipOutputStream finalZip = new ZipOutputStream(inReq.getOutputStream());
		
		try
		{
			new ZipUtil().addTozip(assets.toString(),"assets.xml" ,finalZip);
			inReq.getOutputStream().flush();
			StringWriter catalogs = new StringWriter();
			archive.getAssetExport().exportCatalogsWithAssets(archive, catalogs);
			new ZipUtil().addTozip(catalogs.toString(),"categories.xml" ,finalZip);
			finalZip.close();
			inReq.setHasRedirected(true);
		}
		catch ( IOException ex )
		{
			throw new OpenEditException(ex);
		}
	}
	
	public void loadCrumbs(WebPageRequest inReq) throws Exception
	{
		Category category = (Category)inReq.getPageValue("category");
		if( category == null)
		{
			Asset asset = (Asset)inReq.getPageValue("asset");
			if( asset != null)
			{
				category = asset.getDefaultCategory();
			}
		}
		if( category != null)
		{
			String name = inReq.findValue("linktreename");
			if( name != null)
			{
				String treename = inReq.findValue("tree-name");
				String root = inReq.findValue(treename + "root");
				MediaArchive archive = getMediaArchive(inReq);
				Category toplevel = archive.getCategoryArchive().getCategory(root);
				LinkTree tree = (LinkTree)inReq.getPageValue(name);
				if( tree != null)
				{
					tree.clearCrumbs();
					for (Iterator iterator = category.listAncestorsAndSelf( 0 ).iterator(); iterator.hasNext();)
					{
						Category parent = (Category) iterator.next();
						if( toplevel != null )
						{
							if( !toplevel.hasCatalog(parent.getId()) )
							{
								continue;
							}
						}
						tree.addCrumb( archive.getCatalogHome() +  "/categories/" + parent.getId() + ".html",parent.getName());
					}
					tree.setSelectedLink((Link)null);
				}
			}
		}
		
	}
	
	public void downloadSelected(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String path = "/" + archive.getCatalogHome() + "/download/convert/zip/results.zip";
		// forward to zip generator
		inReq.redirect(path);
	}
	
	public void listAssetsForUploading( WebPageRequest inReq )
	{
		Searcher searcher = getMediaArchive(inReq).getAssetSearcher();
		SearchQuery query = searcher.createSearchQuery();
		query.addMatches("importstatus", "uploading");
		
		String user = inReq.getRequestParameter("user");
		if( user!= null )
		{
			query.addMatches("owner", user);
		}
		
		HitTracker hits = searcher.search(query);
		inReq.putPageValue("hits", hits);
	}

	public void loadHomeCategory(WebPageRequest inReq )
	{
		MediaArchive archive = getMediaArchive(inReq);
		String catid = "index";
		String username = inReq.getUserName();
		if( username != null)
		{
			catid = "users_" + username;
		}
		Category cat = archive.getCategory(catid);
		inReq.putPageValue("category", cat);
	}
	public void removeNode(WebPageRequest inReq)
	{
		String catid = inReq.getRequestParameter("nodeID");
		MediaArchive archive = getMediaArchive(inReq);
		Category child = archive.getCategory(catid);
		if( child != null)
		{
			archive.getCategoryArchive().deleteCategory(child);
		}
		inReq.setRequestParameter("reload", "true");
		getCatalogTree(inReq);
	}
	public void addNode(WebPageRequest inReq)
	{
		String catid = inReq.getRequestParameter("nodeID");
		MediaArchive archive = getMediaArchive(inReq);
		Category parent = archive.getCategory(catid);
		if( parent != null)
		{
			String text = inReq.getRequestParameter("addtext");
			Category child = archive.getCategoryArchive().createNewCategory(text);
			if( archive.getCategory(child.getId()) != null )
			{
				//fix duplicate id
				child.setId(catid + "-" + child.getId());
			}
			parent.addChild(child);
			archive.getCategoryArchive().cacheCategory(child);
			archive.getCategoryArchive().saveAll();
		}
		inReq.setRequestParameter("reload", "true");
		WebTree tree = getCatalogTree(inReq);
//		if( tree != null)
//		{
//			tree.getTreeRenderer().expandNode(parent);
//		}
	}
	public void editNode(WebPageRequest inReq)
	{
		String catid = inReq.getRequestParameter("nodeID");
		MediaArchive archive = getMediaArchive(inReq);
		Category parent = archive.getCategory(catid);
		if( parent != null)
		{
			String text = inReq.getRequestParameter("edittext");
			parent.setName(text);
			archive.getCategoryArchive().saveAll();
		}
		inReq.setRequestParameter("reload", "true");
		getCatalogTree(inReq);
	}
	
	public void loadCategory(WebPageRequest inContext) throws OpenEditException 
	{
		String catalogid = inContext.findValue("categoryid");
		if (catalogid == null) 
		{
			catalogid = PathUtilities.extractPageName(inContext.getPath());
		}
		if (catalogid != null) 
		{
			// load up catalog and assets
			Category catalog = getMediaArchive(inContext).getCategory(catalogid);
			if (catalog != null) 
			{
				inContext.putPageValue("category", catalog);
			}
		}
	}
	public void reBuildTree(WebPageRequest inReq) throws OpenEditException
	{
		WebTree tree = getCatalogTree(inReq);
		MediaArchive archive = getMediaArchive(inReq);
		archive.getCategoryEditor().reBuildCategories();
		reloadTree(inReq);
	}	

}