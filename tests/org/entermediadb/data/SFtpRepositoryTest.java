package org.entermediadb.data;

import java.util.List;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.MediaArchive;
import org.openedit.page.Page;

public class SFtpRepositoryTest extends BaseEnterMediaTest
{
	
	
	
	public void testMountedFtp() throws Exception
	{
		//requires a mount to be setup in /WEB-INF/oemounts.xml
//		<?xml version="1.0" encoding="UTF-8"?>
//
//		<mounts>
//		  <mount path="/sftp" externalpath="sftp://sftptest@dev.openedit.org" repositorytype="sftpRepository"/>
//		</mounts>

		
		MediaArchive archive = getMediaArchive();
		
		Page testfile = archive.getPageManager().getPage("/index.html");
		Page dest = archive.getPageManager().getPage("/sftpmount/");
		archive.getPageManager().copyPage(testfile, dest);
		assertTrue(dest.exists());
		List files = archive.getPageManager().getChildrenPaths("/sftpmount/");
		assertTrue(files.size() >0);
		archive.getPageManager().removePage(dest);
		assertFalse(dest.exists());
		
	}
}
