package org.enteremdiadb.postiz;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.MediaArchive;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;

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

    public JSONObject createPost(String inPostContent, Date inPostDate, String postType, List<String> inFilePaths, List<String> integrationIds) {
        String apiKey = getApiKey();
        assert apiKey != null : "API Key is required";

        String endpoint = getApiEndpoint() + "/posts";
        HttpPost postMethod = new HttpPost(endpoint);
        postMethod.addHeader("Authorization", "Bearer " + apiKey);
        postMethod.setHeader("Content-Type", "application/json");

        try {
            // Construct the post payload
            JSONObject payload = new JSONObject();
            payload.put("type", postType);

            if (inPostDate != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                payload.put("date", dateFormat.format(inPostDate));
            }

            payload.put("content", inPostContent);

            // Add integrations
            if (integrationIds != null && !integrationIds.isEmpty()) {
                JSONArray integrationsArray = new JSONArray();
                for (String integrationId : integrationIds) {
                    JSONObject integrationObject = new JSONObject();
                    integrationObject.put("id", integrationId);
                    integrationsArray.add(integrationObject);
                }
                payload.put("integrations", integrationsArray);
            }

            // Add files
            if (inFilePaths != null && !inFilePaths.isEmpty()) {
                JSONArray filesArray = new JSONArray();
                for (String filePath : inFilePaths) {
                    String fileId = uploadFile(filePath);
                    if (fileId != null) {
                        filesArray.add(fileId);
                    }
                }
                payload.put("files", filesArray);
            }

            postMethod.setEntity(new StringEntity(payload.toJSONString(), "UTF-8"));

            CloseableHttpResponse response = getSharedClient().execute(postMethod);
            String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");

            // Parse the response as JSON
            JSONObject result = (JSONObject) new org.json.simple.parser.JSONParser().parse(jsonResponse);

            response.close();
            return result;

        } catch (Exception e) {
            log.error("Error creating post in Postiz", e);
            throw new RuntimeException("Failed to create post", e);
        }
    }

    public JSONArray listIntegrations() {
        String apiKey = getApiKey();
        assert apiKey != null : "API Key is required";

        String endpoint = getApiEndpoint() + "/integrations";
        HttpGet getMethod = new HttpGet(endpoint);
        getMethod.addHeader("Authorization",  "{" + apiKey  + "}");

        try {
            CloseableHttpResponse response = getSharedClient().execute(getMethod);
            
            String jsonResponse = EntityUtils.toString(response.getEntity(), "UTF-8");
            
            // Parse the response as JSON
            JSONArray integrations = (JSONArray) new org.json.simple.parser.JSONParser().parse(jsonResponse);
            response.close();
            return integrations;

        } catch (Exception e) {
            log.error("Error listing integrations in Postiz", e);
            throw new RuntimeException("Failed to list integrations", e);
        }
    }

    private String uploadFile(String filePath) {
        String apiKey = getApiKey();
        assert apiKey != null : "API Key is required";

        String endpoint = getApiEndpoint() + "/upload";
        HttpPost postMethod = new HttpPost(endpoint);
        postMethod.addHeader("Authorization", "Bearer " + apiKey);

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
            JSONObject result = (JSONObject) new org.json.simple.parser.JSONParser().parse(jsonResponse);

            response.close();
            return (String) result.get("id");

        } catch (Exception e) {
            log.error("Error uploading file to Postiz", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public String getApiEndpoint() {
        return getMediaArchive().getCatalogSettingValue("postiz-url");
    }

    public String getApiKey() {
        return getMediaArchive().getCatalogSettingValue("postiz-key");
    }
}
