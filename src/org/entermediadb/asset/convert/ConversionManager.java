package org.entermediadb.asset.convert;

import java.util.Collection;
import java.util.Map;

import org.openedit.repository.ContentItem;

public interface ConversionManager
{
	public ContentItem findOutputFile(ConvertInstructions inStructions);

	public ConvertInstructions createInstructions(Map inSettings, String inSourcePath, String inOutputType);
	
	public ConvertResult createOutputIfNeeded(Map inCreateProperties, String inSourcePath, String inOutputType);
	//public ConvertInstructions createInstructions(MediaArchive inArchive, Asset inAsset,Data inPreset,String inOutputType);
	//public ConvertInstructions createInstructions(MediaArchive inArchive, Map inCreateProperties, Asset inAsset, String inSourcePath, Data inPreset,String inOutputType);

	public ConvertResult createOutput(ConvertInstructions inStructions);
	void setInputLoaders(Collection inList);
}