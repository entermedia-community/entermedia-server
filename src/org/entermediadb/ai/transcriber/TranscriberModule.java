package org.entermediadb.ai.transcriber;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.entermediadb.translator.TranslationManager;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;

public class TranscriberModule extends BaseMediaModule {
	
	private static final Log log = LogFactory.getLog(TranscriberModule.class);
	
	public WhisperTranscriberManager getTranscriberManager(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		WhisperTranscriberManager transcriberManager = (WhisperTranscriberManager) getMediaArchive(catalogid).getBean("whisperTranscriberManager");
		return transcriberManager;
	}
	
	public void transcribeAsset(WebPageRequest inReq)
	{
		String assetid = inReq.getRequestParameter("assetid");
		MultiValued inAsset = (MultiValued) getMediaArchive(inReq).getAsset(assetid);
		inReq.putPageValue("asset", inAsset);
		
		String mediatype = getMediaArchive(inReq).getMediaRenderType(inAsset);

		if( !"video".equals(mediatype) && !"audio".equals(mediatype) )
		{
			return; //only video and audio
		}
		
		if(inAsset.getValue("length") == null) 
		{
			///Can't process if no lenght defined
			throw new OpenEditException("Asset with no lenght: " + inAsset);
		}
		
		Searcher captionSearcher = getMediaArchive(inReq).getSearcher("videotrack");
		
		Data inTrack = captionSearcher.query().exact("assetid", inAsset.getId()).searchOne();
		
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
			inReq.putPageValue("track", inTrack);
			
			getTranscriberManager(inReq).transcribe(inAsset, inTrack);
			inTrack.setValue("transcribestatus", "complete");
		}
		catch (Exception e) 
		{
			log.info("Could not transcribe " + inAsset, e);
			inTrack.setValue("transcribestatus", "error");
		}
		finally
		{
			inTrack.setValue("completeddate", new Date());
			
		}
		
		captionSearcher.saveData(inTrack);
	}
	
	public void translateTranscription(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Searcher captionSearcher = archive.getSearcher("videotrack");
		
		String assetid = inReq.getRequestParameter("assetid");
		MultiValued inAsset = (MultiValued) archive.getAsset(assetid);
		inReq.putPageValue("asset", inAsset);
		
		String mediatype = getMediaArchive(inReq).getMediaRenderType(inAsset);

		if( !"video".equals(mediatype) && !"audio".equals(mediatype) )
		{
			return; //only video and audio
		}
		String sourceLang = "en";
		String target = inReq.getRequestParameter("targetlang");
		Collection<String> targetLangs = new ArrayList<>();
		
		if("all".equals(target))
		{
			Collection<Data> langs = archive.getList("locale");
			for (Data langobj : langs)
			{
				String lang = langobj.getId();
				if(!sourceLang.equals(lang))
				{
					Data checkTrack = captionSearcher.query().exact("assetid", assetid).exact("sourcelang", lang).searchOne();
					if(checkTrack != null)
					{
						continue; //already have this translation
					}
					targetLangs.add(lang);
				}
			}
		}
		else if( target != null )
		{
			Data checkTrack = captionSearcher.query().exact("assetid", assetid).exact("sourcelang", target).searchOne();
			if(checkTrack != null)
			{
				return; //already have this translation
			}
			targetLangs.add(target);
		}
		else
		{
			return;
		}
		
		MultiValued inSourceTrack = (MultiValued) captionSearcher.query().exact("assetid", assetid).exact("sourcelang", "en").searchOne();
		
		if( inSourceTrack == null)
		{
			throw new IllegalArgumentException("No English transcription found for asset " + assetid);
		}
		
		inSourceTrack.setValue("transcribestatus", "translating");
		captionSearcher.saveData(inSourceTrack);
		inReq.putPageValue("track", inSourceTrack);
		
		Collection sourceCaptions = inSourceTrack.getValues("captions");
		if(sourceCaptions == null || sourceCaptions.isEmpty())
		{
			throw new IllegalArgumentException("No captions found in transcription for asset " + assetid);
		}
		
		TranslationManager translationManager = (TranslationManager) getMediaArchive(inReq).getBean("translationManager");
		
		Map<String, Collection<Map>> translatedCaptions = new HashMap<>();
		
		for (Iterator iterator2 = sourceCaptions.iterator(); iterator2.hasNext();) {
			
			Map caption = (Map) iterator2.next();
			
			String cliplabel = (String) caption.get("cliplabel");
			
			Map translations = translationManager.translatePlainText(sourceLang, targetLangs, cliplabel);
			
		
			for (Iterator iterator = translations.keySet().iterator(); iterator.hasNext();) {

				
				String lang = (String) iterator.next();
				
				Collection<Map> captionList = translatedCaptions.get(lang);
				if(captionList == null)
				{
					captionList = new ArrayList<>();
					translatedCaptions.put(lang, captionList);
				}
				
				String translatedCliplabel = (String) translations.get(lang);
				
				if(translatedCliplabel == null || translatedCliplabel.isEmpty())
				{
					continue;
				}
				
				Map newCaption = new HashMap();
				newCaption.put("cliplabel", translatedCliplabel);
				newCaption.put("timecodestart", caption.get("timecodestart"));
				newCaption.put("timecodelength", caption.get("timecodelength"));
				
				captionList.add(newCaption);
			}
		}
		
		Collection<Data> translatedTracks = new ArrayList<>();
		
		for (Iterator iterator = translatedCaptions.keySet().iterator(); iterator.hasNext();) {
			
			String lang = (String) iterator.next();
			Collection<Map> captions = translatedCaptions.get(lang);
			
			Data inTrack = captionSearcher.createNewData();
			
			inTrack.setValue("captions", captions);
			inTrack.setProperty("assetid", assetid);
			inTrack.setValue("length", inSourceTrack.getValue("length"));
			inTrack.setValue("requesteddate", new Date());
			inTrack.setValue("completeddate", new Date());
			inTrack.setValue("sourcelang", lang);
			inTrack.setValue("transcribestatus", "complete");
			translatedTracks.add(inTrack);
		}
		captionSearcher.saveAllData(translatedTracks, null);
		
		inSourceTrack.setValue("transcribestatus", "complete");
		captionSearcher.saveData(inSourceTrack);
		
		inReq.putPageValue("track", inSourceTrack);

	}

}
