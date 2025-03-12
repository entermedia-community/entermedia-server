/*
 * Created on Apr 5, 2005
 */
package org.entermediadb.modules.dates;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.openedit.WebPageRequest;
import org.openedit.modules.BaseModule;
import org.openedit.page.PageAction;
import org.openedit.util.DateStorageUtil;

/**
 * @author cburkey
 *
 */
public class DateModule extends BaseModule
{
	public static final String UNIVERSAL_DATE_FORMAT = "MM/dd/yyyy HH:mm:ss Z";

	public void now(WebPageRequest inReq) throws Exception
	{
		TimeZone zone = inReq.getTimeZone();

		Calendar now = null;
		
		if( zone == null)
		{
			now = Calendar.getInstance(); 
		}
		else
		{
			now = Calendar.getInstance(zone); 
		}
		Date date = now.getTime();
		inReq.putPageValue("now",date);

		inReq.putPageValue("storeddate",DateStorageUtil.getStorageUtil().formatForStorage(date));

		String format = inReq.findValue("format");
		
		if ( format != null)
		{
			SimpleDateFormat sformat = new SimpleDateFormat(format);
			String fdate = sformat.format(date);
			inReq.putPageValue("formatteddate",fdate);
		}
	}
}
