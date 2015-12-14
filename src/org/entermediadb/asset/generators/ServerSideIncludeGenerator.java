/*
 * Created on Feb 8, 2006
 */
package org.entermediadb.asset.generators;

import java.io.IOException;
import java.io.Writer;

import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.BaseGenerator;
import org.openedit.generators.Output;
import org.openedit.page.Page;

public class ServerSideIncludeGenerator extends BaseGenerator
{
	public void generate(WebPageRequest inContext, Page inPage, Output inOut) throws OpenEditException
	{
		String text = inPage.getContent();
		String converted = parseSSI( text  );
		Writer inOutput = inOut.getWriter();
		try
		{
			inOutput.write(converted);
		} catch (IOException ex)
		{
			throw new OpenEditException(ex);
		}
	}

	public String parseSSI(String text) throws OpenEditException
	{
		//first check for []
		StringBuffer out = new StringBuffer(text.length() + 100);
		int count = 0;
		while( true )
		{
			int found = text.indexOf("<!--#include ", count);
			if( found != -1)
			{
				out.append(text.substring(count,found));
				count = found;
				out.append("$pages.include(");
				int firstquote = text.indexOf("\"",found);
				if( firstquote == -1)
				{
					break;
				}
				int end = text.indexOf	("-->", found);
				if( end == -1)
				{
					break;
				}
				String path = text.substring(firstquote, end);
				out.append( path);
				out.append(")");
				count = end + 3;
			}
			else
			{
				break;
			}
		}
		if( count < text.length())
		{
			out.append(text.substring(count,text.length()));
		}

		return out.toString();
	}

}
