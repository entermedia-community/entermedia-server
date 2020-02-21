package org.entermediadb.elemental;

import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.dom4j.Element;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.net.HttpSharedConnection;
import org.openedit.BaseWebPageRequest;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;
import org.openedit.profile.UserProfile;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.RequestUtils;
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

	public HttpSharedConnection getClient()
	{
		HttpSharedConnection connection = new HttpSharedConnection();
		return connection;
	}
	
	public void getJobs()  {
		
		String elementalroot = getMediaArchive().getCatalogSettingValue("elementalserver");
		//curl -H "Accept: application/xml" http://<server_ip>/api/jobs
		String addr = elementalroot + "/api/jobs";
		
		
		HttpGet method = new HttpGet(addr);
		setHeaders(method, "/jobs");
		
		try
		{
			HttpResponse resp = getClient().sharedExecute(method);
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
		log.info("X-Auth-User = " + elementaluser );
		log.info("X-Auth-Expires = " + String.valueOf(time));
		log.info("X-Auth-Key ="  + finalkey );
		
	}

	public ConvertResult updateJobStatus(Data inTask)
	{
		String elementalroot = getMediaArchive().getCatalogSettingValue("elementalserver");
		String addr = elementalroot + "/api/jobs/" + inTask.get("externalid") + "/status";

		String jobid =  inTask.get("externalid");
		log.info("found " + jobid);
		HttpGet method = new HttpGet(addr);
		
		
		setHeaders(method, "/jobs/" + jobid + "/status");
		ConvertResult result = new ConvertResult();
		
		try
		{
			HttpResponse resp = getClient().sharedExecute(method);
			log.info(resp.getStatusLine());
			String xml = EntityUtils.toString(resp.getEntity());
			log.info(xml);
			Element elem = getXmlUtil().getXml(resp.getEntity().getContent(), "UTF-8");
			
			//If job is done
			
			
			//16x9  540p
			
			
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
			fieldRootPath = "/mnt/Meld/Playback";
		}
		return fieldRootPath;
	}

	public void setRootPath(String inRootPath)
	{
		fieldRootPath = inRootPath;
	}

	public Element createJob(ConvertInstructions inStructions)
	{
		if( true ) return null;
		
		String elementalroot = getMediaArchive().getCatalogSettingValue("elementalserver");
		
		///mnt/Meld-Playback/temp/18955.mp4  Input test file
		
		///mnt/Meld-Playback/temp-out 
		
		ContentItem item = inStructions.getInputFile(); 
		RequestUtils rutil = (RequestUtils) getMediaArchive().getBean("requestUtils");
		
		User user = (User) getMediaArchive().getData("user","admin");
		UserProfile profile = (UserProfile) getMediaArchive().getData("userprofile","admin");
		BaseWebPageRequest context = (BaseWebPageRequest) rutil.createVirtualPageRequest(getMediaArchive().getCatalogHome() + "/configuration/elementaljob.xml",user,profile); 

		String outputpath = getRootPath()  + "/" + inStructions.getAssetSourcePath();
		context.putPageValue("inputpath",item.getAbsolutePath());
		context.putPageValue("outputpath",outputpath);
		
		String elementalpresetname = inStructions.getProperty("elementalpresetname");
		context.putPageValue("elementalpresetname",elementalpresetname);
		
		
		context.getPageStreamer().render();
		String jobsubmit = context.getWriter().toString();
		
		log.info("Sending job xml: " + jobsubmit);
		
		String addr = elementalroot + "/api/jobs";
		HttpPost method = new HttpPost(addr);
		
		setHeaders(method, "/jobs");
		StringEntity params = new StringEntity(jobsubmit, "UTF-8");
		params.setContentType("application/xml");
		method.setEntity(params);
		try
		{
			HttpResponse response2 = getClient().sharedExecute(method);
			
			//String xml = EntityUtils.toString(response2.getEntity());
			String body = IOUtils.toString(response2.getEntity().getContent(), "UTF-8");
			log.info("Got this back:" + body);
			StatusLine sl = response2.getStatusLine();
			int status = sl.getStatusCode();
			if (status >= 400)
			{
				log.error("error from server " + status + "  " + sl.getReasonPhrase());
				return null;
			}
			//https://docs.aws.amazon.com/mediaconvert/latest/apireference/jobs.html
//			{
//				  "jobs": [
//				    {
//				      "arn": "string",
//				      "id": "string",
//				      "createdAt": "string",
//				      "jobTemplate": "string",
//				      "queue": "string",
//				      "userMetadata": {
//				      },
			Element job = getXmlUtil().getXml(jobsubmit, "UTF-8");

			job.addAttribute("jobid", "someid");
			return job;
		}
		catch (Exception e)
		{
			throw new OpenEditException (e);
		}
		
	}
	

	

	
	
	
	
}
