package org.entermediadb.asset.convert.inputloaders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.entermediadb.asset.convert.InputLoader;
import org.openedit.repository.ContentItem;

public class OriginalInputLoader implements InputLoader
{
	private static final Log log = LogFactory.getLog(OriginalInputLoader.class);

	@Override
	public ContentItem loadInput(ConvertInstructions inStructions)
	{
    	return inStructions.getOriginalDocument();
	}
}