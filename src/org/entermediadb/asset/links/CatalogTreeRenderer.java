package org.entermediadb.asset.links;

import org.entermediadb.asset.Category;
import org.entermediadb.webui.tree.HtmlTreeRenderer;
import org.entermediadb.webui.tree.TreeRenderer;
import org.entermediadb.webui.tree.WebTree;

/**
 * A {@link TreeRenderer} that renders {@link WebTree}s whose nodes are
 * {@link Category}s.
 * 
 * @author Eric Galluzzo
 */
public class CatalogTreeRenderer extends HtmlTreeRenderer
{
	public CatalogTreeRenderer()
	{
		super();
	}

	public CatalogTreeRenderer( WebTree inWebTree )
	{
		super( inWebTree );
	}

	public String toName( Object inNode )
	{
		return ( (Category) inNode ).getName();
	}

	public String toUrl( Object inNode )
	{
		return getWebTree().getModel().getId(inNode);
	}
}
