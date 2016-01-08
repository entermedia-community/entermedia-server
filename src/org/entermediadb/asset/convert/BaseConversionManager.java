package org.entermediadb.asset.convert;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.openedit.repository.ContentItem;

public abstract class BaseConversionManager implements ConversionManager
{
	protected MediaArchive fieldMediaArchive;
	protected Collection fieldInputLoaders;
	
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
    @Override
	public ConvertResult createOutput(ConvertInstructions inStructions)
	{
    	ContentItem input = null;

    	//Load input
    	for (Iterator iterator = fieldInputLoaders.iterator(); iterator.hasNext();)
		{
			InputLoader loader = (InputLoader) iterator.next();
			input = loader.loadInput(inStructions);
			if( input != null)
			{
				break;
			}
		}
    	//TODO: create input
    	
    	//use original
    	
    	
    	inStructions.setInputFile(input);
    	return transcode(inStructions);
	}
    public ConvertResult createOutput(ConvertInstructions inStructions, ContentItem input)
    {
    	//TOOD: Create 1024 file
    	getTranscoder().
    }


}
