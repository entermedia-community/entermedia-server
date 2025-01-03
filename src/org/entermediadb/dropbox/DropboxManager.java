package org.entermediadb.dropbox;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.repository.ContentItem;
import org.openedit.util.OutputFiller;

public class DropboxManager implements CatalogEnabled {
	
	private static final Log log = LogFactory.getLog(DropboxManager.class);

    protected String fieldCatalogId;
    protected MediaArchive fieldMediaArchive;
    protected ModuleManager fieldModuleManager;
    protected OutputFiller filler = new OutputFiller();
    protected HttpSharedConnection connection;

    public String getCatalogId() {
        return fieldCatalogId;
    }

    @Override
    public void setCatalogId(String inCatalogId) {
        fieldCatalogId = inCatalogId;
    }

    protected MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}

  
    public ModuleManager getModuleManager() {
        return fieldModuleManager;
    }

    public void setModuleManager(ModuleManager inModuleManager) {
        fieldModuleManager = inModuleManager;
    }

    public OutputFiller getFiller() {
        return filler;
    }

    public void setFiller(OutputFiller inFiller) {
        filler = inFiller;
    }

    public HttpSharedConnection getConnection() {
        if (connection == null) {
            connection = new HttpSharedConnection();
        }
        return connection;
    }

    /**
     * Placeholder for namespace retrieval.
     * Assume this returns a valid namespace ID as a String.
     */
    public String getNamespace() {
        return "11484857059"; // Replace with dynamic retrieval if needed
    }

    /**
     * List folders in Dropbox for a given path.
     */
    private Collection<JSONObject> listEntries(String path, String type) throws Exception {
        Collection<JSONObject> entries = new ArrayList<>();
        String url = "https://api.dropboxapi.com/2/files/list_folder";

        JSONObject requestPayload = new JSONObject();
        requestPayload.put("path", path);

        String namespaceId = getNamespace();
        HttpPost method = new HttpPost(url);
        method.addHeader("Authorization", "Bearer " + getAccessToken());
        method.addHeader("Dropbox-API-Path-Root", "{\"namespace_id\": \"" + namespaceId + "\", \".tag\": \"namespace_id\"}");
        method.setHeader("Content-Type", "application/json");

        String payload = requestPayload.toJSONString();
        method.setEntity(new StringEntity(payload, "UTF-8"));

        CloseableHttpResponse resp = getConnection().sharedExecute(method);
        JSONObject json = getConnection().parseJson(resp);

        if (json != null) {
            JSONArray jsonEntries = (JSONArray) json.get("entries");
            for (Iterator iterator = jsonEntries.iterator(); iterator.hasNext();) {
                JSONObject entry = (JSONObject) iterator.next();
                String tag = (String) entry.get(".tag");
                if (tag.equals(type)) {
                    entries.add(entry);
                }
            }
        }

        return entries;
    }

    /**
     * List folders in Dropbox for a given path.
     */
    public Collection<JSONObject> listFolders(String path) throws Exception {
        return listEntries(path, "folder");
    }

    /**
     * List files in Dropbox for a given path.
     */
    public Collection<JSONObject> listFiles(String path) throws Exception {
        return listEntries(path, "file");
    }

    protected String getAccessToken() {
    	Data authinfo = getMediaArchive().getData("oauthprovider", "dropbox");
		String accesstoken = authinfo.get("accesstoken");
		return accesstoken;		
	}
    
    public File saveFile(String inAccessToken, Asset inAsset) throws Exception {
        // Get the Dropbox file ID from the asset
        String fileId = (String) inAsset.get("dropboxid");

        // Get the ContentItem for the asset's original content path
        ContentItem item = getMediaArchive().getOriginalContent(inAsset);
        File output = new File(item.getAbsolutePath());
        output.getParentFile().mkdirs();

        log.info("Downloading file for asset: " + inAsset.getName() + " to " + item.getPath());

        // Use the existing downloadFile method to handle the file download
        downloadFile(fileId, item.getAbsolutePath());

        // Update asset metadata and save it
        getMediaArchive().getAssetImporter().getAssetUtilities().getMetaDataReader()
                .updateAsset(getMediaArchive(), item, inAsset);
        inAsset.setProperty("previewstatus", "converting");
        getMediaArchive().saveAsset(inAsset);

        // Fire an event to indicate the asset was imported
        getMediaArchive().fireMediaEvent("assetimported", null, inAsset);

        return output;
    }

    

	public void downloadFile( String fileId, String outputPath) throws Exception {
        String url = "https://content.dropboxapi.com/2/files/download";

        JSONObject apiArg = new JSONObject();
        apiArg.put("path", fileId);

        String namespaceId = getNamespace();
        HttpPost method = new HttpPost(url);
        method.addHeader("Authorization", "Bearer " + getAccessToken());
        method.addHeader("Dropbox-API-Path-Root", "{\"namespace_id\": \"" + namespaceId + "\", \".tag\": \"namespace_id\"}");
        method.addHeader("Dropbox-API-Arg", apiArg.toJSONString());

        CloseableHttpResponse resp = getConnection().sharedExecute(method);
        try {
            if (resp.getStatusLine().getStatusCode() == 200) {
                // Ensure directories exist
                File output = new File(outputPath);
                output.getParentFile().mkdirs();
                
                log.info("Dropbox Manager Downloading to " + outputPath);

                // Use OutputFiller to save content to the file
                filler.fill(resp.getEntity().getContent(), new FileOutputStream(output), true);
            } else {
                log.error("Failed to download file. HTTP Status: " + resp.getStatusLine());
                throw new Exception("Failed to download file. Response: " + resp.getStatusLine());
            }
        } finally {
            resp.close();
        }
    }

    /**
     * Download a file from Dropbox.
     */
   
}
