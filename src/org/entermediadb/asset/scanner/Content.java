package org.entermediadb.asset.scanner;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.links.Link;
import org.openedit.util.OutputFiller;

public class Content
{
	private static final Log log = LogFactory.getLog(Content.class);
	protected Link fieldUrl;
	
	public Link getUrl()
	{
		return fieldUrl;
	}

	public void setUrl(Link inUrl)
	{
		fieldUrl = inUrl;
	}

	public byte[] getContent() throws Exception
	{
		URL url = new URL(getUrl().getPath());
		URLConnection connect = url.openConnection();
		connect.connect();
		if( connect.getContentLength() > 10000 * 1024 )
		{
			log.error(getUrl() +" is over 10 megs");
			return null;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream in = connect.getInputStream();
		try
		{
			new OutputFiller().fill(in, out);
		}
		finally
		{
			in.close();
		}
		return out.toByteArray();
	}

}
