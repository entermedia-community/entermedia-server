package org.entermediadb.asset.convert;

import java.util.Collection;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.transcoders.WaterMarkTranscoder;
import org.openedit.Data;
import org.openedit.repository.ContentItem;

public interface ConversionManager
{
	public ConvertInstructions createInstructions(Asset inAsset);
	public ConvertInstructions createInstructions(Asset inAsset, Data inPreset);
	public ConvertInstructions createInstructions(Asset inAsset, String inOutputFileName);
	public ConvertInstructions createInstructions(String inSourcePath, String inExportName, Map inSettings);
	public ConvertInstructions createInstructions(Asset inAsset, Data inPreset, Map inSetings);
	
	//public ContentItem findOutputFile(ConvertInstructions inStructions);
	public ConvertResult loadExistingOuput(Map inSettings, String inSourcePath, String exportName);

	public ConvertResult createOutput(ConvertInstructions inStructions);
	public ConvertResult createOutputIfNeeded(Asset inAsset, String inSourcePath, String inExportName, Map inCreateProperties);
	public ConvertResult updateStatus(Data inTask, ConvertInstructions inStructions);

	public void setInputLoaders(Collection inList);
	public void setMediaArchive(MediaArchive inMediaArchive);
	public ContentItem findInput(ConvertInstructions inStructions);

	public WaterMarkTranscoder getWaterMarkTranscoder();

}