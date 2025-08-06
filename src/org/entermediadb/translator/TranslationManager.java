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
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.ModuleManager;
import org.openedit.modules.translations.LanguageMap;

public class TranslationManager implements CatalogEnabled {
	
	private static Log log = LogFactory.getLog(TranslationManager.class);

	protected HttpSharedConnection connection;
	
	protected String fieldCatalogId;
	protected MediaArchive fieldMediaArchive;
	protected ModuleManager fieldModuleManager;
	
	protected HttpSharedConnection getConnection()
	{

		connection = new HttpSharedConnection();

		return connection;
	}
	
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
		ArrayList<String> texts = new ArrayList<>(Arrays.asList(text));

		JSONObject translations = translateFields(texts, sourceLang, targetLangs);
		
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
	public LanguageMap translateField(String field, LanguageMap languageMap, String sourceLang, Collection<String> targetLangs)
	{
		ArrayList<String> fieldNames = new ArrayList();
		fieldNames.add(field);
		ArrayList<String> fieldValues = new ArrayList();
		fieldValues.add(languageMap.getText(sourceLang));
		
		JSONObject translations = translateFields(fieldValues, sourceLang, targetLangs);
		if(translations == null)
		{
			return null;
		}
		
		for (Iterator iterator = targetLangs.iterator(); iterator.hasNext();)
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
	public Map<String, LanguageMap> translateFields(Map fields, String sourceLang, Collection<String> targetLangs)
	{
		ArrayList<String> fieldNames = new ArrayList();
		ArrayList<String> fieldValues = new ArrayList();
		
		Map<String, LanguageMap> sourceLangMap = new HashMap<String, LanguageMap>();
		
		for (Iterator iterator = fields.keySet().iterator(); iterator.hasNext();) 
		{
			String key = (String) iterator.next();

			LanguageMap valueMap = (LanguageMap) fields.get(key);
			if(valueMap != null)
			{				
				String value = valueMap.getText(sourceLang);
				if(value != null && !value.equals(""))
				{				
					fieldNames.add(key);
					fieldValues.add(value);
					LanguageMap lm = new LanguageMap();
					lm.setText(sourceLang, value);
					sourceLangMap.put(key, lm);
				}
			}
		}
		
		try {
			if(fieldValues.isEmpty())
			{
				return null;
			}
			JSONObject translations = translateFields(fieldValues, sourceLang, targetLangs);
			if(translations == null)
			{
				return null;
			}
			
			
			return processTranslations(translations, fieldNames, sourceLangMap, targetLangs);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
	
	public JSONObject translateFields(ArrayList<String> texts, String sourceLang, Collection<String> targetLangs)
	{
		JSONObject payload = new JSONObject();
		
		JSONArray q = new JSONArray();
		
		for (Iterator iterator = texts.iterator(); iterator.hasNext();) {
			String text = (String) iterator.next();
			q.add(text);
		}
		
		payload.put("q", q);
		payload.put("source", sourceLang);
		
		JSONArray targets = new JSONArray();
		
		for (Iterator iterator = targetLangs.iterator(); iterator.hasNext();) {
			String target = (String) iterator.next();
			targets.add(target);
		}
		payload.put("target", targets);
		
		log.info("Translating " + q + " from " + sourceLang + " to " + targetLangs);

		String translationserver = getMediaArchive().getCatalogSettingValue("ai_translate_server");
		String endpoint = translationserver +"/translate";
		
		try {			
			HttpPost method = new HttpPost(endpoint);
			method.setHeader("Content-Type", "application/json");
			method.setEntity(new StringEntity(payload.toJSONString(), "UTF-8"));
			
			CloseableHttpResponse resp = getConnection().sharedExecute(method);
			JSONObject json = getConnection().parseJson(resp);
			
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
  
}
