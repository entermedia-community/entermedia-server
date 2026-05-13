package org.entermediadb.translator.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.MultiValued;
import org.openedit.modules.translations.LanguageMap;

public class FixCustomFieldSkill extends BaseSkill
{

	Collection<MultiValued> fieldOrganizations;

	public Collection<MultiValued> getOrganizations()
	{
		if (fieldOrganizations == null)
		{
			fieldOrganizations = new ArrayList(getMediaArchive().getList("creditorg"));

			// Longest should be sorted first.
			Collections.sort((List) fieldOrganizations, new Comparator<MultiValued>() {
				@Override
				public int compare(MultiValued o1, MultiValued o2)
				{
					String name1 = o1.getLanguageMap("name").getText("en");
					String name2 = o2.getLanguageMap("name").getText("en");
					if (name1.length() > name2.length())
					{
						return -1;
					}
					else
						if (name1.length() < name2.length())
						{
							return 1;
						}
					return 0;
				}
			});
		}

		return fieldOrganizations;
	}

	@Override
	public void process(AgentContext inContext)
	{
		InformaticsContext mycontext = new InformaticsContext(inContext);
		Collection pageofhits = mycontext.getAssetsToProcess();

		if (pageofhits == null || pageofhits.isEmpty())
		{
			pageofhits = mycontext.getRecordsToProcess();
		}
		if (pageofhits != null && !pageofhits.isEmpty())
		{
			// Process Assets or Records
			for (Iterator iterator = pageofhits.iterator(); iterator.hasNext();)
			{
				MultiValued data = (MultiValued) iterator.next();

				LanguageMap credit = data.getLanguageMap("credit");
				fixTranslation(mycontext, "en", credit);
			}
		}
		else
		{
			String sourceLang = (String) inContext.getContextValue("sourceLang");
			Collection<String> targetLangs = (Collection<String>) inContext.getContextValue("targetLangs");
			for (Iterator iterator = targetLangs.iterator(); iterator.hasNext();)
			{

			}

			Map<String, String> translations = (Map<String, String>) inContext.getContextValue("translations");
			String sourceLang = (String) inContext.getContextValue("sourceLang");
			String text = (String) inContext.getContextValue("text");
			if (translations == null)
			{
				inContext.info("Nothing to Fix");
				return; // Missing required context values
			}

			LanguageMap field = new LanguageMap();
			field.setText(sourceLang, text);
			for (Iterator iterator = translations.keySet().iterator(); iterator.hasNext();)
			{
				String lang = (String) iterator.next();
				field.setText(lang, translations.get(lang));
			}
			fixTranslation(mycontext, sourceLang, field);
			Map<String, String> fixedtranslations = new HashMap<>();
			for (Iterator iterator2 = field.keySet().iterator(); iterator2.hasNext();)
			{
				String lang = (String) iterator2.next();
				String value = field.getText(lang);
				if (value == null)
				{
					continue;
				}
				fixedtranslations.put(lang, value);

			}
			inContext.getParentContext().addContext("translations", fixedtranslations);
			// inContext.addContext("translations", fixedtranslations);

		}

		super.process(inContext);
	}

	private void preFixTranslation(AgentContext inContext, String inSourceLang, LanguageMap inField)
	{
		if (inField != null)
		{
			// Get source language
			String sourceFieldValue = inField.getText(inSourceLang);
			if (sourceFieldValue == null)
			{
				return;
			}

			LanguageMap orgMap = findOrgLanguageMap(inSourceLang, sourceFieldValue);

			if (orgMap == null)
			{
				return;
			}

			Collection<String> targetLangs = (Collection<String>) inContext.getContextValue("targetLangs");

			int count = 0;
			for (Iterator iterator2 = targetLangs.iterator(); iterator2.hasNext();)
			{
				String lang = (String) iterator2.next();
				String value = inField.getText(inSourceLang); //copy same 

				String matchedName = orgMap.getText(inSourceLang);
				String newOrgName = orgMap.getText(lang);

				if (newOrgName != null)
				{
					// Not matching since value already was translated by AI: "Noticias de la ONU"
					String fixedValue = value.replace(matchedName, newOrgName);
					inField.setText(lang, fixedValue);
					count++;
				}

			}
			if (count > 0)
			{
				inContext.info("Fixed " + count + " translation(s) for custom field of " + sourceFieldValue);
			}

		}
	}

	private void fixTranslation(AgentContext inContext, String inSourceLang, LanguageMap inField)
	{
		if (inField != null)
		{
			// Get source language
			String sourceFieldValue = inField.getText(inSourceLang);
			if (sourceFieldValue == null)
			{
				return;
			}

			LanguageMap orgMap = findOrgLanguageMap(inSourceLang, sourceFieldValue);

			if (orgMap == null)
			{
				return;
			}
			int count = 0;
			for (Iterator iterator2 = inField.keySet().iterator(); iterator2.hasNext();)
			{
				String lang = (String) iterator2.next();
				String value = inField.getText(lang);
				if (value == null)
				{
					continue;
				}
				String matchedName = orgMap.getText(inSourceLang);
				String newOrgName = orgMap.getText(lang);

				if (newOrgName != null)
				{
					// Not matching since value already was translated by AI: "Noticias de la ONU"
					String fixedValue = value.replace(matchedName, newOrgName);
					inField.setText(lang, fixedValue);
					count++;
				}

			}
			if (count > 0)
			{
				inContext.info("Fixed " + count + " translation(s) for custom field of " + sourceFieldValue);
			}

		}
	}

	public LanguageMap findOrgLanguageMap(String inSourceLang, String englishOrg)
	{
		for (MultiValued org : getOrganizations())
		{
			LanguageMap orgName = org.getLanguageMap("name");
			if (orgName != null)
			{
				String dbName = orgName.getText(inSourceLang);
				// Should we ignorecase?

				// UN always win: UN News, UN Photo, UN Photo - John M.
				if (englishOrg.contains(dbName))
				{
					return orgName;
				}
			}
		}
		return null;
	}

}
