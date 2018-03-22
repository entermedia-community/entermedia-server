package org.entermediadb.video;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.TranscodeTools;
import org.entermediadb.google.GoogleManager;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.repository.RepositoryException;
import org.openedit.util.OutputFiller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CloudTranscodeManager implements CatalogEnabled {
	protected String fieldCatalogId;
	protected ModuleManager fieldModuleManager;
	protected MediaArchive fieldMediaArchive;
	protected GoogleManager fieldGoogleManager;
	private static final Log log = LogFactory.getLog(CloudTranscodeManager.class);

	public GoogleManager getGoogleManager() {
		if (fieldGoogleManager == null) {
			fieldGoogleManager = (GoogleManager) getModuleManager().getBean(getCatalogId(), "googleManager");

		}

		return fieldGoogleManager;
	}

	public MediaArchive getMediaArchive() {
		if (fieldMediaArchive == null) {
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive) {
		fieldMediaArchive = inMediaArchive;
	}

	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager) {
		fieldModuleManager = inModuleManager;
	}

	public String getCatalogId() {
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId) {
		fieldCatalogId = inCatalogId;
	}

	public void transcodeCaptions(Data inTrack) throws RepositoryException, IOException {

		Asset inAsset = getMediaArchive().getAsset(inTrack.get("assetid"));
		String inLang = inTrack.get("sourcelang");

		// start saving the transcode into the database
		Searcher tracksearcher = getMediaArchive().getSearcher("videotrack");
		// Data lasttrack = tracksearcher.query().exact("assetid",
		// inAsset.getId()).exact("sourcelang", inLang).searchOne();
		// tracksearcher.delete(lasttrack, null);

		MediaArchive archive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		Data authinfo = archive.getData("oauthprovider", "google");

		TranscodeTools transcodetools = archive.getTranscodeTools();
		Map all = new HashMap(); // TODO: Get parent ones as well
		ConversionManager manager = archive.getTranscodeTools().getManagerByFileFormat("flac");
		ConvertInstructions instructions = manager.createInstructions(inAsset, "audio.flac");
		ContentItem item = manager.findInput(instructions);

		if (item == null) {
			item = archive.getOriginalContent(inAsset);
		}
		instructions.setInputFile(item);
		double length = (Double) inAsset.getValue("length");

		Searcher captionsearcher = archive.getSearcher("videotrack");

		Collection captions = new ArrayList();
		inTrack.setValue("captions", captions);

		for (double i = 0; i < length; i += 58) {

			instructions.setProperty("timeoffset", String.valueOf(i));
			instructions.setProperty("duration", "58");
			instructions.setProperty("compressionlevel", "12");

			// instructions.setStre(true);
			Page page = archive.getPageManager().getPage("/WEB-INF/temp/" + inAsset.getId() + "data.flac");
			archive.getPageManager().removePage(page);
			ContentItem tempfile = page.getContentItem();

			instructions.setOutputFile(tempfile);

			ConvertResult result = manager.createOutput(instructions);
			if (!result.isOk()) {
				throw new OpenEditException("Could not transcode flac");
			}
			try {
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				OutputFiller filler = new OutputFiller();
				filler.fill(tempfile.getInputStream(), output);
				JsonObject elem = getTranscodeData(authinfo, output.toByteArray());
				if (elem == null) {
					log.error("Security error ");
					throw new OpenEditException("Security error");
				}
				JsonArray results = (JsonArray) elem.get("results");
				if (results == null) {
					log.error("Got back " + elem);
					throw new OpenEditException("Invalid results " + elem);
				}
				for (Iterator iterator2 = results.iterator(); iterator2.hasNext();) {
					Map cuemap = new HashMap();
					JsonObject alternative = (JsonObject) iterator2.next();
					JsonArray alternatives = (JsonArray) alternative.get("alternatives");
					for (Iterator iterator = alternatives.iterator(); iterator.hasNext();) {

						JsonObject data = (JsonObject) iterator.next();
						String cliplabel = data.get("transcript").getAsString();
						JsonArray words = data.get("words").getAsJsonArray();
						JsonObject firstword = (JsonObject) words.get(0);
						String offsetstring = firstword.get("startTime").getAsString().replaceAll("s", "");
						JsonObject lastword = (JsonObject) words.get(words.size() - 1);
						String laststring = lastword.get("endTime").getAsString().replaceAll("s", "");

						double extraoffset = Double.parseDouble(offsetstring);
						double finaloffset = Double.parseDouble(laststring);

						cuemap.put("cliplabel", cliplabel);
						cuemap.put("timecodestart", Math.round((i + extraoffset) * 1000d));
						cuemap.put("timecodelength", Math.round((finaloffset - extraoffset) * 1000d));
						log.info("Saved " + cliplabel + " : " + " " + i + " " + finaloffset);
						captions.add(cuemap);
					}
				}
				captionsearcher.saveData(inTrack);

			} catch (Exception e) {
				if (e instanceof OpenEditException) {
					throw (OpenEditException) e;
				}
				throw new OpenEditException(e);
			} finally {
				archive.getPageManager().removePage(page);
			}

		}
	}

	public JsonObject getTranscodeData(Data inAuthinfo, byte[] inAudioContent) throws Exception {
		String url = "https://speech.googleapis.com/v1/speech:recognize";

		String encodedString = Base64.encodeBase64String(inAudioContent);

		CloseableHttpClient httpclient;
		httpclient = HttpClients.createDefault();
		HttpPost httpmethod = new HttpPost(url);
		String accesstoken = getGoogleManager().getAccessToken(inAuthinfo);
		httpmethod.addHeader("authorization", "Bearer " + accesstoken);
		httpmethod.addHeader("Content-Type", "application/json; charset=utf-8");

		JsonObject object = new JsonObject();

		JsonObject config = new JsonObject();
		config.addProperty("encoding", "FLAC");
		// config.addProperty("sampleRateHertz", 44100);
		config.addProperty("languageCode", "en-US");
		config.addProperty("enableWordTimeOffsets", true);
		object.add("config", config);

		JsonObject audio = new JsonObject();
		audio.addProperty("content", encodedString);
		object.add("audio", audio);

		String jsonstring = object.toString();

		StringEntity params = new StringEntity(jsonstring, "UTF-8");
		// method.setEntity(params);

		// HttpEntity entity = new ByteArrayEntity(data.getBytes("UTF-8"));
		// StringEntity xmlEntity = new StringEntity(data,
		// "application/atom+xml;type=entry","UTF-8");

		httpmethod.setEntity(params);

		HttpResponse resp = httpclient.execute(httpmethod);

		if (resp.getStatusLine().getStatusCode() != 200) {
			log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + ":"
					+ resp.getStatusLine().getReasonPhrase());
			String returned = EntityUtils.toString(resp.getEntity());
			log.info(returned);
			return null;
		}

		else {
			String returned = EntityUtils.toString(resp.getEntity());
			JsonParser parser = new JsonParser();
			JsonObject elem = (JsonObject) parser.parse(returned);
			return elem;

		}
	}

	public void asyncTranscodeCaptions(Data inTrack) throws RepositoryException, IOException {

		Asset inAsset = getMediaArchive().getAsset(inTrack.get("assetid"));
		String inLang = inTrack.get("sourcelang");

		// start saving the transcode into the database
		Searcher tracksearcher = getMediaArchive().getSearcher("videotrack");
		// Data lasttrack = tracksearcher.query().exact("assetid",
		// inAsset.getId()).exact("sourcelang", inLang).searchOne();
		// tracksearcher.delete(lasttrack, null);

		MediaArchive archive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		Data authinfo = archive.getData("oauthprovider", "google");

		TranscodeTools transcodetools = archive.getTranscodeTools();
		Map all = new HashMap(); // TODO: Get parent ones as well

		String currentstatus = inTrack.get("transcribestatus");
		
		if ("needstranscribe".equals(currentstatus)) {
			inAsset.setValue("closecaptionstatus", "inprogress");
			ConversionManager manager = archive.getTranscodeTools().getManagerByFileFormat("flac");
			ConvertInstructions instructions = manager.createInstructions(inAsset, "audio.flac");
			ContentItem item = manager.findInput(instructions);

			if (item == null) {
				item = archive.getOriginalContent(inAsset);
			}
			instructions.setInputFile(item);
			instructions.setProperty("compressionlevel", "12");
			Searcher captionsearcher = archive.getSearcher("videotrack");

			Collection captions = new ArrayList();
			inTrack.setValue("captions", captions);
			ConvertResult result = manager.createOutput(instructions);

			String bucket = getMediaArchive().getCatalogSettingValue("transcodebucket");
			JsonObject data = new JsonObject();
			String googlename = "temp/" + inAsset.getId() + "data.flac";
			data.addProperty("name", googlename);
			try {
				if(inTrack.get("selfLink") == null){
				JsonObject response = getGoogleManager().uploadToBucket(authinfo, bucket, result.getOutput(),data.toString());
				
				inTrack.setValue("googleid", response.get("id").getAsString());
				String selflink = response.get("selfLink").getAsString();
				inTrack.setValue("selfLink", selflink);
				inTrack.setValue("mediaLink", response.get("mediaLink").getAsString());
				inTrack.setValue("transcribestatus", "inprogress");
				}
				
				String url = "https://speech.googleapis.com/v1/speech:longrunningrecognize";


				CloseableHttpClient httpclient;
				httpclient = HttpClients.createDefault();
				HttpPost httpmethod = new HttpPost(url);

				String accesstoken = getGoogleManager().getAccessToken(authinfo);
				httpmethod.addHeader("authorization", "Bearer " + accesstoken);
				httpmethod.addHeader("Content-Type", "application/json; charset=utf-8");

				JsonObject object = new JsonObject();

				JsonObject config = new JsonObject();
			//	config.addProperty("encoding", "FLAC");
			//	 config.addProperty("sampleRateHertz", 48000);
				config.addProperty("languageCode", "en-US");
				config.addProperty("enableWordTimeOffsets", true);
				object.add("config", config);
				
				JsonObject audio = new JsonObject();
				audio.addProperty("uri", "gs://" + bucket + "/" + googlename);
				object.add("audio", audio);
				
				
				String jsonstring = object.toString();

				StringEntity params = new StringEntity(jsonstring, "UTF-8");
				// method.setEntity(params);

				// HttpEntity entity = new ByteArrayEntity(data.getBytes("UTF-8"));
				// StringEntity xmlEntity = new StringEntity(data,
				// "application/atom+xml;type=entry","UTF-8");

				httpmethod.setEntity(params);

				HttpResponse resp = httpclient.execute(httpmethod);

				if (resp.getStatusLine().getStatusCode() != 200) {
					log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + ":"
							+ resp.getStatusLine().getReasonPhrase());
					String returned = EntityUtils.toString(resp.getEntity());
					log.info(returned);

				}

				HttpEntity entity = resp.getEntity();
				JsonParser parser = new JsonParser();
				String content = IOUtils.toString(entity.getContent());

				JsonElement elem = parser.parse(content);
				JsonObject taskinfo = elem.getAsJsonObject();
				
				inTrack.setValue("taskname", taskinfo.get("name").getAsString());
				
				inTrack.setValue("transcribestatus", "inprogress");
				
				
			} catch (Exception e) {
				throw new OpenEditException(e);
			}

		} else if("inprogress".equals(currentstatus)){
			inAsset.setValue("closecaptionstatus", "inprogress");

			log.info("Making progress!");
			String url = "https://speech.googleapis.com/v1/operations/" + inTrack.get("taskname");
			CloseableHttpClient httpclient;
			httpclient = HttpClients.createDefault();
			HttpGet httpmethod = new HttpGet(url);
			
			String accesstoken = getGoogleManager().getAccessToken(authinfo);
			httpmethod.addHeader("authorization", "Bearer " + accesstoken);
			httpmethod.addHeader("Content-Type", "application/json; charset=utf-8");
			
			
			HttpResponse resp = httpclient.execute(httpmethod);

			if (resp.getStatusLine().getStatusCode() != 200) {
				log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + ":"
						+ resp.getStatusLine().getReasonPhrase());
				String returned = EntityUtils.toString(resp.getEntity());
				log.info(returned);

			}

			HttpEntity entity = resp.getEntity();
			JsonParser parser = new JsonParser();
			String content = IOUtils.toString(entity.getContent());

			JsonElement elem = parser.parse(content);
			JsonObject taskinfo = elem.getAsJsonObject();
			
			if(taskinfo.get("done") != null && taskinfo.get("done").getAsBoolean()){
				
				Collection captions = new ArrayList();
				inTrack.setValue("captions", captions);
				
				JsonArray results = taskinfo.get("response").getAsJsonObject().get("results").getAsJsonArray();
				
				for (Iterator iterator2 = results.iterator(); iterator2.hasNext();) {
					JsonObject alternative = (JsonObject) iterator2.next();
					JsonArray alternatives = (JsonArray) alternative.get("alternatives");
					for (Iterator iterator = alternatives.iterator(); iterator.hasNext();) {

						JsonObject data = (JsonObject) iterator.next();
						//String cliplabel = data.get("transcript").getAsString();
						JsonArray words = data.get("words").getAsJsonArray();
						
						StringBuffer buffer = new StringBuffer();
						int charcount = 0;
						
						
						JsonObject firstword = (JsonObject) words.get(0);
						JsonObject lastword = null;
						
					
						
						
						for (Iterator iterator3 = words.iterator(); iterator3.hasNext();) {
							
							JsonObject worddata = (JsonObject) iterator3.next();
							String word = worddata.get("word").getAsString();
							charcount += word.length();
							
							if(charcount > 200 || !iterator3.hasNext()){
								if(!iterator3.hasNext()){
									buffer.append(word);
									buffer.append(" ");
									lastword = worddata;

								}
								
														
								String offsetstring = firstword.get("startTime").getAsString().replaceAll("s", "");
								String  laststring = lastword.get("endTime").getAsString().replaceAll("s", "");

								
								
								double extraoffset = Double.parseDouble(offsetstring);
								double finaloffset = Double.parseDouble(laststring);
								
								Map cuemap = new HashMap();
								cuemap.put("timecodestart", Math.round((extraoffset) * 1000d));
								cuemap.put("timecodelength", Math.round((finaloffset - extraoffset) * 1000d)-1);
								cuemap.put("cliplabel", buffer.toString());
								captions.add(cuemap);
								firstword = (JsonObject) worddata;
								buffer = new StringBuffer();
								charcount = 0;
								
								
							} 
							lastword = worddata;
							buffer.append(word);
							buffer.append(" ");
							
						}
						
						
						
						
						
						
						
						
						
					}
				}
				inTrack.setValue("transcribestatus", "complete");
				inTrack.setValue("completeddate", new Date());

				tracksearcher.saveData(inTrack);
				
				inAsset.setValue("closecaptionstatus", "complete");

				
				
			} else{
				try{
				int percent = taskinfo.get("metadata").getAsJsonObject().get("progressPercent").getAsInt();
				inTrack.setValue("percentcomplete", percent);
				
				} catch(Exception e){
					log.info("didn't get percentage yet");
					
				}
			}
			
			//go check google and parse if we're done
			getMediaArchive().saveAsset(inAsset);
			
			
		}

	}

}
