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

	protected MediaTranscoder fieldMediaTranscoder;
	
	
	
	public MediaTranscoder getMediaTranscoder()
	{
		return fieldMediaTranscoder;
	}

	public void setMediaTranscoder(MediaTranscoder inMediaTranscoder)
	{
		fieldMediaTranscoder = inMediaTranscoder;
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

	//Come up with the expected output path based on the input parameters
	//All image handlers will use a standard file saving conversion
	@Override
	public ConvertResult createOutputIfNeeded(Map inSettings, String inSourcePath)
	{
		//First thing is first. We need to check out cache and make sure this file is not already in existence
		ConvertInstructions instructions = createInstructions(inSettings,inSourcePath);
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
	public ConvertInstructions createInstructions(Map inSettings, Asset inAsset, Data inPreset)
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
	
	public ConvertInstructions createInstructions(Map inSettings, String inSourcePath)
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
	
	public ConvertInstructions createInstructions(Map inSettings,Asset inAsset)
	{ 
		ConvertInstructions instructions = createInstructions();
		instructions.loadSettings(inSettings);
		//instructions.loadPreset(inPreset);
		instructions.setAsset(inAsset);
		
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
		return getMediaTranscoder().convert(inStructions);
	}

	@Override
	public ConvertResult updateStatus(Data inTask, ConvertInstructions inStructions)
	{
		return getMediaTranscoder().updateStatus(inTask, inStructions);
	}
	
	protected abstract ContentItem createCacheFile(ConvertInstructions inStructions, ContentItem inInput);

	@Override
	public ConvertInstructions createInstructions()
	{
		return new ConvertInstructions(getMediaArchive());
	}
	protected abstract String getCacheName();

}
