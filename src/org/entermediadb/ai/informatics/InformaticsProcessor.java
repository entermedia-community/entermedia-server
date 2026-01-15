package org.entermediadb.ai.informatics;


import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.classify.ClassifyManager;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.MultiValued;

public abstract class InformaticsProcessor extends BaseAiManager 
{
	private static final Log log = LogFactory.getLog(ClassifyManager.class);

	public abstract void processInformaticsOnAssets(ScriptLogger inLog,MultiValued inConfig, Collection<MultiValued> assets );
	public abstract void processInformaticsOnEntities(ScriptLogger inLog,MultiValued inConfig, Collection<MultiValued> records );
	
}
