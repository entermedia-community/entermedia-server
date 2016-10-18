package org.entermediadb.asset.convert;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.data.SearcherManager;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;
/**
 * This deals with actual conversions from one file to another
 * @author shanti
 *
 */
public class TranscodeTools
{
	private static final Log log = LogFactory.getLog(TranscodeTools.class);
	protected SearcherManager fieldSearcherManager;
	protected ModuleManager fieldModuleManager;
	protected Map fieldRenderTypeCache = new HashMap(5);
	protected Map fieldManagerCache = new HashMap(5);
	//protected Map<String,String> fieldTranscoderForFileFormatCache = new HashMap<String,String>(5);
	
	public Map getRenderTypeCache()
	{
		return fieldRenderTypeCache;
	}
	public void setRenderTypeCache(Map inRenderTypeCache)
	{
		fieldRenderTypeCache = inRenderTypeCache;
	}


	public Map getManagerCache()
	{
		return fieldManagerCache;
	}
	public void setManagerCache(Map inManagerCache)
	{
		fieldManagerCache = inManagerCache;
	}

	protected PageManager fieldPageManager;
	protected MediaArchive fieldMediaArchive;
	

//	public ConvertResult createOutput(ConvertInstructions inStructions)
//	{
//		ConverterManager creator = getManagerByFileFormat( inStructions.getOutputExtension());
//		return creator.cre(inStructions);
//	}

	// filetype is jpg asset.fileformat
	
	public String getRenderTypeByFileFormat(String inFileType)
	{
		if( inFileType == null)
		{
			return null;
		}
		inFileType = inFileType.toLowerCase();
		String render = (String)getRenderTypeCache().get(inFileType);
		if( render == null)
		{
			Data row = (Data) getSearcherManager().getSearcher(getMediaArchive().getCatalogId(), "fileformat").searchById(inFileType);
			if( row != null)
			{
				render = row.get("rendertype");
			}
			else
			{
				render = "default";
			}
			getRenderTypeCache().put( inFileType, render);
		}
		return render;
	}
/*	public String getTranscoderByFileFormat(String inFileType)
	{
		if( inFileType == null)
		{
			return null;
		}
		inFileType = inFileType.toLowerCase();
		String render = (String)getFileFormatCache().get(inFileType);
		if( render == null)
		{
			Data row = (Data) getSearcherManager().getSearcher(getMediaArchive().getCatalogId(), "fileformat").searchById(inFileType);
			if( row != null)
			{
				render = row.get("creator");
				getFileFormatCache().put( inFileType, render);
			}
		}
		return render;
	}
*/
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}


	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

//	public HttpClient getHttpClient()
//	{
//		if (fieldHttpClient == null)
//		{
//			fieldHttpClient = new org.apache.commons.httpclient.HttpClient();
//			
//		}
//
//		return fieldHttpClient;
//	}
//
//	public void setHttpClient(HttpClient inHttpClient)
//	{
//		fieldHttpClient = inHttpClient;
//	}
	//For example: canConvert("jpg","pdf") returns true
	//We use the output as they key converter then let it decide if it can read in a certain input type
//	public boolean canConvert(String inInput, String inOutput)
//	{
//		MediaTranscoder con = getMediaCreatorByOutputFormat(inOutput);
//		//If we get a converter for this output we are in good shape
//		return con != null && con.canReadIn(getMediaArchive(), inInput);
//	}

//	public List run(boolean inCreateT, boolean inCreateM, boolean inReplaceT,
//			boolean inReplaceM, HitTracker hits) 
//	{
//		log.info("Creating new images:");
//		log.info("createthumb:" + inCreateT);
//		log.info("createmedium:" + inCreateM);
//		List failures = new ArrayList();
//
//		if(hits == null || hits.size() == 0)
//		{
//			log.info("Batch Thumbnail Creation Failed: No Hits Selected.");
//			return failures;
//		}
//		log.info("Checking " + hits.getTotal() + " images");
//
//		//Go to each page and run the generator?
//		
//		//get the height from the folder area
//		Page thumconfig = getPageManager().getPage(getMediaArchive().getCatalogHome() + "/downloads/preview/thumb/_site.xconf");
//		ConvertInstructions inThumbStructions = new ConvertInstructions();
//		String w = thumconfig.getProperty("prefwidth");
//		String h = thumconfig.getProperty("prefheight");
//		inThumbStructions.setMaxScaledSize(new Dimension(Integer.parseInt(w),Integer.parseInt(h)));
//		inThumbStructions.setOutputExtension("jpg");
//
//		ConvertInstructions inMediumStructions = new ConvertInstructions();
//		w = thumconfig.getProperty("prefwidth");
//		h = thumconfig.getProperty("prefheight");
//		inMediumStructions.setMaxScaledSize(new Dimension(Integer.parseInt(w),Integer.parseInt(h)));
//		inMediumStructions.setOutputExtension("jpg");
//
//		for (int i = 0; i < hits.getTotal(); i++)
//		{
//			Data hit = (Data)hits.get(i);
//			// check for medium size. If not then convert to jpg and render
//			String sourcePath = hit.getSourcePath();
//			inMediumStructions.setAssetSourcePath(sourcePath);
//			inThumbStructions.setAssetSourcePath(sourcePath);
//			
//			if (inCreateM)
//			{
//				createOutput(inMediumStructions);
//			}
//			
//			if (inCreateT)
//			{
//				createOutput(inThumbStructions);
//			}
//		}
//		log.info("Completed image processing");
//		log.info("failures: ");
//		for (Iterator iterator = failures.iterator(); iterator.hasNext();)
//		{
//			String failure = (String) iterator.next();
//			log.info(failure);
//		}
//		return failures;
//	}


	
	public ConversionManager getManagerByFileFormat(String inFileType)
	{
		if( inFileType == null)
		{
			return null;
		}
		String type = getMediaArchive().getMediaRenderType(inFileType);
		return getManagerByRenderType(type);
	}
//	public ConversionManager getManagerByTranscoder(String inTranscoder)
//	{
//		ConversionManager handler = (ConversionManager)fieldManagerCache.get(inTranscoder);
//		if( handler == null)
//		{
//			synchronized (this)
//			{
//				handler = (ConversionManager)fieldManagerCache.get(inTranscoder);
//				if( handler == null)
//				{
//					handler = (ConversionManager)getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(), inTranscoder + "ConversionManager");
//					handler.setMediaArchive(getMediaArchive());
//					fieldManagerCache.put( inTranscoder, handler);
//				}	
//			}
//		}
//		return handler;		
//	}
	public ConversionManager getManagerByRenderType(String inRenderType)
	{
		ConversionManager handler = (ConversionManager)fieldManagerCache.get(inRenderType);
		if( handler == null)
		{
			synchronized (this)
			{
				handler = (ConversionManager)fieldManagerCache.get(inRenderType);
				if( handler == null)
				{
					handler = (ConversionManager)getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(), inRenderType + "ConversionManager");
					handler.setMediaArchive(getMediaArchive());
					fieldManagerCache.put( inRenderType, handler);
				}	
			}
		}
		return handler;		
	}
	
	
	public ConvertResult createOutputIfNeeded(Map inCreateProperties, Map inParameters, String inSourcePath, String inExportName)
	{
		//Minimal information here. We dont know what kind of input we have
		ConversionManager manager = getManagerByFileFormat(PathUtilities.extractPageType(inExportName));
		
		ConvertResult result = null;
		if( inCreateProperties == null )
		{
			inCreateProperties = new HashMap();
		}
		
		if( inParameters == null || inParameters.isEmpty() )
		{
			result = manager.loadExistingOuput(inCreateProperties,inSourcePath, inExportName);
			if(result.isComplete()  && result.getOutput().getLength() >2){
				if( result.getOutput() == null)
				{
					throw new OpenEditException("Output not found " + inSourcePath);
				}
				return result;
			}
		}
		Asset asset = getMediaArchive().getAssetBySourcePath(inSourcePath);
		if(asset == null)
		{
			asset = getMediaArchive().getAssetImporter().createAsset(getMediaArchive(), inSourcePath);
		}
		if( asset == null)
		{
			result = new ConvertResult();			
			result.setOk(false);
			result.setError("No Asset Found " + inSourcePath);
			result.setComplete(false);
			return result;
		}
		manager = getManagerByFileFormat(asset.getFileFormat()); //video input?
		if( inParameters != null)
		{
			inCreateProperties.putAll(inParameters);
		}
		result = manager.createOutputIfNeeded(asset, inSourcePath, inExportName, inCreateProperties);
		if( result.isComplete() )
		{
			if( result.getOutput() == null)
			{
				throw new OpenEditException("Output not found " + inSourcePath);
			}
		}
		return result;
	}
//	public ConvertInstructions createInstructions(Asset inAsset,Data inPreset,String inOutputType)
//	{
//		
//	}
//	public ConvertInstructions createInstructions(Map inCreateProperties, Asset inAsset, String inSourcePath, Data inPreset,String inOutputType)
//	{
//		
//	}


	
}
