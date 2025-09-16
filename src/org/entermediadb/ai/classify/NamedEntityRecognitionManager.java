package org.entermediadb.ai.classify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.users.User;

public class NamedEntityRecognitionManager extends ClassifyManager {

	// @Override
	// protected boolean processOneAsset(MultiValued inConfig, LlmConnection llmvisionconnection, LlmConnection llmsemanticconnection, Map<String, String> models, Asset asset) throws Exception
	// {
	// 	Collection<Map> tables = (Collection<Map>) inConfig.getValue("tables");
	// 	if (tables == null || tables.isEmpty())
	// 	{
	// 		return false;
	// 	}
		
		
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
