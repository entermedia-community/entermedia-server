/*
 * Created on Apr 5, 2005
 */
package com.openedit.modules.dates;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.openedit.WebPageRequest;
import com.openedit.modules.BaseModule;
import com.openedit.page.PageAction;

/**
 * @author cburkey
 *
 */
public class DateModule extends BaseModule
{
	public static final String UNIVERSAL_DATE_FORMAT = "MM/dd/yyyy HH:mm:ss Z";

	public void now(WebPageRequest inReq) throws Exception
	{
		PageAction inAction = inReq.getCurrentAction();
		
		Date date = new Date();
		inReq.putPageValue("now",date);
		
		String format = inReq.getRequestParameter("format");
		
		if ( format == null)
		{
			format = inAction.getChildValue("format");
		}
		if ( format != null)
		{
			SimpleDateFormat sformat = new SimpleDateFormat(format);
			String fdate = sformat.format(date);
			inReq.putPageValue("formatteddate",fdate);
		}
	}
}
