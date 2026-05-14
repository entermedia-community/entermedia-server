package org.entermediadb.translator.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.Asset;
import org.openedit.MultiValued;
import org.openedit.data.BaseData;
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
		// Only process assets for now
		if (pageofhits != null && !pageofhits.isEmpty())
		{
			fixTranslation(mycontext, "en", pageofhits);

			/*
			 * String sourceLang = (String) inContext.getContextValue("sourceLang"); String text = (String)
			 * inContext.getContextValue("text"); Asset dataCopy = (Asset) inContext.getCurrentEntity();
			 * Collection<Asset> records = new ArrayList<>(); records.add(dataCopy);
			 * assetonlycontext.setAssetsToProcess(records); fixTranslation(assetonlycontext, sourceLang,
			 * records);
			 */
		}
		else
		{
			super.process(inContext);
		}

	}

	private void fixTranslation(AgentContext inContext, String inSourceLang, Collection<MultiValued> inRecords)
	{
		for (MultiValued record : inRecords)
		{
			LanguageMap inField = record.getLanguageMap("credit");

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

				Map<String, LanguageMap> orgLookup = (Map<String, LanguageMap>) inContext.getContextValue("orgLookup");

				int count = 0;
				String inputValue = inField.getText(inSourceLang); // copy same
				String matchedName = orgMap.getText(inSourceLang);

				// Not matching since value already was translated by AI: "Noticias de la ONU"
				String randomkey = createRandomKey();
				String fixedValue = inputValue.replace(matchedName, randomkey);
				orgLookup.put(randomkey, orgMap);
				inField.setText(inSourceLang, fixedValue);
				super.process(inContext);

			}
		}
	}

	// Generate a 10 character uppercase keys randomly for each org name to be replaced in the
	// translation and then replaced back after translation is done. This is to avoid the issue of AI
	// translating the org name which we want to keep as is.
	String createRandomKey()
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
