package org.entermediadb.asset.convert;

import org.openedit.repository.ContentItem;

public interface InputLoader
{
	ContentItem loadInput(ConvertInstructions inStructions);
}