/*
 * Created on Oct 1, 2004
 */
package org.openedit.entermedia.scanner;

import org.openedit.entermedia.ConvertStatus;
import org.openedit.entermedia.MediaArchive;

/**
 * @author cburkey
 * 
 */
public abstract class Scanner
{
	public abstract void importAssets(MediaArchive inStore, ConvertStatus inErrorLog) throws Exception;

}
