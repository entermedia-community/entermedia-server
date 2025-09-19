package org.entermediadb.ai.classify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.LlmConnection;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;

public class NamedEntityRecognitionManager extends ClassifyManager 
{
	private static final Log log = LogFactory.getLog(NamedEntityRecognitionManager.class);

	 @Override
	 protected boolean processOneAsset(MultiValued inConfig, LlmConnection llmvisionconnection, LlmConnection llmsemanticconnection, Map<String, String> models, MultiValued inData)
	 {
		 boolean ok = processOneEntity(inConfig, llmvisionconnection, llmsemanticconnection, models, inData, "asset");
		 return ok;
	 }
	 
	 @Override
	 protected boolean processOneEntity(MultiValued inConfig, LlmConnection llmvisionconnection, LlmConnection llmsemanticconnection, Map<String, String> models, MultiValued inData, String inModuleId)
	 {
	 	Collection<Data> tables =  new ArrayList(getMediaArchive().getList("informaticsnertable"));
	 	//Validate tables
	 	if (tables == null || tables.isEmpty())
	 	{
	 		log.info(inConfig.get("bean") +" No tables configured to check for names in " + inData.getId() + " " + inData.getName());
	 		return false;
	 	}
	 	Map<String,Map> contextfields = populateFields(inModuleId,inData);
	 	
	 	for (Iterator iterator = tables.iterator(); iterator.hasNext();) {
			Map map = (Map) iterator.next();			
			PropertyDetail detail = getMediaArchive().getSearcher(inModuleId).getDetail((String)map.get("sourcetype"));
			contextfields.remove(map.get("sourcetype"));
			if( detail == null || !detail.isList() )
			{
				iterator.remove();
				//log.info("Removing invalid table " + map.get("sourcetype") + " from config");
			}
		}
	 	if( tables.isEmpty() )
	 	{
	 		log.info(inConfig.get("bean") + " - No valid tables to check for names in " + inData.getId() + " " + inData.getName());
	 		return false;
	 	}
		if(contextfields.isEmpty())
		{
			log.info(inConfig.get("bean") +" No fields to check for names in " + inData.getId() + " " + inData.getName());
			return false;
		}

 		Map params = new HashMap();
 		params.put("data", inData);
 		params.put("fieldparams", inConfig);
 		params.put("contextfields", contextfields);
 		params.put("tables", tables);
 		
		String functionname = inConfig.get("aifunctionname");
		Map results = llmsemanticconnection.callStructuredOutputList(functionname, models.get("semantic"),  params);
		Map categories = (Map) results.get("categories");
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
	 	
	 	//Then See if the field exists on the target table on a data case by case basis and add to it. Create the record if needed
		
		return true;
	 	
	 }

	protected Data saveIfNeeded(MultiValued inConfig, PropertyDetail inDetail, String inlabel)
	{

		Map config = findConfigForTable(inConfig, inDetail.getId());
		if( config == null )
		{
			return null;
		}
		//person:John Smith
		String listid = inDetail.getId();
		Data found = getMediaArchive().query(listid).match("name", inlabel).searchOne();
		if( found == null && Boolean.parseBoolean( (String)config.get("autocreate") ) )
		{
			found = getMediaArchive().getSearcher(listid).createNewData();
			found.setName(inlabel);
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
				return table;
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
