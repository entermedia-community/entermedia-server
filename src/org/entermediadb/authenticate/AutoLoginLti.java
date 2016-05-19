package org.entermediadb.authenticate;

import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;

public class AutoLoginLti implements AutoLoginProvider
{
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
			String sha1 = calculateRFC2104HMAC(inPrivateKey + "&", base.toString());
			//log.info("created " + base);
			//log.info("sha " + sha1);

			boolean ok = sha1.equals(sha1expected);
			return ok;
		}

		/**
		 * Computes RFC 2104-compliant HMAC signature. * @param data The data to be
		 * signed.
		 * 
		 * @param key
		 *            The signing key.
		 * @return The Base64-encoded RFC 2104-compliant HMAC signature.
		 * @throws java.security.SignatureException
		 *             when signature generation fails
		 */
		public String calculateRFC2104HMAC(String privatekey, String data)
		{
			String HMAC_SHA1_ALGORITHM = "HmacSHA1";

			byte[] result;
			try
			{
				// get an hmac_sha1 key from the raw key bytes
				SecretKeySpec signingKey = new SecretKeySpec(privatekey.getBytes(), HMAC_SHA1_ALGORITHM);

				// get an hmac_sha1 Mac instance and initialize with the signing key
				Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
				mac.init(signingKey);

				// compute the hmac on input data bytes
				byte[] rawHmac = mac.doFinal(data.getBytes());

				// base64-encode the hmac
				org.apache.commons.codec.binary.Base64 base64encoder = new org.apache.commons.codec.binary.Base64();
				
				result = base64encoder.encode(rawHmac);
				return new String(result, "UTF8");
			}
			catch (Exception e)
			{
				throw new OpenEditException("Failed to generate HMAC : " + e.getMessage(), e);
			}
		}
	}

	@Override
	public boolean autoLogin(WebPageRequest inReq)
	{
	    Map map = inReq.getParameterMap();	
	    String url = "https://weatherfordcollege.entermediadb.net/lti/index.html";
	    String expected = (String)map.get("oauth_signature");
		// TODO Auto-generated method stub
		return false;
	}

}
