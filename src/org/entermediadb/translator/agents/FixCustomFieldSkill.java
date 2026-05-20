package org.entermediadb.translator.agents;

import java.util.ArrayList;
import java.util.Collection;
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

			Boolean fixed = fixTranslation(mycontext, inSourceLang, pageofhits);
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

	private Boolean fixTranslation(InformaticsContext inContext, String inSourceLang, Collection<MultiValued> inRecords)
	{
		boolean found = false;
		for (MultiValued record : inRecords)
		{
			LanguageMap inField = record.getLanguageMap("credit");

			if (inField != null)
			{
				// Get source language
				String sourceFieldValue = inField.getText(inSourceLang);
				if (sourceFieldValue == null)
				{
					continue;
				}

				Matcher matcher = pattern.matcher(sourceFieldValue);

				if (!matcher.find())
				{
					continue;
				}

				String matchedSourceText = matcher.group(1).trim();

				LanguageMap orgMap = findOrgLanguageMap(inSourceLang, matchedSourceText);

				if (orgMap == null)
				{
					continue;
				}

				// Collection<String> targetLangs = (Collection<String>) inContext.getContextValue("targetLangs");

				Map<String, LanguageMap> orgLookup = new HashMap<>();

				// int count = 0;
				String inputValue = inField.getText(inSourceLang); // copy same
				String matchedName = orgMap.getText(inSourceLang);

				String randomkey = createRandomKey();
				String fixedValue = inputValue.replace(matchedName, randomkey);
				orgLookup.put(randomkey, orgMap);
				inField.setText(inSourceLang, fixedValue);
				record.setValue("credit", inField);
				inContext.put("orgLookup", orgLookup);
				found = true;
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
							String value = inField.getText(lang);
							String fixedValue = fixOrganization(sourceFieldValue, lang, orgMap);
							inField.setText(lang, fixedValue);
						}

						break;
					}
				}
				// inContext.log(inField.toJson());
				record.setValue("credit", inField);
				inContext.info("Fixed: " + inField.size() + " translations on field credit");
			}
		}
	}

	// Generate a 10 character uppercase keys randomly for each org name
	public String createRandomKey()
	{
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		StringBuilder key = new StringBuilder();
		for (int i = 0; i < 10; i++)
		{
			int randomIndex = (int) (Math.random() * chars.length());
			key.append(chars.charAt(randomIndex));
		}
		return key.toString();
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
				String fixedValue = value.replaceAll("©(.*?)/", "© " + orgName + "/");
				return fixedValue;
			}
			else
			{
				// default to english
				orgName = orgMap.getText("en");
				String fixedValue = value.replaceAll("©(.*?)/", "© " + orgName + "/");
				return fixedValue;
			}
		}
		return value;
	}

}
