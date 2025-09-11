package org.entermediadb.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.manager.BaseManager;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.profile.UserProfile;

public class BaseAiManager extends BaseManager 
{
	public Map<String, String> getModels()
	{
		Map<String, String> models = new HashMap<>();
		String visionmodel = getMediaArchive().getCatalogSettingValue("llmvisionmodel");
		if(visionmodel == null) {
			visionmodel = "gpt-5-nano";
		}
		models.put("vision", visionmodel);

		String semanticmodel = getMediaArchive().getCatalogSettingValue("llmsemanticmodel");
		if(semanticmodel == null) {
			semanticmodel = "gpt-4o-mini";
		}
		models.put("semantic", semanticmodel);
		
		String ragmodel = getMediaArchive().getCatalogSettingValue("llmragmodel");
		if(ragmodel == null) {
			ragmodel = "qwen3:4b";
		}
		models.put("ragmodel", ragmodel);
		
		return models;
	}
	
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
