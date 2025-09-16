package org.entermediadb.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.manager.BaseManager;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.hittracker.HitTracker;
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
		
		String metadatamodel = getMediaArchive().getCatalogSettingValue("llmmetadatamodel");
		if(metadatamodel == null) {
			metadatamodel = "gpt-5-nano";
		}
		models.put("metadata", metadatamodel);

		String semanticmodel = getMediaArchive().getCatalogSettingValue("llmsemanticmodel");
		if(semanticmodel == null) {
			semanticmodel = "qwen3:4b";
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
	
	protected String collectText(Collection inValues)
	{
		StringBuffer words = new StringBuffer();
		if( inValues == null)
		{
			return null;
		}
		for (Iterator iterator = inValues.iterator(); iterator.hasNext();)
		{
			String text = (String) iterator.next();
			words.append(text);
			if (iterator.hasNext())
			{
				words.append(", ");
			}
			
		}
		return words.toString();
	}

	protected void clearAllCaches()
	{
//		// TODO Auto-generated method stub
//		getMediaArchive().getCacheManager().clear("aifacedetect"); //Standard cache for this fieldname
//		getMediaArchive().getCacheManager().clear("faceboxes"); //All related boxes. TODO: Limit to this record
//		//getMediaArchive().getCacheManager().clear("facepersonlookuprecord");
//		//?
////		getMediaArchive().getCacheManager().clear("aifacedetect");
////		getMediaArchive().getCacheManager().clear("faceboxes");
////		getMediaArchive().getCacheManager().clear("aifacedetect"); 

	}

	
}
