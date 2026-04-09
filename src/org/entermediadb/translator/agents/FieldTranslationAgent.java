package org.entermediadb.translator.agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
import org.openedit.hittracker.HitTracker;
import org.openedit.modules.translations.LanguageMap;

public class FieldTranslationAgent extends BaseAgent
{

	@Override
	public void process(AgentContext inContext)
	{
		Collection<MultiValued> pageofhits =
				(Collection<MultiValued>) inContext.getContextValue("hits");
		translateDataFields(inContext, pageofhits);
		super.process(inContext);
	}


	public void translateDataFields(AgentContext inConfig,
			Collection<MultiValued> inRecordsToTranslate)
	{
		HitTracker locales =
				getMediaArchive().query("locale").exact("translatemetadata", true).cachedSearch();

		if (locales.size() == 1 && "en".equals(locales.get(0).getId()))
		{
			return;
		}

		Collection<String> availableTargets =
				Arrays.asList("en,es,fr,de,ar,pt,bn,hi,ur,ru,zh,zht,sw".split(","));

		Collection<String> targetLangs = new ArrayList();

		for (Iterator iterator = locales.iterator(); iterator.hasNext();)
		{
			Data locale = (Data) iterator.next();
			String code = locale.getId();
			if (code == "en")
			{
				continue;
			}
			if ("zh-Hans".equals(code))
			{
				code = "zh";
			}
			else
				if ("zh_TW".equals(code))
				{
					code = "zht";
				}
			if (availableTargets.contains(code))
			{
				targetLangs.add(code);
			}
		}

		// inLog.headline("Translating " + inRecordsToTranslate.size() + " record(s)");

		int count = 1;
		for (Iterator iterator = inRecordsToTranslate.iterator(); iterator.hasNext();)
		{
			MultiValued data = (MultiValued) iterator.next();
			String moduleid = data.get("entitysourcetype");
			if (moduleid == null || moduleid.isEmpty())
			{
				moduleid = "asset";
			}
			// inLog.info("Translating (" + count + "/" + inRecordsToTranslate.size() + ") type: " +
			// moduleid + ", " + data.getName());
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
					if (value != null && value.getText("en") != null
							&& !value.getText("en").isEmpty())
					{
						// inLog.info("Translating field: " + inKey);
						LanguageMap translated = translateField(inKey, value, "en", targetLangs);
						results.put(inKey, translated);
					}
				}
			}

			try
			{
				if (results.size() > 0)
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
				// inLog.info("Total translation took: " + duration + "ms");
			}
			catch (Exception e)
			{
				// inLog.error("Translation Error", e);
				continue;
			}
		}

	}


	public JSONObject translate(String text, String sourceLang, Collection<String> targetLangs)
	{
		if (text == null || text.isEmpty())
		{
			throw new OpenEditException("Text to translate cannot be null or empty");
		}
		if (sourceLang == null || sourceLang.isEmpty())
		{
			throw new OpenEditException("Source language cannot be null or empty");
		}
		if (targetLangs == null || targetLangs.size() == 0)
		{
			throw new OpenEditException("Target languages cannot be null or empty");
		}

		JSONObject payload = new JSONObject();

		JSONArray q = new JSONArray();
		q.add(text);
		payload.put("q", q);

		payload.put("source", sourceLang);

		JSONArray targets = new JSONArray();
		targets.addAll(targetLangs);
		payload.put("target", targets);

		// log.info("Translating " + q + " from " + sourceLang + " to " + targetLangs);

		LlmConnection connection = getMediaArchive().getLlmConnection("translateFields");

		LlmResponse resp = connection.callJson("/translate", payload);

		JSONObject translatedText = (JSONObject) resp.getRawResponse().get("translatedText");

		return translatedText;

	}


	public LanguageMap translateField(String field, LanguageMap languageMap, String sourceLang,
			Collection<String> targetLangs)
	{
		String sourceText = languageMap.getText(sourceLang);

		if (sourceText == null || sourceText.equals(""))
		{
			return languageMap;
		}

		Collection<String> validTargets = new ArrayList();

		for (Iterator iterator = targetLangs.iterator(); iterator.hasNext();)
		{
			String target = (String) iterator.next();

			String value = languageMap.getText(target);
			if (value == null || value.equals(""))
			{
				validTargets.add(target);
			}

		}
		if (validTargets.isEmpty())
		{
			return languageMap;
		}

		JSONObject translations = translate(sourceText, sourceLang, validTargets);
		if (translations == null)
		{
			return languageMap;
		}

		for (Iterator iterator = validTargets.iterator(); iterator.hasNext();)
		{
			String lang = (String) iterator.next();
			JSONArray fieldTranslations = (JSONArray) translations.get(lang);
			if (fieldTranslations == null || fieldTranslations.isEmpty())
			{
				continue;
			}
			String value = (String) fieldTranslations.get(0);
			languageMap.setText(lang, value);

		}

		return languageMap;
	}

}
