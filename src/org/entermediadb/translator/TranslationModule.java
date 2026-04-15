package org.entermediadb.translator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.automation.AutomationManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.ModuleManager;
import org.openedit.WebPageRequest;

public class TranslationModule extends BaseMediaModule
{

	private static final Log log = LogFactory.getLog(TranslationModule.class);

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
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}
		return fieldMediaArchive;
	}

	public AutomationManager getAutomationManager(WebPageRequest inReq)
	{
		AutomationManager manager = (AutomationManager) getMediaArchive(inReq).getBean("automationManager");
		inReq.putPageValue("automationManager", manager);
		return manager;
	}

	public void translateField(WebPageRequest inReq)
	{
		Map params = inReq.getJsonRequest();
		if (params == null)
		{
			log.info("No JSON parameters");
			inReq.putPageValue("status", "No JSON parameters");
			return;
		}
		String sourceLang = (String) params.get("source");
		String targetLangStr = (String) params.get("targets");
		Collection<String> targets = Arrays.asList(targetLangStr.split(","));
		Collection<String> targetLangs = new ArrayList<String>();

		for (Iterator iterator = targets.iterator(); iterator.hasNext();)
		{
			String lang = (String) iterator.next();
			if (lang != null && lang.length() > 1)
			{
				targetLangs.add(lang);
			}
		}

		String text = (String) params.get("text");

		MediaArchive archive = getMediaArchive(inReq);

		// TranslationManager manager = (TranslationManager) archive.getBean("translationManager");
		// Map<String, String> translations = manager.translatePlainText(sourceLang , targetLangs ,
		// text);

		AgentContext context = new AgentContext();
		context.put("sourceLang", sourceLang);
		context.put("targetLangs", targetLangs);
		context.put("text", text);

		getAutomationManager(inReq).runScenario("informatics_translate", context);

		inReq.putPageValue("sourcelang", context.getContextValue("sourceLang"));
		inReq.putPageValue("translations", context.getContextValue("translations"));
	}
}
// [headline, assettitle, keywords, longcaption,
