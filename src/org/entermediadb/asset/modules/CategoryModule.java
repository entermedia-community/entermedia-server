package org.entermediadb.asset.modules;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.links.CatalogTreeRenderer;
import org.entermediadb.asset.links.CategoryWebTreeModel;
import org.entermediadb.asset.xmldb.CategorySearcher;
import org.entermediadb.links.Link;
import org.entermediadb.links.LinkTree;
import org.entermediadb.webui.tree.WebTree;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.util.PathUtilities;
import org.openedit.util.RequestUtils;
import org.openedit.util.ZipUtil;

public class CategoryModule extends BaseMediaModule
{

	private static final Log log = LogFactory.getLog(CategoryModule.class);
	private RequestUtils fieldRequestUtils;

	public RequestUtils getRequestUtils()
	{
		return fieldRequestUtils;
	}

	public void setRequestUtils(RequestUtils inRequestUtils)
	{
		fieldRequestUtils = inRequestUtils;
	}

	/**
	 * Installs a {@link WebTree} that shows the catalog tree from a specified
	 * root catalog on down.
	 * 
	 * @param inReq
	 *            The web page request
	 */
	public WebTree getCatalogTree(WebPageRequest inReq) throws OpenEditException
	{
		MediaArchive archive = getMediaArchive(inReq);
		if (archive == null)
		{
			return null;
		}
		String name = null;
		if (inReq.getCurrentAction() != null)
		{
			name = inReq.getCurrentAction().getChildValue("tree-name");
		}
		if (name == null)
		{
			name = inReq.findValue("tree-name");
		}
		if (name == null)
		{
			name = inReq.findValue("treename");
		}
		String appid = inReq.findValue("applicationid");
		String root = inReq.getRequestParameter(name + "root");

		String treeid = inReq.getRequestParameter("treeid");
		if (treeid == null)
		{
			treeid = name + "_" + appid + "_" + archive.getCatalogId() + "_" + inReq.getUserName() + getCategorySearcher(inReq).getIndexId();
		}
		WebTree webTree = (WebTree) inReq.getPageValue(treeid);

		if (root == null)
		{
			root = inReq.findValue(name + "root");
		}
		if (root == null)
		{
			root = inReq.findValue("root");
		}

		CategorySearcher searcher = getCategorySearcher(inReq);

		Category main = searcher.getCategory(root);
		if (main == null)
		{
			log.error("No such category named " + root);
			main = searcher.getRootCategory();
		}

		CategoryWebTreeModel model;
		if (webTree != null && webTree.getModel().getRoot() == main)
		{
			model = (CategoryWebTreeModel) webTree.getModel();
		}
		else
		{
			String treeModel = inReq.findValue("treemodel");
			if (treeModel == null)
			{
				treeModel = "categoryTreeModel";
			}
			CategoryWebTreeModel amodel = (CategoryWebTreeModel) getModuleManager().getBean(archive.getCatalogId(), treeModel, false);
			amodel.setMediaArchive(archive);
			amodel.setCatalogId(archive.getCatalogId());
			amodel.setRoot(main);
			amodel.setCategorySearcher(searcher);
			amodel.setRequestUtils(getRequestUtils());
			model = amodel;
		}
		model.setUserProfile(inReq.getUserProfile());

		String reload = inReq.getRequestParameter("reloadtree");

		if (root != null && webTree != null && !root.equals(webTree.getRootId()))
		{
			reload = "true";
		}
		Object needsreload = inReq.getSessionValue("reloadcategorytree");//Some other way?
		if (needsreload != null)
		{
			webTree = null;
			inReq.removeSessionValue("reloadcategorytree");
		}

		if (Boolean.parseBoolean(reload))
		{
			webTree = null;
		}
		if (webTree == null)
		{
			if (name == null)
			{
				return null;
			}
			log.debug("No Category in Session, creating new " + treeid);
			WebTree oldwebTree = webTree;
			webTree = new WebTree(model);
			webTree.setName(name);
			webTree.setId(treeid);

			CatalogTreeRenderer renderer = null;
			if (oldwebTree == null)
			{

				renderer = new CatalogTreeRenderer(webTree);
				renderer.setFoldersLinked(true);
				String prefix = inReq.findValue("url-prefix");
				prefix = inReq.getPage().getPageSettings().replaceProperty(prefix);
				renderer.setUrlPrefix(prefix);
				String postfix = inReq.findValue("url-postfix");
				renderer.setUrlPostfix(postfix);

			}
			else
			{
				renderer = (CatalogTreeRenderer) oldwebTree.getTreeRenderer();
			}
			webTree.setTreeRenderer(renderer);

			String autoexpand = inReq.findValue("autoexpand");
			if (autoexpand == null || Boolean.parseBoolean(autoexpand))
			{
				renderer.expandNode(webTree.getModel().getRoot());
			}

			String home = (String) inReq.getPageValue("home");
			renderer.setHome(home);
			String iconHome = (String) inReq.findValue("iconhome");
			renderer.setIconHome(iconHome);
			String allowselections = inReq.findValue("allowselections");
			renderer.setAllowSelections(Boolean.parseBoolean(allowselections));

			String editable = inReq.findValue("editabletree");
			if (editable == null || Boolean.parseBoolean(editable))
			{
				Boolean val = (Boolean) inReq.getPageValue("caneditcategories");
				if (val != null)
				{
					editable = val.toString();
				}
			}
			renderer.setEditable(Boolean.parseBoolean(editable));

			String iconwidth = (String) inReq.getPageProperty("iconwidth"); //must be saved to page path
			if (iconwidth != null)
			{
				renderer.setIconWidth(Integer.parseInt(iconwidth));
			}
			String expandparents = inReq.findValue("expandroots");
			if (expandparents != null)
			{
				expandChildren(webTree, main, Integer.parseInt(expandparents));
			}

			inReq.putSessionValue(treeid, webTree);
			inReq.putPageValue(webTree.getName(), webTree);
			//	inRequest.putPageValue("selectednodes", webTree.getTreeRenderer().getSelectedNodes());
		}
		else
		{
			inReq.putPageValue(webTree.getName(), webTree);
			//inRequest.putPageValue("selectednodes", webTree.getTreeRenderer().getSelectedNodes());
		}

		String clear = inReq.getRequestParameter("clearselection");
		if (Boolean.parseBoolean(clear))
		{
			webTree.getTreeRenderer().selectNodes(null);
		}

		return webTree;
	}

	public CategorySearcher getCategorySearcher(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);

		String categorysearchertype = inReq.findValue("categorysearchtype");
		if (categorysearchertype == null)
		{
			categorysearchertype = "category";
		}
		return (CategorySearcher) archive.getSearcher(categorysearchertype);
	}

	private void expandChildren(WebTree inWebTree, Category inParent, int inParseInt)
	{
		inWebTree.getTreeRenderer().expandNode(inParent);
		inParseInt--;
		if (inParseInt > 0 && inParent.hasChildren())
		{
			for (Iterator iterator = inParent.getChildren().iterator(); iterator.hasNext();)
			{
				Category child = (Category) iterator.next();
				expandChildren(inWebTree, child, inParseInt);
			}
		}
	}

	public void selectNodes(WebPageRequest inReq)
	{
		String selecting = inReq.getRequestParameter("selecting");
		if (!Boolean.parseBoolean(selecting))
		{
			return;
		}

		WebTree tree = getCatalogTree(inReq);
		//check param data
		String cats = inReq.getRequestParameter("categories");
		if (cats != null)
		{
			String[] selected = cats.replace(' ', '|').split("\\|");
			MediaArchive archive = getMediaArchive(inReq);
			for (int i = 0; i < selected.length; i++)
			{
				Category found = getCategorySearcher(inReq).getCategory(selected[i].trim());
				if (found != null)
				{
					tree.getTreeRenderer().selectNode(found);
				}
			}
		}
		else
		{
			String catid = inReq.getRequestParameter("nodeID");
			if (catid != null)
			{
				Object target = tree.getModel().getChildById(catid);
				//tree.getTreeRenderer().expandNode(target);
				tree.getTreeRenderer().selectNode(target);
			}
		}

		String clear = inReq.getRequestParameter("clearselection");
		if (Boolean.parseBoolean(clear))
		{
			tree.getTreeRenderer().selectNodes(null);
		}

	}

	public void deselectNodes(WebPageRequest inReq)
	{
		WebTree tree = getCatalogTree(inReq);

		//check param data
		String cats = inReq.getRequestParameter("categories");
		if (cats != null)
		{
			String[] selected = cats.replace(' ', '|').split("\\|");
			MediaArchive archive = getMediaArchive(inReq);
			for (int i = 0; i < selected.length; i++)
			{
				Category found = getCategorySearcher(inReq).getCategory(selected[i].trim());
				if (found != null)
				{
					tree.getTreeRenderer().unSelectNode(found);
				}
			}
		}

	}

	public void expandNode(WebPageRequest inReq)
	{
		WebTree tree = getCatalogTree(inReq);
		String catid = inReq.getRequestParameter("nodeID");
		Object target = tree.getModel().getChildById(catid);
		tree.getTreeRenderer().expandNode(target);

	}

	public void collapseNode(WebPageRequest inReq)
	{
		WebTree tree = getCatalogTree(inReq);
		String catid = inReq.getRequestParameter("nodeID");
		Object target = tree.getModel().getChildById(catid);
		tree.getTreeRenderer().collapseNode(target);

	}

	public void reloadTree(WebPageRequest inReq) throws OpenEditException
	{
		WebTree tree = getCatalogTree(inReq);
		getMediaArchive(inReq).getCategoryArchive().clearCategories();
		if (tree != null)
		{
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
			new ZipUtil().addTozip(assets.toString(), "assets.xml", finalZip);
			inReq.getOutputStream().flush();
			StringWriter catalogs = new StringWriter();
			archive.getAssetExport().exportCatalogsWithAssets(archive, catalogs);
			new ZipUtil().addTozip(catalogs.toString(), "categories.xml", finalZip);
			finalZip.close();
			inReq.setHasRedirected(true);
		}
		catch (IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}

	public void loadCrumbs(WebPageRequest inReq) throws Exception
	{
		Category category = (Category) inReq.getPageValue("category");
		if (category == null)
		{
			Asset asset = (Asset) inReq.getPageValue("asset");
			if (asset != null)
			{
				category = asset.getDefaultCategory();
			}
		}
		if (category != null)
		{
			String name = inReq.findValue("linktreename");
			if (name != null)
			{
				String treename = inReq.findValue("tree-name");
				String root = inReq.findValue(treename + "root");
				MediaArchive archive = getMediaArchive(inReq);
				Category toplevel = getCategorySearcher(inReq).getCategory(root);
				LinkTree tree = (LinkTree) inReq.getPageValue(name);
				if (tree != null)
				{
					tree.clearCrumbs();
					for (Iterator iterator = category.listAncestorsAndSelf(0).iterator(); iterator.hasNext();)
					{
						Category parent = (Category) iterator.next();
						if (toplevel != null)
						{
							if (!toplevel.hasCatalog(parent.getId()))
							{
								continue;
							}
						}

						//TODO:  Make generic
						tree.addCrumb(archive.getCatalogHome() + "/categories/" + parent.getId() + ".html", parent.getName());
					}
					tree.setSelectedLink((Link) null);
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

	public void listAssetsForUploading(WebPageRequest inReq)
	{
		Searcher searcher = getMediaArchive(inReq).getAssetSearcher();
		SearchQuery query = searcher.createSearchQuery();
		query.addMatches("importstatus", "uploading");

		String user = inReq.getRequestParameter("user");
		if (user != null)
		{
			query.addMatches("owner", user);
		}

		HitTracker hits = searcher.search(query);
		inReq.putPageValue("hits", hits);
	}

	public void loadHomeCategory(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String catid = "index";
		String username = inReq.getUserName();
		if (username != null)
		{
			catid = "users_" + username;
		}
		Category cat = getCategorySearcher(inReq).getCategory(catid);
		inReq.putPageValue("category", cat);
	}

	public void removeNode(WebPageRequest inReq)
	{
		String catid = inReq.getRequestParameter("nodeID");
		MediaArchive archive = getMediaArchive(inReq);
		Category child = getCategorySearcher(inReq).getCategory(catid);
		if (child != null)
		{
			getCategorySearcher(inReq).deleteCategoryTree(child);
		}
		inReq.setRequestParameter("reload", "true");
	}

	public void addNode(WebPageRequest inReq)
	{
		String catid = inReq.getRequestParameter("nodeID");
		MediaArchive archive = getMediaArchive(inReq);
		Category parent = getCategorySearcher(inReq).getCategory(catid);
		if (parent != null)
		{
			String text = inReq.getRequestParameter("addtext");
			Category child = (Category) getCategorySearcher(inReq).createNewData();
			//cat.setId(id);
			child.setName(text);
			if (child.getId() != null && getCategorySearcher(inReq).getCategory(child.getId()) != null)
			{
				//fix duplicate id
				child.setId(catid + "-" + child.getId());
			}
			parent.addChild(child);
			getCategorySearcher(inReq).saveCategory(child);
			//getCategorySearcher(inReq).getCategoryArchive().saveAll();
		}
		inReq.setRequestParameter("reload", "true");
		WebTree tree = getCatalogTree(inReq);
		//		if( tree != null)
		//		{
		//			tree.getTreeRenderer().expandNode(parent);
		//		}
	}

	public void loadNode(WebPageRequest inReq)
	{
		String catid = inReq.getRequestParameter("nodeID");
		MediaArchive archive = getMediaArchive(inReq);
		Category parent = getCategorySearcher(inReq).getCategory(catid);
		inReq.putPageValue("node", parent);
	}

	public void saveNode(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		String catid = null;
		Category tosave = null;
		String action = inReq.getRequestParameter("action");

		if (action != null && action.equals("rename"))
		{
			catid = inReq.getRequestParameter("nodeID");
		}

		if (catid != null)
		{
			tosave = getCategorySearcher(inReq).getCategory(catid);
		}
		else
		{
			String parentid = inReq.getRequestParameter("parentNodeID");
			if (parentid == null)
			{
				parentid = "index";
			}
			Category parent = getCategorySearcher(inReq).getCategory(parentid);
			tosave = (Category) getCategorySearcher(inReq).createNewData();
			parent.addChild(tosave);
		}
		if (tosave != null)
		{
			String text = inReq.getRequestParameter("edittext");
			if (text != null)
			{
				text = text.trim();
			}
			tosave.setName(text);
			getCategorySearcher(inReq).saveCategory(tosave);
		}
		//getCatalogTree(inReq);
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
	
	public void loadCategoryByPath(WebPageRequest inContext) throws OpenEditException
	{
		String categorypath = inContext.getRequestParameter("categorypath");

		if (categorypath != null)
		{
			// load up catalog and assets
			Category category = getMediaArchive(inContext).getCategorySearcher().loadCategoryByPath(categorypath);
			if (category != null)
			{
				inContext.putPageValue("category", category);
			}
		}
	}
	
	//	public void reBuildTree(WebPageRequest inReq) throws OpenEditException
	//	{
	//		WebTree tree = getCatalogTree(inReq);
	//		MediaArchive archive = getMediaArchive(inReq);
	//		getCategorySearcher(inReq).getCategoryEditor().reBuildCategories();
	//		reloadTree(inReq);
	//	}	

	public void copyCategoriesToCategory(WebPageRequest inReq) throws OpenEditException
	{
		String targetcategoryid = inReq.getRequestParameter("targetcategoryid");
		if (targetcategoryid == null) {
			return;
		}
			
		//Copy all the children and assets as well...
		MediaArchive archive = getMediaArchive(inReq);
		String[] catids = inReq.getRequestParameters("categoryid");

		archive.getCategoryEditor().copyEverything(inReq.getUser(), catids,targetcategoryid);
	}
}