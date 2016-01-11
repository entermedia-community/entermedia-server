package org.entermediadb.asset.convert;

import java.util.Collection;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.openedit.Data;
import org.openedit.repository.ContentItem;

public interface ConversionManager
{
	public ContentItem findOutputFile(ConvertInstructions inStructions);

	public ConvertInstructions createInstructions(Map inSettings, String inSourcePath);
	
	//public ConvertInstructions createInstructions(MediaArchive inArchive, Asset inAsset,Data inPreset,String inOutputType);
	public ConvertInstructions createInstructions(Map inSetings, Asset inAsset, String inSourcePath, Data inPreset);

	public ConvertResult createOutput(ConvertInstructions inStructions);
	public ConvertResult createOutputIfNeeded(Map inCreateProperties, String inSourcePath);
	void setInputLoaders(Collection inList);
}