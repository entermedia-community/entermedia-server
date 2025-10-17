package org.entermediadb.ai.transcriber;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.MultiValued;
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
		
		Searcher captionSearcher = getMediaArchive(inReq).getSearcher("videotrack");
		
		Data inTrack = captionSearcher.query().exact("assetid", inAsset.getId()).searchOne();
		
		if( inTrack == null)
		{
			inTrack = captionSearcher.createNewData();
			inTrack.setProperty("assetid",  inAsset.getId());
			inTrack.setValue("length", inAsset.getValue("length"));
		}
		
		inTrack.setValue("transcribestatus", "needstranscribe");
		inTrack.setValue("requesteddate", new Date());
		inTrack.setValue("sourcelang", "en");
		
		try 
		{
			getTranscriberManager(inReq).transcribe(inAsset, inTrack);
			inTrack.setValue("transcribestatus", "complete");
		}
		catch (Exception e) 
		{
			log.error("Could not transcribe " + inAsset, e);
			inTrack.setValue("transcribestatus", "error");
		}
		finally
		{
			inTrack.setValue("completeddate", new Date());
			captionSearcher.saveData(inTrack);
		}
	}

}
