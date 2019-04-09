package org.entermediadb.asset.convert;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.page.PageSettings;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public abstract class BaseTranscoder implements MediaTranscoder
{
	private static final Log log = LogFactory.getLog(BaseTranscoder.class);
	protected PageManager fieldPageManager;
	//protected String fieldWaterMarkPath;
	protected Exec fieldExec;
//    protected Collection fieldPreProcessors;
	protected Boolean fieldOnWindows;


	public ConvertResult createOutputIfNeeded(ConvertInstructions inStructions)
	{
		if (!inStructions.isForce() )
		{
			//ContentItem stub = getPageManager().getRepository().getStub(inStructions.getOutputPath());
			ContentItem stub = inStructions.getOutputFile();
			if (stub.getLength() > 2)
			{
				ConvertResult result = new ConvertResult();
				result.setOk(true);
				result.setComplete(true);
				return result;
			}
		}
		return createOutput(inStructions);
	}
	/**
	 * Instructions require a completely pre-dermined output page in case we already coverted it
	 * Previously
	 */
	public ConvertResult createOutput(ConvertInstructions inStructions)
	{
		//We need to load up the asset right away
		Asset asset = inStructions.getAsset();
		String sourcePath = inStructions.getAssetSourcePath();
		if (asset == null)
		{
			asset = inStructions.getMediaArchive().getAssetBySourcePath(inStructions.getAssetSourcePath());
		}			
		
		if (asset == null)
		{
			asset = inStructions.getMediaArchive().getAssetImporter().createAsset(inStructions.getMediaArchive(), sourcePath); //virtual assets
		}
		if(asset == null)
		{
			asset = inStructions.getMediaArchive().getAssetImporter().createAsset(inStructions.getMediaArchive(), inStructions.getInputPath());
		}
		if (asset == null)
		{
			return null;
		}
		inStructions.setAsset(asset);
		//The output formats might need to use helpers to get it working
		ConvertResult ok = convert(inStructions);
		if (!ok.isOk())
		{
			//should already be logged missing original?
			log.error("Convert failed " + asset.getSourcePath() + " for " + getClass().getName());
			return null;
		}
		return ok;
	}


	/**
	public ConvertInstructions createInstructions(Page inDef, MediaArchive inArchive, String inOutputType, String inSourcePath)
	{
		Map all = new HashMap();
		for (Iterator iterator = inDef.getPageSettings().getAllProperties().iterator(); iterator.hasNext();)
		{
			PageProperty type = (PageProperty) iterator.next();
			all.put(type.getName(), type.getValue());
		}
		return createInstructions(all, inArchive, inOutputType, inSourcePath);
	}
	*/

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	
	protected boolean runExec(String inCommandName, List<String> inCom) throws OpenEditException
	{
		return runExec(inCommandName, inCom, -1);
	}
	
	protected boolean runExec(String inCommandName, List<String> inCom, long inTimeout) throws OpenEditException
	{
		ExecResult result = getExec().runExec(inCommandName, inCom, inTimeout);
		return result.isRunOk();
	}
	
	
	@Override
	public ConvertResult convertIfNeeded(ConvertInstructions inStructions)
	{
		if( inStructions.isForce() || inStructions.getOutputFile().getLength() < 2)
		{
			//Be aware that if you force ImageMagick to convert this may have the
			//same input and output and zero out the file
			return convert(inStructions);
		}
		ConvertResult result = new ConvertResult();
		result.setInstructions(inStructions);
		result.setOutput(inStructions.getOutputFile());
		result.setComplete(true);
		result.setOk(true);
		return result;

	}

	public Exec getExec() {
		return fieldExec;
	}

	public void setExec(Exec exec) {
		fieldExec = exec;
	}

	public boolean isOnWindows()
	{
		return getOnWindows().booleanValue();
	}

	public Boolean getOnWindows()
	{
		if (fieldOnWindows == null)
		{
			if (System.getProperty("os.name").toUpperCase().contains("WINDOWS"))
			{
				fieldOnWindows = Boolean.TRUE;
			}
			else
			{
				fieldOnWindows = Boolean.FALSE;
			}
			
		}
		return fieldOnWindows;
	}

	protected void createFallBackContent(Page inRealContent, Page inXConf)
	{
		//instead of copying, create magic xconf
		String temp = inXConf.getPath();
		int index = temp.lastIndexOf(".");
		if(index != -1)
		{
			temp = temp.substring(0, index);
			temp= temp + ".xconf";
		}
		PageSettings xconf = getPageManager().getPageSettingsManager().getPageSettings(temp);
		PageProperty fallback = new PageProperty("fallbackcontentpath");
		fallback.setValue(inRealContent.getPath());
		xconf.putProperty(fallback);
		getPageManager().getPageSettingsManager().saveSetting(xconf);
		getPageManager().clearCache(inXConf);
	}
	@Override
	public ConvertResult updateStatus(Data inTask, ConvertInstructions inStructions )
	{
		ConvertResult status = new ConvertResult();
		status.setComplete(true);
		status.setOk(true);
		return status;
	}
	protected void setValue(String inName, String inDefault, ConvertInstructions inStructions, List comm)
	{
		String value = inStructions.get(inName);
		if (value != null || inDefault != null)
		{
			comm.add("-" + inName);
			if (value != null)
			{
				comm.add(value);
			}
			else if (inDefault != null)
			{
				comm.add(inDefault);
			}
		}

	}

}
