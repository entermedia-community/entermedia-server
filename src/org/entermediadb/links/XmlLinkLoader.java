/*
 * Created on Dec 23, 2004
 */
package org.entermediadb.links;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.util.FileUtils;
import org.openedit.util.XmlUtil;

/**
 * @author cburkey
 *
 */
public class XmlLinkLoader
{
	protected XmlUtil fieldXmlUtil;

	public XmlUtil getXmlUtil() {
		if (fieldXmlUtil == null) {
			fieldXmlUtil = new XmlUtil();
			
		}

		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil xmlUtil) {
		fieldXmlUtil = xmlUtil;
	}

	public LinkTree loadLinks(Page inPage, LinkTree inTree) throws OpenEditException
	{
		//take in this XML file and use it to sync up to root node and below
		if( inTree == null)
		{
			inTree = new LinkTree();
		}

		Element root = getXmlUtil().getXml(inPage.getReader(),inPage.getCharacterEncoding());
		readElementsInto(root, inTree, inPage);
		return inTree;
	}

	/**
	 * @param inRoot
	 * @param inTree
	 * @throws OpenEditException 
	 */
	protected void readElementsInto(Element element, LinkTree inTree, Page inPage) throws OpenEditException
	{
		for (Iterator iter = element.elementIterator("a"); iter.hasNext();)
		{
			Element child = (Element) iter.next();
			String parentId = element.attributeValue("id");
			Link link = readElement(child, inPage);
			
			inTree.addLink( parentId, link );
			readElementsInto(child, inTree, inPage);
		}
	}
	
	protected Link readElement(Element element, Page inPage) throws OpenEditException
	{
		String id = element.attributeValue("id");
		String userdata = element.attributeValue("userdata");
		Link link = new Link();
		link.setId(id);
		link.setUserData(userdata);
		link.setText(element.attributeValue("text"));
		
		String href = element.attributeValue("href");
		href = inPage.getPageSettings().replaceProperty(href);
		link.setPath(href);
		link.setRedirectPath(element.attributeValue("redirectpath"));
		link.setAccessKey(element.attributeValue("accesskey"));
		link.setAutoLoadChildren(Boolean.parseBoolean(element.attributeValue("autoloadchildren")));
		link.setConfirmText(element.attributeValue("confirm"));

		checkLink(element, link);
		return link;
	}

	protected void checkLink(Element inElement, Link inLink) throws OpenEditException
	{
		//may be overriden
	}

	/**
	 * Returns the link information as an XML string in the specified encoding.
	 * 
	 * @param inTree      The link tree
	 * @param inEncoding  The encoding
	 * 
	 * @return  An XML document representing the link tree
	 * 
	 * @throws Exception
	 */
	public String saveLinks(LinkTree inTree, String inEncoding) throws Exception
	{
		StringWriter out = new StringWriter();
		saveLinks(inTree, out, inEncoding);
		return out.toString();
	}

	public void saveLinks(LinkTree inTree, Writer inXmlOutput, String inEncoding) throws OpenEditException
	{
		Document doc = DocumentHelper.createDocument();
		Element root = DocumentHelper.createElement("linktree");
//		root.addAttribute("textprefix", inTree.getTextPrefix());
//		root.addAttribute("textpostfix", inTree.getTextPostfix());
//		root.addAttribute("linkprefix", inTree.getLinkPrefix());
//		root.addAttribute("linkpostfix", inTree.getLinkPostfix());
		if (inTree.getRootLink() != null)
		{
			readLinkInto(inTree.getRootLink(), root);
		}
		doc.setRootElement(root);

		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding(inEncoding);
		try
		{
			new XMLWriter(inXmlOutput, format).write(doc);
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			FileUtils.safeClose(inXmlOutput);
		}
	}

	/**
	 * @param inTree
	 * @param inRoot
	 */
	private void readLinkInto(Link inLink, Element inElement)
	{
		Element element = inElement.addElement("a");
		element.addAttribute("href", inLink.getHref());
		element.addAttribute("userdata", inLink.getUserData());
		element.addAttribute("id", inLink.getId());
		element.addAttribute("text", inLink.getText());
		element.addAttribute( "redirectpath", inLink.getRedirectPath());
		if( inLink.isAutoLoadChildren())
		{
			element.addAttribute( "autoloadchildren", "true");
		}
		if (inLink.hasChildren())
		{
			for (Iterator iter = inLink.getChildren().iterator(); iter.hasNext();)
			{
				Link link = (Link) iter.next();
				readLinkInto(link, element);
			}
		}
	}
}
