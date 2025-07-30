package org.entermediadb.translator;

import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.ListHitTracker;
import org.openedit.modules.translations.LanguageMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;



public class TranslationModule extends BaseMediaModule {
	
	private static final Log log = LogFactory.getLog(TranslationModule.class);
	
	public void checkForTranslation(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		
		String[] fields = inReq.getRequestParameters("field");
		if (fields == null)
		{
			return;
		}
		String ok = inReq.findValue("save");
		if (!Boolean.parseBoolean(ok))
		{
			log.info("Save was not set to true");
			return;
		}
		
		Asset asset = getAsset(inReq);
		
		if(asset == null)
		{
			return;
		}
		
		Map data = new HashMap();
		
		data.put("translateAsset", asset);
		
		Map<String, String> translateFields = new HashMap<String, String>();
		
		for (int i = 0; i < fields.length; i++)
		{
			String field = fields[i];
			
			String skipfield = inReq.getRequestParameter(field + ".enabletranslation");
			if(skipfield != null)
			{
				String source = inReq.getRequestParameter(field + ".sourcelang");
				if(source != null)
				{
					translateFields.put(field, source);
				}
			}
		}
		
		data.put("translateFields", translateFields);
		
		archive.firePathEvent("llm/translateonedit", inReq.getUser(), data);
	}
}
//[headline, assettitle, keywords, longcaption,