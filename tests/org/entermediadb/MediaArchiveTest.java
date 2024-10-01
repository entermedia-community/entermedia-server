
package org.entermediadb;

import static org.junit.Assert.assertEquals;

import org.entermediadb.asset.MediaArchive;
import org.junit.Test;

public class MediaArchiveTest
{
	MediaArchive archive = new MediaArchive();

	@Test
	public void testFormatLength()
	{
		String actual = archive.formatLength("5");
		assertEquals("00:00:05", actual);
	}
	@Test
	public void testFormatMinutesAndSecondsOnly()
	{
		assertEquals(":10", archive.formatMinutesAndSeconds(String.valueOf(10)));
		assertEquals(":35", archive.formatMinutesAndSeconds(String.valueOf(35)));
		assertEquals("3:10", archive.formatMinutesAndSeconds(String.valueOf(60*3+10)));
		assertEquals("10:20", archive.formatMinutesAndSeconds(String.valueOf(60*10+20)));
		assertEquals("82:17", archive.formatMinutesAndSeconds(String.valueOf(60*82+17)));
		assertEquals("165:00", archive.formatMinutesAndSeconds(String.valueOf(60*165)));
	}

}
