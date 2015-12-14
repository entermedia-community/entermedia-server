/*
 * Created on Nov 29, 2006
 */
package org.entermediadb.links;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.page.manage.PageManager;
import org.openedit.util.PathUtilities;

public class HtmlLinkLoader extends XmlLinkLoader
{
	protected PageManager fieldPageManager;

	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager pageManager) {
		fieldPageManager = pageManager;
	}

	private static final Log log=LogFactory.getLog(HtmlLinkLoader.class);

	protected void checkLink(Element element, Link link) throws OpenEditException
	{
		String load = element.attributeValue("autoloadchildren");
		if( Boolean.parseBoolean(load))
		{
			Page dir = getPageManager().getPage(link.getPath());
			if( !dir.isFolder())
			{
				dir = getPageManager().getPage(dir.getDirectory() );
			}
			List paths = getPageManager().getChildrenPaths(dir.getPath());
			findLinks(link, dir, paths);
			link.sortChildren();

		}
				
	}

	protected void findLinks(Link link, Page inDir, List inPaths) throws OpenEditException
	{
		List paths = inPaths;
		if( inDir.getPageSettings().getFallback() != null)
		{
			paths = new ArrayList();
			paths.addAll(inPaths);
			String path = inDir.getPageSettings().getFallback().getPath();
			path = PathUtilities.extractDirectoryPath(path);
			String root = inDir.getPath();
			for (Iterator iterator = getPageManager().getChildrenPaths(path).iterator(); iterator.hasNext();)
			{
				String fallback = (String) iterator.next();
				paths.add(root + fallback.substring(path.length()));
			}
		}
		
		//These is a problem here trying to load up stuff from the fallback directory. We are getting duplicates
		for (Iterator iterator = paths.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			if( path.endsWith(".versions") || path.endsWith("CVS"))
			{
				continue;
			}
			Page page = getPageManager().getPage(path);

			PageProperty autolink = page.getPageSettings().getProperty("autoloadlink");
			if( autolink != null )
			{
				if( !Boolean.parseBoolean(autolink.getValue() ) )
				{
					continue;
				}
			}
			
			if( page.isHtml() && page.exists())
			{
				if( page.isDraft() || page.getName().startsWith("index.htm") )
				{
					continue;
				}
				Link dlink = createLinkObject(page);
				dlink.setPath(path);
				link.addChild(dlink);
			}
			else if( page.isFolder() || !page.exists())  //This may be in the fallback directory
			{
				if( page.getName() != null && page.getName().startsWith("."))
				{
					continue;
				}
				//this could be a non HTML file in the fallback directory
				if(!page.isFolder() && !page.isHtml()){
					continue;
				}
				Link dlink = null;
				Page index = getPageManager().getPage(page.getPath() + "/index.html");
				if(index.exists()){
					dlink= createLinkObject(index);
					dlink.setPath(index.getPath());
				}
				else
				{
					dlink= createLinkObject(page);
				}
				link.addChild(dlink);
				List childpaths = getPageManager().getRepository().getChildrenNames(path);
				findLinks(dlink, page, childpaths);
				dlink.sortChildren();
				//log.info(dlink.getChildren());
			}
		}
	}

	private Link createLinkObject(Page page)
	{
		Link dlink = new Link();
		dlink.setId(PathUtilities.makeId(page.getPath()));

		PageProperty title = page.getPageSettings().getFieldProperty("linktext");
		if( title == null)
		{
		 title = page.getPageSettings().getFieldProperty("title");
		}
		if( title == null && page.getPageSettings().getFallback() !=  null)
		{
			title = page.getPageSettings().getFallback().getFieldProperty("title");
		}
		if( title != null)
		{
			dlink.setText(title.getValue());
		}
		else
		{
			String name = fixCase(page);
			dlink.setText(name);
		}
		
		PageProperty rank = page.getPageSettings().getProperty("pagerank"); //The file may be in base directory so we need
		//to look over fallback values as well
		if( rank != null)
		{
			dlink.setRank(Integer.parseInt(rank.getValue()));
		}
		
		return dlink;
	}

	private String fixCase(Page inPage)
	{
		String pname = null;
		if( inPage.isFolder() )
		{
			pname = inPage.getName();
		}
		else if(inPage.getName().equals("index.html") )
		{
			pname = inPage.getDirectoryName();
		}
		else
		{
			pname =	PathUtilities.extractPageName(inPage.getPath());
		}
		pname = pname.replace('-', ' ');
		pname = pname.replace('_', ' ');
		
		StringBuffer name = new StringBuffer();
		boolean upercasenextword = true;
		for (int i = 0; i < pname.length(); i++)
		{
			char charAt = pname.charAt(i);
			if(Character.isWhitespace(charAt))
			{
				upercasenextword  =true;
				name.append(' ');							
			}
			//If it already is uppercase then add spacer
			else if( i > 0 && Character.isUpperCase(charAt) && !Character.isWhitespace(pname.charAt(i-1)) )
			{
				name.append(' ');							
			}
			else if( upercasenextword )
			{
				charAt = Character.toUpperCase(charAt);
				upercasenextword = false;
			}

			
			name.append(charAt);
		}
		return name.toString();
	}

}