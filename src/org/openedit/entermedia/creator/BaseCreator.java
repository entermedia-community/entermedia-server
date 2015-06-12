package org.openedit.entermedia.creator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.PageProperty;
import com.openedit.page.PageSettings;
import com.openedit.page.manage.PageManager;
import com.openedit.util.Exec;
import com.openedit.util.ExecResult;
import com.openedit.util.PathUtilities;

public abstract class BaseCreator implements MediaCreator
{
	private static final Log log = LogFactory.getLog(BaseCreator.class);

	protected PageManager fieldPageManager;
	protected String fieldWaterMarkPath;
	protected Exec fieldExec;
    protected Collection fieldPreProcessors;

	protected Boolean fieldOnWindows;

	public Page createOutput(MediaArchive inArchive, ConvertInstructions inStructions)
	{
		String sourcePath = inStructions.getAssetSourcePath();
		Page converted = getPageManager().getPage(inStructions.getOutputPath());
		if (inStructions.isForce() || !converted.exists() || converted.getContentItem().getLength() == 0)
		{
			//We need to load up the asset right away
			Asset asset = inArchive.getAssetBySourcePath(sourcePath);
			if (asset == null)
			{
				asset = createAsset(inArchive , sourcePath); //virtual assets
			}
			if(asset == null){
				asset = createAsset(inArchive, inStructions.getInputPath());
			}
			if (asset == null)
			{
				return null;
			}
//				converted.getParentFile().mkdirs(); //This should be in the sourcepath that already exists
//				try
//				{
//					converted.createNewFile();
//					// keep two people from converting
//				}
//				catch (IOException e)
//				{
//					throw new OpenEditException(e);
//				} 
			//The output formats might need to use helpers to get it working
			ConvertResult ok = convert(inArchive, asset, converted, inStructions);
			if (!ok.isOk())
			{
				//should already be logged missing original?
				log.error("Convert failed " + asset.getSourcePath() + " for " + getClass().getName());
				return null;
			}
		}

//		if (inStructions.isWatermark())
//		{
//			String generatedDirPath = "/" + inArchive.getCatalogId() + "/assets/images/generated/";
//			String watermarkPath = generatedDirPath + inStructions.createWatermarkPath(sourcePath);
//
//			return makeWaterMark(inStructions, converted, watermarkPath);
//		}
//		else
//		{
			return getPageManager().getPage(converted.getPath());
//		}
	}

	public ConvertInstructions createInstructions(WebPageRequest inReq, MediaArchive inArchive, String inOutputType, String inSourcePath)
	{
		Map all = new HashMap(); //TODO: Get parent ones as well
		for (Iterator iterator = inReq.getContentPage().getPageSettings().getAllProperties().iterator(); iterator.hasNext();)
		{
			PageProperty type = (PageProperty) iterator.next();
			all.put(type.getName(), type.getValue());
		}
//		log.info(all);
		
		all.putAll( inReq.getPageMap()); //these could be objects
		
		all.putAll( inReq.getParameterMap() );
		
		return createInstructions(all, inArchive, inOutputType, inSourcePath);
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

	/**
	 * Returns an object that has the output path set on it
	 * @param inReq
	 * @param inArchive
	 * @param inPage
	 * @param inSourcePath Should be passed in with the results of $asset.getSourcePathToAttachment("image|video")
	 * @return
	 */
    
	public ConvertInstructions createInstructions(Map inCreateProperties, MediaArchive inArchive, String inOutputType, String inSourcePath)
	{
		ConvertInstructions inStructions = new ConvertInstructions();
		inStructions.setAssetSourcePath(inSourcePath);
				
		inStructions.setOutputExtension(inOutputType);

		//Maybe this is too much stuff?
		for (Iterator iterator = inCreateProperties.keySet().iterator(); iterator.hasNext();)
		{
			String key = iterator.next().toString();
			Object value = inCreateProperties.get(key);
			if( value instanceof String || value instanceof Boolean)
			{
				inStructions.addProperty(key, String.valueOf(value));
			}
		}
		
		populateParameters(inArchive, inStructions);		

		
		//populateOutputPath(inArchive,inStructions);
		
		return inStructions;
	}

	protected void populateParameters(MediaArchive inArchive, ConvertInstructions inStructions)
	{
		String presetdataid = inStructions.get("presetdataid");
		//Load up the parameter data
		if( presetdataid != null )
		{
			Searcher paramsearcher = inArchive.getSearcherManager().getSearcher(inArchive.getCatalogId(), "presetparameter" );
			Collection params = paramsearcher.fieldSearch("parameterdata",presetdataid,"id");
			if( params.size() > 0 )
			{
				inStructions.setParameters(params);
				
				for (Iterator iterator = params.iterator(); iterator.hasNext();)
				{
					Data row = (Data) iterator.next();
					inStructions.setProperty(row.getName(), row.get("value"));
				}
				
			}
		}
	}

	public String getWaterMarkPath(String inThemePrefix)
	{
		if (fieldWaterMarkPath == null)
		{
			Page water = getPageManager().getPage(inThemePrefix + "/images/watermark.png");
			fieldWaterMarkPath = water.getContentItem().getAbsolutePath(); // Strings for performance
		}
		return fieldWaterMarkPath;
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
  
	public ConvertResult updateStatus(MediaArchive inArchive,Data inTask, Asset inAsset,ConvertInstructions inStructions )
	{
		ConvertResult status = new ConvertResult();
		status.setComplete(true);
		status.setOk(true);
		return status;
	}
	@Override
	public String populateOutputPath(MediaArchive inArchive, ConvertInstructions inStructions, Data inPreset)
	{
		StringBuffer outputpage = new StringBuffer();
		outputpage.append("/WEB-INF/data/" );
		outputpage.append(inArchive.getCatalogId());
		outputpage.append("/generated/" );
		outputpage.append(inStructions.getAssetSourcePath() );
		outputpage.append("/" );
		String output = inPreset.get("outputfile");
		int pagenumber = inStructions.getPageNumber();
		if( pagenumber > 1 )
		{
			String name = PathUtilities.extractPageName(output);
			String ext = PathUtilities.extractPageType(output);
			output = name + "page" + pagenumber + "." + ext;
		}
		outputpage.append(output);
		
		return outputpage.toString();
	}

}
