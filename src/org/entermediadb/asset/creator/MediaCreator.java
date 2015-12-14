package org.entermediadb.asset.creator;

import java.util.Collection;
import java.util.Map;

import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.util.Exec;

public interface MediaCreator
{
	/**
	 * The creator is selected based on the output format as found here: fileformat.xml. The question becomes can this creator handle this input format?
	 * @param inArchive
	 * @param inInputType
	 * @return
	 */
	boolean canReadIn(MediaArchive inArchive, String inInputType);

	/**
	 * Create a set of instruction based on the HTTP request
	 * @param inReq
	 * @param inPage
	 * @param inSourcePath
	 * @return
	 */

	public ConvertInstructions createInstructions(MediaArchive inArchive, String inSourcePath, Data inPreset, String inOutputType);
	public ConvertInstructions createInstructions(MediaArchive inArchive, Asset inAsset,Data inPreset,String inOutputType);
	public ConvertInstructions createInstructions(MediaArchive inArchive, Map inCreateProperties, Asset inAsset, String inSourcePath, Data inPreset,String inOutputType);
	/**File
	 * This is the main API to create a file output
	 * @param inArchive
	 * @param inStructions
	 * @return
	 */
	ConvertResult createOutputIfNeeded(ConvertInstructions inStructions);
	ConvertResult createOutput(ConvertInstructions inStructions);

	
	/**
	 * We now need to actually do the creation of this output file
	 * @param inArchive
	 * @param inAsset
	 * @param inOut
	 * @param inStructions
	 * @return
	 */
	ConvertResult convert(ConvertInstructions inStructions);

	public ConvertResult updateStatus(Data inTask, ConvertInstructions inStructions );

	void setPageManager(PageManager inPageManager);

	void setExec(Exec inExec);
	
	void setPreProcessors(Collection inList);
}