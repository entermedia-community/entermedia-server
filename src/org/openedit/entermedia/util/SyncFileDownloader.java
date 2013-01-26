package org.openedit.entermedia.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.repository.ContentItem;
import org.openedit.repository.filesystem.FileItem;
import org.openedit.util.WindowsUtil;

import com.openedit.OpenEditException;
import com.openedit.page.manage.PageManager;
import com.openedit.util.FileUtils;
import com.openedit.util.XmlUtil;
import com.openedit.util.ZipUtil;

public class SyncFileDownloader
{
	public static final String DIR = "dir";
	public static final String FILE = "file";

	public static final String PATH = "path";
	public static final String DATE = "date";
	private static final Log log = LogFactory.getLog(SyncFileDownloader.class); 
	
	protected HttpClient fieldHttpClient;
	protected File fieldRoot;
	protected String fieldUsername;
	protected String fieldPassword;
	protected XmlUtil fieldXmlUtils;
	protected ZipUtil fieldZipUtils;
	protected WindowsUtil fieldWindowsUtil;
	protected FileUtils fieldFileUtils;
	protected PageManager fieldPageManager;
	protected String fieldLastChecked;
	protected String fieldServerUrl; //http://localhost
	protected String fieldSyncPath; //depend if this is data or files
	protected String fieldLoginPath = "/entermedia/services/rest/login.xml";
	protected String fieldDownloadPath = "/entermedia/services/rest/downloadfiles.zip";
	
	public String getDownloadPath()
	{
		return fieldDownloadPath;
	}

	public void setDownloadPath(String inDownloadPath)
	{
		fieldDownloadPath = inDownloadPath;
	}

	public String getLoginPath()
	{
		return fieldLoginPath;
	}

	public void setLoginPath(String inLoginPath)
	{
		fieldLoginPath = inLoginPath;
	}

	public String getLastChecked()
	{
		return fieldLastChecked;
	}

	public void setLastChecked(String inLastChecked)
	{
		fieldLastChecked = inLastChecked;
	}

	public String fieldListXml = "/entermedia/tools/sync/listfiles.xml";

	public String getListXml()
	{
		return fieldListXml;
	}

	public void setListXml(String inListXml)
	{
		fieldListXml = inListXml;
	}

	public Element listRemoteChanges(String inRemotePath)
	{
		String path = getServerUrl() + getListXml();
		PostMethod postMethod = createPostMethod(path);
		if( getLastChecked() != null)
		{
			postMethod.addParameter("since",getLastChecked() );
		}
		postMethod.addParameter("syncpath", inRemotePath);
		// client.getHttpConnectionManager().getParams().setConnectionTimeout(0);
		int statusCode1 = executePostMethod(postMethod);
		// postMethod.releaseConnection(); //Is this needed?
		Element remotelist = xmlElementFromPost(postMethod);
		return remotelist;
	}
	
	protected int executePostMethod(PostMethod postMethod) 
	{
		int statusCode1 = 0;
		String error = null;
		try
		{
			statusCode1 = getHttpClient().executeMethod(postMethod);
			if( statusCode1 != 200)
			{
				error = postMethod.getResponseBodyAsString();
			}
		}
		catch(Exception ex)
		{
			throw new OpenEditException(ex);
		}
		if( statusCode1 != 200)
		{
			throw new OpenEditException(postMethod +  " error: " + statusCode1 + " " + error);
		}
		return statusCode1;

	}

	protected PostMethod createPostMethod(String inUrl)
	{
		PostMethod postMethod = new PostMethod(inUrl);
		//postMethod.addParameter( "relative", "" + isRelativeSync() );
//		String md5 = getCookieEncryption().getPasswordMd5(getPassword());
//		String value = getUsername() + "md542" + md5;
//		postMethod.addParameter("entermedia.key",value);
//		postMethod.addParameter("accountname", getUsername());
//		postMethod.addParameter("password", getPassword());
		for (Iterator iterator = getZipUtils().getExcludes().iterator(); iterator.hasNext();)
		{
			String exclude = (String) iterator.next();
			postMethod.addParameter("exclude", exclude);
		}
		return postMethod;

	}

	protected boolean isDirectory(Element remotefile)
	{
		return DIR.equalsIgnoreCase(remotefile.getName());
	}

	protected void unzip(InputStream in) throws IOException
	{
		File local = getFile(getSyncPath());

		//		String localPath = resolveLocalPath( element.attributeValue( PATH ) );
		//		long dateStamp = Long.parseLong( dated ) * 1000;
		//		File file = getFile( localPath );
		//
		//		

		getZipUtils().unzip(in, local, getSyncPath().substring(1));
	}

	/**
	 * We reset the time stamps on all downloaded files just to be sure for next
	 * time
	 * 
	 * @param inRemote
	 * @throws ParseException
	 */
	protected void resetTimeStamps(Map inRemote) throws ParseException
	{
		for (Iterator iterator = inRemote.values().iterator(); iterator.hasNext();)
		{
			Element element = (Element) iterator.next();
			if (isDirectory(element))
			{
				continue;
			}
			String dated = element.attributeValue(DATE);
			String localPath = element.attributeValue(PATH);
			long dateStamp = Long.parseLong(dated) * 1000; //zero millisecond
			File file = getFile(localPath);

			if (!file.setLastModified(dateStamp))
			{
				log.error("Failed to reset timestamp on file: " + file.getAbsolutePath());
			}
			if (log.isDebugEnabled())
			{
				log.info("Reset timestamp on " + file.getAbsolutePath());
			}
		}
	}

	protected File getFile(String inLocalPath)
	{
		ContentItem item = getPageManager().getRepository().getStub(inLocalPath);
		//This is temporary until we can redo the ZipUtil to only use ContentItems
		FileItem file = (FileItem) item;
		return file.getFile();
	}

	public HttpClient getHttpClient()
	{
		if (fieldHttpClient == null)
		{
			fieldHttpClient = new HttpClient();
		}

		return fieldHttpClient;
	}

	public XmlUtil getXmlUtils()
	{
		if (fieldXmlUtils == null)
		{
			fieldXmlUtils = new XmlUtil();
		}

		return fieldXmlUtils;
	}

	public ZipUtil getZipUtils()
	{
		if (fieldZipUtils == null)
		{
			fieldZipUtils = new ZipUtil();
			if (getRoot() == null)
			{
				throw new OpenEditException("ZipUtil root cannot be null.");
			}
			fieldZipUtils.setRoot(getRoot());

			fieldZipUtils.setPageManager(getPageManager());
		}
		return fieldZipUtils;
	}

	protected Element xmlElementFromPost(PostMethod postMethod)
	{
		try
		{
			InputStream body = postMethod.getResponseBodyAsStream();
			Reader reader = new InputStreamReader(body, "UTF-8");
			Element root = getXmlUtils().getXml(reader, "UTF-8");
			return root;
		}
		catch ( Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}

	public String getSyncPath()
	{
		return fieldSyncPath;
	}

	public void setSyncPath(String localPath)
	{
		fieldSyncPath = localPath;
	}

	public void addExclude(String inPattern)
	{
		getZipUtils().addExclude(inPattern);
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public FileUtils getFileUtils()
	{
		if (fieldFileUtils == null)
		{
			fieldFileUtils = new FileUtils();

		}

		return fieldFileUtils;
	}

	public WindowsUtil getWindowsUtil()
	{
		if (fieldWindowsUtil == null)
		{
			fieldWindowsUtil = new WindowsUtil();
			fieldWindowsUtil.setRoot(getRoot());
		}

		return fieldWindowsUtil;
	}

	public String getUsername()
	{
		return fieldUsername;
	}

	public void setUsername(String inUsername)
	{
		fieldUsername = inUsername;
	}

	public String getPassword()
	{
		return fieldPassword;
	}

	public void setPassword(String inPassword)
	{
		fieldPassword = inPassword;
	}

	public String getServerUrl()
	{
		return fieldServerUrl;
	}

	public void setServerUrl(String url)
	{
		if (!url.startsWith("http://") && !url.startsWith("https://"))
		{
			url = "http://" + url;
		}

		if (url.endsWith("/"))
		{
			url = url.substring(0,url.length()-1);
		}
		fieldServerUrl = url;
	}

	public File getRoot()
	{
		return fieldRoot;
	}

	public void setRoot(File inRoot)
	{
		fieldRoot = inRoot;
	}

	protected void downloadAllFiles(Map inRemote) throws Exception
	{
		if ( inRemote.size() == 0 )
		{
			return;
		}
		String url = getServerUrl();
		// log.info("posting here:" + url);
		url += getDownloadPath();
		PostMethod postMethod = createPostMethod( url );
	
		//TODO:  We should adhere to the URL limit of 255 characters if possible.
		// This is working fine server to server, but it may not work if we have 
		// to pass through a proxy or a firewall.
	
		// Some ideas to get around this:
		// 1.  Download ~10 files at a time
		// 2.  Send the directory as an independent parameter so that it is not being
		// repeated for every file.
		// 3.  If we start downloading 10 at a time, we could mutlti-thread the download
		// and have this method launch 10 threads downloading 10 at a time.
		// 4.  We could also send the download file list as an XML file attachment
		for ( Iterator iterator = inRemote.values().iterator(); iterator.hasNext(); )
		{
			Element file = (Element) iterator.next();
			String path = file.attributeValue( PATH );
			postMethod.addParameter( FILE, path );
		}
		// Not sure if this is efficient or not
	
		// client.getHttpConnectionManager().getParams().setConnectionTimeout(0);
		int statusCode1 = executePostMethod( postMethod );
	
		if ( statusCode1 == 200 )
		{
			InputStream in = postMethod.getResponseBodyAsStream();
			unzip( in );
			in.close();
	
			//TODO: find out if this is necessary
			resetTimeStamps( inRemote );
		}
		else
		{
			log.error( "SyncToServer returned status: " + statusCode1 );
		}
	}

	//This should setup the cookie
	public boolean login()
	{
		PostMethod method = new PostMethod(getServerUrl() + getLoginPath() );
		method.addParameter(new NameValuePair("accountname", getUsername()));
		method.addParameter(new NameValuePair("password", getPassword()));

		int statusCode =0;
		try
		{
			statusCode = getHttpClient().executeMethod(method);
		}
		catch (HttpException e)
		{
			throw new OpenEditException(e);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return statusCode == 200;
		
	}

}
