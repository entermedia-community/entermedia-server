package org.entermediadb.elemental;

import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.repository.ContentItem;
import org.openedit.util.XmlUtil;


public class ElementalManager implements CatalogEnabled
{
	
	private static final Log log = LogFactory.getLog(ElementalManager.class);

	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	protected XmlUtil fieldXmlUtil;

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}
	
	protected MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}
	public XmlUtil getXmlUtil()
	{
		if (fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}

	public HttpClient getClient()
	{

		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();

		return httpClient;
	}
	
	public void getJobs()  {
		
		String elementalroot = getMediaArchive().getCatalogSettingValue("elementalserver");
		//curl -H "Accept: application/xml" http://<server_ip>/api/jobs
		String addr = elementalroot + "/api/jobs";
		
		
		HttpGet method = new HttpGet(addr);
		setHeaders(method, "/jobs");
		
		try
		{
			HttpResponse resp = getClient().execute(method);
			String xml = EntityUtils.toString(resp.getEntity());

		//	Element elem = getXmlUtil().getXml(resp.getEntity().getContent(), "UTF-8");
			log.info(resp.getStatusLine());
			
			
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		
		

		
	}

	private void setHeaders(HttpRequestBase inMethod, String inUrl)
	{
	
		inMethod.setHeader("Accept", "application/xml");
	//	inMethod.setHeader("Content-type", "application/xml");

		///md5(api_key + md5(url + X-Auth-User + api_key + X-Auth-Expires))
		
		
		String elementaluser = getMediaArchive().getCatalogSettingValue("elementaluser");
		String elemantalkey = getMediaArchive().getCatalogSettingValue("elementalkey");
		
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(new Date());
		cal.add(GregorianCalendar.DAY_OF_MONTH, 1); 
		
		long time = cal.getTimeInMillis() / 1000; //unix time, good till tomorrow
		String initial = inUrl +  elementaluser + elemantalkey + time;
		String digest = DigestUtils.md5Hex(initial);
		String combined = elemantalkey + digest;
		
		
		String  finalkey = DigestUtils.md5Hex(combined);
		
		inMethod.setHeader("X-Auth-User", elementaluser );
		inMethod.setHeader("X-Auth-Expires", String.valueOf(time));
		inMethod.setHeader("X-Auth-Key", finalkey );
		
		
	}

	public ConvertResult updateJobStatus(Data inTask)
	{
		String elementalroot = getMediaArchive().getCatalogSettingValue("elementalserver");
		String addr = elementalroot + "/api/jobs/" + inTask.get("externalid") + "/status";

		HttpGet method = new HttpGet(addr);
		setHeaders(method, "/jobs/" + inTask.get("externalid") + "/status");
		ConvertResult result = new ConvertResult();
		
		try
		{
			HttpResponse resp = getClient().execute(method);
			String xml = EntityUtils.toString(resp.getEntity());
		//	Element elem = getXmlUtil().getXml(resp.getEntity().getContent(), "UTF-8");
			log.info(resp.getStatusLine());
			
			
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		
		return result;
		
		
	}

	public String fieldRootPath;
	
	
	public String getRootPath()
	{
		if(fieldRootPath == null)
		{
			fieldRootPath = "/mnt/Meld/Playback/";
		}
		return fieldRootPath;
	}

	public void setRootPath(String inRootPath)
	{
		fieldRootPath = inRootPath;
	}

	public Element createJob(ConvertInstructions inStructions)
	{
		String elementalroot = getMediaArchive().getCatalogSettingValue("elementalserver");
		
//		<?xml version="1.0" encoding="UTF-8"?>
//		<job>
//		  <input>
//		    <file_input>
//		        <uri>/data/server/elemental.mov</uri>
//		    </file_input>
//		  </input>
//		  <profile>1</profile>
//		</job>
		
		
		///mnt/Meld-Playback/temp/18955.mp4  Input test file
		
		///mnt/Meld-Playback/temp-out 
		
		ContentItem item = inStructions.getInputFile(); 
		
		String preset = inStructions.getProperty("preset");
		Element job = DocumentHelper.createElement("job");
		Element input = job.addElement("input");
		Element file = input.addElement("file_input");
		Element uri = file.addElement("uri");
		//uri.setText("/mnt/Meld/Playback/temp/18955.mp4");  //Input
		uri.setText(item.getAbsolutePath());
		//uri.setText(SOME URI);
		Element og = job.addElement("output_group");
		og.addElement("file_output_group").addElement("destination").addElement("uri").setText(getRootPath()  + "/" + inStructions.getAssetSourcePath() );
		og.addElement("output").addElement("preset").setText(preset);
		//	job.addElement("preset").setText(preset);
	//	job.addElement("destination").addElement("uri").setText("/mnt/Meld-Playback/temp-out");
	//	job.addElement("profile").setText("1");
		
		
		String addr = elementalroot + "/api/jobs";
		HttpPost method = new HttpPost(addr);
		setHeaders(method, "/jobs");
		String asXML = job.asXML();
		StringEntity params = new StringEntity(asXML, "UTF-8");
		method.setEntity(params);
		
		try
		{
			HttpResponse response2 = getClient().execute(method);
			
			
			//String xml = EntityUtils.toString(response2.getEntity());
			String body = IOUtils.toString(response2.getEntity().getContent(), "UTF-8");

			StatusLine sl = response2.getStatusLine();
			int status = sl.getStatusCode();
			if (status >= 400)
			{
				throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
			}
		}
		catch (Exception e)
		{
			throw new OpenEditException (e);
		}
		
		return job;
		
	}
	

	

	
	
	
	
}
