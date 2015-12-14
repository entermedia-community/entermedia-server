/*
 * Created on Aug 24, 2006
 */
package org.entermediadb.asset;

import java.io.Writer;

import org.openedit.OpenEditException;

public interface AssetExport
{
	public void exportAllAssets (MediaArchive inStore, Writer inOut) throws OpenEditException;

	public void exportCatalogsWithAssets (MediaArchive inStore, Writer inOut) throws OpenEditException;

}
