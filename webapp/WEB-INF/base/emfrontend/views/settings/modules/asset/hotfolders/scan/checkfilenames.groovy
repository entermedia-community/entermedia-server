package importing;

import org.openedit.page.Page 
import org.openedit.data.Searcher 
import org.entermediadb.asset.Asset 
import org.entermediadb.asset.MediaArchive 
import org.openedit.*;

import org.openedit.WebPageRequest;
import org.openedit.hittracker.*;
import org.entermediadb.asset.creator.*;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.CatalogConverter;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.ConvertStatus;
import org.entermediadb.asset.MediaArchive;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import org.openedit.util.*;
import org.openedit.repository.*;
import org.openedit.users.*;
import org.openedit.OpenEditException;
import org.openedit.page.manage.PageManager;
import org.entermediadb.asset.scanner.AssetImporter;

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");
		
		LongPathFinder finder = new LongPathFinder();
		
		finder.setPageManager(archive.getPageManager());

		String from = context.getRequestParameter("from");
		
		String assetRoot = "/WEB-INF/data/" + archive.getCatalogId() + "/originals/" + from;
		finder.setRootPath(assetRoot);
		log.info("Checking for bad paths");
		
		finder.process();
		
		log.info("found ${finder.badfiles.size()} bad files");
		
		
		context.putPageValue("finder",finder);		
		Page root = archive.getPageManager().getPage(assetRoot);
		int edited = 0;
//		for(String sourcepath: finder.badfiles)
//		{
//			Asset asset = archive.getAssetBySourcePath(sourcepath);
//            if( asset != null && asset.get("importstatus") != "error")
//            {
//				asset.setProperty("importstatus", "error");
//				asset.setProperty("pushstatus", "error");
//				archive.saveAsset(asset, null);
//				edited++;
//			}
//		}
		log.info("Complete " + finder.badfiles);
			
}

	class LongPathFinder extends  PathProcessor
		{
			public List folders = new ArrayList();
			public List folders()
			{
				return folders;
			}
			public List badfiles = new ArrayList();
			public List badfiles()
			{
				return badfiles;
			} 
			public FileUtils util = new FileUtils();
			public FileUtils util()
			{
				return util;
			}
			int count = 0;
			public void processDir(ContentItem inContent)
			{
				String path = inContent.getAbsolutePath();
				if( path.length() > 240 )
				{
					path = inContent.getPath().substring(getRootPath().length());
					folders.add(path);
				}
			}
			public  void processFile(ContentItem inContent, User inUser) 
			{ 
				incrementCount();
				String path = inContent.getPath();
			
				if (!util.isLegalFilename(path)) 
				{
					path = inContent.getPath().substring(getRootPath().length());
					badfiles.add(path);
				}
				count++;
				if( count == 10000 )
				{
					System.out.println(  badfiles.size() + " bad files" );
					count = 0;
				}

			}
			public int getTotalCount()
			{
				return count;
			}
			
		}	

init();
