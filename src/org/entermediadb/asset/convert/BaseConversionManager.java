package org.entermediadb.asset.convert;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.repository.ContentItem;

public abstract class BaseConversionManager implements ConversionManager
{
	protected MediaArchive fieldMediaArchive;
	protected Collection fieldInputLoaders;
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
	public ConvertResult createOutputIfNeeded(Map inSettings, String inSourcePath, String inOutputType)
	{
		//First thing is first. We need to check out cache and make sure this file is not already in existence
		ConvertInstructions instructions = createInstructions(inSettings,inSourcePath,inOutputType);
		ContentItem output = instructions.getOutputFile();
		if( output.getLength() < 2 )
		{
			return createOutput(instructions);
		}
		ConvertResult result = new ConvertResult();
		result.setOutput(output);
		result.setOk(true);
		result.setInstructions(instructions);
		return result;
	}
	public ConvertInstructions createInstructions(Map inSettings, String inSourcePath, String inOutputType)
	{ 
		ConvertInstructions instructions = new ConvertInstructions(getMediaArchive());
		instructions.loadSettings(inSettings);
		//instructions.loadPreset(inPreset);
		ContentItem output = findOutputFile(instructions);
		instructions.setOutputFile(output);
		//calculateOutputPath(inPreset);
		return instructions;
		
	}
	
	
	public ConvertInstructions createInstructions(Map inSettings,Asset inAsset, String inOutputType)
	{ 
		ConvertInstructions instructions = new ConvertInstructions(getMediaArchive());
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
    	ContentItem input = null;
    	boolean useoriginal = Boolean.parseBoolean(inStructions.get("useoriginalasinput"));
    	if(!useoriginal){
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
    	//TODO: create input
    	
    	return createOutput(inStructions, input);
    	//use original
    		
    	
    	//return transcode(inStructions);
	}
    
    public ConvertResult createOutput(ConvertInstructions inStructions, ContentItem input)
    {
    	
    	
    	if(input == null){
    		input = inStructions.getOriginalDocument();
    	}
    	return getMediaTranscoder().convert(inStructions);
    	
    }


}
