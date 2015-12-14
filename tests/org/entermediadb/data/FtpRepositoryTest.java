package org.entermediadb.data;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.FtpRepository;
import org.entermediadb.asset.MediaArchive;
import org.openedit.page.Page;

public class FtpRepositoryTest extends BaseEnterMediaTest
{
	
	//unused
	public FtpRepository getRepo()
	{
		MediaArchive archive = getMediaArchive();

		FtpRepository repo = (FtpRepository) archive.getModuleManager().getBean(archive.getCatalogId(), "ftpRepository");
		repo.setExternalPath("dev.openedit.org");
		repo.setUserName("ftptest");
		return repo;
	}
	
	public void testMountedFtp() throws Exception
	{
		//requires a mount to be setup in oemounts.xml
		MediaArchive archive = getMediaArchive();
		
		Page testfile = archive.getPageManager().getPage("/index.html");
		Page dest = archive.getPageManager().getPage("/" + archive.getCatalogId() + "/publishing/smartjog/sub/index.html");
		archive.getPageManager().copyPage(testfile, dest);
		assertTrue(dest.exists());
		archive.getPageManager().removePage(dest);
		assertFalse(dest.exists());
	}
}
