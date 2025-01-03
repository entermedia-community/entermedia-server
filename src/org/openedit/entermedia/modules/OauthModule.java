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
import org.entermediadb.authenticate.BaseAutoLogin;
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
			
			String siteroot = inReq.findValue("siteRoot");

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

				String state = inReq.findValue("state");
				if (state == null)
				{
					state = "login";
				}

				//.setClientId("1028053038230-v8g3isffne0b6d3vj8ceok61h2bfk9hg.apps.googleusercontent.com")
				//.setRedirectURI("http://localhost:8080/googleauth.html")
				//	.setParameter("prompt", "login consent")  Add this for google drive to confirm 
				String requestedpermissions = null;
				
				String clientid = null;
				
				
				if( state.startsWith("hotfolder"))
				{
					String id = state.substring(9);
					  
					/*https://www.googleapis.com/auth/admin.directory.user 
					https://www.googleapis.com/auth/admin.directory.domain https://apps-apis.google.com/a/feeds/domain/ https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email openid  https://www.google.com/m8/feeds/
					*/
					requestedpermissions = "https://www.googleapis.com/auth/drive";
					Data folderinfo = archive.getData("hotfolder", id);
					clientid = folderinfo.get("accesskey");
					if( clientid == null)
					{
						throw new OpenEditException("Need to get clientid from Google Admin as accesskey");
					}
					
					if( folderinfo.get("secretkey") == null)
					{
						throw new OpenEditException("Need to get clientsecret from Google Admin as secretkey");
					}
					
				}
				else
				{
					clientid = authinfo.get("clientid");
					requestedpermissions = inReq.findValue("requestedpermissions");  //TODO: Move this to catalogsettings
	
					if (requestedpermissions == null)
					{
						//requestedpermissions = "https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email openid  https://www.googleapis.com/auth/contacts.readonly"; //Put it in the xocnf
						requestedpermissions = "https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email openid";
					}
				}
				String prompt = inReq.findValue("prompt");
				if (prompt == null)
				{
					prompt = "";
				}
				
				OAuthClientRequest request = OAuthClientRequest.authorizationProvider(OAuthProviderType.GOOGLE).setParameter("state", state).setParameter("prompt", prompt)
						.setClientId(clientid).setRedirectURI(redirect).setParameter("access_type", "offline").setResponseType("code").setScope(requestedpermissions).buildQueryMessage();

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
			
			
			if ("dropbox".equals(provider)) {
			    // Use Dropbox's OAuth 2.0 authorization endpoint
			    String requestedpermissions = authinfo.get("scopes");
			    OAuthClientRequest request = OAuthClientRequest
			            .authorizationLocation("https://www.dropbox.com/oauth2/authorize")
			            .setClientId("zmvi8dlsu09itae") // Client ID from configuration
			            .setRedirectURI(redirect)
			            .setResponseType("code") // Response type for authorization code grant
			            .setScope(requestedpermissions) // Dropbox scopes
			            .setState("login") // State parameter for CSRF protection or custom state
			            .buildQueryMessage();

			    // Redirect the user to Dropbox's authorization page
			    String locationUri = request.getLocationUri();
			    log.info("Redirecting to Dropbox OAuth: " + locationUri);
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
				
				String state = "";
				String loginokpage = inReq.findValue("loginokpage");
				if (loginokpage != null) {
					state = "{loginokpage="+loginokpage+"}";
				}
				
				
				OAuthClientRequest request = OAuthClientRequest.authorizationProvider(OAuthProviderType.FACEBOOK).setParameter("prompt", prompt).setClientId(authinfo.get("clientid")).setRedirectURI(redirect).setResponseType("code").setScope(requestedpermissions).setState(state).buildQueryMessage();

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

		
		String error = inReq.getRequestParameter("error");
		if(error != null) {
			inReq.putPageValue("oatherror", error);
			String errorurl = inReq.findActionValue("errorurl");
			if(errorurl != null) {
				inReq.redirect(errorurl);
			}
			return;//login didn't work. 
		}
		
		
		if ("google".equals(provider))
		{

			Data authinfo = archive.getData("oauthprovider", provider);

			String siteroot = inReq.findValue("siteRoot");

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
			log.info("Login Attempt redirect to: "+redirect);
			String state = inReq.getRequestParameter("state"); 
			String clientid = authinfo.get("clientid");
			String clientsecret = authinfo.get("clientsecret");
			if( state.startsWith("hotfolder"))
			{
				String id = state.substring(9);
				  
				/*https://www.googleapis.com/auth/admin.directory.user 
				https://www.googleapis.com/auth/admin.directory.domain https://apps-apis.google.com/a/feeds/domain/ https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email openid  https://www.google.com/m8/feeds/
				*/
				Data folderinfo = archive.getData("hotfolder", id);
				clientid = folderinfo.get("accesskey");
				if( clientid == null)
				{
					throw new OpenEditException("Need to get clientid from Google Admin as accesskey");
				}
				clientsecret = folderinfo.get("secretkey");
				if( clientsecret  == null)
				{
					throw new OpenEditException("Need to get clientsecret from Google Admin as secretkey");
				}
				
			}
			
			
			OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(inReq.getRequest());
			String code = oar.getCode();
			//GOOGLE

			OAuthClientRequest request = OAuthClientRequest.tokenProvider(OAuthProviderType.GOOGLE).setGrantType(GrantType.AUTHORIZATION_CODE).
					setClientId(clientid).setClientSecret(clientsecret).setRedirectURI(redirect).
					setParameter("state", "test").setCode(code).buildBodyMessage();
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
				
				inReq.putPageValue("accessToken", accessToken);
				inReq.putPageValue("refresh", refresh);
				inReq.putPageValue("oauthresponse", oAuthResponse);
				
				if( state == null || state.equals("login") )
				{
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
					inReq.putPageValue("googledata", data);
					inReq.putPageValue("useraccount", email);
					boolean autocreate = Boolean.parseBoolean(authinfo.get("autocreateusers"));
					
					
					//Create a new user from Google Login
					handleLogin(inReq, email, firstname, lastname, true, autocreate, authinfo, refresh, null);
				}
				else if( state.startsWith("hotfolder"))
				{
					String id = state.substring(9);
					Data folder = archive.getData("hotfolder", id);
					folder.setValue("refreshtoken", refresh);
					folder.setValue("httprequesttoken", null);
					archive.saveData("hotfolder", folder);
					
				}

			}
			catch (Exception e)
			{
				throw new OpenEditException(e);
			}
		}
		if ("dropbox".equals(provider)) {
		    Data authinfo = archive.getData("oauthprovider", provider);

		    // Extract the authorization code from the request
		    OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(inReq.getRequest());
		    String code = oar.getCode();

		    // Determine redirect URI
		    String siteroot = inReq.findValue("siteRoot");
		    URLUtilities utils = (URLUtilities) inReq.getPageValue(PageRequestKeys.URL_UTILITIES);
		    if (siteroot == null && utils != null) {
		        siteroot = utils.siteRoot();
		    }
		    String redirect = inReq.findValue("redirecturi");
		    if (redirect == null) {
		        redirect = siteroot + "/" + appid + authinfo.get("redirecturi");
		    } else {
		        redirect = siteroot + "/" + redirect;
		    }

		    // Build the request to exchange the authorization code for an access token
		    OAuthClientRequest request = OAuthClientRequest.tokenLocation("https://api.dropbox.com/oauth2/token")
		            .setGrantType(GrantType.AUTHORIZATION_CODE)
		            .setClientId(authinfo.get("clientid"))
		            .setClientSecret(authinfo.get("clientsecret"))
		            .setRedirectURI(redirect)
		            .setCode(code)
		            .buildBodyMessage();

		    OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
		    EmTokenResponse oAuthResponse = oAuthClient.accessToken(request, EmTokenResponse.class);

		    // Retrieve the access token from the response
		    String accessToken = oAuthResponse.getAccessToken();

		    String accountid = (String) oAuthResponse.getData().get("account_id");
		    
		

		 

		    // Save the access token if necessary for future API calls
		    authinfo.setValue("accesstoken", accessToken);
		    authinfo.setValue("accountid", accountid);

		    archive.getSearcher("oauthprovider").saveData(authinfo);
		}

		
		if ("facebook".equals(provider))
		{

			Data authinfo = archive.getData("oauthprovider", provider);

			String siteroot = inReq.findValue("siteRoot");

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

			String siteroot = inReq.findValue("siteRoot");

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
			if( domains.equals("*"))
			{
				ok = true;
			}
			else
			{
				String[] domainlist = domains.split(",");
				for (int i = 0; i < domainlist.length; i++)
				{
					String domain = domainlist[i];
					if (email.endsWith(domain))
					{
						ok = true;
					}
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
			inReq.putPageValue("isnewuser", "true");
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

			
			BaseAutoLogin autologin = (BaseAutoLogin)getModuleManager().getBean(inReq.findPathValue("catalogid"),"autoLoginWithCookie");
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
			
			String redirectpage = inReq.findActionValue("redirectpath");
			
			if(redirectpage != null) {
				inReq.redirect(redirectpage);
				return;
			}
			
			
			
			String sendTo = (String) inReq.getSessionValue("fullOriginalEntryPage");
			if( sendTo == null)
			{
				sendTo = inReq.findValue("applink");
				if( sendTo == null)
				{
					sendTo = "/" + inReq.findValue("applicationid");
				}
				sendTo = sendTo + "/index.html";
			}
			inReq.redirect(sendTo);
			
		}

	}

}
