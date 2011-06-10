package org.openedit.entermedia.links;

import org.openedit.entermedia.Category;

import com.openedit.webui.tree.HtmlTreeRenderer;
import com.openedit.webui.tree.TreeRenderer;
import com.openedit.webui.tree.WebTree;

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
