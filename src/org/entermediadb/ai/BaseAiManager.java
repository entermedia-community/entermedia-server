package org.entermediadb.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.entermediadb.manager.BaseManager;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.profile.UserProfile;

public class BaseAiManager extends BaseManager 
{
	public Collection<MultiValued> loadUserSearchModules(UserProfile inProfile)
	{
		Collection<Data> modules = inProfile.getEntities();
		Collection<MultiValued> searchmodules = new ArrayList<MultiValued>();
		for (Iterator iterator = modules.iterator(); iterator.hasNext();)
		{
			MultiValued module = (MultiValued) iterator.next();
			if(module.getBoolean("showonsearch"))
			{
				searchmodules.add(module);
			}
		}
		return searchmodules;
	} 
}
