package org.enteremdiadb.postiz;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
<<<<<<< HEAD
import org.json.simple.parser.JSONParser;
=======
>>>>>>> refs/remotes/origin/main
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.repository.ContentItem;
import org.openedit.util.JSONParser;

public class PostizManager implements CatalogEnabled
{

	private static Log log = LogFactory.getLog(PostizManager.class);

	public static final String POST_TYPE_NOW = "now";
	public static final String POST_TYPE_DRAFT = "draft";
	public static final String POST_TYPE_SCHEDULE = "schedule";

	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected CloseableHttpClient fieldHttpClient;

	protected ModuleManager fieldModuleManager;

	public CloseableHttpClient getSharedClient()
	{
		if (fieldHttpClient == null || true)
		{
			try
			{
				fieldHttpClient = HttpClients.createDefault();
			}
			catch (Throwable e)
			{
				throw new OpenEditException(e);
			}
		}
		return fieldHttpClient;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	@SuppressWarnings("unchecked")
	public JSONObject createPost(String apiKey, String inPostContent, Date inPostDate, String postType, List<String> inAssetIds, List<String> integrationIds)
	{
		assert apiKey != null : "API Key is required";

		String endpoint = getPublicEndpoint() + "/posts";
		HttpPost postMethod = new HttpPost(endpoint);
		postMethod.addHeader("Authorization", apiKey);
		postMethod.setHeader("Content-Type", "application/json");

		try
		{
			// Build payload
			JSONObject payload = new JSONObject();
			payload.put("type", postType);

			SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
			String formattedDate = isoFormat.format(inPostDate != null ? inPostDate : new Date());
			payload.put("date", formattedDate);

			JSONArray postsArray = new JSONArray();

			if (integrationIds != null && !integrationIds.isEmpty())
			{
				for (String rawId : integrationIds)
				{
					String integrationId = rawId;
					String channelId = null;

					// 🔹 Split id if it has ":"
					if (rawId.contains(":"))
					{
						String[] parts = rawId.split(":", 2);
						integrationId = parts[0];
						channelId = parts[1];
					}

					JSONObject postObject = new JSONObject();

					// Integration
					JSONObject integration = new JSONObject();
					integration.put("id", integrationId);
					postObject.put("integration", integration);

					// Values
					JSONArray valuesArray = new JSONArray();
					JSONObject valueObject = new JSONObject();
					valueObject.put("content", inPostContent);

					// Images
					JSONArray imagesArray = new JSONArray();
					if (inAssetIds != null && !inAssetIds.isEmpty())
					{
						for (String assetid : inAssetIds)
						{
							Asset asset = getMediaArchive().getAsset(assetid);
							if (asset != null)
							{
								ContentItem item = getMediaArchive().getGeneratedContent(asset, "image3000x3000.jpg");
								if (item.exists())
								{
									String fileId = uploadFile(item.getAbsolutePath());
									if (fileId != null)
									{
										JSONObject imageObject = new JSONObject();
										imageObject.put("id", fileId);
										imagesArray.add(imageObject);
									}
								}
							}
						}
					}
					valueObject.put("image", imagesArray);
					valuesArray.add(valueObject);
					postObject.put("value", valuesArray);

					// Group
					postObject.put("group", java.util.UUID.randomUUID().toString());

					// Settings
					JSONObject settings = new JSONObject();
					if (channelId != null && !channelId.isEmpty())
					{
						settings.put("channel", channelId);
					}
					postObject.put("settings", settings);

					postsArray.add(postObject);
				}
			}
			else
			{
				throw new OpenEditException("At least one integration ID is required.");
			}

			payload.put("posts", postsArray);
			payload.put("shortLink", false);
			payload.put("tags", new JSONArray());

			postMethod.setEntity(new StringEntity(payload.toJSONString(), "UTF-8"));

			CloseableHttpResponse response = getSharedClient().execute(postMethod);
			String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
			log.info(jsonResponse);
			response.close();

			JSONArray result = (JSONArray) new JSONParser().parse(jsonResponse);
			return (JSONObject) result.get(0);

		}
		catch (Exception e)
		{
			log.error("Error creating post in Postiz", e);
			throw new OpenEditException("Failed to create post", e);
		}
	}

	public JSONArray listIntegrations(String apiKey)
	{
		//String apiKey = getApiKey();
		assert apiKey != null : "API Key is required";

		String endpoint = getPublicEndpoint() + "/integrations";
		HttpGet getMethod = new HttpGet(endpoint);
		getMethod.addHeader("Authorization", apiKey);

		try
		{
			//This is per user!
			JSONArray integrations = (JSONArray) getMediaArchive().getCacheManager().get("postiz", "integrations" + apiKey);
			if (integrations == null)
			{
				CloseableHttpResponse response = getSharedClient().execute(getMethod);

				String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
				integrations = (JSONArray) new org.json.simple.parser.JSONParser().parse(jsonResponse);

				response.close();
				getMediaArchive().getCacheManager().put("postiz", "integrations" + apiKey, integrations);
			}

			// Parse the response as JSON

			return integrations;

		}
		catch (Exception e)
		{
			log.error("Error listing integrations in Postiz", e);
			throw new OpenEditException("Failed to list integrations", e);
		}
	}

<<<<<<< HEAD
	@SuppressWarnings("unchecked")
	public String loginAndGetCookie(String inUsername, String inPassword)
	{
		String baseUrl = getApiEndpoint();
=======
        } catch (Exception e) {
            log.error("Error listing integrations in Postiz", e);
            throw new OpenEditException("Failed to list integrations", e);
        }
    }
    
    
    public JSONArray listsubReddits(WebPageRequest inReq) {
        //String apiKey = getApiKey();
        
 
        try {
            
            JSONArray subreddits = (JSONArray) getMediaArchive().getCacheManager().get("postiz", "subreddits");
            if(subreddits == null) {
                String redditprofile = inReq.getRequestParameter("profile");
                String term = inReq.getRequestParameter("term");
                String endpoint = "https://oauth.reddit.com/subreddits/search?show=public&q="+term+"&sort=activity&show_users=false&limit=10";
                HttpGet getMethod = new HttpGet(endpoint);
                getMethod.addHeader("User-Agent", "Java:MySubredditFetcherApp:v1.0.0 (by /u/"+redditprofile+")");

                CloseableHttpResponse response = getSharedClient().execute(getMethod);

                String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
                subreddits = (JSONArray) new org.openedit.util.JSONParser().parseCollection(jsonResponse);
                response.close();         
                getMediaArchive().getCacheManager().put("postiz", "subreddits", subreddits);
            }
            
            // Parse the response as JSON
            
            return subreddits;

        } catch (Exception e) {
            log.error("Error listing integrations in Postiz", e);
            throw new OpenEditException("Failed to list integrations", e);
        }
    }
>>>>>>> refs/remotes/origin/main

		String cacheKey = "auth_cookie_" + inUsername;
		String cached = (String) getMediaArchive().getCacheManager().get("postiz", cacheKey);
		if (cached != null)
		{
			return cached;
		}

		try
		{
			String loginUrl = baseUrl + "/auth/login";
			HttpPost loginPost = new HttpPost(loginUrl);
			loginPost.addHeader("Content-Type", "application/json");

			JSONObject loginBody = new JSONObject();
			loginBody.put("providerToken", "");
			loginBody.put("provider", "LOCAL");
			loginBody.put("email", inUsername);
			loginBody.put("password", inPassword);

			loginPost.setEntity(new StringEntity(loginBody.toJSONString(), "UTF-8"));

			CloseableHttpResponse loginResponse = getSharedClient().execute(loginPost);
			Header[] setCookies = loginResponse.getHeaders("Set-Cookie");
			loginResponse.close();

			String authCookie = null;
			for (Header h : setCookies)
			{
				if (h.getValue().startsWith("auth="))
				{
					authCookie = h.getValue().split(";", 2)[0]; // just "auth=..."
					break;
				}
			}
			if (authCookie == null)
			{
				throw new OpenEditException("Login failed: no auth cookie returned");
			}

			getMediaArchive().getCacheManager().put("postiz", cacheKey, authCookie);
			return authCookie;

		}
		catch (Exception e)
		{
			log.error("Error logging in to Postiz", e);
			throw new OpenEditException("Failed to login", e);
		}
	}

	@SuppressWarnings("unchecked")
	public JSONArray listIntegrationChannels(String username, String password, String integrationId)
	{
		assert integrationId != null : "Integration ID is required";

		String cacheKey = "channels_secure_" + integrationId;
		Object cached =  getMediaArchive().getCacheManager().get("postiz", cacheKey);
		if (cached != null)
		{
			if (cached instanceof JSONArray)
			{
				return (JSONArray) cached;
			}
			return null;

		}

		String baseUrl = getApiEndpoint();
		String authCookie = loginAndGetCookie(username, password);

		try
		{
			String endpoint = baseUrl + "/integrations/function";
			HttpPost postMethod = new HttpPost(endpoint);
			postMethod.addHeader("Content-Type", "application/json");
			postMethod.addHeader("Cookie", authCookie);

			JSONObject body = new JSONObject();
			body.put("name", "channels");
			body.put("id", integrationId);
			postMethod.setEntity(new StringEntity(body.toJSONString(), "UTF-8"));

			CloseableHttpResponse response = getSharedClient().execute(postMethod);

			if (response.getStatusLine().getStatusCode() == 401)
			{
				response.close();
				getMediaArchive().getCacheManager().remove("postiz", "auth_cookie_" + username);
				return listIntegrationChannels(username, password, integrationId);
			}

			String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
			response.close();

			Object parsed = new org.json.simple.parser.JSONParser().parse(jsonResponse);

			if (parsed instanceof JSONArray)
			{
				getMediaArchive().getCacheManager().put("postiz", cacheKey, parsed);
				return (JSONArray) parsed;

			}
			else
			{
				getMediaArchive().getCacheManager().put("postiz", cacheKey, Boolean.FALSE);

				return null;
			}

		}
		catch (Exception e)
		{
			log.error("Error listing channels (secure) for integration " + integrationId, e);
			return null;
		}
	}

	private String uploadFile(String filePath)
	{
		String apiKey = getApiKey();
		assert apiKey != null : "API Key is required";

		String endpoint = getApiEndpoint() + "/upload";
		HttpPost postMethod = new HttpPost(endpoint);
		postMethod.addHeader("Authorization", apiKey);

		try
		{
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.addBinaryBody("file", new java.io.File(filePath), ContentType.DEFAULT_BINARY, new java.io.File(filePath).getName());

			postMethod.setEntity(builder.build());

			CloseableHttpResponse response = getSharedClient().execute(postMethod);
			String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");

			// Parse the response as JSON
			JSONObject result = (JSONObject) new org.json.simple.parser.JSONParser().parse(jsonResponse);

			response.close();
			return (String) result.get("id");

		}
		catch (Exception e)
		{
			log.error("Error uploading file to Postiz", e);
			throw new OpenEditException("Failed to upload file", e);
		}
	}

	public String getApiEndpoint()
	{
		return getMediaArchive().getCatalogSettingValue("postiz-url") + "/api";
	}

	public String getPublicEndpoint()
	{
		return getMediaArchive().getCatalogSettingValue("postiz-url") + "/api/public/v1";
	}

	public String getApiKey()
	{

		//get it from socialmediaprofile userprofile.
		String apikey = "";

		return getMediaArchive().getCatalogSettingValue("postiz-key");
	}

}
