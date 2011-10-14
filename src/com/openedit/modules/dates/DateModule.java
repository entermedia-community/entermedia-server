/*
 * Created on Apr 5, 2005
 */
package com.openedit.modules.dates;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openedit.util.DateStorageUtil;

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
	
	public void loadFormat(WebPageRequest inReq) throws Exception
	{
		String format = inReq.findValue("dateformat");
		if( format != null)
		{
			DateFormat formater = DateStorageUtil.getStorageUtil().getDateFormat(format);
			inReq.putPageValue("formater", formater);
			
		}
		inReq.putPageValue("datestorageutil", DateStorageUtil.getStorageUtil());
	}
	
}
