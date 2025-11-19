package org.entermediadb.translator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.entermediadb.ai.informatics.InformaticsProcessor;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
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
			else if(lang.equals("zh-Hant"))
			{
				altLang = "zt";
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

		String translationserver = getMediaArchive().getCatalogSettingValue("ai_translate_server");
		String endpoint = translationserver +"/translate";
		
		try {			
			HttpPost method = new HttpPost(endpoint);
			method.setHeader("Content-Type", "application/json");
			method.setEntity(new StringEntity(payload.toJSONString(), "UTF-8"));
			
			CloseableHttpResponse resp = getSharedConnection().sharedExecute(method);
			JSONObject json = getSharedConnection().parseJson(resp);
			
			JSONObject translatedText = (JSONObject) json.get("translatedText");
			
			return translatedText;
		} catch (Exception e) {
			// TODO: handle exception
			return null;
		}
		
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
		inLog.headline("Translating metadata from " + inRecordsToTranslate.size() + " entities");

		HitTracker locales = getMediaArchive().query("locale").exact("translatemetadata", true).search();
		
		if (locales.size() == 1 && "en".equals(locales.get(0).getId())) 
		{
				//log.info("No locales found for translation, defaulting to English");
				return; // No locales to translate, so we exit
		}

		Collection<String> availableTargets = Arrays.asList("en,es,fr,de,ar,pt,bn,hi,ur,ru,zh-Hans,zh-Hant".split(","));
		
		Collection<String> targetLangs = new ArrayList();

		for (Iterator iterator = locales.iterator(); iterator.hasNext();) 
		{
			Data locale = (Data) iterator.next();
			String code = locale.getId();
			if(code == "en")
			{
				continue;
			}
			if ("zh".equals(code))
			{
				code = "zh-Hans";
			}
			else if ("zh_TW".equals(code))
			{
				code = "zh-Hant";
			}
			if(availableTargets.contains(code))
			{
				targetLangs.add(code);
			}
		}
		
		log.info("Translating to " + targetLangs);

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

			Map<String, PropertyDetail> detailsfields = loadActiveDetails(moduleid);
			
			Map<String, LanguageMap> results = new HashMap();
			
			for (Iterator iterator2 = detailsfields.keySet().iterator(); iterator2.hasNext();)
			{
				String inKey = (String) iterator2.next();
				PropertyDetail detail = getMediaArchive().getSearcher(moduleid).getDetail(inKey);
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
