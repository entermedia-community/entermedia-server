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

import java.util.Iterator;
import java.util.Map;

import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.users.UserManagerException;


/**
 * This class helps commands to manipulate property containers.
 *
 * @author Eric Galluzzo
 */
public class PropertyContainerManipulator
{
	/**
	 * Constructor for PropertyContainerHelper.
	 */
	public PropertyContainerManipulator()
	{
		super();
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param inParameters
	 * @param inPropertyContainer
	 *
	 * @throws UserManagerException
	 */
	public void createProperties(Map inParameters, Map inPropertyContainer)
		throws UserManagerException
	{
		// Find all parameters starting with "propertyName", find their
		// corresponding "propertyValue" parameter, and create the property.
		for (Iterator iter = inParameters.entrySet().iterator(); iter.hasNext();)
		{
			Map.Entry entry = (Map.Entry) iter.next();

			if (entry.getKey().toString().startsWith("propertyName"))
			{
				String propertyName = entry.getValue().toString().trim();

				// Do not add properties with empty property names.
				if (propertyName.length() > 0)
				{
					int propertyIndex = Integer.parseInt(entry.getKey().toString().substring(12));
					Object propertyValue = inParameters.get("propertyValue" + propertyIndex);

					if (propertyValue != null)
					{
						inPropertyContainer.put(propertyName, propertyValue.toString());
					}
				}
			}
		}
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param inParameters
	 * @param inPropertyContainer
	 *
	 * @throws UserManagerException
	 */
	public void deleteProperties(WebPageRequest inContext, Map inPropertyContainer)
		throws UserManagerException
	{
		String[] propertyNames = inContext.getRequestParameters("deletePropertyNames");

		for (int i = 0; i < propertyNames.length; i++)
		{
			inPropertyContainer.remove(propertyNames[i]);
		}
		
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param inParameters
	 * @param inPropertyContainer
	 *
	 * @throws UserManagerException
	 */
	public void updateProperties(Map inParameters, Map inPropertyContainer)
		throws UserManagerException
	{
		if (inParameters.containsKey("field"))
		{
			String[] fields = (String[]) inParameters.get("field");
			for (int i=0; i < fields.length; i++)
			{
				String field = fields[i];
				if (field == null || field.trim().length() == 0)
				{
					continue;
				}
				
				String value = (String) inParameters.get(field + ".value");
				if (value == null)
				{
					value = (String) inParameters.get("value-" + field);
				}
				
				inPropertyContainer.put(field, value);
			}
		}
		else
		{
			for (Iterator iter = inParameters.entrySet().iterator(); iter.hasNext();)
			{
				Map.Entry entry = (Map.Entry) iter.next();

				String key = entry.getKey().toString();
				if (key.startsWith("value-"))
				{
					String propertyName = key.substring(6);
					String propertyValue = entry.getValue().toString();
					if (propertyValue.length() > 0)
					{
						inPropertyContainer.put(propertyName, propertyValue);
					}
					else
					{
						inPropertyContainer.remove(propertyName);
					}
				}
				else if (key.endsWith(".value"))
				{
					String propertyName = key.substring(0,key.length() - 6);
					String propertyValue = entry.getValue().toString();
					if (propertyValue.length() > 0)
					{
						inPropertyContainer.put(propertyName, propertyValue);
					}
					else
					{
						inPropertyContainer.remove(propertyName);
					}
				}
			}
		}
	}
	
	public void updateProperties(Map inParameters, Data inPropertyContainer)	throws UserManagerException
	{
		for (Iterator iter = inParameters.entrySet().iterator(); iter.hasNext();)
		{
			Map.Entry entry = (Map.Entry) iter.next();

			String key = entry.getKey().toString();
			if (key.endsWith(".value"))
			{
				String propertyName = key.substring(0,key.length() - 6);
				String propertyValue = entry.getValue().toString();
				inPropertyContainer.setProperty(propertyName, propertyValue);
			}
	}
}

}
