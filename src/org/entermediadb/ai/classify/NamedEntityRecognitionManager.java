package org.entermediadb.ai.classify;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;

public class NamedEntityRecognitionManager extends ClassifyManager 
{
	private static final Log log = LogFactory.getLog(NamedEntityRecognitionManager.class);

	 @Override
	 protected boolean processOneAsset(MultiValued inConfig, LlmConnection llmvisionconnection, LlmConnection llmsemanticconnection, Map<String, String> models, Asset inData)
	 {
		 boolean ok = processOneEntity(inConfig,llmvisionconnection,llmsemanticconnection,models,inData,"asset");
		 return ok;
	 }
	 
	 @Override
	 protected boolean processOneEntity(MultiValued inConfig, LlmConnection llmvisionconnection, LlmConnection llmsemanticconnection, Map<String, String> models, MultiValued inData, String inModuleId)
	 {
	 	Collection<Map> tables = (Collection<Map>) inConfig.getValue("tables");
	 	if (tables == null || tables.isEmpty())
	 	{
	 		return false;
	 	}
	 	///Search for all nounds based on tables
		Map<String,Map> contextfields = populateFields(inModuleId,inData);
		if(contextfields.isEmpty())
		{
			log.info("No fields to check for names in " + inData.getId() + " " + inData.getName());
			return false;
		}

 		Map params = new HashMap();
 		params.put("data", inData);
 		params.put("contextfields", contextfields);
 		
		String requestPayload = llmvisionconnection.loadInputFromTemplate("/" +  getMediaArchive().getMediaDbId() + "/ai/default/systemmessage/analyzenamed.html", params); 
		String functionname = inConfig.get("aifunctionname");
		LlmResponse results = llmvisionconnection.callClassifyFunction(params, models.get("vision"), functionname, requestPayload, null);
		if (results != null)
 		{
 			JSONObject arguments = results.getArguments();
 			if (arguments != null) 
 			{
 				Map metadata =  (Map) arguments.get("metadata");
 				if (metadata == null || metadata.isEmpty())
 				{
 					return false;
 				}
 				for (Iterator iterator2 = metadata.keySet().iterator(); iterator2.hasNext();)
 				{
 					String inKey = (String) iterator2.next();
 					PropertyDetail detail = getMediaArchive().getSearcher(inModuleId).getDetail(inKey);
 					if (detail != null)
 					{
 						String value = (String)metadata.get(inKey);
 						if(detail.isList())
 						{
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

	protected Data saveIfNeeded(MultiValued inConfig, PropertyDetail inDetail, String inValue)
	{
		String[] returned = inValue.split(":");
		
		String aisourcetype = returned[0];
		String label = returned[1];

		Map config = findConfigForTable(inConfig,aisourcetype);
		if( config == null )
		{
			return null;
		}
		//person:John Smith
		String listid = inDetail.getId();
		Data found = getMediaArchive().query(listid).match("name",label).searchOne();
		if( found == null && Boolean.parseBoolean( (String)config.get("autocreate") ) )
		{
			found = getMediaArchive().getSearcher(listid).createNewData();
			found.setName(label);
			getMediaArchive().saveData(listid,found);
		}
		return found;
	}

	private Map findConfigForTable(MultiValued inConfig, String inListid)
	{
		Collection<Map> tables = (Collection<Map>) inConfig.getValue("tables");
		for (Iterator iterator = tables.iterator(); iterator.hasNext();)
		{
			Map table = (Map) iterator.next();
			if( inListid.equals( table.get("sourcetype") ) )
			{
				//Save this to DB
				break;
			}
				
		}
		return null;
	}	
		
	// 	Collection allaifields = getMediaArchive().getAssetPropertyDetails().findAiCreationProperties();
	// 	Collection aifields = new ArrayList();
	// 	for (Iterator iterator2 = allaifields.iterator(); iterator2.hasNext();)
	// 	{
	// 		PropertyDetail aifield = (PropertyDetail)iterator2.next();
	// 	//	if( mediatype.equals("document") )
	// 		//{
	// 			// TODO: add better way to have media type specific fields
	// 		//	continue;
	// 	//	}
	// 		if(asset.hasValue(aifield.getId()) )
	// 		{
	// 			aifields.add(aifield);
	// 		}
	// 	}

	// 	if(!aifields.isEmpty())
	// 	{
	// 		Map params = new HashMap();
	// 		params.put("asset", asset);
	// 		params.put("aifields", aifields);

	// 		String requestPayload = llmvisionconnection.loadInputFromTemplate("/" +  getMediaArchive().getMediaDbId() + "/ai/default/systemmessage/analyzeasset.html", params);
	// 		LlmResponse results = llmvisionconnection.callClassifyFunction(params, models.get("vision"), "generate_asset_metadata", requestPayload, base64EncodedString);

	// 		if (results != null)
	// 		{
	// 			JSONObject arguments = results.getArguments();
	// 			if (arguments != null) {

	// 				Map metadata =  (Map) arguments.get("metadata");
	// 				if (metadata == null || metadata.isEmpty())
	// 				{
	// 					return false;
	// 				}
	// 				Map datachanges = new HashMap();
	// 				for (Iterator iterator2 = metadata.keySet().iterator(); iterator2.hasNext();)
	// 				{
	// 					String inKey = (String) iterator2.next();
	// 					PropertyDetail detail = getMediaArchive().getAssetPropertyDetails().getDetail(inKey);
	// 					if (detail != null)
	// 					{
	// 						String value = (String)metadata.get(inKey);
	// 						if(detail.isList())
	// 						{
	// 							String listId = value.split("\\|")[0];
	// 							datachanges.put(detail.getId(), listId);
	// 						}
	// 						else if (detail.isMultiValue())
	// 						{
	// 							Collection<String> values = Arrays.asList(value.split(","));
	// 							datachanges.put(detail.getId(), values);
	// 						}
	// 						else
	// 						{
	// 							datachanges.put(detail.getId(), value);
	// 						}
	// 					}
	// 				}

	// 				//Save change event
	// 				User agent = getMediaArchive().getUser("agent");
	// 				if( agent != null)
	// 				{
	// 					getMediaArchive().getEventManager().fireDataEditEvent(getMediaArchive().getAssetSearcher(), agent, "assetgeneral", asset, datachanges);
	// 				}

	// 				for (Iterator iterator2 = datachanges.keySet().iterator(); iterator2.hasNext();)
	// 				{
	// 					String inKey = (String) iterator2.next();
	// 					Object value = datachanges.get(inKey);

	// 					asset.setValue(inKey, value);
	// 					log.info("AI updated field "+ inKey + ": "+metadata.get(inKey));
	// 				}
	// 			}
	// 			else {
	// 				log.info("Asset "+asset.getId() +" "+asset.getName()+" - Nothing Detected.");
	// 			}
	// 		}
	// 	}
	// 	return true;
	// }

}
