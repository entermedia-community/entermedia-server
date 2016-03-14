package org.entermediadb.asset.convert;

import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;

public interface InputLoader
{
	ContentItem loadInput(ConvertInstructions inStructions);
}