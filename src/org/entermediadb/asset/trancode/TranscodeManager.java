package org.entermediadb.asset.trancode;

import java.util.HashMap;
import java.util.Map;

import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConvertResult;

public class TranscodeManager
{

	protected MediaArchive fieldMediaArchive;
	protected Map fieldHandlerCache = new HashMap(5);
	
	protected TranscodeHandler getHandler(String inFileFormat)
	{
		TranscodeHandler handler = (TranscodeHandler)fieldHandlerCache.get(inFileFormat);
		if( handler == null)
		{
			synchronized (this)
			{
				handler = (TranscodeHandler)fieldHandlerCache.get(inFileFormat);
				if( handler == null)
				{
					String type = getMediaArchive().getMediaRenderType(inFileFormat);
					handler = (TranscodeHandler)getMediaArchive().getModuleManager().getBean(getMediaArchive().getCatalogId(), type + "Handler");
					fieldHandlerCache.put( inFileFormat, handler);
				}	
			}
		}
		return handler;
	}


	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}
	
	public ConvertResult createOutputIfNeeded(Map inCreateProperties, String inSourcePath, String inOutputType)
	{
		//Minimal information here. We dont know what kind of input we have
		TranscodeHandler handler = getHandler(inOutputType);
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
