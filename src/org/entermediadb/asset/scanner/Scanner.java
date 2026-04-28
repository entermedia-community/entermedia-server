/*
 * Created on Oct 1, 2004
 */
package org.entermediadb.asset.scanner;

import org.entermediadb.asset.ConvertStatus;
import org.entermediadb.asset.MediaArchive;

/**
 * @author cburkey
 * 
 */
public abstract class Scanner
{
	public abstract void importAssets(MediaArchive inStore, ConvertStatus inErrorLog) throws Exception;

}
