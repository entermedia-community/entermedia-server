/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/

package org.entermediadb.modules.admin.filemanager;

import org.openedit.OpenEditException;


/**
 * This exception is thrown when attempting to add a new page that already exists, where the user
 * does not wish to overwrite existing pages.
 *
 * @author Eric Galluzzo
 */
public class PageAlreadyExistsException extends OpenEditException
{
	/**
	 * Constructor for PageAlreadyExistsException.
	 *
	 * @param inMsg
	 */
	public PageAlreadyExistsException(String inMsg)
	{
		super(inMsg);
	}

	/**
	 * Constructor for PageAlreadyExistsException.
	 *
	 * @param inMsg
	 * @param inRootCause
	 */
	public PageAlreadyExistsException(String inMsg, Throwable inRootCause)
	{
		super(inMsg, inRootCause);
	}

	/**
	 * Constructor for PageAlreadyExistsException.
	 *
	 * @param inRootCause
	 */
	public PageAlreadyExistsException(Throwable inRootCause)
	{
		super(inRootCause);
	}
}
