package org.entermediadb.asset.trancode;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.converters.inputloaders.InputLoader;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;

public class ImageHandler extends BaseHandler implements TranscodeHandler
{

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
	//To create the file we need to Look for input in several places
	//1024x768
	//Custom thumb
	//indd file thumb
	//document.pdf
	//video.mp4
	//Original file
    	@Override
	public ConvertResult createOutput(ConvertInstructions inStructions)
	{
    	ContentItem imput = null;
    	for (Iterator iterator = fieldInputLoaders.iterator(); iterator.hasNext();)
		{
			InputLoader loader = (InputLoader) iterator.next();
			if( loader.canLoadInput(inStructions))
			{
				ConvertResult result = loader.loadInput(inStructions);
				
			}
		}
		return null;
	}

	protected ConvertInstructions createInstructions(Map inSettings, String inSourcePath, String inOutputType)
	{ 
		ConvertInstructions instructions = new ConvertInstructions(getMediaArchive());
		instructions.loadSettings(inSettings);
		//instructions.loadPreset(inPreset);
		ContentItem output = findOutputFile(instructions);
		instructions.setOutputFile(output);
		//calculateOutputPath(inPreset);
		return instructions;
		
	}	
	protected ContentItem findOutputFile(ConvertInstructions inStructions)
	{
		StringBuffer path = new StringBuffer();

		//legacy for people who want to keep their images in the old location
		String prefix = inStructions.getProperty("pathprefix");
		if( prefix != null)
		{
			path.append(prefix);
		}
		else
		{
			path.append("/WEB-INF/data");
			path.append(getMediaArchive().getCatalogHome());
			path.append("/generated/");
		}
		path.append(inStructions.getAssetSourcePath());
		path.append("/");

		String postfix = inStructions.getProperty("pathpostfix");
		if( postfix != null)
		{
			path.append(postfix);
		}
		if( "pdf".equals(inStructions.getOutputExtension()) )
		{
			path.append("document");
		}
		else
		{
			path.append("image"); //part of filename
		}
		if (inStructions.getMaxScaledSize() != null) // If either is set then
		{
			path.append(Math.round(inStructions.getMaxScaledSize().getWidth()));
			path.append("x");
			path.append(Math.round(inStructions.getMaxScaledSize().getHeight()));
		}
		if (inStructions.getPageNumber() > 1)
		{
			path.append("page");
			path.append(inStructions.getPageNumber());
		}
		if(inStructions.getProperty("timeoffset") != null)
		{
			path.append("offset");
			path.append(inStructions.getProperty("timeoffset"));
		}
		if(inStructions.isWatermark())
		{
			path.append("wm");
		}
		
		if(inStructions.getProperty("colorspace") != null){
			path.append(inStructions.getProperty("colorspace"));
		}
		if(inStructions.isCrop())
		{
			path.append("cropped");
		}
		if (inStructions.getOutputExtension() != null)
		{
			path.append("." + inStructions.getOutputExtension());
		}
		return getMediaArchive().getContent( path.toString() );
	}




	@Override
	public void setPageManager(PageManager inPageManager)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setExec(Exec inExec)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInputFinders(Collection inList)
	{
		// TODO Auto-generated method stub
		
	}

}
