package org.entermediadb.translator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;

public class TranslationManager extends InformaticsProcessor implements CatalogEnabled {
	
	private static Log log = LogFactory.getLog(TranslationManager.class);

	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}
	
	public String getCatalogId()
	{
		return fieldCatalogId;
	}
	
	public void setCatalogId(String catalogId)
	{
		fieldCatalogId = catalogId;
	}
	
	
	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
	
	public MediaArchive getMediaArchive()
	{
		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}
	public Map<String, String> translatePlainText(String sourceLang, Collection<String> targetLangs, String text)
	{
		JSONObject translations = translate(text, sourceLang, targetLangs);
		
		Map<String, String> results = new HashMap<String, String>();
		for (Iterator iterator = targetLangs.iterator(); iterator.hasNext();)
		{
			String lang = (String) iterator.next();
			String altLang = null;
			if(lang.equals("zh-Hans"))
			{
				altLang = "zh";
			}
			else if(lang.equals("zh-Hant") || lang.equals("zh_TW"))
			{
				altLang = "zht";
			}
			JSONArray fieldTranslations = (JSONArray) translations.get(lang);
			if(fieldTranslations == null)
			{
				if(altLang != null)
				{
					fieldTranslations = (JSONArray) translations.get(altLang);
				}
				if(fieldTranslations == null)
				{					
					continue;
				}
			}
			String tr = (String) fieldTranslations.get(0);
			results.put(lang, tr);
		}
		
		return results;
		
	}
	
	public void translateAssets(WebPageRequest context, ScriptLogger inLog)
	{
		HitTracker assets = (HitTracker) context.getPageValue("assetsToTranslate");
	
		MultiValued config = (MultiValued)getMediaArchive().getCachedData("informatics", "autotranslate");
		
		Collection<MultiValued> records = new ArrayList(assets);
		processInformaticsOnAssets(inLog, config, records);
	}
	
	public LanguageMap translateField(String field, LanguageMap languageMap, String sourceLang, Collection<String> targetLangs)
	{
		String sourceText = languageMap.getText(sourceLang);
		
		if(sourceText == null || sourceText.equals(""))
		{
			return languageMap;
		}
		
		Collection<String> validTargets = new ArrayList();
		
		for (Iterator iterator = targetLangs.iterator(); iterator.hasNext();) {
			String target = (String) iterator.next();  
			
			String value = languageMap.getText(target);
			if(value == null || value.equals(""))
			{				
				validTargets.add(target);
			}
				 
		}
		if(validTargets.isEmpty())
		{
			return languageMap;
		}
		
		JSONObject translations = translate(sourceText, sourceLang, validTargets);
		if(translations == null)
		{
			return languageMap;
		}
		
		for (Iterator iterator = validTargets.iterator(); iterator.hasNext();)
		{
			String lang = (String) iterator.next();
			JSONArray fieldTranslations = (JSONArray) translations.get(lang);
			if(fieldTranslations == null || fieldTranslations.isEmpty())
			{
				continue;
			}
			String value = (String) fieldTranslations.get(0);
			languageMap.setText(lang, value);
			
		}
		
		return languageMap;
	}
	
	public JSONObject translate(String text, String sourceLang, Collection<String> targetLangs)
	{
		JSONObject payload = new JSONObject();
		
		JSONArray q = new JSONArray();
		q.add(text);
		payload.put("q", q);
		
		payload.put("source", sourceLang);
		
		JSONArray targets = new JSONArray();
		targets.addAll(targetLangs);
		payload.put("target", targets);
		
		log.info("Translating " + q + " from " + sourceLang + " to " + targetLangs);
		
		LlmConnection connection = getMediaArchive().getLlmConnection("translateFields");
		
		LlmResponse resp = connection.callJson("/translate", payload);
		
		JSONObject translatedText = (JSONObject) resp.getRawResponse().get("translatedText");
			
		return translatedText;
		
	}
	
	private Map<String, LanguageMap> processTranslations(JSONObject translations, ArrayList<String> fieldNames, Map<String, LanguageMap> sourceLangMap, Collection<String> targetLangs)
	{

//		{
//			"es": [ "hola" ],
//			"fr": [ "bonjour" ]
//		}
		for (int i = 0; i < fieldNames.size(); i++)
		{
			String field = fieldNames.get(i);
			log.info("Translations for: " + field);
			
			LanguageMap lm = new LanguageMap();
			
			for (Iterator iterator = targetLangs.iterator(); iterator.hasNext();)
			{
				String lang = (String) iterator.next();
				JSONArray fieldTranslations = (JSONArray) translations.get(lang);
				if(fieldTranslations == null || fieldTranslations.isEmpty())
				{
					continue;
				}
				String value = (String) fieldTranslations.get(i);
				if(value != null)
				{					
					lm.setText(lang, value);
				}
			}

			sourceLangMap.put(field, lm);
		}
		
		return sourceLangMap;
	}
	@Override
	public void processInformaticsOnAssets(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inAssets)
	{
		processInformaticsOnEntities(inLog,inConfig,inAssets);
	}
	
	@Override
	public void processInformaticsOnEntities(ScriptLogger inLog, MultiValued inConfig, Collection<MultiValued> inRecordsToTranslate)
	{

		HitTracker locales = getMediaArchive().query("locale").exact("translatemetadata", true).search();
		
		if (locales.size() == 1 && "en".equals(locales.get(0).getId())) 
		{
			return;
		}

		Collection<String> availableTargets = Arrays.asList("en,es,fr,de,ar,pt,bn,hi,ur,ru,zh,zht,sw".split(","));
		
		Collection<String> targetLangs = new ArrayList();

		for (Iterator iterator = locales.iterator(); iterator.hasNext();) 
		{
			Data locale = (Data) iterator.next();
			String code = locale.getId();
			if(code == "en")
			{
				continue;
			}
			if ("zh-Hans".equals(code))
			{
				code = "zh";
			}
			else if ("zh_TW".equals(code))
			{
				code = "zht";
			}
			if(availableTargets.contains(code))
			{
				targetLangs.add(code);
			}
		}
		
		inLog.headline("Translating " + inRecordsToTranslate.size() + " record(s)");

		int count = 1;
		for (Iterator iterator = inRecordsToTranslate.iterator(); iterator.hasNext();)
		{
			MultiValued data = (MultiValued) iterator.next();
			String moduleid = data.get("entitysourcetype");
			if(moduleid == null || moduleid.isEmpty())
			{
				moduleid = "asset";
			}
			inLog.info("Translating (" + count + "/" + inRecordsToTranslate.size() + ") type: " + moduleid + ", " + data.getName());
			count++;

			long startTime = System.currentTimeMillis();

			Collection<PropertyDetail> detailsfields = loadActiveDetails(moduleid);
			
			Map<String, LanguageMap> results = new HashMap();
			
			for (Iterator iterator2 = detailsfields.iterator(); iterator2.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator2.next();
				String inKey = detail.getId();
				
				if (detail != null && detail.isMultiLanguage())
				{
					LanguageMap value = data.getLanguageMap(inKey);
					if (value != null && value.getText("en") != null && !value.getText("en").isEmpty())
					{
						inLog.info("Translating field: " + inKey);
						LanguageMap translated = translateField(inKey, value, "en", targetLangs);
						results.put(inKey, translated);
					}
				}
			} 

			try
			{
				if(results.size() > 0)
				{
					for (Iterator iterator2 = results.keySet().iterator(); iterator2.hasNext();) 
					{
						String key = (String) iterator2.next();
						LanguageMap map = results.get(key);
						LanguageMap value = data.getLanguageMap(key);
						value.putAll(map);
						data.setValue(key, value);
					}
				}
				long duration = System.currentTimeMillis() - startTime;
				inLog.info("Total translation took: " + duration + "ms");
			} 
			catch(Exception e){
				inLog.error("Translation Error", e);
				continue;
			}
		}
		
	}

  
}
