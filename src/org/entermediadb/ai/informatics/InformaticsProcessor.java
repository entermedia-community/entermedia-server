package org.entermediadb.ai.informatics;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.classify.ClassifyManager;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.convert.ConvertResult;
import org.entermediadb.asset.util.JsonUtil;
import org.entermediadb.scripts.ScriptLogger;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;

public abstract class InformaticsProcessor extends BaseAiManager 
{
	private static final Log log = LogFactory.getLog(ClassifyManager.class);

	public abstract void processInformaticsOnAssets(ScriptLogger inLog,MultiValued inConfig, Collection<MultiValued> assets );
	public abstract void processInformaticsOnEntities(ScriptLogger inLog,MultiValued inConfig, Collection<MultiValued> records );
	
	
	protected Map<String,Map> populateFields(String inModuleId, MultiValued inData)
	{
		Map<String, PropertyDetail> detailsfields = loadActiveDetails(inModuleId);

		Map<String,Map> contextfields = new HashMap();
		
		JsonUtil jsonutils = new JsonUtil();

		for (Iterator iter = detailsfields.keySet().iterator(); iter.hasNext();)
		{
			String key = (String) iter.next();
			PropertyDetail detail = (PropertyDetail) detailsfields.get(key);

			String fieldId = detail.getId();

			String stringValue = null;

			if (detail.isBoolean() || detail.isDate())
			{
				continue;
			}
			else if (detail.isMultiLanguage())
			{
				stringValue = inData.getText(fieldId, "en");
			}
			else if (detail.isMultiValue() || detail.isList())
			{
				Collection<String> values = inData.getValues(fieldId);
				if (values == null || values.isEmpty())
				{
					log.info("Skipping empty field: " + fieldId);
					continue;
				}

				Collection<String> textValues = new ArrayList<>();
				if (detail.isMultiValue())
				{
					textValues.addAll(values);
				}
				else if (detail.isList())
				{
					for (Iterator iter2 = values.iterator(); iter2.hasNext();)
					{
						String val = (String) iter2.next();
						Data data = getMediaArchive().getCachedData(detail.getListId(), val);
						if (data != null)
						{
							String v = data.getName();
							textValues.add(v);
						}
					}
				}
				stringValue = String.join(", ", textValues);
			}
			else
			{
				stringValue = inData.get(fieldId);
			}

			if (stringValue == null)
			{
				log.info("Skipping empty field: " + fieldId);
				continue;
			}

			String label = detail.getName();

			HashMap fieldMap = new HashMap();
			fieldMap.put("label", jsonutils.escape(label));
			fieldMap.put("text", jsonutils.escape(stringValue));

			contextfields.put(detail.getId(), fieldMap);
		}

		if (inData.getBoolean("hasfulltext"))
		{
			String fulltext = inData.get("fulltext");
			if (fulltext != null)
			{
				fulltext = fulltext.replaceAll("\\s+", " ");
				fulltext = fulltext.substring(0, Math.min(fulltext.length(), 5000));
				HashMap fieldMap = new HashMap();
				fieldMap.put("label", "Parsed Document Content");
				fieldMap.put("text", jsonutils.escape(fulltext));

				contextfields.put("fulltext", fieldMap);
			}
		}
		return contextfields;
	}

	

	protected Map<String, PropertyDetail> loadActiveDetails(String inModuleId)
	{
		//TODO: Cache these!!
		Collection detailsviews = getMediaArchive().query("view").exact("moduleid", inModuleId).exact("systemdefined", false).search();  //Cache this

		Map<String, PropertyDetail> detailsfields = new HashMap();

		for (Iterator iterator = detailsviews.iterator(); iterator.hasNext();)
		{
			Data view = (Data) iterator.next();
			Collection viewfields = getMediaArchive().getSearcher(inModuleId).getDetailsForView(view);
			for (Iterator iterator2 = viewfields.iterator(); iterator2.hasNext();)
			{
				PropertyDetail detail = (PropertyDetail) iterator2.next();
				detailsfields.put(detail.getId(), detail);
			}
		}
		return detailsfields;
	}
	
	protected String loadImageContent(MultiValued inEntity)
	{
		boolean isDocPage = inEntity.get("entitydocument") != null;
		

		String base64EncodedString = null;
		if(isDocPage && inEntity.hasValue("pagenum") )
		{
			String parentasset = inEntity.get("parentasset");
			if(parentasset != null)
			{
				Asset parentAsset = getMediaArchive().getAsset(parentasset);
				//Do the conversion with page number in it
				Map params = new HashMap();
				params.put("pagenum",inEntity.get("pagenum") );
				ConvertResult result = getMediaArchive().getTranscodeTools().createOutputIfNeeded(null,params,parentAsset.getSourcePath(), "image3000x3000.webp"); 
				if( result.isOk() )
				{
					base64EncodedString = loadBase64Image(result.getOutput());
				}
			}
		}
		else
		{
			String primarymedia = inEntity.get("primarymedia");
			Asset inPrimaryAsset = getMediaArchive().getAsset(primarymedia);
			if(inPrimaryAsset == null)
			{
				primarymedia = inEntity.get("primaryimage");
				inPrimaryAsset = getMediaArchive().getAsset(primarymedia);
			}
			if(inPrimaryAsset != null)
			{
				base64EncodedString = loadBase64Image(inPrimaryAsset, "image3000x3000");
			}
		}
		return base64EncodedString;
	}

}
