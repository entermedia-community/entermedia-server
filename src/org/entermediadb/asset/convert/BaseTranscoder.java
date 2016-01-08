package org.entermediadb.asset.convert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.page.PageSettings;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;
import org.openedit.util.PathUtilities;

public abstract class BaseTranscoder implements MediaTranscoder
{
	private static final Log log = LogFactory.getLog(BaseTranscoder.class);
	protected PageManager fieldPageManager;
	//protected String fieldWaterMarkPath;
	protected Exec fieldExec;
    protected Collection fieldPreProcessors;
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
			asset = createAsset(inStructions.getMediaArchive(), sourcePath); //virtual assets
		}
		if(asset == null){
			asset = createAsset(inStructions.getMediaArchive(), inStructions.getInputPath());
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
	
	public boolean hasPreprocessor()
	{
		return fieldPreProcessors != null && fieldPreProcessors.size() > 0;
	}
	public Collection getPreProcessors()
	{
		if (fieldPreProcessors == null)
		{
			fieldPreProcessors = new ArrayList();
		}

		return fieldPreProcessors;
	}
	public void addPreProcessor(ConversionManager inCreator)
	{
		getPreProcessors().add(inCreator);
	}
	public void setPreProcessors(Collection inPreProcessors)
	{
		fieldPreProcessors = inPreProcessors;
	}

	protected MediaTranscoder getPreProcessor(MediaArchive inArchive, String ext)
	{
		if( fieldPreProcessors != null)
		{
			//Loop over the children and find a match
			for (Iterator iterator = getPreProcessors().iterator(); iterator.hasNext();)
			{
				MediaTranscoder type = (MediaTranscoder) iterator.next();
				if( type.canReadIn(inArchive, ext) )
					return type;
			}
		}
		return null;
	}

	protected boolean canPreProcess(MediaArchive inArchive, String inInput)
	{
		for (Iterator iterator = getPreProcessors().iterator(); iterator.hasNext();)
		{
			MediaTranscoder type = (MediaTranscoder) iterator.next();
			if( type.canReadIn(inArchive, inInput))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * For this to work, inSourcePath needs to have an extention, i.e.
	 * newassets/admin/118/picture.jpg
	 * @param inStructions
	 * @param inSourcePath
	 * @return
	 */
	protected Asset createAsset(MediaArchive inArchive, String inSourcePath)
	{
		String extension = PathUtilities.extractPageType(inSourcePath);
		if (extension != null)
		{
			Asset asset = new Asset(); //throw away
			asset.setCatalogId(inArchive.getCatalogId());
	//		asset.setId(inArchive.getAssetArchive().nextAssetNumber());
			asset.setSourcePath(inSourcePath);
			extension = extension.toLowerCase();
			asset.setProperty("fileformat", extension);
	//		inArchive.saveAsset(asset, null);
			return asset;
		}
		return null;
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
