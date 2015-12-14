/**
 * 
 */
package org.entermediadb.events;

import org.openedit.OpenEditException;


/**
 * @author asivitz
 *
 */
public interface PathEventListener
{
	public void taskActionComplete(PathEvent inTask) throws OpenEditException;

	public void taskStarted(PathEvent inTask) throws OpenEditException;
	
	public void taskFailed(PathEvent inTask) throws OpenEditException;
}
