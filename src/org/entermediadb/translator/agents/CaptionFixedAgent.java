package org.entermediadb.translator.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.MultiValued;
import org.openedit.modules.translations.LanguageMap;

public class CaptionFixedAgent extends BaseAgent
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
		Collection<MultiValued> pageofhits =
			(Collection<MultiValued>) inContext.getContextValue("hits");

		// or get it from context "translations" then fix it.

		for (MultiValued data : pageofhits)
		{
			LanguageMap caption = data.getLanguageMap("caption");
			if (caption != null)
			{

				// Get "en"
				String englishCaption = caption.getText("en");
				if (englishCaption == null)
				{
					continue;
				}

				Matcher matcher = pattern.matcher(englishCaption);

				if (matcher.find())
				{
					// matcher.group(1) gets the text inside the parentheses
					String englishOrg = matcher.group(1).trim();

					LanguageMap orgMap = findOrgLanguageMap(englishOrg);

					for (Iterator iterator = caption.keySet().iterator(); iterator.hasNext();)
					{
						String lang = (String) iterator.next();
						String value = caption.getText(lang);
						if (value == null)
						{
							continue;
						}
						String fixedValue = fixOrganization(value , lang , orgMap);

						if (fixedValue != null)
						{
							caption.setText(lang , fixedValue);
						}
					}

				}

			}
		}

		super.process(inContext);
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
				return value.replaceAll("@(.*?)/" , "@" + orgName + "/");
			}
		}
		return value;
	}

}
