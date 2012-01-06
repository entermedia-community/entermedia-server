package org.openedit.entermedia.creator;

import java.util.Map;

import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.util.Exec;

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
	public ConvertInstructions createInstructions(Map inProperties, MediaArchive inArchive, String inOutputType, String inSourcePath);

	/**
	 * Is this used?
	 * @param inReq
	 * @param inArchive
	 * @param inOputputype
	 * @param inSourcePath
	 * @return
	 */
	ConvertInstructions createInstructions(WebPageRequest inReq, MediaArchive inArchive, String inOputputype, String inSourcePath);

	
	/**
	 * This is the standard path that creators put their output
	 * @param inArchive
	 * @param inStructions
	 */
	String populateOutputPath(MediaArchive inArchive,ConvertInstructions inStructions);

	
	/**File
	 * This is the main API to create a file output
	 * @param inArchive
	 * @param inStructions
	 * @return
	 */
	Page createOutput(MediaArchive inArchive, ConvertInstructions inStructions);

	
	/**
	 * We now need to actually do the creation of this output file
	 * @param inArchive
	 * @param inAsset
	 * @param inOut
	 * @param inStructions
	 * @return
	 */
	ConvertResult convert(MediaArchive inArchive, Asset inAsset, Page inOut, ConvertInstructions inStructions);

	public ConvertResult updateStatus(MediaArchive inArchive,Data inTask, Asset inAsset,ConvertInstructions inStructions );

	void setPageManager(PageManager inPageManager);

	void setExec(Exec inExec);
	
}