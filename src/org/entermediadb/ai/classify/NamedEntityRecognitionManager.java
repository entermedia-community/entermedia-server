package org.entermediadb.ai.classify;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.asset.util.JsonUtil;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;

public class NamedEntityRecognitionManager extends ClassifyManager 
{
	private static final Log log = LogFactory.getLog(NamedEntityRecognitionManager.class);

	@Override
	public LlmConnection getLlmConnection()
	{
		Map<String, String> models = getModels();
		return getMediaArchive().getLlmConnection(models.get("metadata"));
	}
	
	@Override
	protected boolean processOneAsset(MultiValued inConfig, Map<String, String> models, MultiValued inData)
	{
		boolean ok = processOneEntity(inConfig, models, inData, "asset");
		return ok;
	}
	 
	@Override
	protected boolean processOneEntity(MultiValued inConfig, Map<String, String> models, MultiValued inData, String inModuleId)
	{
	 	Collection<PropertyDetail> autocreatefields = getMediaArchive().getSearcher(inModuleId).getPropertyDetails().findAiAutoCreatedProperties();
	 	
	 	//Validate tables
	 	if (autocreatefields.isEmpty())
	 	{
	 		return false;
	 	}
	 	Map<String, Map> contextfields = populateFields(inModuleId,inData);
	 	
	 	for (Iterator iterator = autocreatefields.iterator(); iterator.hasNext();) {
	 		PropertyDetail detail = (PropertyDetail) iterator.next();
			contextfields.remove(detail.getId());

			Collection val = inData.getValues(detail.getId());
			if(!detail.isList() || (val != null && val.size() > 0))
			{
				//Invalid filed or already has a value
				iterator.remove();
			}
		}
		if(contextfields.isEmpty())
		{
			log.info(inConfig.get("bean") +" No fields to check for names in " + inData.getId() + " " + inData.getName());
			return false;
		}
		if( autocreatefields.isEmpty())
		{
			// Check again after removing any fields that already have values
			log.info(inConfig.get("bean") +" No fields to create in " + inData.getId() + " " + inData.getName());
			return false;
		}
		
		// Non assets that are not split enabled and have a primarymedia with fulltext
		if(!inModuleId.equals("asset") && !contextfields.keySet().contains("fulltext") && inData.get("pagenum") == null)
		{
			addPrimaryMediaFulltext(inData, contextfields);
		}

 		Map params = new HashMap();
 		params.put("data", inData);
 		params.put("fieldparams", inConfig);
 		params.put("contextfields", contextfields);
 		params.put("autocreatefields", autocreatefields);
 		
		String functionname = inConfig.get("aifunctionname");
		Map results = getLlmConnection().callStructuredOutputList(functionname,  params);
		Map categories = (Map) results.get("categories");
		if(categories != null)
		{			
			for (Iterator iterator = categories.keySet().iterator(); iterator.hasNext();) {
				String sourcetype = (String) iterator.next();
				Collection values = (Collection) categories.get(sourcetype);
				if( values == null || values.isEmpty() )
				{
					continue;
				}
				PropertyDetail detail = getMediaArchive().getSearcher(inModuleId).getDetail(sourcetype);
				if (detail != null)
				{
					if(detail.isList())
					{
						for (Iterator iterator2 = values.iterator(); iterator2.hasNext();) {
							String value = (String) iterator2.next();
							Data savedrecord = saveIfNeeded(inConfig, detail, value);
							if( savedrecord != null)
							{
								inData.addValue(detail.getId(), savedrecord.getId());
							}
						}
						
					}
				}
				
			}
		}
	 	
	 	//Then See if the field exists on the target table on a data case by case basis and add to it. Create the record if needed
		
		return true;
	 	
	}

	private void addPrimaryMediaFulltext(MultiValued inData, Map<String, Map> contextfields) {
		String primarymedia = inData.get("primarymedia");
		if(primarymedia == null || primarymedia.isEmpty())
		{
			primarymedia = inData.get("primaryimage");
		}
		if(primarymedia != null)
		{
			MultiValued primaryasset = getMediaArchive().getAsset(primarymedia);
			if(primaryasset != null)
			{
				if (primaryasset.getBoolean("hasfulltext"))
				{
					String mediatype = getMediaArchive().getMediaRenderType(primaryasset);
					if(mediatype.equals("document"))
					{
						String fulltext = primaryasset.get("fulltext");
						if (fulltext != null)
						{
							fulltext = fulltext.replaceAll("\\s+", " ");
							fulltext = fulltext.substring(0, Math.min(4000, fulltext.length()));
							HashMap fieldMap = new HashMap();
							fieldMap.put("label", "Parsed Document Content");
							JsonUtil jsonutils = new JsonUtil();
							fieldMap.put("text", jsonutils.escape(fulltext));
							contextfields.put("fulltext", fieldMap);
						}
					}
				}
			}
		}
	}

	protected Data saveIfNeeded(MultiValued inConfig, PropertyDetail inDetail, String inlabel)
	{
		String listid = inDetail.getId();
		Data found = getMediaArchive().query(listid).match("name", inlabel).searchOne();
		if( found == null ) 
		{
			found = getMediaArchive().getSearcher(listid).createNewData();
			found.setName(inlabel);
			getMediaArchive().saveData(listid,found);
		}
		return found;
	}

}
