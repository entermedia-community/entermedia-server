/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/

package org.entermediadb.modules.admin.users;

import org.openedit.OpenEditException;


/**
 * This exception is thrown when an end-user attempts to create a user or change a user's password,
 * when the original and new passwords do not match.
 *
 * @author Eric Galluzzo
 */
public class PasswordMismatchException extends OpenEditException
{
	public PasswordMismatchException(String inMsg)
	{
		super(inMsg);
	}

	public PasswordMismatchException(String inMsg, Throwable inRootCause)
	{
		super(inMsg, inRootCause);
	}

	public PasswordMismatchException(Throwable inRootCause)
	{
		super(inRootCause);
	}
}
