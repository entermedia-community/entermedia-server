package org.openedit.entermedia.cluster;

import org.dom4j.Element;
import org.openedit.data.SearcherManager;

import com.openedit.OpenEditException;
import com.openedit.WebServer;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.util.XmlUtil;

public class NodeManager
{
	protected Node fieldLocalNode;
	protected XmlUtil fieldXmlUtil;
	protected PageManager fieldPageManager;
	protected SearcherManager fieldSearcherManager;
	protected WebServer fieldWebServer;
	
	public WebServer getWebServer()
	{
		return fieldWebServer;
	}

	public void setWebServer(WebServer inWebServer)
	{
		fieldWebServer = inWebServer;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public XmlUtil getXmlUtil()
	{
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}

	public Node getLocalNode()
	{
		if (fieldLocalNode == null)
		{
			Page page = getPageManager().getPage("/WEB-INF/node.xml");
			if( !page.exists())
			{
				throw new OpenEditException("WEB-INF/node.xml is not defined");
			}
			Element root = getXmlUtil().getXml(page.getInputStream(),"UTF-8");
			
			fieldLocalNode = new Node(root);
			String nodeid = getWebServer().getNodeId();
			if( nodeid != null)
			{
				fieldLocalNode.setId( nodeid );
			}
			
		}

		return fieldLocalNode;
	}
	public String getLocalNodeId()
	{
		return getLocalNode().getId();
	}
}
