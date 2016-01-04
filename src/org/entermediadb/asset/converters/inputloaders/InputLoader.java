package org.entermediadb.asset.converters.inputloaders;

import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.ConvertResult;
import org.openedit.Data;
import org.openedit.page.manage.PageManager;
import org.openedit.util.Exec;

public interface InputLoader
{
	/**
	 * The creator is selected based on the output format as found here: fileformat.xml. The question becomes can this creator handle this input format?
	 * @param inArchive
	 * @param inInputType
	 * @return
	 */
	boolean canLoadInput(ConvertInstructions inStructions);

	
	/**
	 * We now need to actually do the creation of this output file
	 * @param inArchive
	 * @param inAsset
	 * @param inOut
	 * @param inStructions
	 * @return
	 */
	ConvertResult loadInput(ConvertInstructions inStructions);

	void setExec(Exec inExec);
	
}