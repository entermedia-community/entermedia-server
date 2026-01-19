package org.enteremdiadb.postiz;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.repository.ContentItem;
import org.openedit.util.JSONParser;

public class PostizManager implements CatalogEnabled {

    private static Log log = LogFactory.getLog(PostizManager.class);

    public static final String POST_TYPE_NOW = "now";
    public static final String POST_TYPE_DRAFT = "draft";
    public static final String POST_TYPE_SCHEDULE = "schedule";

    protected String fieldCatalogId;
    protected MediaArchive fieldMediaArchive;
	protected CloseableHttpClient fieldHttpClient;

    protected ModuleManager fieldModuleManager;

    
    
    public CloseableHttpClient getSharedClient() {
        if (fieldHttpClient == null || true) {
            try {
                fieldHttpClient = HttpClients.createDefault();
            } catch (Throwable e) {
                throw new OpenEditException(e);
            }
        }
        return fieldHttpClient;
    }


    public ModuleManager getModuleManager() {
        return fieldModuleManager;
    }

    public void setModuleManager(ModuleManager inModuleManager) {
        fieldModuleManager = inModuleManager;
    }

    public MediaArchive getMediaArchive() {
        if (fieldMediaArchive == null) {
            fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
        }
        return fieldMediaArchive;
    }

    public String getCatalogId() {
        return fieldCatalogId;
    }

    public void setCatalogId(String inCatalogId) {
        fieldCatalogId = inCatalogId;
    }

    public JSONObject createPost(String apiKey, String inPostContent, Date inPostDate, String postType, List<String> inAssetIds, List<String> integrationIds) {
        //String apiKey = getApiKey();
        assert apiKey != null : "API Key is required";

        String endpoint = getApiEndpoint() + "/posts";
        HttpPost postMethod = new HttpPost(endpoint);
        postMethod.addHeader("Authorization", apiKey); // per your requirements
        postMethod.setHeader("Content-Type", "application/json");

        try {
            // Construct the main payload
            JSONObject payload = new JSONObject();
            payload.put("type", postType);

            // Set ISO 8601 date
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            String formattedDate = isoFormat.format(inPostDate != null ? inPostDate : new Date());
            payload.put("date", formattedDate);

            // Construct posts array
            JSONArray postsArray = new JSONArray();

            if (integrationIds != null && !integrationIds.isEmpty()) {
                for (String integrationId : integrationIds) {
                    JSONObject postObject = new JSONObject();

                    // Add integration
                    JSONObject integration = new JSONObject();
                    integration.put("id", integrationId);
                    postObject.put("integration", integration);

                    // Add values array
                    JSONArray valuesArray = new JSONArray();
                    JSONObject valueObject = new JSONObject();
                    valueObject.put("content", inPostContent);

                    // Handle file uploads if provided
                 // Add images if any
                    JSONArray imagesArray = new JSONArray();
                    if (inAssetIds != null && !inAssetIds.isEmpty()) {
                        for (String assetid : inAssetIds) {
                            Asset asset = getMediaArchive().getAsset(assetid);
                            if (asset != null) {
                                ContentItem item = getMediaArchive().getGeneratedContent(asset, "image3000x3000.jpg");
                                if (item.exists()) {
                                    String fileId = uploadFile(item.getAbsolutePath());
                                    if (fileId != null) {
                                        JSONObject imageObject = new JSONObject();
                                        imageObject.put("id", fileId);
                                        imagesArray.add(imageObject);
                                    }
                                }
                            }
                        }
                    }
                        valueObject.put("image", imagesArray);
                    

                    // Now ALWAYS add value
                    valuesArray.add(valueObject);
                    postObject.put("value", valuesArray);
                    

                    // Add a random group ID to group these posts if needed
                    postObject.put("group", java.util.UUID.randomUUID().toString());

                    // Add default settings as an empty object
                    JSONObject settings = new JSONObject();
                    postObject.put("settings", settings);

                    postsArray.add(postObject);
                }
            } else {
                throw new OpenEditException("At least one integration ID is required.");
            }

            payload.put("posts", postsArray);

            // **Hardcode the required fields**
            payload.put("shortLink", false);
            payload.put("tags", new JSONArray());

            // Set the payload as the request body
            postMethod.setEntity(new StringEntity(payload.toJSONString(), "UTF-8"));

            // Execute the request
            CloseableHttpResponse response = getSharedClient().execute(postMethod);
            String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");

            // Parse the response as JSON
            JSONArray result = (JSONArray) new JSONParser().parseCollection(jsonResponse);

            response.close();
            return (JSONObject) result.get(0);

        } catch (Exception e) {
            log.error("Error creating post in Postiz", e);
            throw new OpenEditException("Failed to create post", e);
        }
    }



    public JSONArray listIntegrations(String apiKey) {
        //String apiKey = getApiKey();
        assert apiKey != null : "API Key is required";

        String endpoint = getApiEndpoint() + "/integrations";
        HttpGet getMethod = new HttpGet(endpoint);
        getMethod.addHeader("Authorization", apiKey);

        try {
            
            JSONArray integrations = (JSONArray) getMediaArchive().getCacheManager().get("postiz", "integrations");
            if(integrations == null) {
                CloseableHttpResponse response = getSharedClient().execute(getMethod);

                String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
            	integrations = (JSONArray) new org.openedit.util.JSONParser().parseCollection(jsonResponse);
                response.close();         
                getMediaArchive().getCacheManager().put("postiz", "integrations", integrations);
            }
            
            // Parse the response as JSON
            
            return integrations;

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

    private String uploadFile(String filePath) {
        String apiKey = getApiKey();
        assert apiKey != null : "API Key is required";

        String endpoint = getApiEndpoint() + "/upload";
        HttpPost postMethod = new HttpPost(endpoint);
        postMethod.addHeader("Authorization",  apiKey);

        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file",
                    new java.io.File(filePath),
                    ContentType.DEFAULT_BINARY,
                    new java.io.File(filePath).getName());

            postMethod.setEntity(builder.build());

            CloseableHttpResponse response = getSharedClient().execute(postMethod);
            String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");

            // Parse the response as JSON
            JSONObject result = (JSONObject) new org.openedit.util.JSONParser().parse(jsonResponse);

            response.close();
            return (String) result.get("id");

        } catch (Exception e) {
            log.error("Error uploading file to Postiz", e);
            throw new OpenEditException("Failed to upload file", e);
        }
    }

    public String getApiEndpoint() {
        return getMediaArchive().getCatalogSettingValue("postiz-url") + "/public/v1"        		;
    }

    public String getApiKey() {
    	
    	//get it from socialmediaprofile userprofile.
    	String apikey = "";
    	
        return getMediaArchive().getCatalogSettingValue("postiz-key");
    }
    
    

    
    
}


