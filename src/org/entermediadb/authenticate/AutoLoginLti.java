package org.entermediadb.authenticate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.entermedia.util.RFC3986;
import org.openedit.users.User;
import org.openedit.util.StringEncryption;

public class AutoLoginLti extends BaseAutoLogin implements AutoLoginProvider
{
	//http://www.imsglobal.org/wiki/step-2-valid-lti-launch-request
		private static final Log log = LogFactory.getLog(AutoLoginLti.class);
		protected StringEncryption fieldStringEncryption;
		RFC3986 percent = new RFC3986();
		boolean fieldAllowUnsecureLogin;
		
		public boolean isAllowUnsecureLogin()
		{
			return fieldAllowUnsecureLogin;
		}

		public void setAllowUnsecureLogin(boolean inAllowUnsecureLogin)
		{
			fieldAllowUnsecureLogin = inAllowUnsecureLogin;
		}

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
			
			return percent.encode(inHtml);
			/*
			//urlencode_rfc3986
			//String encoded = MimeUtility.encodeText(inHtml,"UTF-8","Q");
			//String encoded = java.util.Base64.getUrlEncoder().encode(inHtml.getBytes());
			String encoded = null;
			try
			{
				encoded = java.net.URLEncoder.encode(inHtml,"utf-8");
			}
			catch (UnsupportedEncodingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			                // OAuth encodes some characters differently:
			//encoded = encoded.replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
			
			encoded = encoded.replace("+", "%20").replace("*", "%2A").replace("=", "%3D").replace("~", "%7e").replace(" ", "%20");
			
			//We needed to encode everything in the values
			return encoded;
			*/
		}

		public String createRequest(String inPrivateKey, String inUrl, String sha1expected, Map inParameters)
		{
			//https://lti.tools/test/tc.php
			inParameters.remove("oauth_signature");
			
			List sorted = new ArrayList(inParameters.keySet());
			Collections.sort( sorted );
			//TODO: Sort array values
			StringBuffer base = new StringBuffer();
			for (Iterator iterator = sorted.iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				String value = (String) inParameters.get(key);
//				if( value == null || value.isEmpty())
//				{
//					continue;
//				}
				base.append(encode(key));
				base.append("=");
				//base.append("%3D");
				//Why do this twice?
				base.append(encode(value));
				//if( log.isDebugEnabled() )
				{
					log.info(key +  ":" + value);
				}
				
				if (iterator.hasNext())
				{
					base.append("&");
				}
			}
			//Why is there an & on the private key?
			//HMAC-SHA1
			String encoded = encode(base.toString());
			//String encoded = base.toString();
			String basestring = "POST&" + encode(inUrl) + "&" + encoded;  //Double encoded $parts = OAuthUtil::urlencode_rfc3986($parts);

			
			String sha1 = getStringEncryption().calculateRFC2104HMAC(inPrivateKey, basestring);
			//if(log.isDebugEnabled())
			{
				log.info("created " + basestring);
				log.info("sha " + sha1);
			}	

			return sha1;
		}


	@Override
	public AutoLoginResult autoLogin(WebPageRequest inReq)
	{
		//https://oauth.net/core/1.0/#nonce
		//log.info("ecoding:" + inReq.getRequest().getCharacterEncoding() );
	    Map map = inReq.getParameterMap();	
	    String expected = (String)map.get("oauth_signature");
	    String url = null;
	    if( expected != null)
	    {
		    url = (String)inReq.getPageValue("originalurl");
		    if( url == null)
		    {
		    	url = inReq.getSiteUrl();//"https://weatherfordcollege.entermediadb.net/lti/index.html";
		    }
		    String inPrivateKey = getStringEncryption().getEncryptionKey("ltiautologinkey");  //TODO: Cache this?
		    if( inPrivateKey == null)
		    {
		    	throw new OpenEditException("ltiautologinkey is not defined in WEB-INF/data/system/lists/systemsettings/custom.xml");
		    }
		    	
		    String sha1 = createRequest(inPrivateKey, url, expected, map);
		    if( expected.equals(sha1) || unsecureLogin(map) )
		    {
				AutoLoginResult result = new AutoLoginResult();
				String username = (String)map.get("ext_user_username");
				
				String email = (String)map.get("lis_person_contact_email_primary");
				if( username == null && email != null && email.contains("@"))
				{
					username = email;
					username = email.substring(0,email.indexOf("@"));
				}
				User user = getUserManager(inReq).getUser(username);
				if( user == null)
				{
					user = getUserManager(inReq).createUser(username, null);
				}
				if( email != null)
				{
					user.setEmail(email);
				}
				String given = (String)map.get("lis_person_name_given");
				if( given != null)
				{
					user.setFirstName(given);
					user.setLastName( (String)map.get("lis_person_name_family") );
				}	
				result.setUser(user);
				return result;
		    }
		    else	
		    {
		    	log.info("Trying to login and failing expected: " + expected + " we got " + sha1 + " using private key of " + inPrivateKey + " on url " + url);
		    }
	    }    
		return null;
	}

	protected boolean unsecureLogin(Map inMap)
	{
		if( !isAllowUnsecureLogin() )
		{
			return false;
		}
		String time = (String)inMap.get("oauth_timestamp");
		if( time != null)
		{
			long timepassed = System.currentTimeMillis() - (Long.parseLong(time) * 1000L); 
			if( timepassed  > 1000*60*5)
			{
				return false;
			}
		}
		String username = (String)inMap.get("ext_user_username");
		if( username == null)
		{
			String email = (String)inMap.get("lis_person_contact_email_primary");
			if( email == null)
			{
				log.error("No username found");
				return false;
			}
		}
		return true;
	}
}
