/*
 * Created on Aug 24, 2006
 */
package org.openedit.entermedia;

import java.io.Writer;

import com.openedit.OpenEditException;

public interface AssetExport
{
	public void exportAllAssets (MediaArchive inStore, Writer inOut) throws OpenEditException;

	public void exportCatalogsWithAssets (MediaArchive inStore, Writer inOut) throws OpenEditException;

}
