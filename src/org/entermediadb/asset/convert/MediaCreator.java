package org.entermediadb.asset.convert;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
/**
 * This deals with actual conversions from one file to another
 * @author shanti
 *
 */
public class MediaCreator
{
	private static final Log log = LogFactory.getLog(MediaCreator.class);

	protected SearcherManager fieldSearcherManager;
	protected ModuleManager fieldModuleManager;
	protected Map fieldConverterCache;
	protected Map fieldFileFormatCache;
	protected PageManager fieldPageManager;
	//protected HttpClient fieldHttpClient;
	protected MediaArchive fieldMediaArchive;

	public ConvertResult createOutput(ConvertInstructions inStructions)
	{
		MediaTranscoder creator = getMediaCreatorByOutputFormat(inStructions.getOutputExtension());
		return creator.convert(inStructions);
	}

	// filetype is jpg asset.fileformat
	public MediaTranscoder getMediaCreatorByOutputFormat(String inFileType)
	{
		MediaTranscoder converter = (MediaTranscoder) getConverterCache().get(inFileType);
		if (converter == null)
		{
			if( inFileType == null)
			{
				return null;
			}
			String catid = getMediaArchive().getCatalogId();
			
			Searcher searcher = getSearcherManager().getSearcher(catid, "fileformat");
			Data row = (Data) searcher.searchById(inFileType);
			if (row == null)
			{
				return null;
			}
			else
			{
				String type = row.get("converter"); 
				if( type == null)
				{
					 type = row.get("creator"); //legacy
				}
				if( type == null)
				{
					return null; //TODO: Cache a Null value?
				}
				String bean = type + "Converter";
				converter = (MediaTranscoder) getModuleManager().getBean(bean);
			}
			getConverterCache().put(inFileType, converter);
		}
		return converter;
	}

	public String getRenderTypeByFileFormat(String inFileType)
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
				render = row.get("rendertype");
			}
			else
			{
				render = "default";
			}
			getFileFormatCache().put( inFileType, render);
		}
		return render;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public Map getConverterCache()
	{
		if (fieldConverterCache == null)
		{
			fieldConverterCache = new HashMap();
		}
		return fieldConverterCache;
	}

	public void setConverterCache(Map inCache)
	{
		fieldConverterCache = inCache;
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

	public Map getFileFormatCache()
	{
		if (fieldFileFormatCache == null)
		{
			fieldFileFormatCache = new HashMap();
		}
		return fieldFileFormatCache;
	}

	public void setFileFormatCache(Map inFileFormatCache)
	{
		fieldFileFormatCache = inFileFormatCache;
	}

	protected Map fieldHandlerCache = new HashMap(5);
	
	protected ConversionManager getManager(String inFileFormat)
	{
		ConversionManager handler = (ConversionManager)fieldHandlerCache.get(inFileFormat);
		if( handler == null)
		{
			synchronized (this)
			{
				handler = (ConversionManager)fieldHandlerCache.get(inFileFormat);
				if( handler == null)
				{
					String type = getMediaArchive().getMediaRenderType(inFileFormat);
					handler = (ConversionManager)getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(), type + "Handler");
					fieldHandlerCache.put( inFileFormat, handler);
				}	
			}
		}
		return handler;
	}

	
	public ConvertResult createOutputIfNeeded(Map inCreateProperties, String inSourcePath, String inOutputType)
	{
		//Minimal information here. We dont know what kind of input we have
		ConversionManager handler = getManager(inOutputType);
		return handler.createOutputIfNeeded(inCreateProperties,inSourcePath, inOutputType);
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
