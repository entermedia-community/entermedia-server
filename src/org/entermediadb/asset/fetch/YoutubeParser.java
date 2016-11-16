package org.entermediadb.asset.fetch;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.openedit.OpenEditException;

public class YoutubeParser
{
	private static Log log = LogFactory.getLog(YoutubeParser.class);
	
	public Map<String, String> parseUrl( String inYoutubeUrl )
	{
		
		if( inYoutubeUrl == null || !inYoutubeUrl.contains("youtube.") )
		{
			return null;
		}
		
		  HttpClient client = HttpClients.createDefault();
			
		  RequestConfig globalConfig = RequestConfig.custom()
	                .setCookieSpec(CookieSpecs.DEFAULT)
	                .build();
	        CloseableHttpClient httpClient = HttpClients.custom()
	                .setDefaultRequestConfig(globalConfig)
	                .build();

		HttpGet req = new HttpGet(inYoutubeUrl);
		Map<String, String> result = new HashMap<String, String>();
		Map<String, Pattern> patterns = new HashMap<String, Pattern>();
		
		patterns.put("video", Pattern.compile("\"fmt_url_map\"\\s*:\\s*\"[^\"]*(http[^\"]+?)*(http[^\"]+)\""));
		patterns.put("thumb", Pattern.compile("\"rv\\.\\d\\.thumbnailUrl\"\\s*:\\s*\"(.+?)\""));
		patterns.put("id", Pattern.compile("'VIDEO_ID'\\s*:\\s*'(.+?)'"));
		patterns.put("assettitle", Pattern.compile("'VIDEO_TITLE'\\s*:\\s*'(.+?)'"));
		
		try
		{
			HttpResponse res = client.execute(req);
			if( res.getStatusLine().getStatusCode() < 200 || res.getStatusLine().getStatusCode() > 299 )
			{
				throw new OpenEditException("Did not get a 200 status code.");
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(res.getEntity().getContent(),"UTF-8"));

			do
			{
				String line = in.readLine();
				if(line == null)
				{
					break;
				}
				for(String item: patterns.keySet())
				{
					Matcher m = patterns.get(item).matcher(line);
					if(m.find())
					{
						String value = m.group(m.groupCount());
						value = URLDecoder.decode(value, "UTF-8");
						if( value != null)
						{
							value = value.replace("\\\"", "\"");
						}
						result.put(item, value);
						log.debug("Got " + item + ": " + value);
					}
				}
			} while(true);
		}
		catch (Exception e)
		{
			log.info("Couldn't get YouTube video: " + e.getLocalizedMessage());
			return null;
		}
		finally
		{
			req.releaseConnection();
		}
		return result;
	}
	
	public static void main(String[] args)
	{
		YoutubeParser ytp = new YoutubeParser();
		ytp.parseUrl("http://www.youtube.com/watch?v=pEpQpyKapcQ");
	}
}
