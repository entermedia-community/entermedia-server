package org.entermediadb.asset.trancode;

import java.util.Collection;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.Data;
import org.openedit.page.manage.PageManager;
import org.openedit.util.Exec;

public interface TranscodeHandler
{
	/**
	 * Create a set of instruction based on the HTTP request
	 * @param inReq
	 * @param inPage
	 * @param inSourcePath
	 * @return
	 */

	public ConvertResult createOutputIfNeeded(Map inCreateProperties, String inSourcePath, String inOutputType);
	//public ConvertInstructions createInstructions(MediaArchive inArchive, Asset inAsset,Data inPreset,String inOutputType);
	//public ConvertInstructions createInstructions(MediaArchive inArchive, Map inCreateProperties, Asset inAsset, String inSourcePath, Data inPreset,String inOutputType);

	public ConvertResult createOutput(ConvertInstructions inStructions);
	/**
	 * We now need to actually do the creation of this output file
	 * @param inArchive
	 * @param inAsset
	 * @param inOut
	 * @param inStructions
	 * @return
	 */

	void setPageManager(PageManager inPageManager);

	void setExec(Exec inExec);
	
	void setInputFinders(Collection inList);
}