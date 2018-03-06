package org.entermediadb.video;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.TranscodeTools;
import org.entermediadb.google.GoogleManager;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.repository.ContentItem;
import org.openedit.repository.RepositoryException;
import org.openedit.util.OutputFiller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CloudTranscodeManager implements CatalogEnabled
{
	protected String fieldCatalogId;
	protected ModuleManager fieldModuleManager;
	protected MediaArchive fieldMediaArchive;
	protected GoogleManager fieldGoogleManager;
	private static final Log log = LogFactory.getLog(CloudTranscodeManager.class);

	public GoogleManager getGoogleManager()
	{
	if (fieldGoogleManager == null) {
		fieldGoogleManager = (GoogleManager) getModuleManager().getBean(getCatalogId(), "googleManager");
		
	}

	return fieldGoogleManager;
	}


	
	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
		}
		return fieldMediaArchive;
	}


	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}


	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}


	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}


	public String getCatalogId()
	{
		return fieldCatalogId;
	}


	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}


	public void transcodeCaptions(Asset inAsset, String inLang) throws RepositoryException, IOException
	{
		//start saving the transcode into the database
		Searcher tracksearcher = getMediaArchive().getSearcher("videotrack");
//		Data lasttrack = tracksearcher.query().exact("assetid", inAsset.getId()).exact("sourcelang", inLang).searchOne();
//		tracksearcher.delete(lasttrack, null);

		MediaArchive archive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		
	
		ContentItem item = archive.getOriginalContent(inAsset);
		
	
		TranscodeTools transcodetools = archive.getTranscodeTools();
		Map all = new HashMap(); //TODO: Get parent ones as well
		ConversionManager manager = archive.getTranscodeTools().getManagerByFileFormat("flac");
		ConvertInstructions instructions = manager.createInstructions(inAsset, "audio.flac");
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		instructions.setOutputStream(output);
		instructions.setProperty("timeoffset", "0");
		instructions.setProperty("duration", "50");
		//https://stackoverflow.com/questions/20295398/ffmpeg-clip-audio-interval-with-starting-and-end-time
		
		
		manager.createOutput(instructions);
		
		
		
		
		Data authinfo = archive.getData("oauthprovider", "google");
		
		try {
			getTranscodeData(authinfo, output.toByteArray());
		} catch (Exception e) {
			throw new OpenEditException(e);
		}
		
	
	}
	
	
	
	
	
	public ArrayList getTranscodeData(Data inAuthinfo, byte[]  inAudioContent) throws Exception 
	{
		String url = "https://speech.googleapis.com/v1/speech:recognize";
	
		
		
		String encodedString = Base64.encodeBase64String(inAudioContent);
		
		
		//		String json = "		 'config': { 
		//	    'encoding': 'MP3',
		//	    'sampleRateHertz': 16000,
		//	    'languageCode': 'en-US',
		//	    'enableWordTimeOffsets': false
		//	  },
		//	  'audio': {
		//	    'content': '/9j/7QBEUGhvdG9zaG9...base64-encoded-audio-content...fXNWzvDEeYxxxzj/Coa6Bax//Z'
		//	  }
		//	";
		
		
		
		CloseableHttpClient httpclient;
		httpclient = HttpClients.createDefault();
		HttpPost httpmethod = new HttpPost(url);
		String accesstoken = getGoogleManager().getAccessToken(inAuthinfo);
		httpmethod.addHeader("authorization", "Bearer " + accesstoken);
		httpmethod.addHeader("Content-Type","application/json; charset=utf-8"); 
		
		
		

		
		JsonObject object = new JsonObject();
		
		JsonObject config = new JsonObject();
		config.addProperty("encoding", "FLAC");
		config.addProperty("sampleRateHertz", 16000);
		config.addProperty("languageCode", "en-US");
		config.addProperty("enableWordTimeOffsets", true);
		object.add("config", config);
		
		JsonObject audio = new JsonObject();
		audio.addProperty("content", encodedString);
		object.add("audio", audio);
		
		String jsonstring = object.toString();
		
		
		StringEntity params = new StringEntity(jsonstring, "UTF-8");
		//				method.setEntity(params);

		//HttpEntity entity = new ByteArrayEntity(data.getBytes("UTF-8"));
		//StringEntity xmlEntity = new StringEntity(data, "application/atom+xml;type=entry","UTF-8");

		httpmethod.setEntity(params);
		
		
		
		
		HttpResponse resp = httpclient.execute(httpmethod);

		if (resp.getStatusLine().getStatusCode() != 200)
		{
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + ":"  + resp.getStatusLine().getReasonPhrase());
			String returned = EntityUtils.toString(resp.getEntity());
			log.info(returned);

		}

		else {
			
			String returned = EntityUtils.toString(resp.getEntity());
			JsonParser parser = new JsonParser();
			JsonElement elem = parser.parse(returned);
			// log.info(content);
		
		}
		return null;
	}
	
	
	
	
}
