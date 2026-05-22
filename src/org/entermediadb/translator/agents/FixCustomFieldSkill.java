package org.entermediadb.translator.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.Asset;
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
			Collections.sort((ArrayList<MultiValued>) fieldOrganizations, (a, b) -> {
				String nameA = a.getName();
				String nameB = b.getName();
				if (nameA.length() > nameB.length())
				{
					return -1;
				}
				else
					if (nameA.length() < nameB.length())
					{
						return 1;
					}
					else
					{
						return 0;
					}

			});

		}
		return fieldOrganizations;
	}

	Pattern pattern = Pattern.compile("\\u00A9(.*?)/"); // matches the © symbol ORG and /

	@Override
	public void process(AgentContext inContext)
	{
		InformaticsContext mycontext = new InformaticsContext(inContext);

		String inSourceLang = (String) inContext.getContextValue("sourceLang");
		if (inSourceLang == null)
		{
			inSourceLang = "en";
		}

		Collection pageofhits = mycontext.getRecordsToProcess();
		if (pageofhits == null || pageofhits.isEmpty())
		{
			if (inContext.getContextValue("records") != null)
			{
				mycontext.setRecordsToProcess((Collection<MultiValued>) inContext.getContextValue("records"));
				pageofhits = mycontext.getRecordsToProcess();
			}
		}
		if (pageofhits == null || pageofhits.isEmpty())
		{
			pageofhits = mycontext.getAssetsToProcess();
		}
		if (pageofhits != null && !pageofhits.isEmpty())
		{

			Boolean fixed = findKeywords(mycontext, inSourceLang, pageofhits);
			if (fixed)
			{
				super.process(mycontext);

				// Replace the random key with the correct org name in all languages after translation is done
				postfixTranslation(mycontext, inSourceLang, pageofhits);
			}
			else
			{
				super.process(inContext);
			}
		}
		else
		{
			super.process(inContext);
		}

	}

	private Boolean findKeywords(InformaticsContext inContext, String inSourceLang, Collection<MultiValued> inRecords)
	{
		boolean found = false;
		for (MultiValued record : inRecords)
		{
			LanguageMap inField = record.getLanguageMap("credit");

			if (inField != null)
			{
				// Get source language
				if (inField.getText(inSourceLang) == null)
				{
					continue;
				}

				// Collection<LanguageMap> orgMap = findOrgLanguageMap(inSourceLang, sourceFieldValue);

				// Collection<String> targetLangs = (Collection<String>) inContext.getContextValue("targetLangs");

				Map<String, LanguageMap> orgLookup = new HashMap<>();
				long counter = 0L;
				for (MultiValued org : getOrganizations())
				{
					LanguageMap orgName = org.getLanguageMap("name");
					if (orgName != null)
					{
						String matchedName = orgName.getText(inSourceLang);
						if (inField.getText(inSourceLang).contains(matchedName))
						{
							String inputValue = inField.getText(inSourceLang); // copy same

							String randomkey = "[__ARGS" + (counter++) + "__]";
							String fixedValue = inputValue.replace(matchedName, randomkey);
							orgLookup.put(randomkey, orgName);
							inField.setText(inSourceLang, fixedValue);
							record.setValue("credit", inField);

							found = true;
						}
					}
				}
				inContext.put("orgLookup", orgLookup);

				// int count = 0;

			}
		}

		return found;
	}

	public void postfixTranslation(AgentContext inContext, String inSourceLang, Collection<MultiValued> inRecords)
	{
		for (MultiValued record : inRecords)
		{
			LanguageMap inField = record.getLanguageMap("credit");

			if (inField != null)
			{
				String sourceFieldValue = inField.getText(inSourceLang);
				if (sourceFieldValue == null)
				{
					continue;
				}

				// inContext.log(inField.toJson());

				Map<String, LanguageMap> orgLookup = (Map<String, LanguageMap>) inContext.getContextValue("orgLookup");

				for (String key : orgLookup.keySet())
				{
					if (sourceFieldValue.contains(key))
					{
						LanguageMap orgMap = orgLookup.get(key);
						for (Iterator iterator2 = inField.keySet().iterator(); iterator2.hasNext();)
						{
							String lang = (String) iterator2.next();
							String translatedValue = inField.getText(lang);
							String staticValue = orgMap.getText(lang);
							String fixedValue = null;
							if (staticValue == null)
							{
								staticValue = orgMap.getText("en");

							}

							fixedValue = translatedValue.replace(key, staticValue);

							// String fixedValue = fixOrganization(translatedValue, lang, orgMap);
							inField.setText(lang, fixedValue);
						}

					}
				}
				// inContext.log(inField.toJson());
				record.setValue("credit", inField);
				inContext.info("Fixed: " + inField.size() + " translations on field credit");
			}
		}
	}

	long counter = 0L;

	// Generate a 10 character uppercase keys randomly for each org name
	public String createRandomKey()
	{
		return "__ARGS" + counter++ + "__";
	}

	public Collection<LanguageMap> findOrgLanguageMap(String inSourceLang, String englishOrg)
	{

		for (MultiValued org : getOrganizations())
		{
			LanguageMap orgName = org.getLanguageMap("name");
			if (orgName != null)
			{
				String foundName = orgName.getText(inSourceLang);
				if (englishOrg.equals(foundName))
				{
					;
				}
			}
		}
		return null;
	}

	public String fixOrganization(String translatedValue, String lang, LanguageMap orgMap)
	{
		if (orgMap != null)
		{
			String orgName = orgMap.getText(lang);
			if (orgName != null)
			{
				String fixedValue = translatedValue.replaceAll("©(.*?)/", "© " + orgName + "/");
				return fixedValue;
			}
			else
			{
				// default to english
				orgName = orgMap.getText("en");
				String fixedValue = translatedValue.replaceAll("©(.*?)/", "© " + orgName + "/");
				return fixedValue;
			}
		}
		return translatedValue;
	}

}
