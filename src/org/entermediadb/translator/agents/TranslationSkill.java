package org.entermediadb.translator.agents;

import java.util.Collection;
import java.util.Map;

import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.translator.TranslationManager;
import org.openedit.data.PropertyDetail;

public class TranslationSkill extends BaseSkill
{

	protected TranslationManager getTranslationManager()
	{
		return (TranslationManager) getMediaArchive().getBean("translationManager");
	}

	@Override
	public void process(AgentContext inContext)
	{
		InformaticsContext mycontext = new InformaticsContext(inContext);

		// Process Assets
		Collection pageofhits = mycontext.getAssetsToProcess();
		if (pageofhits == null || pageofhits.isEmpty())
		{
			pageofhits = mycontext.getRecordsToProcess();
		}
		if (pageofhits != null && !pageofhits.isEmpty())
		{
			// Process Assets or Records
			long startTime = System.currentTimeMillis();
			Collection<PropertyDetail> detailstotranslate = (Collection<PropertyDetail>) mycontext.getContextValue("detailsToTranslate");
			getTranslationManager().translateDataFields(pageofhits, detailstotranslate);
			long duration = System.currentTimeMillis() - startTime;
			mycontext.info("Translated: " + pageofhits.size() + " items took " + (duration > 1000L ? duration / 1000L + "s" : duration + " ms"));
		}
		else
		{
			// nullchecks sourceLang , targetLangs , text
			String sourceLang = (String) inContext.getContextValue("sourceLang");
			Collection<String> targetLangs = (Collection<String>) inContext.getContextValue("targetLangs");
			String text = (String) inContext.getContextValue("text");

			if (sourceLang == null || targetLangs == null || text == null)
			{
				inContext.info("Nothing to Translate");
				return; // Missing required context values
			}
			Map<String, String> translations = getTranslationManager().translatePlainText(sourceLang, targetLangs, text);

			mycontext.info("Translated text to: " + translations.keySet() + " languages.");

			inContext.addContext("sourcelang", sourceLang);
			inContext.addContext("translations", translations);
		}

		super.process(inContext);
	}

}
