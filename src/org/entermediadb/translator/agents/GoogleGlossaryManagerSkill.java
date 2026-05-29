package org.entermediadb.translator.agents;

import java.util.Map;
import org.entermediadb.ai.BaseSkill;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.google.GoogleManager;

public class GoogleGlossaryManagerSkill extends BaseSkill
{

	@Override
	public void process(AgentContext inContext)
	{
		// get googleprohectid, googlelocationid, and glossaryid from inContext.getContextValue() and then
		// call getGoogleManager().createGlossary() with those values
		getGoogleManager().createGlossary(inContext.getContext());

		super.process(inContext);
	}

	protected GoogleManager getGoogleManager()
	{
		return (GoogleManager) getMediaArchive().getBean("googleManager");
	}

}
