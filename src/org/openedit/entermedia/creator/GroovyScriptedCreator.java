package org.openedit.entermedia.creator;

import java.util.HashMap;
import java.util.Map;

import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.ModuleManager;
import com.openedit.entermedia.scripts.GroovyScriptRunner;
import com.openedit.entermedia.scripts.Script;
import com.openedit.entermedia.scripts.ScriptManager;
import com.openedit.page.Page;

public class GroovyScriptedCreator extends BaseImageCreator
{
	protected ScriptManager fieldScriptManager;
	protected String fieldScriptName;
	protected ModuleManager fieldModuleManager;
    protected ThreadLocal perThreadCache = new ThreadLocal();

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public ScriptManager getScriptManager()
	{
		return fieldScriptManager;
	}

	public void setScriptManager(ScriptManager inScriptManager)
	{
		fieldScriptManager = inScriptManager;
	}

	public String getScriptName()
	{
		return fieldScriptName;
	}

	public void setScriptName(String inScriptName)
	{
		fieldScriptName = inScriptName;
	}

	public boolean canReadIn(MediaArchive inArchive, String inInputType)
	{
		MediaCreator creator = loadMediaCreator(inArchive.getCatalogId());
		return creator.canReadIn(inArchive, inInputType);
	}
	
	protected MediaCreator loadMediaCreator(String inCatalogId)
	{
		Map ref = (Map) perThreadCache.get(); //one per thread please
		if( ref == null)
		{
		 	ref = new HashMap();
		  	// use weak reference to prevent cyclic reference during GC
		   perThreadCache.set(ref);
		}
		MediaCreator creator = (MediaCreator)ref.get(inCatalogId); 
		if( creator == null)
		{
			Script script = getScriptManager().loadScript("/" + inCatalogId + "/events/scripts/conversions/creators/" + getScriptName() + "Creator.groovy");
			GroovyScriptRunner runner = (GroovyScriptRunner)getModuleManager().getBean("groovyScriptRunner");
			creator = (MediaCreator)runner.newInstance(script);
			
		    ref.put(inCatalogId,creator);
		}
	     
		return creator;
	}

	public ConvertResult convert(MediaArchive inArchive, Asset inAsset, Page inOut, ConvertInstructions inStructions)
	{
		MediaCreator creator = loadMediaCreator(inArchive.getCatalogId());
		return creator.convert(inArchive, inAsset, inOut, inStructions);
	}
	public ConvertInstructions createInstructions(Map inProperties, MediaArchive inArchive, String inOutputType, String inSourcePath)
	{
		MediaCreator creator = loadMediaCreator(inArchive.getCatalogId());
		return creator.createInstructions(inProperties, inArchive, inOutputType, inSourcePath);
	}

	public String populateOutputPath(MediaArchive inArchive, ConvertInstructions inStructions)
	{
		MediaCreator creator = loadMediaCreator(inArchive.getCatalogId());
		return creator.populateOutputPath(inArchive, inStructions);
	}
	
	public ConvertResult updateStatus(MediaArchive inArchive,Data inTask, Asset inAsset,ConvertInstructions inStructions )
	{
		MediaCreator creator = loadMediaCreator(inArchive.getCatalogId());
		return creator.updateStatus(inArchive, inTask, inAsset, inStructions);
	}

}
