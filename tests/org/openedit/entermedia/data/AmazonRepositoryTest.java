package org.openedit.entermedia.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import org.entermedia.amazon.S3Repository;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.MediaArchive;
import org.openedit.repository.ContentItem;

import com.openedit.page.Page;

public class AmazonRepositoryTest extends BaseEnterMediaTest
{
	
	
	public S3Repository getRepo()
	{
		MediaArchive archive = getMediaArchive();

		S3Repository repo = (S3Repository) archive.getModuleManager().getBean(archive.getCatalogId(), "S3Repository");
		
		repo.setBucket("visualdatatest");
		repo.setAccessKey("AKIAIHFKVZJBUPJOZKWA");
		repo.setSecretKey("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");
		
		return repo;
	}
	
	public void testMountedS3() throws Exception
	{
		//requires a mount to be setup in oemounts.xml
		MediaArchive archive = getMediaArchive();
		
		Page testfile = archive.getPageManager().getPage("/clients/embed/index.html");
		
		getRepo().put(testfile.getContentItem());
	
		ContentItem i = getRepo().get("/clients/embed/index.html");
		assertNotNull(i);
		assertTrue(i.getInputStream().available() >0);
		Calendar now = Calendar.getInstance();
		now.add(Calendar.DAY_OF_YEAR, -1);
		 
		URL url = getRepo().getPresignedURL("/clients/embed/index.html", now.getTime());
		assertNotNull(url);
		String urlstring = url.toString();
		assertNotNull(urlstring);
		
//		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
//        while (true) {
//            String line = reader.readLine();
//            if (line == null) break;
//            assertTrue(line.contains("expired"));
//         
//        }
//        
//        reader.close();
        
       
        now.add(Calendar.DAY_OF_YEAR, 2);
        
        url = getRepo().getPresignedURL("/clients/embed/index.html", now.getTime());
		
    	urlstring = url.toString();
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        while (true) {
            String line = reader.readLine();
            if (line == null) break;
            assertFalse(line.contains("expired"));
         
        }        
		
//		assertTrue(getRepo().doesExist("/clients/embed/index.html"));
		
		
		//Page dest = archive.getPageManager().getPage("/" + archive.getCatalogId() + "/publishing/smartjog/sub/index.html");
//		archive.getPageManager().copyPage(testfile, dest);
//		assertTrue(dest.exists());
//		archive.getPageManager().removePage(dest);
//		assertFalse(dest.exists());
	}
}
