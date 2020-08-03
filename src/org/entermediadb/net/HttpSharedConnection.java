package org.entermediadb.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.OpenEditException;
import org.openedit.util.URLUtilities;




public class HttpSharedConnection
{
	
	private static final Log log = LogFactory.getLog(HttpSharedConnection.class);
	
	Charset UTF8 = Charset.forName("UTF-8");
	ContentType contentType = ContentType.create("text/plain", UTF8);
	ContentType octectType = ContentType.create("application/octect-stream", UTF8);
	
	public boolean DEBUG = false;
	
	protected HttpClient fieldHttpClient;
	protected Collection fieldSharedHeaders;
    protected BasicCookieStore cookieStore = new BasicCookieStore();

	public Collection getSharedHeaders()
	{
		if (fieldSharedHeaders == null)
		{
			fieldSharedHeaders = new ArrayList();
		}

		return fieldSharedHeaders;
	}


	public void setSharedHeaders(Collection inSharedHeaders)
	{
		fieldSharedHeaders = inSharedHeaders;
	}
	
	public HttpClient getSharedClient()
	{
		if (fieldHttpClient == null)
		{

			try {
				SSLContext sslContext;
				sslContext = SSLContextBuilder.create().useProtocol("TLSv1.2").build();
				RequestConfig globalConfig = RequestConfig.custom()
			            .setCookieSpec(CookieSpecs.DEFAULT)
			            .setConnectTimeout(15 * 1000)
			            .setSocketTimeout(120 * 1000)
			            .build();
				fieldHttpClient = HttpClients.custom().useSystemProperties()
			            .setDefaultRequestConfig(globalConfig)
			            .setSSLContext(sslContext)
			            .build();
			} catch ( Throwable e )
			{
				throw new OpenEditException(e);
			}
		}
		return fieldHttpClient;
	}



	public void reset()
	{
		fieldHttpClient = null;
	}
	
	public CloseableHttpResponse sharedPost(String path, Map<String,String> inParams)
	{
			HttpPost method = new HttpPost(path);
			method.setEntity(build(inParams));
			CloseableHttpResponse response2 = sharedExecute(method);
			return response2;
	
	}
	public CloseableHttpResponse sharedPostWithJson(String inUrl,JSONObject inBody)
	{
		HttpPost post = new HttpPost(inUrl);
		
		post.setEntity(new StringEntity(inBody.toString(), "UTF-8"));
		post.addHeader("Content-Type", "application/json");
		post.setProtocolVersion(HttpVersion.HTTP_1_1);
		
		return sharedPost(post);
		
	}
	
	
	public CloseableHttpResponse sharedPost(HttpPost inPost)
	{
		CloseableHttpResponse response2 = sharedExecute(inPost);
		return response2;
		
	}
	public void release(CloseableHttpResponse response2)
	{
		if( response2 == null)
		{
			return;
		}

		try
		{
			response2.close();
		}
		catch (IOException e)
		{
			log.error("Could not close" ,e);
		}
	}
	public CloseableHttpResponse sharedGet(String inUrl)
	{
		return sharedGet(inUrl,null);
	}
	public CloseableHttpResponse sharedGet(String inUrl,Map extraHeaders)
	{
		HttpGet method = new HttpGet(inUrl);
		CloseableHttpResponse response2 = (CloseableHttpResponse) sharedExecute(method);
		return response2;
	}
	
	protected HttpEntity build(Map <String, String> inMap){
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

		for (Iterator iterator = inMap.keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			String val = inMap.get(key);
			  nameValuePairs.add(new BasicNameValuePair(key, val));

			
		}
		 return new UrlEncodedFormEntity(nameValuePairs, UTF8);
		
	}
//	public String getText(String inUrl) 
//	{
//
//	}
	//httpmethod.addHeader("authorization", "Bearer " + inAccessToken);
	public JSONObject getJson(String inUrl) 
	{
		return getJson(inUrl,null);
	}	
	public JSONObject getJson(String inUrl,Map extraHeaders) 
	{
		CloseableHttpResponse resp = sharedGet(inUrl,extraHeaders);
		JSONObject elem = parseJson(resp);
		return elem;
	}


	public String parseText(CloseableHttpResponse inCreaterequest)
	{
		String errormessage = null;
		try
		{
			StatusLine statusLine = inCreaterequest.getStatusLine();
			if (statusLine.getStatusCode() != 200)
			{
				String returned = EntityUtils.toString(inCreaterequest.getEntity());
				errormessage = "HTTP Error:" + statusLine.getStatusCode() + ":" + statusLine.getReasonPhrase() + " Body: \n" + returned;
			}
			else
			{
				return EntityUtils.toString(inCreaterequest.getEntity());
			}
		}
		catch (Throwable e) 
		{
			throw new OpenEditException(e);
		}
		finally
		{
			release(inCreaterequest);
		}
		throw new OpenEditException(errormessage);
	}

	public JSONObject parseJson(CloseableHttpResponse resp) 
	{
		try {

			if (resp.getStatusLine().getStatusCode() == 404)
			{
				return null;
			}
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				String returned = EntityUtils.toString(resp.getEntity());
				throw new OpenEditException("HTTP Error:" + resp.getStatusLine().getStatusCode() + ":" + resp.getStatusLine().getReasonPhrase() + " Body: \n" + returned);
				//throw new OpenEditException("Could not process " + returned);
			}
			
			HttpEntity entity = resp.getEntity();
			
			JSONObject json = null;
			JSONParser parser = new JSONParser();

			if (DEBUG)
			{
				String returned = EntityUtils.toString(resp.getEntity());
				log.info(returned);
				json = (JSONObject)parser.parse(returned);
			}
			else
			{
				String charset = "utf-8";
				if( entity.getContentEncoding() != null )
				{
					charset = entity.getContentEncoding().getValue();
				}
				InputStreamReader reader = new InputStreamReader(entity.getContent(),charset);
				json = (JSONObject)parser.parse(reader);
				
			}
			// log.info(content);
			return json;
		}
		catch (Throwable e) 
		{
			throw new OpenEditException(e);
		}
		finally
		{
			release(resp);
		}
	}

	public CloseableHttpResponse sharedPost(String path,HttpEntity inBuild)
	{
			HttpPost method = new HttpPost(URLUtilities.urlEscape(path));
			method.setEntity(inBuild);
			
			CloseableHttpResponse response2 = sharedExecute(method);
			return response2;
		
	}

	public CloseableHttpResponse sharedExecute(HttpRequestBase method) 
	{
		CloseableHttpResponse resp = null;
		try 
		{
			for (Iterator iterator = getSharedHeaders().iterator(); iterator.hasNext();)
			{
				Header header =  (Header)iterator.next();
				method.addHeader(header);
			}

			resp = (CloseableHttpResponse)getSharedClient().execute(method);
			return resp;
		} 
		catch (Throwable e)
		{
			release(resp);
			throw new OpenEditException(e);
		}
	}
	
	public CloseableHttpResponse sharedMimePost(String path, Map<String,Object> inParams)
	{
		HttpEntity entity = buildMime(inParams);
		return sharedPost(path,entity);
	}
	
	protected HttpEntity buildMime(Map <String, Object> inMap)
	{
		HttpMimeBuilder builder = new HttpMimeBuilder();

		for (Iterator iterator = inMap.keySet().iterator(); iterator.hasNext();)
		{
			String key = (String) iterator.next();
			Object value = inMap.get(key);
			if( value instanceof String)
			{
				builder.addPart(key, (String)value);
			}
			else if(value instanceof File)
			{
				builder.addPart(key, (File)value);
			}
			else if( value instanceof JSONObject)
			{
				builder.addPart(key, ((JSONObject) value).toJSONString(), "application/json" );
			}
			
		}
		return builder.build();
	}

	public void addSharedHeader(String inType, String inVal)
	{
		BasicHeader header = new BasicHeader(inType, inVal);
		getSharedHeaders().add(header);
	}
	
	public void putSharedHeader(String inType, String inVal)
	{
		Collection copy = new ArrayList(getSharedHeaders());
		for (Iterator iterator = copy.iterator(); iterator.hasNext();)
		{
			BasicHeader header = (BasicHeader) iterator.next();
			if( header.getName().equals(inType) )
			{
				getSharedHeaders().remove(header);
			}
		}
		
		BasicHeader header = new BasicHeader(inType, inVal);
		getSharedHeaders().add(header);
	}

	
	public void addSharedCookie(String domain, String inKey,String inVal)
	{
		BasicClientCookie cookie = new BasicClientCookie(inKey, inVal);
		cookie.setPath("/");
		cookie.setDomain(domain);
    	Calendar cal = new GregorianCalendar();
    	cal.add(Calendar.MONTH, 1);
    	cookie.setExpiryDate(cal.getTime());
    	cookieStore.addCookie(cookie);
	}

	
	
}
