package org.entermediadb.ai.transcriber; 

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.convert.TranscodeTools;
import org.entermediadb.asset.convert.managers.AudioConversionManager;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.Searcher;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.repository.RepositoryException;

public class WhisperTranscriberManager extends InformaticsProcessor {
	
	private static final Log log = LogFactory.getLog(WhisperTranscriberManager.class);
	
	@Override
	public void processInformaticsOnEntities(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inRecords)
	{
		// Do nothing
	}

	@Override
	public void processInformaticsOnAssets(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inAssets)
	{
		Collection<MultiValued> toprocess = new ArrayList<MultiValued>();

		for (Iterator iterator = inAssets.iterator(); iterator.hasNext();)
		{
			MultiValued inAsset = (MultiValued) iterator.next();
			
			String mediatype = getMediaArchive().getMediaRenderType(inAsset);

			if( !"video".equals(mediatype) && !"audio".equals(mediatype) )
			{
				continue;
			}
			toprocess.add(inAsset);
			
		}
		
		if(toprocess.size() > 0) 
		{
			inLog.headline("Transcribing " + toprocess.size() + " asset(s)");
			
			for (Iterator iterator = toprocess.iterator(); iterator.hasNext();)			
			{
				MultiValued inAsset = (MultiValued) iterator.next();
				
				if(inAsset.getValue("length") == null) 
				{
					///Can't process if no lenght defined
					inAsset.setValue("llmerror", true);
					getMediaArchive().saveData("asset",inAsset);
					iterator.remove();
					inLog.info("Skiping Asset with no lenght defined: " + inAsset);
					continue;
				}

				inLog.info("Transcribing: " + inAsset);
				
				long starttime = System.currentTimeMillis();
				boolean ok = transcribeOneAsset(inLog, inAsset);
				long duration = (System.currentTimeMillis() - starttime) / 1000L;
				
				if(ok)
				{					
					inLog.info("Transcribed successfully! Took: " + duration + " seconds");
				}
				
			}
		}
		
	}
	
	public boolean transcribeOneAsset(ScriptLogger inLog, MultiValued inAsset)
	{
		
		Searcher captionSearcher = getMediaArchive().getSearcher("videotrack");
		
		Data inTrack = captionSearcher.query().exact("assetid", inAsset.getId()).searchOne();
		
		if( inTrack != null)
		{
			String status = inTrack.get("transcribestatus");
			if("complete".equals(status) || "inprogress".equals(status))
			{
				inLog.info("Asset already assigned to a videotrack");
				return false; //already done or in progress
			}
			
		}
		if( inTrack == null)
		{
			inTrack = captionSearcher.createNewData();
			inTrack.setProperty("assetid",  inAsset.getId());
			inTrack.setValue("length", inAsset.getValue("length"));
		}
		
		inTrack.setValue("requesteddate", new Date());
		inTrack.setValue("sourcelang", "en");
		
		try 
		{
			inTrack.setValue("transcribestatus", "inprogress");
			captionSearcher.saveData(inTrack);
			
			transcribe(inAsset, inTrack);
			inTrack.setValue("transcribestatus", "complete");
			return true;
		}
		catch (Exception e) 
		{
			inLog.error("Could not transcribe " + inAsset, e);
			inTrack.setValue("transcribestatus", "error");
			return false;
		}
		finally
		{
			inTrack.setValue("completeddate", new Date());
			captionSearcher.saveData(inTrack);
		}
	}

	public void transcribe(MultiValued inAsset, Data inTrack) throws RepositoryException, IOException 
	{
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");

		TranscodeTools transcodetools = archive.getTranscodeTools();
		AudioConversionManager manager = (AudioConversionManager) transcodetools.getManagerByFileFormat("mp3");
		ConvertInstructions instructions = manager.createInstructions((Asset) inAsset, "audio.mp3");
		ContentItem item = manager.findInput(instructions);

		if (item == null) 
		{
			item = archive.getOriginalContent(inAsset);
		}
		
		instructions.setInputFile(item);
		
		if (inAsset.getValue("length") == null)
		{
			log.info("Asset with no lenght, can't transcribe.");
			return;
		}
		
		double length = (Double) inAsset.getValue("length");

		Collection captions = new ArrayList();

		for (double timeoffset = 0; timeoffset < length; timeoffset += 300) 
		{

			instructions.setProperty("timeoffset", String.valueOf(timeoffset));
			instructions.setProperty("duration", "300");
			instructions.setProperty("resample", "16000");
			
			Page page = archive.getPageManager().getPage("/WEB-INF/temp/" + inAsset.getId() + "data.mp3");
			archive.getPageManager().removePage(page);
			
			ContentItem tempfile = page.getContentItem();
			

			instructions.setOutputFile(tempfile);

			ConvertResult result = manager.createOutput(instructions, true);
			if (!result.isOk()) {
				throw new OpenEditException("Could not transcode audio");
			}
			try {

				JSONObject transcriptions = getTranscribedData(tempfile);
				
				if (transcriptions == null) {
					log.error("Transcriber server error");
					throw new OpenEditException("Transcriber server error");
				}
	
				for (Iterator iterator2 = transcriptions.iterator(); iterator2.hasNext();) 
				{
					Map cuemap = new HashMap();
					JSONObject transcription = (JSONObject) iterator2.next();
					
					double start = (double) transcription.get("start");
					double end = (double) transcription.get("end");
					String text = (String) transcription.get("text");
					String speaker = (String) transcription.get("speaker");
					
					if (speaker == null) {
						speaker = "Speaker 1";
					}
					
					cuemap.put("cliplabel", text);
					cuemap.put("speaker", speaker);
					cuemap.put("timecodestart", Math.round((timeoffset + start) * 1000d));
					cuemap.put("timecodelength", Math.round((end - start) * 1000d));
					
					captions.add(cuemap);
					
				}
				
				log.info("Transcribed " + (timeoffset - 300) + "s - " + timeoffset + "s of " + inAsset);

			} 
			catch (Exception e) 
			{
				if (e instanceof OpenEditException) 
				{
					throw (OpenEditException) e;
				}
				throw new OpenEditException(e);
			} 
			finally 
			{
				archive.getPageManager().removePage(page);
			}

		}
		
		inTrack.setValue("captions", captions);
	}
	
	
	
	public JSONObject getTranscribedData(ContentItem audio) throws FileNotFoundException, Exception {
		
		File audioFile = new File(audio.getAbsolutePath());
		if(!audioFile.exists())
		{
			throw new FileNotFoundException("File not found: " + audioFile);
		}
		LlmConnection connection = getMediaArchive().getLlmConnection("transcribeFile");
		
		Map headers = new HashMap();
		headers.put("Authorization", "Bearer " + connection.getApiKey());
		
		Map params = new HashMap();
		params.put("file", audioFile);
		
		LlmResponse resp = connection.callJson("/transcribe", headers, params);
		
		JSONObject result = (JSONObject) resp.getRawResponse();  
		return result;

	}
	
	
/*
	public JSONArray getTranscribedData_OLD(ContentItem audio) throws FileNotFoundException, Exception {
		String endpoint = getMediaArchive().getCatalogSettingValue("ai_transcriber_server") + "/transcribe";

		HttpPost method = new HttpPost(endpoint);
		method.addHeader("Authorization", "Bearer YOUR_SECRET_TOKEN");
		
		File audioFile = new File(audio.getAbsolutePath());
		if(!audioFile.exists())
		{
			throw new FileNotFoundException("File not found: " + audioFile);
		}
		
		HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("file", audioFile, ContentType.create("audio/mp3"), audioFile.getName())
                .build();
		
		method.setEntity(entity);

		CloseableHttpResponse resp = getSharedConnection().sharedExecute(method);


		if (resp.getStatusLine().getStatusCode() != 200) {
			log.info("Transcriber server error returned " + resp.getStatusLine().getStatusCode() + ":"
					+ resp.getStatusLine().getReasonPhrase());
			String returned = EntityUtils.toString(resp.getEntity());
			log.info(returned);
			return null;
		}

		else {
			String returned = EntityUtils.toString(resp.getEntity());
			JSONArray result = (JSONArray) new JSONParser().parseMapArray(returned);
			return result;

		}
	}
	*/
}
