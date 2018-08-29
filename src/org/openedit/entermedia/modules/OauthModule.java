package org.openedit.entermedia.modules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.authenticate.AutoLoginWithCookie;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.entermedia.util.EmTokenResponse;
import org.openedit.event.EventManager;
import org.openedit.event.WebEvent;
import org.openedit.page.PageRequestKeys;
import org.openedit.users.User;
import org.openedit.users.UserSearcher;
import org.openedit.util.StringEncryption;
import org.openedit.util.URLUtilities;

public class OauthModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(OauthModule.class);

	protected StringEncryption fieldCookieEncryption;
	protected EventManager fieldEventManager;

	public EventManager getEventManager()
	{
		return fieldEventManager;
	}

	public void setEventManager(EventManager inEventManager)
	{
		fieldEventManager = inEventManager;
	}

	public StringEncryption getCookieEncryption()
	{
		return fieldCookieEncryption;
	}

	public void setCookieEncryption(StringEncryption inCookieEncryption)
	{
		fieldCookieEncryption = inCookieEncryption;
	}

	public void redirectToHost(WebPageRequest inReq)
	{
		//http://yfrankfeng.blogspot.ca/2015/07/working-example-on-oauth2-spring.html

		try
		{

			String provider = inReq.findValue("provider");
			MediaArchive archive = getMediaArchive(inReq);
			String appid = inReq.findValue("applicationid");
			Data authinfo = archive.getData("oauthprovider", provider);
			String siteroot = inReq.findValue("siteroot");

			URLUtilities utils = (URLUtilities) inReq.getPageValue(PageRequestKeys.URL_UTILITIES);
			if (siteroot == null && utils != null)
			{

				siteroot = utils.siteRoot();
			}
			String redirect = inReq.findValue("redirecturi");
			if (redirect == null)
			{
				redirect = siteroot + "/" + appid + authinfo.get("redirecturi");
			}
			else
			{
				redirect = siteroot + "/" + redirect;
			}
			if ("google".equals(provider))
			{

				//.setClientId("1028053038230-v8g3isffne0b6d3vj8ceok61h2bfk9hg.apps.googleusercontent.com")
				//.setRedirectURI("http://localhost:8080/googleauth.html")
				//	.setParameter("prompt", "login consent")  Add this for google drive to confirm 
				String requestedpermissions = inReq.findValue("requestedpermissions");

				if (requestedpermissions == null)
				{
					//requestedpermissions = "https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email openid  https://www.googleapis.com/auth/contacts.readonly"; //Put it in the xocnf
					requestedpermissions = "https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email openid";
				}

				String prompt = inReq.findValue("prompt");
				if (prompt == null)
				{
					prompt = "";
				}
				
				
				OAuthClientRequest request = OAuthClientRequest.authorizationProvider(OAuthProviderType.GOOGLE).setParameter("prompt", prompt).setClientId(authinfo.get("clientid")).setRedirectURI(redirect).setParameter("access_type", "offline").setResponseType("code").setScope(requestedpermissions).buildQueryMessage();

				String locationUri = request.getLocationUri();
				inReq.redirect(locationUri);
			}

			if ("drupal".equals(provider))
			{
				//"https://devcondrupal.genieve.com/oauth2/authorize"
				//"devemgenieve"
				OAuthClientRequest request = OAuthClientRequest.authorizationLocation(authinfo.get("remoteroot") + "/oauth2/authorize").setParameter("prompt", "consent").setClientId(authinfo.get("clientid")).setRedirectURI(redirect).setResponseType("code").setScope("openid email profile").setState("login").buildQueryMessage();

				String locationUri = request.getLocationUri();
				inReq.redirect(locationUri);

			}
			
			
			
			if ("facebook".equals(provider))
			{

				//.setClientId("1028053038230-v8g3isffne0b6d3vj8ceok61h2bfk9hg.apps.googleusercontent.com")
				//.setRedirectURI("http://localhost:8080/googleauth.html")
				//	.setParameter("prompt", "login consent")  Add this for google drive to confirm 
				String requestedpermissions = null;//inReq.findValue("requestedpermissions");

				if (requestedpermissions == null)
				{
					requestedpermissions = "email public_profile";
				}

				String prompt = inReq.findValue("prompt");
				if (prompt == null)
				{
					prompt = "";
				}
				
				
				OAuthClientRequest request = OAuthClientRequest.authorizationProvider(OAuthProviderType.FACEBOOK).setParameter("prompt", prompt).setClientId(authinfo.get("clientid")).setRedirectURI(redirect).setResponseType("code").setScope(requestedpermissions).buildQueryMessage();

				String locationUri = request.getLocationUri();
				inReq.redirect(locationUri);
			}
			
			
			
			

		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
	}

	public void login(WebPageRequest inReq) throws Exception
	{

		String provider = inReq.findValue("provider");
		String appid = inReq.findValue("applicationid");
		MediaArchive archive = getMediaArchive(inReq);

		if ("google".equals(provider))
		{

			Data authinfo = archive.getData("oauthprovider", provider);

			String siteroot = inReq.findValue("siteroot");

			URLUtilities utils = (URLUtilities) inReq.getPageValue(PageRequestKeys.URL_UTILITIES);
			if (siteroot == null && utils != null)
			{

				siteroot = utils.siteRoot();
			}
			String redirect = inReq.findValue("redirecturi");
			if (redirect == null)
			{
				redirect = siteroot + "/" + appid + authinfo.get("redirecturi");
			}
			else
			{
				redirect = siteroot + "/" + redirect;
			}

			OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(inReq.getRequest());
			String code = oar.getCode();
			//GOOGLE

			OAuthClientRequest request = OAuthClientRequest.tokenProvider(OAuthProviderType.GOOGLE).setGrantType(GrantType.AUTHORIZATION_CODE).setClientId(authinfo.get("clientid")).setClientSecret(authinfo.get("clientsecret")).setRedirectURI(redirect).setCode(code).buildBodyMessage();
			//	OAuthClientRequest refreshtoken = OAuthClientRequest.tokenProvider(OAuthProviderType.GOOGLE).setGrantType(GrantType.AUTHORIZATION_CODE).setClientId(authinfo.get("clientid")).setClientSecret(authinfo.get("clientsecret")).setRedirectURI(siteroot + "/" + appid + authinfo.get("redirecturi")).setCode(code).buildBodyMessage();

			try
			{

				OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
				//Facebook is not fully compatible with OAuth 2.0 draft 10, access token response is
				//application/x-www-form-urlencded, not json encoded so we use dedicated response class for that
				//Own response class is an easy way to deal with oauth providers that introduce modifications to
				//OAuth specification
				EmTokenResponse oAuthResponse = oAuthClient.accessToken(request, EmTokenResponse.class);
				// final OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, "POST");
				// final OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request);
				String accessToken = oAuthResponse.getAccessToken();
				String refresh = oAuthResponse.getRefreshToken();
				boolean systemwide = Boolean.parseBoolean(inReq.findValue("systemwide"));

				if (refresh != null && systemwide)
				{
					authinfo.setValue("refreshtoken", refresh);
					authinfo.setValue("httprequesttoken", null);
					archive.getSearcher("oauthprovider").saveData(authinfo);

				}

				OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest("https://www.googleapis.com/oauth2/v1/userinfo").setAccessToken(accessToken).buildQueryMessage();

				OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, "GET", OAuthResourceResponse.class);
				String userinfoJSON = resourceResponse.getBody();
				JSONParser parser = new JSONParser();

				JSONObject data = (JSONObject) parser.parse(userinfoJSON);
				String email = (String) data.get("email");
				String firstname = (String) data.get("given_name");
				String lastname = (String) data.get("family_name");
				boolean autocreate = Boolean.parseBoolean(authinfo.get("autocreateusers"));
				handleLogin(inReq, email, firstname, lastname, true, autocreate, authinfo, refresh, null);

			}
			catch (Exception e)
			{
				throw new OpenEditException(e);
			}
		}
		
		
		if ("facebook".equals(provider))
		{

			Data authinfo = archive.getData("oauthprovider", provider);

			String siteroot = inReq.findValue("siteroot");

			URLUtilities utils = (URLUtilities) inReq.getPageValue(PageRequestKeys.URL_UTILITIES);
			if (siteroot == null && utils != null)
			{

				siteroot = utils.siteRoot();
			}
			String redirect = inReq.findValue("redirecturi");
			if (redirect == null)
			{
				redirect = siteroot + "/" + appid + authinfo.get("redirecturi");
			}
			else
			{
				redirect = siteroot + "/" + redirect;
			}

			OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(inReq.getRequest());
			String code = oar.getCode();
			//GOOGLE

			OAuthClientRequest request = OAuthClientRequest.tokenProvider(OAuthProviderType.FACEBOOK).setGrantType(GrantType.AUTHORIZATION_CODE).setClientId(authinfo.get("clientid")).setClientSecret(authinfo.get("clientsecret")).setRedirectURI(redirect).setCode(code).buildBodyMessage();
			//	OAuthClientRequest refreshtoken = OAuthClientRequest.tokenProvider(OAuthProviderType.GOOGLE).setGrantType(GrantType.AUTHORIZATION_CODE).setClientId(authinfo.get("clientid")).setClientSecret(authinfo.get("clientsecret")).setRedirectURI(siteroot + "/" + appid + authinfo.get("redirecturi")).setCode(code).buildBodyMessage();

			try
			{

				OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
				//Facebook is not fully compatible with OAuth 2.0 draft 10, access token response is
				//application/x-www-form-urlencded, not json encoded so we use dedicated response class for that
				//Own response class is an easy way to deal with oauth providers that introduce modifications to
				//OAuth specification
				EmTokenResponse oAuthResponse = oAuthClient.accessToken(request, EmTokenResponse.class);
				// final OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, "POST");
				// final OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request);
				String accessToken = oAuthResponse.getAccessToken();
				String refresh = oAuthResponse.getRefreshToken();
				boolean systemwide = Boolean.parseBoolean(inReq.findValue("systemwide"));

				if (refresh != null && systemwide)
				{
					authinfo.setValue("refreshtoken", refresh);
					authinfo.setValue("httprequesttoken", null);
					archive.getSearcher("oauthprovider").saveData(authinfo);

				}

				OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest("https://graph.facebook.com/me?fields=name,email").setAccessToken(accessToken).buildQueryMessage();

				OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, "GET", OAuthResourceResponse.class);
				String userinfoJSON = resourceResponse.getBody();
				JSONParser parser = new JSONParser();

				JSONObject data = (JSONObject) parser.parse(userinfoJSON);
				String facebookid = (String) data.get("id");
				String screenname = (String) data.get("name");
				String email = (String) data.get("email");
				String firstname = (String) data.get("given_name");
				String lastname = (String) data.get("family_name");
				boolean autocreate = Boolean.parseBoolean(authinfo.get("autocreateusers"));
				handleLogin(inReq, email, firstname, lastname, true, autocreate, authinfo, refresh, "fb" + facebookid);

			}
			catch (Exception e)
			{
				throw new OpenEditException(e);
			}
		}

		
		
		
		

		if ("drupal".equals(provider))
		{
			Data authinfo = archive.getData("oauthprovider", provider);

			OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(inReq.getRequest());
			String code = oar.getCode();

			String siteroot = inReq.findValue("siteroot");

			URLUtilities utils = (URLUtilities) inReq.getPageValue(PageRequestKeys.URL_UTILITIES);
			if (siteroot == null && utils != null)
			{

				siteroot = utils.siteRoot();
			}
			String redirect = inReq.findValue("redirecturi");
			if (redirect == null)
			{
				redirect = siteroot + "/" + appid + authinfo.get("redirecturi");
			}
			else
			{
				redirect = siteroot + "/" + redirect;
			}

			OAuthClientRequest request = OAuthClientRequest.tokenLocation(authinfo.get("remoteroot") + "/oauth2/token").setGrantType(GrantType.AUTHORIZATION_CODE).setClientId(authinfo.get("clientid")).setClientSecret(authinfo.get("clientsecret")).setRedirectURI(redirect).setCode(code).buildBodyMessage();
			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
			EmTokenResponse oAuthResponse = oAuthClient.accessToken(request, EmTokenResponse.class);
			// final OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, "POST");
			// final OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request);

			String accessToken = oAuthResponse.getAccessToken();

			OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(authinfo.get("remoteroot") + "/oauth2/UserInfo").setAccessToken(accessToken).buildQueryMessage();

			OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, "GET", OAuthResourceResponse.class);
			String userinfoJSON = resourceResponse.getBody();
			JSONParser parser = new JSONParser();

			JSONObject data = (JSONObject) parser.parse(userinfoJSON);
			handleLogin(inReq, (String) data.get("email"), (String) data.get("name"), (String) data.get("lastname"), false, true, authinfo, null, null);

		}
		//	inReq.redirect("/" + appid + "/index.html");
	}

	protected void handleLogin(WebPageRequest inReq, String email, String firstname, String lastname, boolean matchOnEmail, boolean autocreate, Data authinfo, String refreshtoken, String userid)
	{

		if (authinfo.getValue("alloweddomains") != null)
		{

			boolean ok = false;
			String domains = authinfo.get("alloweddomains");
			String[] domainlist = domains.split(",");
			for (int i = 0; i < domainlist.length; i++)
			{
				String domain = domainlist[i];
				if (email.endsWith(domain))
				{
					ok = true;
				}
			}
			if (!ok)
			{
				String appid = inReq.findValue("applicationid");
				inReq.redirect("/" + appid + "/authentication/nopermissions.html");
				return;
			}
		}

		MediaArchive archive = getMediaArchive(inReq);
		UserSearcher searcher = (UserSearcher) archive.getSearcher("user");
		User target = null;
		
		if( email != null) {
			target = searcher.getUserByEmail(email);
			
		}
		
		
		if(target == null && userid != null) {
			target = searcher.getUser(userid);
		}
		
		
		
		
		if (autocreate && target == null)
		{
			target = (User) searcher.createNewData();
			target.setFirstName(firstname);
			target.setLastName(lastname);
			target.setEmail(email);
			target.setEnabled(true);
			target.setId(userid);
			searcher.saveData(target, null);
		}
		
		
		
		if (target != null)
		{

			inReq.putSessionValue(searcher.getCatalogId() + "user", target);
			String md5 = getCookieEncryption().getPasswordMd5(target.getPassword());
			String value = target.getUserName() + "md542" + md5;
			inReq.putPageValue("entermediakey", value);
			inReq.putPageValue("user", target);
			if (refreshtoken != null)
			{
				target.setProperty("refreshtoken", refreshtoken);

				target.setProperty("clientid", authinfo.get("clientid"));
			}
			target.setProperty("httprefreshtoken", null);
			archive.getSearcher("user").saveData(target);

			
			AutoLoginWithCookie autologin = (AutoLoginWithCookie)getModuleManager().getBean(inReq.findValue("catalogid"),"autoLoginWithCookie");
			autologin.saveCookieForUser(inReq, target);

			
			if (getEventManager() != null)
			{
				WebEvent event = new WebEvent();
				event.setSearchType("userprofile");
				event.setCatalogId(searcher.getCatalogId());
				event.setOperation("saved");
				event.setProperty("dataid", target.getId());
				event.setProperty("id", target.getId());

				event.setProperty("applicationid", inReq.findValue("applicationid"));

				getEventManager().fireEvent(event);
			}

		}

	}

}
