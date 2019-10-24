package org.entermediadb.net;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.OpenEditException;

import com.google.gson.JsonElement;




public class HttpSharedConnection
{
	
	private static final Log log = LogFactory.getLog(HttpSharedConnection.class);
	
	Charset UTF8 = Charset.forName("UTF-8");
	ContentType contentType = ContentType.create("text/plain", UTF8);
	ContentType octectType = ContentType.create("application/octect-stream", UTF8);
	
	protected HttpClient fieldHttpClient;
	
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
		try
		{
			HttpPost method = new HttpPost(path);
			method.setEntity(build(inParams));
			CloseableHttpResponse response2 = (CloseableHttpResponse)getSharedClient().execute(method);
			return response2;
		}
		catch ( Throwable e )
		{
			throw new OpenEditException(e);
		}
	}
	public CloseableHttpResponse sharedPost(HttpPost inPost)
	{
		try
		{
			CloseableHttpResponse response2 = (CloseableHttpResponse)getSharedClient().execute(inPost);
			return response2;
		}
		catch ( Throwable e )
		{
			throw new OpenEditException(e);
		}
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
		try
		{
			HttpGet method = new HttpGet(inUrl);
			CloseableHttpResponse response2 = (CloseableHttpResponse) getSharedClient().execute(method);
			return response2;
		}
		catch ( Exception ex )
		{
			throw new RuntimeException(ex);
		}
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
	public JSONObject getJson(String inUrl) 
	{
		CloseableHttpResponse resp = sharedGet(inUrl);
		try
		{
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + ":" + resp.getStatusLine().getReasonPhrase());
				String returned = EntityUtils.toString(resp.getEntity());
				log.info(returned);
				return null;
			}
			HttpEntity entity = resp.getEntity();
			JSONParser parser = new JSONParser();
			
			String charset = "utf-8";
			if( entity.getContentEncoding() != null )
			{
				charset = entity.getContentEncoding().getValue();
			}
			InputStreamReader reader = new InputStreamReader(entity.getContent(),charset);
			JSONObject elem = (JSONObject)parser.parse(reader);
			// log.info(content);
			//JsonObject json = elem.getAsJsonObject();
			return elem;
		}
		catch (Throwable ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			if( resp != null)
			{
				try {
					resp.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
