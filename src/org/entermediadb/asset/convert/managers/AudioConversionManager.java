package org.entermediadb.asset.convert.managers;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.BaseConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;

public class AudioConversionManager extends BaseConversionManager
{
	private static final Log log = LogFactory.getLog(AudioConversionManager.class);

	public boolean canReadIn(MediaArchive inArchive, String inOutputType)
	{
		return true;
	}

	public ConvertResult convert(ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();
		Asset asset = inStructions.getAsset();
		Page inputpage = inStructions.getMediaArchive().getOriginalDocument(asset);

		if (inputpage == null || !inputpage.exists())
		{
			//no such original
			log.info("Original does not exist: " + asset.getSourcePath());
			result.setOk(false);

			return result;
		}
		/*
		 * 
		 * <property id="flac" rendertype="audio"
		 * synctags="false">Flac</property> <property id="m4a"
		 * rendertype="audio" synctags="false">M4A</property> <property id="aac"
		 * rendertype="audio" synctags="false">aac</property>
		 */
		String inputExt = PathUtilities.extractPageType(inputpage.getContentItem().getAbsolutePath());
		String outputExt = inStructions.getOutputExtension();
//		String useoriginalmediawhenpossible = inStructions.getProperty("useoriginalmediawhenpossible");
//		if (Boolean.parseBoolean(useoriginalmediawhenpossible) && outputExt != null && outputExt.equals(inputExt))
//		{
//			createFallBackContent(inputpage, inStructions.getOutputFile());
//			result.setOk(true);
//		}
//		else
//		{
		long timeout = inStructions.getConversionTimeout();
		String inOutputType = inStructions.getOutputExtension();
		
		//call transocder
		
		if (result.isOk())
		{
			result.setComplete(true);
		}

		return result;
	}


	@Override
	public ContentItem findOutputFile(ConvertInstructions inStructions)
	{
		StringBuffer path = new StringBuffer();
		String prefix = inStructions.getProperty("pathprefix");
		if (prefix != null)
		{
			path.append(prefix);
		}
		else
		{
			path.append("/WEB-INF/data");
			path.append(inStructions.getMediaArchive().getCatalogHome());
			path.append("/generated/");
		}
		path.append(inStructions.getAssetSourcePath());

		path.append("/audio." + inStructions.getOutputExtension());
		//inStructions.setOutputPath(path.toString());
		return getMediaArchive().getContent( path.toString() );	}

	@Override
	public ConvertResult createOutput(ConvertInstructions inStructions)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ContentItem createCacheFile(ConvertInstructions inStructions, ContentItem inInput)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getCacheName()
	{
		return "audio";
	}


}