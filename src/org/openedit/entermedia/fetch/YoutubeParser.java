package org.openedit.entermedia.fetch;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openedit.OpenEditException;

public class YoutubeParser
{
	private static Log log = LogFactory.getLog(YoutubeParser.class);
	
	public Map<String, String> parseUrl( String inYoutubeUrl )
	{
		
		if( inYoutubeUrl == null || !inYoutubeUrl.contains("youtube.") )
		{
			return null;
		}
		
		HttpClient client = new org.apache.commons.httpclient.HttpClient();
		GetMethod req = new GetMethod(inYoutubeUrl);
		Map<String, String> result = new HashMap<String, String>();
		Map<String, Pattern> patterns = new HashMap<String, Pattern>();
		
		patterns.put("video", Pattern.compile("\"fmt_url_map\"\\s*:\\s*\"[^\"]*(http[^\"]+?)*(http[^\"]+)\""));
		patterns.put("thumb", Pattern.compile("\"rv\\.\\d\\.thumbnailUrl\"\\s*:\\s*\"(.+?)\""));
		patterns.put("id", Pattern.compile("'VIDEO_ID'\\s*:\\s*'(.+?)'"));
		patterns.put("assettitle", Pattern.compile("'VIDEO_TITLE'\\s*:\\s*'(.+?)'"));
		
		try
		{
			client.executeMethod(req);
			if( req.getStatusCode() < 200 || req.getStatusCode() > 299 )
			{
				throw new OpenEditException("Did not get a 200 status code.");
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(req.getResponseBodyAsStream()));

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
		return result;
	}
	
	public static void main(String[] args)
	{
		YoutubeParser ytp = new YoutubeParser();
		ytp.parseUrl("http://www.youtube.com/watch?v=pEpQpyKapcQ");
	}
}
