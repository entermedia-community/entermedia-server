package utils;

import java.util.Iterator;
import java.util.List;

import com.openedit.WebPageRequest;
import com.openedit.page.Page
import com.openedit.page.manage.PageManager
import com.openedit.util.PathUtilities;

import org.openedit.entermedia.MediaArchive
import org.openedit.*;

import com.openedit.hittracker.*;

import org.openedit.repository.ContentItem;

class copytool
{
	WebPageRequest context;
	def log;
	
	PageManager pageManager;
	int found = 0;
	int totalfound = 0;
	
	public void copyAll()
	{
			String root = "/WEB-INF/data/media/catalogs/public/assetsALL/";
			ContentItem item = pageManager.getRepository().getStub(root);
			
			copyFolder(item);
			
	}
	
	protected boolean createAttachments(List inPaths)
	{
		for (Iterator iterator2 = inPaths.iterator(); iterator2.hasNext();)
			{
				String path = (String) iterator2.next();
				if( path.endsWith("/Fonts") || path.endsWith("/Links") )
				{
					return true;
				}
			}
	
		return false;
	}
	public void copyFolder(ContentItem inSource)
	{
		if( found == 100 )
		{
			log.info("copied " + totalfound );
			found = 0;
		}

		List paths = pageManager.getChildrenPaths(inSource.getPath());
		if( paths.size() == 0 )
		{
			return;
		}
		if( createAttachments(paths) )
		{
			return; //had a font folder
		}

		for (Iterator iterator = paths.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			ContentItem item = pageManager.getRepository().getStub(path);
			if( item.isFolder() )
			{
				copyFolder(item);
			}
			else 
			{
				if( !item.getPath().contains("/.") )
				{
					String saveto = item.getPath().replaceAll("assetsALL","assets");
					ContentItem target = pageManager.getRepository().getStub(saveto);
					pageManager.getRepository().copy( item,  target);
					found++;
					totalfound++;
				}
			}
		}
	}
}

def copyt = new copytool();
copyt.context = context;
copyt.log = log;
MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
copyt.pageManager = archive.getPageManager();
copyt.copyAll();
