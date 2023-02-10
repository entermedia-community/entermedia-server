package org.entermediadb.users;

import java.util.HashMap;
import java.util.Map;

public class EntityPermissions
{
	String fieldSettingsGroup;
	Map fieldEntityPermissions;
	
	public Map<String,Map> getEntityPermissions()
	{
		if (fieldEntityPermissions == null)
		{
			fieldEntityPermissions = new HashMap();
			
		}

		return fieldEntityPermissions;
	}

	public void setEntityPermissions(Map inEntityPermissions)
	{
		fieldEntityPermissions = inEntityPermissions;
	}

	public Map getEntityPermissions(String inEntityId)
	{
		Map permissions = getEntityPermissions().get(inEntityId);
		if( permissions == null)
		{
			permissions = new HashMap()
			getEntityPermissions().put(inEntityId,permissions);
		}
		return permissions;
	}
	
	public void putPermission(String inEntityId, String inId, Object value)
	{
		Map permissions = getEntityPermissions(inEntityId);
		permissions.put(inId,Boolean.valueOf(value.toString()));
	}
	
}
