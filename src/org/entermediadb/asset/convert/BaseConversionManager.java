package org.entermediadb.asset.convert;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.repository.ContentItem;

public abstract class BaseConversionManager implements ConversionManager
{
	protected MediaArchive fieldMediaArchive;
	protected Collection fieldInputLoaders;
	protected Collection fieldOutputFilters;
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

	
	public ConvertResult loadExistingOuput(Map inSettings, String inSourcePath)
	{
		//First thing is first. We need to check out cache and make sure this file is not already in existence
		ConvertInstructions instructions = createInstructions(inSourcePath, inSettings);
		ContentItem output = instructions.getOutputFile();
		ConvertResult result = new ConvertResult();
		result.setOutput(output);
		result.setInstructions(instructions);
		result.setOk(true);

		if( output.getLength() < 2 )
		{
			result.setComplete(false);
		} else{
			result.setComplete(true);
		}				
		return result;
		
	}
	
	//Come up with the expected output path based on the input parameters
	//All image handlers will use a standard file saving conversion
	@Override
	public ConvertResult createOutputIfNeeded(String inSourcePath, Map inSettings)
	{
		//First thing is first. We need to check out cache and make sure this file is not already in existence
		ConvertInstructions instructions = createInstructions(inSourcePath, inSettings);
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
	public ConvertInstructions createInstructions(Asset inAsset, Data inPreset)
	{ 
		ConvertInstructions instructions = createInstructions();
		instructions.loadPreset(inPreset);
		instructions.setAsset(inAsset);
		
		ContentItem output = findOutputFile(instructions);
		instructions.setOutputFile(output);
		return instructions;
		
	}

	public ConvertInstructions createInstructions(Asset inAsset, Data inPreset, Map inSettings)
	{
		ConvertInstructions instructions = new ConvertInstructions(getMediaArchive());
		instructions.loadSettings(inSettings);
		instructions.loadPreset(inPreset);
		//instructions.setAssetSourcePath(inSourcePath);
		instructions.setAsset(inAsset);
		ContentItem output = findOutputFile(instructions);
		instructions.setOutputFile(output);
		return instructions;
	}
	
	public ConvertInstructions createInstructions(String inSourcePath, Map inSettings)
	{ 
		ConvertInstructions instructions = new ConvertInstructions(getMediaArchive());
		instructions.loadSettings(inSettings);
		instructions.setAssetSourcePath(inSourcePath);
		//instructions.loadPreset(inPreset);
		ContentItem output = findOutputFile(instructions);
		instructions.setOutputFile(output);
		//calculateOutputPath(inPreset);
		return instructions;
		
	}

	
    @Override
	public ConvertResult createOutput(ConvertInstructions inStructions)
	{
    	ContentItem input = inStructions.getInputFile();
    	if( input == null)
    	{
	    	boolean useoriginal = Boolean.parseBoolean(inStructions.get("useoriginalasinput"));
	    	if(!useoriginal && fieldInputLoaders != null)
	    	{
		    	//Load input
		    	for (Iterator iterator = getInputLoaders().iterator(); iterator.hasNext();)
				{
					InputLoader loader = (InputLoader) iterator.next();
					input = loader.loadInput(inStructions);
					if( input != null)
					{
						break;
					}
				}
	    	}
    	}	
    	return createOutput(inStructions, input);
	}
    
    protected MediaTranscoder findTranscoder(ConvertInstructions inStructions)
    {
    	//look at the preset object or output filename and do a search
    	Data preset = inStructions.getConvertPreset();
    	if( preset != null)
    	{
    		return findTranscoderByPreset(preset);
    	}
    	return null;
    }

	public MediaTranscoder findTranscoderByPreset(Data preset)
	{
		String creator = preset.get("creator");
		if(creator == null){
			creator = preset.get("transcoderid");
		}
		MediaTranscoder transcoder = (MediaTranscoder)getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(), creator + "Transcoder");
		return transcoder;
	}
    
    public ConvertResult createOutput(ConvertInstructions inStructions, ContentItem input)
    {
    	if(input == null || !input.exists())
		{
			input = createCacheFile(inStructions, input);
		}
    	if(input == null || !input.exists())
    	{
    		input = inStructions.getOriginalDocument();
    	}
		inStructions.setInputFile(input);
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

	public ConvertResult transcode(ConvertInstructions inStructions)
	{
		MediaTranscoder transcoder = findTranscoder(inStructions);
		return transcoder.convert(inStructions);
	}

	@Override
	public ConvertResult updateStatus(Data inTask, ConvertInstructions inStructions)
	{
		MediaTranscoder transcoder = findTranscoder(inStructions);
		return transcoder.convert(inStructions);
	}
	
	protected abstract ContentItem createCacheFile(ConvertInstructions inStructions, ContentItem inInput);

	@Override
	public ConvertInstructions createInstructions()
	{
		return new ConvertInstructions(getMediaArchive());
	}
	protected abstract String getCacheName();

}
