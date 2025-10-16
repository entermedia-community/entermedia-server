package org.entermediadb.ai.transcriber;

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
import org.openedit.util.JSONParser;

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
		for (Iterator iterator = inAssets.iterator(); iterator.hasNext();)
		{
			MultiValued inAsset = (MultiValued) iterator.next();
			String mediatype = getMediaArchive().getMediaRenderType(inAsset);
			if( mediatype.equals("default") )
			{
				inLog.info(inConfig.get("bean") + " - Skipping asset " + inAsset);
				continue;
			}
			if( !mediatype.equals("video") && !mediatype.equals("audio") )
			{
				inLog.info(inConfig.get("bean") + " - Skipping asset " + inAsset);
				continue;
			}
			
			Searcher captionSearcher = getMediaArchive().getSearcher("videotrack");
			
			Data inTrack = captionSearcher.query().exact("assetid", inAsset.getId()).searchOne();
			
			if( inTrack != null)
			{
				String status = inTrack.get("transcribestatus");
				if(status != null && status.equals("complete"))
				{
					continue; //already done
				}
				else if(status == null ||  status.equals("error"))
				{
					inTrack.setValue("transcribestatus", "needstranscribe");
				}
				
			}
			if( inTrack == null)
			{
				inTrack = captionSearcher.createNewData();
				inTrack.setProperty("assetid",  inAsset.getId());
				inTrack.setValue("transcribestatus", "needstranscribe");
				inTrack.setValue("length", inAsset.getValue("length"));
			}
			
			inTrack.setValue("requesteddate", new Date());
			inTrack.setValue("sourcelang", "en");
			
			try 
			{
				transcribeAsset(inLog, inAsset, inTrack);
				inTrack.setValue("transcribestatus", "complete");
			}
			catch (Exception e) 
			{
				inLog.error("Could not transcribe " + inAsset, e);
				inTrack.setValue("transcribestatus", "error");
			}
			finally
			{
				inTrack.setValue("completeddate", new Date());
				captionSearcher.saveData(inTrack);
			}
		}
		
	}

	public void transcribeAsset(ScriptLogger inLog, MultiValued inAsset, Data inTrack) throws RepositoryException, IOException 
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

				JSONArray transcriptions = getTranscribedData(tempfile);
				
				if (transcriptions == null) {
					inLog.error("Transcriber server error");
					throw new OpenEditException("Transcriber server error");
				}
	
				for (Iterator iterator2 = transcriptions.iterator(); iterator2.hasNext();) 
				{
					Map cuemap = new HashMap();
					JSONObject transcription = (JSONObject) iterator2.next();
					
					double start = (double) transcription.get("start");
					double end = (double) transcription.get("end");
					String text = (String) transcription.get("text");

					cuemap.put("cliplabel", text);
					cuemap.put("timecodestart", Math.round((timeoffset + start) * 1000d));
					cuemap.put("timecodelength", Math.round((end - start) * 1000d));
					
					captions.add(cuemap);
					
				}
				
				inLog.info("Transcribed " + (timeoffset - 300) + "s - " + timeoffset + "s of " + inAsset);

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

	public JSONArray getTranscribedData(ContentItem audio) throws FileNotFoundException, Exception {
		return (JSONArray) new JSONParser().parseJSONArray("[{\"start\":0.91,\"end\":4.11,\"text\":\"The stale smell of old beer lingers.\"},{\"start\":4.11,\"end\":6.69,\"text\":\"It takes heat to bring out the odor\"},{\"start\":6.69,\"end\":9.73,\"text\":\"A cold dip restores health in zest.\"},{\"start\":9.73,\"end\":12.42,\"text\":\"A salt pickle tastes fine with ham.\"},{\"start\":12.42,\"end\":14.82,\"text\":\"Tacos Al pastor are my favorite.\"},{\"start\":14.82,\"end\":18.03,\"text\":\"A zestful food is the hot cross bun.\"}]");
		
//		String endpoint = getMediaArchive().getCatalogSettingValue("ai_transcriber_server") + "/transcribe";
//
//		HttpPost method = new HttpPost(endpoint);
//		method.addHeader("Authorization", "Bearer YOUR_SECRET_TOKEN");
//		
//		File audioFile = new File(audio.getAbsolutePath());
//		if(!audioFile.exists())
//		{
//			throw new FileNotFoundException("File not found: " + audioFile);
//		}
//		
//		HttpEntity entity = MultipartEntityBuilder.create()
//                .addBinaryBody("file", audioFile, ContentType.create("audio/mp3"), audioFile.getName())
//                .build();
//		
//		method.setEntity(entity);
//
//		HttpSharedConnection connection = new HttpSharedConnection();
//		CloseableHttpResponse resp = connection.sharedExecute(method);
//
//
//		if (resp.getStatusLine().getStatusCode() != 200) {
//			log.info("Transcriber server error returned " + resp.getStatusLine().getStatusCode() + ":"
//					+ resp.getStatusLine().getReasonPhrase());
//			String returned = EntityUtils.toString(resp.getEntity());
//			log.info(returned);
//			return null;
//		}
//
//		else {
//			String returned = EntityUtils.toString(resp.getEntity());
//
//			JSONArray result = (JSONArray) new JSONParser().parseJSONArray(returned);
//			
//			return result;
//
//		}
	}
	
}
