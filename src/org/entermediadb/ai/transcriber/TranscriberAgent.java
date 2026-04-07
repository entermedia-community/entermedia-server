package org.entermediadb.ai.transcriber;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.openedit.MultiValued;

public class TranscriberAgent extends BaseAgent {
	public TranscriberManager getTranscriberManager() {
		TranscriberManager manager = (TranscriberManager) getMediaArchive().getBean("transcriberManager");
		return manager;
	}

	@Override
	public void process(AgentContext inContext) {
		InformaticsContext mycontext = new InformaticsContext(inContext);

		Collection pageofhits = mycontext.getAssetsToProcess();
		if (pageofhits != null && !pageofhits.isEmpty()) {
			List workinghits = new ArrayList(pageofhits);
			getTranscriberManager().transcribeAssets(inContext.getScriptLogger(),
					mycontext.getCurrentAgentEnable().getAgentData(), mycontext.getAssetsToProcess());
			for (Iterator iterator2 = pageofhits.iterator(); iterator2.hasNext();) {
				MultiValued data = (MultiValued) iterator2.next();
				if (data.getBoolean("llmerror")) {
					workinghits.remove(data); // We do not process more.
				}
			}
			mycontext.setAssetsToProcess(workinghits);
		}
		super.process(mycontext);
	}

}
