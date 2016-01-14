package org.entermediadb.asset.convert;

import java.util.Collection;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.repository.ContentItem;

public interface ConversionManager
{

	public ConvertInstructions createInstructions();

	public ConvertInstructions createInstructions(Map inSettings, String inSourcePath);
	
	//public ConvertInstructions createInstructions(MediaArchive inArchive, Asset inAsset,Data inPreset,String inOutputType);
	public ConvertInstructions createInstructions(Map inSetings, Asset inAsset, Data inPreset);

	public ContentItem findOutputFile(ConvertInstructions inStructions);

	public ConvertResult createOutput(ConvertInstructions inStructions);
	public ConvertResult createOutputIfNeeded(Map inCreateProperties, String inSourcePath);
	void setInputLoaders(Collection inList);

	public ConvertResult updateStatus(Data inTask, ConvertInstructions inStructions);

	public void setMediaArchive(MediaArchive inMediaArchive);

	public ConvertResult transcode(ConvertInstructions inStructions);
}