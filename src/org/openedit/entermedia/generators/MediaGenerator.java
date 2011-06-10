package org.openedit.entermedia.generators;

import com.openedit.ModuleManager;
import com.openedit.generators.BaseGenerator;

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
