/*
 * Created on Sep 26, 2003
 *
/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/
package org.entermediadb.email;

import javax.mail.internet.InternetAddress;

/**
 * @author Matt Avery, mavery@einnovation.com
 * @deprecated use InternetAddress
 */
public class Recipient 
{
	protected InternetAddress fieldInternetAddress;

	protected String fieldEmailAddress;
	protected String fieldFirstName;
	protected String fieldLastName;
	public String getEmailAddress()
	{
		return fieldEmailAddress;
	}

	public String getFirstName()
	{
		return fieldFirstName;
	}
	public InternetAddress getInternetAddress()
	{
		return fieldInternetAddress;
	}

	public void setInternetAddress(InternetAddress inInternetAddress)
	{
		fieldInternetAddress = inInternetAddress;
	}
	public String getLastName()
	{
		return fieldLastName;
	}

	public void setEmailAddress(String string)
	{
		fieldEmailAddress = string;
	}

	public void setFirstName(String string)
	{
		fieldFirstName = string;
	}

	public void setLastName(String string)
	{
		fieldLastName = string;
	}

	public String getFullName()
	{
		StringBuffer name = new StringBuffer();
		if ( getFirstName() != null)
		{
			name.append(getFirstName());
		}
		if ( getLastName() != null)
		{
			if( name.length() > 0)
			{
				name.append(" ");
			}
			name.append(getLastName());
		}
		if ( name.indexOf(",") > -1 || name.indexOf(".") > -1 )
		{
			name.insert(0,"\"");
			name.append("\"");
		}
		return name.toString();
	}
	public String toString()
	{
		return getFullName() + " <" + getEmailAddress() + ">";
	}
}
