
package org.entermedia.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.junit.Test;
import org.openedit.util.HttpSharedConnection;

public class SharedHttpTest
{
	MediaArchive archive = new MediaArchive();
	private static final Log log = LogFactory.getLog(SharedHttpTest.class);

	@Test
	public void testFormatLength() throws Exception
	{
		HttpSharedConnection connection = new org.entermediadb.net.HttpSharedConnection();//(HttpSharedConnection)archive.getBean("httpSharedConnection");
		String url = "http://47.186.29.91:53739";
		String url2 = "https://test.emediaworkspace.com";
		for (int i = 0; i < 10; i++)
		{
			long start = System.currentTimeMillis();
			String json = connection.getResponseString(url);
			long end = System.currentTimeMillis();
			log.info(i + " Time: " + (end-start));

			long start2 = System.currentTimeMillis();
			String json2 = connection.getResponseString(url);
			long end2 = System.currentTimeMillis();
			log.info(i + " Time: " + (end2-start2));

			Thread.sleep(2000);
			
		}
		
	}
	

}
