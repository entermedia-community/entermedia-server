package org.entermediadb.translator.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.MultiValued;
import org.openedit.modules.translations.LanguageMap;

public class FixCustomFieldAgent extends BaseAgent
{

	Collection<MultiValued> fieldOrganizations;

	public Collection<MultiValued> getOrganizations()
	{
		if (fieldOrganizations == null)
		{
			fieldOrganizations = new ArrayList(getMediaArchive().getList("creditorg"));

		}
		return fieldOrganizations;
	}

	Pattern pattern = Pattern.compile("\\u00A9(.*?)/"); // \\u00A9 is the Unicode for the © symbol

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

			Matcher matcher = pattern.matcher(sourceFieldValue);

			if (matcher.find())
			{
				// matcher.group(1) gets the text inside the parentheses
				String matchedText = matcher.group(1).trim();

				LanguageMap orgMap = findOrgLanguageMap(inSourceLang, matchedText);
				int count = 0;
				for (Iterator iterator2 = inField.keySet().iterator(); iterator2.hasNext();)
				{
					String lang = (String) iterator2.next();
					String value = inField.getText(lang);
					if (value == null)
					{
						continue;
					}
					String fixedValue = fixOrganization(value, lang, orgMap);

					if (fixedValue != null)
					{
						inField.setText(lang, fixedValue);
						count++;
					}
				}
				if (count > 0)
				{
					inContext.info("Fixed " + count + " translation(s) for custom field of " + matchedText);
				}

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
				String foundName = orgName.getText(inSourceLang);
				if (englishOrg.equals(foundName))
				{
					return orgName;
				}
			}
		}
		return null;
	}

	public String fixOrganization(String value, String lang, LanguageMap orgMap)
	{
		if (orgMap != null)
		{
			String orgName = orgMap.getText(lang);
			if (orgName != null)
			{
				String fixedValue = value.replaceAll("©(.*?)/", "©" + orgName + " /");
				return fixedValue;
			}
		}
		return value;
	}

}
