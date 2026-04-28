package org.entermediadb.asset.generators;

import org.openedit.ModuleManager;
import org.openedit.generators.BaseGenerator;

public abstract class MediaGenerator extends BaseGenerator
{
	protected ModuleManager fieldModuleManager;

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

}
