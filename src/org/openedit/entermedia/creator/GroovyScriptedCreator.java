package org.openedit.entermedia.creator;

import java.util.Collection;
import java.util.Map;

import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.util.Exec;


public class GroovyScriptedCreator implements MediaCreator
{

	@Override
	public boolean canReadIn(MediaArchive inArchive, String inInputType)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ConvertInstructions createInstructions(Map inProperties, MediaArchive inArchive, String inOutputType, String inSourcePath)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConvertInstructions createInstructions(WebPageRequest inReq, MediaArchive inArchive, String inOputputype, String inSourcePath)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String populateOutputPath(MediaArchive inArchive, ConvertInstructions inStructions)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String populateOutputPath(MediaArchive inArchive, ConvertInstructions inStructions, Data inPreset)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page createOutput(MediaArchive inArchive, ConvertInstructions inStructions)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConvertResult convert(MediaArchive inArchive, Asset inAsset, Page inOut, ConvertInstructions inStructions)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConvertResult updateStatus(MediaArchive inArchive, Data inTask, Asset inAsset, ConvertInstructions inStructions)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPageManager(PageManager inPageManager)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setExec(Exec inExec)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPreProcessors(Collection inList)
	{
		// TODO Auto-generated method stub
		
	}
//	
//	private static final Log log = LogFactory.getLog(GroovyScriptedCreator.class);
//
//	protected ScriptManager fieldScriptManager;
//	protected String fieldScriptName;
//	protected ModuleManager fieldModuleManager;
//    protected ThreadLocal perThreadCache = new ThreadLocal();
//    protected PageManager fieldPageManager;
//    protected Collection fieldPreProcessors;
//    
//    public GroovyScriptedCreator()
//    {
//    	
//    }
//    public Collection getPreProcessors()
//	{
//		return fieldPreProcessors;
//	}
//
//	public void setPreProcessors(Collection inPreProcessors)
//	{
//		fieldPreProcessors = inPreProcessors;
//	}
//
//	public PageManager getPageManager()
//	{
//		return fieldPageManager;
//	}
//
//	public Exec getExec()
//	{
//		return fieldExec;
//	}
//
//	protected Exec fieldExec;
//    
//	public ModuleManager getModuleManager()
//	{
//		return fieldModuleManager;
//	}
//
//	public void setModuleManager(ModuleManager inModuleManager)
//	{
//		fieldModuleManager = inModuleManager;
//	}
//
//	public ScriptManager getScriptManager()
//	{
//		return fieldScriptManager;
//	}
//
//	public void setScriptManager(ScriptManager inScriptManager)
//	{
//		fieldScriptManager = inScriptManager;
//	}
//
//	public String getScriptName()
//	{
//		return fieldScriptName;
//	}
//
//	public void setScriptName(String inScriptName)
//	{
//		fieldScriptName = inScriptName;
//	}
//
//	public boolean canReadIn(MediaArchive inArchive, String inInputType)
//	{
//		MediaCreator creator = loadMediaCreator(inArchive.getCatalogId());
//		return creator.canReadIn(inArchive, inInputType);
//	}
//	
//	protected MediaCreator loadMediaCreator(String inCatalogId)
//	{
//		Map ref = (Map) perThreadCache.get(); //one per thread please
//		if( ref == null)
//		{
//			synchronized (this)
//			{
//				ref = (Map) perThreadCache.get(); //one per thread please
//				if( ref == null)
//				{				
//					ref = new HashMap();
//					// use weak reference to prevent cyclic reference during GC
//					perThreadCache.set(ref);
//				}
//			}
//		}
//		MediaCreator creator = (MediaCreator)ref.get(inCatalogId); 
//		if( creator == null)
//		{
//			synchronized (this)
//			{
//				creator = (MediaCreator)ref.get(inCatalogId);
//				if( creator != null)
//				{
//					return creator;
//				}				
//				if( getScriptManager() == null)
//				{
//					log.info("creater requires a script manager");
//					return null;
//				}
//				log.info("created new one per thread " + Thread.currentThread().getId() );
//				Script script = getScriptManager().loadScript("/" + inCatalogId + "/events/scripts/conversions/creators/" + getScriptName() + "Creator.groovy");
//				GroovyScriptRunner runner = (GroovyScriptRunner)getModuleManager().getBean("groovyScriptRunner");
//				creator = (MediaCreator)runner.newInstance(script);
//				creator.setPageManager(getPageManager());
//				creator.setExec(getExec());
//				creator.setPreProcessors(getPreProcessors());
//			    ref.put(inCatalogId,creator);
//			}
//		}
//		return creator;
//	}
//
//	public ConvertResult convert(MediaArchive inArchive, Asset inAsset, Page inOut, ConvertInstructions inStructions)
//	{
//		MediaCreator creator = loadMediaCreator(inArchive.getCatalogId());
//		return creator.convert(inArchive, inAsset, inOut, inStructions);
//	}
//	public ConvertInstructions createInstructions(Map inProperties, MediaArchive inArchive, String inOutputType, String inSourcePath)
//	{
//		MediaCreator creator = loadMediaCreator(inArchive.getCatalogId());
//		//log.info("Create instructions");
//		return creator.createInstructions(inProperties, inArchive, inOutputType, inSourcePath);
//	}
//
//	public String populateOutputPath(MediaArchive inArchive, ConvertInstructions inStructions, Data inPreset)
//	{
//		MediaCreator creator = loadMediaCreator(inArchive.getCatalogId());
//		return creator.populateOutputPath(inArchive, inStructions, inPreset);
//	}
//
//	public String populateOutputPath(MediaArchive inArchive, ConvertInstructions inStructions)
//	{
//		MediaCreator creator = loadMediaCreator(inArchive.getCatalogId());
//		return creator.populateOutputPath(inArchive, inStructions);
//	}
//	
//
//	public ConvertResult updateStatus(MediaArchive inArchive,Data inTask, Asset inAsset,ConvertInstructions inStructions )
//	{
//		MediaCreator creator = loadMediaCreator(inArchive.getCatalogId());
//		return creator.updateStatus(inArchive, inTask, inAsset, inStructions);
//	}
//
//	public ConvertInstructions createInstructions(WebPageRequest inReq, MediaArchive inArchive, String inOputputype, String inSourcePath)
//	{
//		MediaCreator creator = loadMediaCreator(inArchive.getCatalogId());
//		if( creator == null )
//		{
//			return null;
//		}
//		return creator.createInstructions(inReq, inArchive, inOputputype, inSourcePath);
//	}
//
//	public Page createOutput(MediaArchive inArchive, ConvertInstructions inStructions)
//	{
//		MediaCreator creator = loadMediaCreator(inArchive.getCatalogId());
//		return creator.createOutput(inArchive, inStructions);
//	}
//
//	public void setPageManager(PageManager inPageManager)
//	{
//		fieldPageManager = inPageManager;
//		
//	}
//
//	public void setExec(Exec inExec)
//	{
//		fieldExec = inExec;
//	}

}
