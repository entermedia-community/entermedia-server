package org.entermediadb.asset.convert;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.PathUtilities;

public abstract class BaseConversionManager implements ConversionManager
{
	private static final Log log = LogFactory.getLog(BaseConversionManager.class);

	protected MediaArchive fieldMediaArchive;
	protected Collection fieldInputLoaders;
	protected Collection fieldOutputFilters;
	protected MediaTranscoder fieldDefaultTranscoder;
	
	public MediaTranscoder getDefaultTranscoder()
	{
		return fieldDefaultTranscoder;
	}

	public void setDefaultTranscoder(MediaTranscoder inDefaultTranscoder)
	{
		fieldDefaultTranscoder = inDefaultTranscoder;
	}

	public Collection getOutputFilters()
	{
		return fieldOutputFilters;
	}

	public void setOutputFilters(Collection inOutputFilters)
	{
		fieldOutputFilters = inOutputFilters;
	}
	
	public Collection getInputLoaders()
	{
		return fieldInputLoaders;
	}

	public void setInputLoaders(Collection inInputLoaders)
	{
		fieldInputLoaders = inInputLoaders;
	}

	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	
	public ConvertResult loadExistingOuput(Map inSettings, String inSourcePath, String exportName)
	{
//		ConvertInstructions instructions = createInstructions(inSourcePath, inSettings);
//		ContentItem output = instructions.getOutputFile();
		
		ContentItem existing = getMediaArchive().getContent( "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/generated/" + inSourcePath + "/" + exportName);
		ConvertResult result = new ConvertResult();
		result.setOutput(existing);
		result.setOk(true);
		if( existing.exists() )
		{
			result.setComplete(true);
		}
		else
		{
			String useoriginalmediawhenpossible = (String)inSettings.get("useoriginalmediawhenpossible");
			if (Boolean.parseBoolean(useoriginalmediawhenpossible) )
			{
				Asset asset = getMediaArchive().getAssetBySourcePath(inSourcePath);
				String type = PathUtilities.extractPageType(exportName);
				if( asset.getFileFormat().equals(type) )
				{
					Page original = getMediaArchive().getOriginalDocument(asset);
					if( original.exists() )
					{
						result.setComplete(true);
						result.setOutput(original.getContentItem());
					}
				}
			}

		}
		return result;
		//Require export name to be set
		
//		String outputextension = PathUtilities.extractPageType( exportName );
//		inSettings.put("outputextension", outputextension);
//		if( exportName.startsWith("image") && exportName.length() > 10 && exportName.contains("x"))
//		{
//			//see if there is a with and height?
//			String size = exportName.substring(5,exportName.length() - outputextension.length() - 1 );
//			int cutoff= size.indexOf("x");
//			String width = size.substring(0,cutoff);
//			String height = size.substring(cutoff + 1,size.length());	
//			inSettings.put("prefwidth", width);
//			inSettings.put("prefheight", height);
//		}
//		Data preset = null;
//		if( exportName.startsWith("image") || exportName.startsWith("video") || exportName.startsWith("document") || exportName.startsWith("audio") ) )
//		{
//			preset = getMediaArchive().getPresetManager().getPresetByOutputName(getMediaArchive(), getRenderType(), exportName);
//		}
//
//		
//		//First thing is first. We need to check out cache and make sure this file is not already in existence
//		ConvertInstructions instructions = createInstructions(inSourcePath, inSettings);
//		
//
//		
//		return result;
		
	}
	
	//Come up with the expected output path based on the input parameters
	//All image handlers will use a standard file saving conversion
	@Override
	public ConvertResult createOutputIfNeeded(Asset inAsset, String inSourcePath,String inExportName, Map inSettings)
	{
		//First thing is first. We need to check out cache and make sure this file is not already in existence
		ConvertInstructions instructions = createInstructions(inAsset, inSourcePath, inExportName, inSettings);
		ContentItem output = instructions.getOutputFile();
		if( output.getLength() < 2 )
		{
			return createOutput(instructions);
		}
		ConvertResult result = new ConvertResult();
		result.setOutput(output);
		result.setOk(true);
		result.setComplete(true);
		result.setInstructions(instructions);
		return result;
	}
	protected abstract String getRenderType();

	@Override
	public ConvertInstructions createInstructions(Asset inAsset)
	{
		ConvertInstructions instructions = createNewInstructions();
		instructions.setAsset(inAsset);
		return instructions;		
	}


	@Override
	public ConvertInstructions createInstructions(Asset inAsset, String inExportName)
	{
		Data preset = getMediaArchive().getPresetManager().getPresetByOutputName(getMediaArchive(), getRenderType(), inExportName);
		ConvertInstructions instructions = createNewInstructions();
		instructions.setAsset(inAsset);
		instructions.loadPreset(preset);
//		ContentItem output = findOutputFile(instructions);
//		instructions.setOutputFile(output);
		return instructions;		
	}


	public ConvertInstructions createInstructions(Asset inAsset, Data inPreset)
	{ 
		ConvertInstructions instructions = createNewInstructions();
		instructions.loadPreset(inPreset);
		instructions.setAsset(inAsset);
		
//		ContentItem output = findOutputFile(instructions);
//		instructions.setOutputFile(output);
		return instructions;
		
	}

	protected ConvertInstructions createNewInstructions()
	{
		ConvertInstructions ins = new ConvertInstructions(getMediaArchive());
	
		return ins;
	}

	public ConvertInstructions createInstructions(Asset inAsset, Data inPreset, Map inSettings)
	{
		ConvertInstructions instructions = createNewInstructions();
		instructions.loadSettings(inSettings);
		instructions.loadPreset(inPreset);
		//instructions.setAssetSourcePath(inSourcePath);
		instructions.setAsset(inAsset);
//		ContentItem output = findOutputFile(instructions);
//		instructions.setOutputFile(output);
		return instructions;
	}
	public ConvertInstructions createInstructions(String inSourcePath, String inExportName, Map inSettings)
	{
		return createInstructions( null,inSourcePath,inExportName,inSettings);
	}
	public ConvertInstructions createInstructions(Asset inAsset, String inSourcePath, String inExportName, Map inSettings)
	{ 
		ConvertInstructions instructions = createNewInstructions();
		Data preset = getMediaArchive().getPresetManager().getPresetByOutputName(getMediaArchive(), getRenderType(), inExportName);
		instructions.loadPreset(preset);
		if( inAsset != null)
		{
			instructions.setAsset(inAsset);
		}
		//log.error("No preset defined for export file name" + inExportName);
		instructions.setOutputExtension(PathUtilities.extractPageType(inExportName)); //For cases where the output is not configured
		instructions.loadSettings(inSettings);
		instructions.setAssetSourcePath(inSourcePath);
		return instructions;
		
	}

	
    @Override
	public ConvertResult createOutput(ConvertInstructions inStructions)
	{
    	ContentItem input = inStructions.getInputFile();
    	if( input == null && getInputLoaders() != null)
    	{
	    	//Load input
	    	input = findInput(inStructions);
			inStructions.setInputFile(input);
    	}	
    	if(input == null || !input.exists())
		{
			//throw new OpenEditException("Input is " + input + " input loaders failed to load");
    		ConvertResult result = new ConvertResult();
    		result.setInstructions(inStructions);
    		result.setError("Input is " + input + " input loaders failed to load");
    		return result;
		}
    	if( !inStructions.isForce() && !inStructions.isStreaming() )
    	{
	    	ContentItem output = inStructions.getOutputFile();
			if( output.getLength() > 2 )
			{
				ConvertResult result = new ConvertResult();
				result.setOutput(output);
				result.setOk(true);
				result.setComplete(true);
				result.setInstructions(inStructions);
				return result;
			}
    	}
    	ConvertResult result = transcode(inStructions);

    	if( getOutputFilters() != null && result.isOk() && result.isComplete() )
    	{
    		for (Iterator iterator = getOutputFilters().iterator(); iterator.hasNext();)
			{
				OutputFilter filter = (OutputFilter) iterator.next();
				ConvertResult tmpresult = filter.filterOutput(inStructions);
				if( tmpresult != null)
				{
					result = tmpresult;
				}
			}
    	}
    	return result;
	}

	public ContentItem findInput(ConvertInstructions inStructions) {
		for (Iterator iterator = getInputLoaders().iterator(); iterator.hasNext();)
		{
			InputLoader loader = (InputLoader) iterator.next();
			ContentItem input = loader.loadInput(inStructions);
			if( input != null)
			{
				return input;
			}
		}
		return null;
	}
    
    protected ConvertResult transcode(ConvertInstructions inStructions)
    {
    	MediaTranscoder transcoder = findTranscoder(inStructions);
    	ConvertResult result = transcoder.convert(inStructions);
    	return result;
    }
    
    protected MediaTranscoder findTranscoder(ConvertInstructions inStructions)
    {
    	//look at the preset object or output filename and do a search
    	Data preset = inStructions.getConvertPreset();
    	if( preset != null)
    	{
    		return findTranscoderByPreset(preset);
    	}
    	return getDefaultTranscoder();
    }

	protected MediaTranscoder findTranscoderByPreset(Data preset)
	{
		String creator = preset.get("transcoderid");
		MediaTranscoder transcoder = (MediaTranscoder)getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(), creator + "Transcoder");
		return transcoder;
	}

	@Override
	public ConvertResult updateStatus(Data inTask, ConvertInstructions inStructions)
	{
		MediaTranscoder transcoder = findTranscoder(inStructions);
		return transcoder.convert(inStructions);
	}
	protected ContentItem makeCustomInput(BaseTranscoder imTranscoder, String format, ConvertInstructions inStructions)
	{
		if(inStructions.getAsset() == null){
			return null;
		}
		String colorspace = inStructions.getAsset().get("colorspace");
		if( "4".equals( colorspace ) ||  "5".equals(colorspace ))
		{
			
			ContentItem originalDocument = inStructions.getOriginalDocument();
			boolean exists = originalDocument.exists();
			if( inStructions.isCrop() || !exists ) //Let it use the standard 1020x768
			{
				return null;
			}
//			if( !isCMYKProfile(inInputFile) )
//			{
				Asset asset = inStructions.getAsset();
				ContentItem custom = getMediaArchive().getContent( "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/generated/" + asset.getSourcePath() + "/customthumb." + format);
				if( !custom.exists() )
				{
					ConvertInstructions instructions = createInstructions(asset);
					instructions.setForce(true);
					instructions.setInputFile(originalDocument);
					instructions.setOutputFile(custom);
					imTranscoder.convert(instructions);
				}
				return custom;
//			}
		}
		return null;
	}

}
