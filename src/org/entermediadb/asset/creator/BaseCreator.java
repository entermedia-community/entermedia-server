package org.entermediadb.asset.creator;

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

public abstract class BaseCreator implements MediaCreator
{
	private static final Log log = LogFactory.getLog(BaseCreator.class);
	protected PageManager fieldPageManager;
	//protected String fieldWaterMarkPath;
	protected Exec fieldExec;
    protected Collection fieldPreProcessors;
	protected Boolean fieldOnWindows;

	public ConvertResult createOutputIfNeeded(ConvertInstructions inStructions)
	{
		if (!inStructions.isForce() )
		{
			ContentItem stub = getPageManager().getRepository().getStub(inStructions.getOutputPath());
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
	@Override
	public ConvertInstructions createInstructions(MediaArchive inArchive, String inSourcePath, Data inPreset, String inOutputType)
	{
		return createInstructions(inArchive,null,null,inSourcePath,inPreset,inOutputType);
	}
	@Override
	public ConvertInstructions createInstructions(MediaArchive inArchive, Asset inAsset,Data inPreset,String inOutputType)
	{
		return createInstructions(inArchive,null,inAsset,null,inPreset,inOutputType);
	}
	/**
	 * Returns an object that has the output path set on it
	 * @param inReq
	 * @param inArchive
	 * @param inPage
	 * @param inSourcePath Should be passed in with the results of $asset.getSourcePathToAttachment("image|video")
	 * @return
	 */
	public ConvertInstructions createInstructions(MediaArchive inArchive, Map inSettings, Asset inAsset, String inSourcePath, Data inPreset,String inOutputType)
	{
		ConvertInstructions inStructions = createNewInstructions(inArchive);
		inStructions.setAssetSourcePath(inSourcePath);
		inStructions.setOutputExtension(inOutputType);
		inStructions.setAsset(inAsset);
		inStructions.loadSettings(inSettings, inPreset);
		return inStructions;
	}

	protected ConvertInstructions createNewInstructions(MediaArchive inArchive)
	{
		return new ConvertInstructions(inArchive);
	}
	
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
	public void addPreProcessor(MediaCreator inCreator)
	{
		getPreProcessors().add(inCreator);
	}
	public void setPreProcessors(Collection inPreProcessors)
	{
		fieldPreProcessors = inPreProcessors;
	}

	protected MediaCreator getPreProcessor(MediaArchive inArchive, String ext)
	{
		if( fieldPreProcessors != null)
		{
			//Loop over the children and find a match
			for (Iterator iterator = getPreProcessors().iterator(); iterator.hasNext();)
			{
				MediaCreator type = (MediaCreator) iterator.next();
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
			MediaCreator type = (MediaCreator) iterator.next();
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
	
	public long getConversionTimeout(MediaArchive inArchive, Asset inAsset){
		long timeout = -1;
		String fileformat = inAsset.get("fileformat");
		if (fileformat!=null && !fileformat.isEmpty()){
			Data format = inArchive.getData("fileformat",fileformat);
			if (format!=null && format.get("conversiontimeout")!=null){
				try{
					timeout = Long.parseLong(format.get("conversiontimeout"));
				}catch(Exception e){}//not handled
			}
		}
		return timeout;
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
	
}
