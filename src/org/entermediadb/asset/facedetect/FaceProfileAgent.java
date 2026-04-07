package org.entermediadb.asset.facedetect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.asset.Asset;
import org.openedit.MultiValued;

public class FaceProfileAgent extends BaseAgent {
	public FaceProfileManager getFaceProfileManager() {
		FaceProfileManager manager = (FaceProfileManager) getMediaArchive().getBean("faceProfileManager");
		return manager;
	}

	@Override
	public void process(AgentContext inContext) {
		InformaticsContext mycontext = new InformaticsContext(inContext);

		Collection<Asset> assets = mycontext.getAssetsToProcess();
		if (assets != null && !assets.isEmpty()) {
			Collection<Asset> workinghits = new ArrayList(assets);

			getFaceProfileManager().processAssets(inContext.getScriptLogger(),
					mycontext.getCurrentAgentEnable().getAgentData(), assets);

			for (Iterator iterator2 = assets.iterator(); iterator2.hasNext();) {
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
