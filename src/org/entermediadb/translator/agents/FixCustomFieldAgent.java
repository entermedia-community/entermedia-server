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

	Pattern pattern = Pattern.compile("@(.*?)/");

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
				fixTranslation(mycontext , credit);
			}
		}
		else
		{
			Map<String, String> translations =
				(Map<String, String>) inContext.getContextValue("translations");
			String sourceLang = (String) inContext.getContextValue("sourceLang");
			String text = (String) inContext.getContextValue("text");
			if (translations == null)
			{
				inContext.info("Nothing to Fix");
				return; // Missing required context values
			}

			LanguageMap credit = new LanguageMap();
			credit.setText(sourceLang , text);
			for (Iterator iterator = translations.keySet().iterator(); iterator.hasNext();)
			{
				String lang = (String) iterator.next();
				credit.setText(lang , translations.get(lang));
			}
			fixTranslation(mycontext , credit);
			Map<String, String> fixedtranslations = new HashMap<>();
			for (Iterator iterator2 = credit.keySet().iterator(); iterator2.hasNext();)
			{
				String lang = (String) iterator2.next();
				String value = credit.getText(lang);
				if (value == null)
				{
					continue;
				}
				fixedtranslations.put(lang , value);

			}
			inContext.addContext("translations" , fixedtranslations);
		}

		super.process(inContext);
	}

	private void fixTranslation(AgentContext inContext, LanguageMap inField)
	{
		if (inField != null)
		{
			// Get "en"
			String englishCredit = inField.getText("en");
			if (englishCredit == null)
			{
				return;
			}

			Matcher matcher = pattern.matcher(englishCredit);

			if (matcher.find())
			{
				// matcher.group(1) gets the text inside the parentheses
				String englishOrg = matcher.group(1).trim();

				LanguageMap orgMap = findOrgLanguageMap(englishOrg);
				int count = 0;
				for (Iterator iterator2 = inField.keySet().iterator(); iterator2.hasNext();)
				{
					String lang = (String) iterator2.next();
					String value = inField.getText(lang);
					if (value == null)
					{
						continue;
					}
					String fixedValue = fixOrganization(value , lang , orgMap);

					if (fixedValue != null)
					{
						inField.setText(lang , fixedValue);
						count++;
					}
				}
				if (count > 0)
				{
					inContext.info("Fixed " + count + " translation(s) for Credit field of "
						+ englishOrg);
				}

			}
		}
	}

	public LanguageMap findOrgLanguageMap(String englishOrg)
	{
		for (MultiValued org : getOrganizations())
		{
			LanguageMap orgName = org.getLanguageMap("name");
			if (orgName != null)
			{
				String orgEnglishName = orgName.getText("en");
				if (englishOrg.equals(orgEnglishName))
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
				return value.replaceAll("@(.*?)/" , "@" + orgName + " / ");
			}
		}
		return value;
	}

}
