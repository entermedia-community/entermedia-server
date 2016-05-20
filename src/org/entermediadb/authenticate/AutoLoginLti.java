package org.entermediadb.authenticate;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.openedit.WebPageRequest;
import org.openedit.util.StringEncryption;

public class AutoLoginLti implements AutoLoginProvider
{
		protected StringEncryption fieldStringEncryption;

		public StringEncryption getStringEncryption()
		{
			return fieldStringEncryption;
		}

		public void setStringEncryption(StringEncryption inStringEncryption)
		{
			fieldStringEncryption = inStringEncryption;
		}

		public String encode(String inHtml)
		{
			//String encoded = MimeUtility.encodeText(inHtml,"UTF-8","Q");
			//String encoded = java.util.Base64.getUrlEncoder().encode(inHtml.getBytes());
			String encoded = java.net.URLEncoder.encode(inHtml);
			//We needed to encode everything in the values
			encoded = encoded.replace("+", " ").replace("~", "%7e").replace("=", "%3D").replace(" ", "%20");
			return encoded;
		}

		public boolean compareRequest(String inPrivateKey, String inUrl, String sha1expected, Map inParameters)
		{
			inParameters.remove("oauth_signature");
			Map sorted = new TreeMap(inParameters);
			//TODO: Sort array values
			StringBuffer base = new StringBuffer();
			base.append("POST&");
			base.append(encode(inUrl));
			base.append("&");
			for (Iterator iterator = sorted.keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				String value = (String) sorted.get(key);
				base.append(encode(key));
				base.append("%3D");
				base.append(encode(encode(value)));
				//base.append(value);
				if (iterator.hasNext())
				{
					base.append("%26");
				}
			}
			String sha1 = getStringEncryption().calculateRFC2104HMAC(inPrivateKey + "&", base.toString());
			//log.info("created " + base);
			//log.info("sha " + sha1);

			boolean ok = sha1.equals(sha1expected);
			return ok;
		}



	@Override
	public boolean autoLogin(WebPageRequest inReq)
	{
	    Map map = inReq.getParameterMap();	
	    String url = inReq.getSiteUrl();//"https://weatherfordcollege.entermediadb.net/lti/index.html";
	    String expected = (String)map.get("oauth_signature");

	    String inPrivateKey = getStringEncryption().getEncryptionKey();
	    
	    compareRequest(inPrivateKey, url, expected, map);
	    
		return false;
	}

}
