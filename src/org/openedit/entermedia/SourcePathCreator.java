package org.openedit.entermedia;

import org.openedit.Data;

public interface SourcePathCreator {
	public String createSourcePath(Data inAsset, String inUrlToOriginal);
}
