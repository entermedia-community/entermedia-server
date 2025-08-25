package org.entermediadb.translator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.net.HttpSharedConnection;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;
import org.openedit.util.DateStorageUtil;

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
	
	
	
	public void translateAssets(WebPageRequest context, ScriptLogger inLog)
	{
		MediaArchive archive = getMediaArchive();
		Searcher searcher = archive.getAssetSearcher();
		PropertyDetails details = searcher.getPropertyDetails();
		
		HitTracker locales = archive.query("locale").exact("translatemetadata", true).search();
		
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
		
		HitTracker assets = (HitTracker) context.getPageValue("assetsToTranslate");
		
		if( assets == null || assets.isEmpty())
		{
			QueryBuilder query = searcher.query();
			query.exact("previewstatus", "2")
				.exact("taggedbyllm", true)
				.exact("translatesuccess",false)
				.exact("translaterror",false);
	
			String startdate = getMediaArchive().getCatalogSettingValue("ai_metadata_startdate");
			if (startdate != null)
			{
				Date date = DateStorageUtil.getStorageUtil().parseFromStorage(startdate);
				query.after("assetaddeddate", date);
			}
			
			assets = query.search();
			assets.enableBulkOperations();
		}
		
		
		

		if(assets.isEmpty())
		{
			inLog.info("No asset found for Metadata Translation");
			return;
		}

		Collection<String> checkfields = Arrays.asList(
			"headline",
			"longcaption",
			"assettitle",
			"alternatetext"
		);

		int count = 1;
		List tosave = new ArrayList();
		for (Iterator iterator = assets.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Asset asset = (Asset) archive.getAssetSearcher().loadData(data);
			inLog.info("Translating asset (" + count + "/" + assets.size() + ") Id: " + asset.getId() + ", " + asset.getName());
			count++;

			try{
				long startTime = System.currentTimeMillis();

				Map fieldsmap = new HashMap();

				for (Iterator iterator2 = checkfields.iterator(); iterator2.hasNext();)
				{
					String inKey = (String) iterator2.next();
					PropertyDetail detail = archive.getAssetPropertyDetails().getDetail(inKey);
					if (detail != null && detail.isMultiLanguage())
					{
						
						Object value = asset.getValue(inKey);
						if(value instanceof String) {
							LanguageMap lm = new LanguageMap();
							lm.setText("en", (String) value);
							value = lm;
						}
						fieldsmap.put(inKey, value);
					}
				} 

				Map<String, LanguageMap> results = translateFields(fieldsmap, "en", targetLangs);

				if(results != null)
				{
					for (Iterator iterator2 = results.keySet().iterator(); iterator2.hasNext();) 
					{
						String key = (String) iterator2.next();
						LanguageMap map = results.get(key);
						Object value = asset.getValue(key);
						if (value instanceof LanguageMap) 
						{
							LanguageMap existing = (LanguageMap) value;
							existing.putAll(map);
							map = existing;
						}
						else {
							map.setText("en", (String) value);
						}
						 
						asset.setValue(key, map);
					}
					inLog.info("Found translation for "+ asset.getId() + ", " + asset.getName());
				}
				
				asset.setValue("translatesuccess", true);
				tosave.add(asset);
				long duration = (System.currentTimeMillis() - startTime) / 1000L;
				inLog.info("Asset translation took: "+duration +"s");
				
			} 
			catch(Exception e){
				inLog.error("Translation Error", e);
				asset.setValue("translaterror", true);
				tosave.add(asset);
				continue;
			}
			if( tosave.size() == 1000)	{
				archive.saveAssets(tosave);
				//searcher.saveAllData(tosave, null);
				inLog.info("Saved: " + tosave.size() + " assets - " + searcher.getSearchType());
				tosave.clear();
			}
			
		}
		archive.saveAssets(tosave);
		inLog.info("Saved: " + tosave.size() + " assets - " + searcher.getSearchType());
		tosave.clear();
	}
	
  
}
