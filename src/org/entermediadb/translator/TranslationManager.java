package org.entermediadb.translator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
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
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;

public class TranslationManager extends BaseAiManager implements CatalogEnabled
{

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
			fieldMediaArchive =
					(MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}

	public Map<String, String> translatePlainText(String sourceLang, Collection<String> targetLangs,
			String text)
	{
		JSONObject translations = translate(text, sourceLang, targetLangs);

		Map<String, String> results = new HashMap<String, String>();
		for (Iterator iterator = targetLangs.iterator(); iterator.hasNext();)
		{
			String lang = (String) iterator.next();

			String altLang = null;
			if (lang.equals("zh-Hans"))
			{
				altLang = "zh";
			}
			else
				if (lang.equals("zh-Hant") || lang.equals("zh_TW"))
				{
					altLang = "zht";
				}
			JSONArray fieldTranslations = (JSONArray) translations.get(lang);
			if (fieldTranslations == null)
			{
				if (altLang != null)
				{
					fieldTranslations = (JSONArray) translations.get(altLang);
				}
				if (fieldTranslations == null)
				{
					continue;
				}
			}
			String tr = (String) fieldTranslations.get(0);
			results.put(lang, tr);
		}

		return results;

	}



	private Map<String, LanguageMap> processTranslations(JSONObject translations,
			ArrayList<String> fieldNames, Map<String, LanguageMap> sourceLangMap,
			Collection<String> targetLangs)
	{

		// {
		// "es": [ "hola" ],
		// "fr": [ "bonjour" ]
		// }
		for (int i = 0; i < fieldNames.size(); i++)
		{
			String field = fieldNames.get(i);
			log.info("Translations for: " + field);

			LanguageMap lm = new LanguageMap();

			for (Iterator iterator = targetLangs.iterator(); iterator.hasNext();)
			{
				String lang = (String) iterator.next();
				JSONArray fieldTranslations = (JSONArray) translations.get(lang);
				if (fieldTranslations == null || fieldTranslations.isEmpty())
				{
					continue;
				}
				String value = (String) fieldTranslations.get(i);
				if (value != null)
				{
					lm.setText(lang, value);
				}
			}

			sourceLangMap.put(field, lm);
		}

		return sourceLangMap;
	}



	// public void translateAssets(WebPageRequest context, ScriptLogger inLog)
	// {
	// HitTracker assets = (HitTracker) context.getPageValue("assetsToTranslate");
	//
	// MultiValued config = (MultiValued)getMediaArchive().getCachedData("informatics",
	// "autotranslate");
	// Collection<MultiValued> records = new ArrayList(assets);
	//
	//// InformaticsContext agentcontext = new InformaticsContext();
	//// agentcontext.setScriptLogger(inLog);
	//// agentcontext.setAssetsToProcess(records);
	//
	// translateDataFields(inLog, config, records);
	// }



}
