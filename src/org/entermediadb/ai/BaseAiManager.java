package org.entermediadb.ai;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.util.JsonUtil;
import org.entermediadb.manager.BaseManager;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.profile.UserProfile;
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public class BaseAiManager extends BaseManager implements ChatMessageHandler
{
	private static final Log log = LogFactory.getLog(BaseAiManager.class);
	
	/*
	public Map<String, String> getModels()
	{
		Map<String, String> models = new HashMap<>();
		String visionmodel = getMediaArchive().getCatalogSettingValue("llmvisionmodel");
		if(visionmodel == null) {
			visionmodel = "gpt-5-nano";
		}
		models.put("vision", visionmodel);
		
		String entityclassificationmodel = getMediaArchive().getCatalogSettingValue("llmentityclassificationmodel");
		if(entityclassificationmodel == null) {
			entityclassificationmodel = "qwen3_4b";
		}
		models.put("entityclassification", entityclassificationmodel);
		
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
	*/
	protected Collection<PropertyDetail> loadActiveDetails(String inModuleId)
	{
		//TODO: Cache these!!
		Collection detailsviews = getMediaArchive().query("view").exact("moduleid", inModuleId).exact("systemdefined", false).search();  //Cache this

		if( detailsviews == null)
		{
			return null;
		}
		
		Collection<PropertyDetail> detailsfields = new ArrayList<PropertyDetail>();

		for (Iterator iterator = detailsviews.iterator(); iterator.hasNext();)
		{
			Data view = (Data) iterator.next();
			Collection viewfields = getMediaArchive().getSearcher(inModuleId).getDetailsForView(view);
			if( viewfields != null)
			{
				for (Iterator iterator2 = viewfields.iterator(); iterator2.hasNext();)
				{
					PropertyDetail detail = (PropertyDetail) iterator2.next();
					detailsfields.add(detail);
				}
			}
		}
		return detailsfields;
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
	
	
	protected String loadBase64Png(Data inAsset, String imagesize)
	{
		ContentItem item = getMediaArchive().getGeneratedContent(inAsset, imagesize);
		if(!item.exists())
		{
			log.info("Missing " + imagesize + " generated image for asset ("+inAsset.getId()+") " + inAsset.getName());
			return null;
		}
		return loadBase64Image(item);
	}
	
	protected String loadBase64Png(ContentItem item)
	{
		if(!item.exists())
		{
			log.info("Missing generated image " + item.getAbsolutePath());
			return null;
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		long starttime = System.currentTimeMillis();
		ArrayList<String> args = new ArrayList<String>();
		args.add("-density");
		args.add("300");
		args.add(item.getAbsolutePath());
		args.add("-antialias"); 
		args.add("-resize");
		args.add("1500x1500>");
		args.add("-background");
		args.add("white");
		args.add("-alpha");
		args.add("remove");
		args.add("-alpha");
		args.add("off");
		args.add("-strip");
		args.add("png:-");
		
		
		//convert  -density 600  Ford.pdf[3] -antialias -background white -alpha remove  -strip  -resize 11% ford4.png 

		
		Exec exec = (Exec)getMediaArchive().getBean("exec");
		exec.runExecStream("convert", args, output, 5000);
		
		byte[] bytes = output.toByteArray();  // Read InputStream as bytes
		String base64EncodedString = Base64.getEncoder().encodeToString(bytes); // Encode to Base64
		
		long duration = (System.currentTimeMillis() - starttime) ;
		log.info("Loaded and encoded " + item.getName() + " in "+duration+"ms");
		
		if(base64EncodedString == null || base64EncodedString.length() < 100)
		{
			return null;
		}
		
		return "data:image/png;base64," + base64EncodedString;

	}
	
	protected String loadBase64Image(Data inAsset, String imagesize)
	{
		ContentItem item = getMediaArchive().getGeneratedContent(inAsset, imagesize);
		if(!item.exists())
		{
			log.info("Missing " + imagesize + " generated image for asset ("+inAsset.getId()+") " + inAsset.getName());
			return null;
		}
		return loadBase64Image(item);
	}
	
	protected String loadBase64Image(ContentItem item)
	{
		if(!item.exists())
		{
			log.info("Missing generated image " + item.getAbsolutePath());
			return null;
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		long starttime = System.currentTimeMillis();
		ArrayList<String> args = new ArrayList<String>();
		args.add(item.getAbsolutePath());
		args.add("-resize");
		args.add("1500x1500>");
		args.add("jpg:-");
		Exec exec = (Exec)getMediaArchive().getBean("exec");

		ExecResult result = exec.runExecStream("convert", args, output, 5000);
		byte[] bytes = output.toByteArray();  // Read InputStream as bytes
		String base64EncodedString = Base64.getEncoder().encodeToString(bytes); // Encode to Base64
		long duration = (System.currentTimeMillis() - starttime) ;
		log.info("Loaded and encoded " + item.getName() + " in "+duration+"ms");
		
		return "data:image/jpeg;base64," + base64EncodedString;

	}
	
	protected String loadTranscript(Data inAsset)
	{
		Searcher captionSearcher = getMediaArchive().getSearcher("videotrack");
		Data inTrack = captionSearcher.query().exact("assetid", inAsset.getId()).searchOne();
		if( inTrack != null)
		{
			String status = inTrack.get("transcribestatus");
			if(status != null && status.equals("complete"))
			{
				Collection captions = (Collection) inTrack.getValue("captions");
				if( captions != null)
				{
					StringBuffer fulltext = new StringBuffer();
					for (Iterator iterator = captions.iterator(); iterator.hasNext();)
					{
						Map caption = (Map) iterator.next();
						String text = (String) caption.get("cliplabel");
						if( text != null)
						{
							fulltext.append(text);
							fulltext.append(" ");
						}
					}
					return fulltext.toString();
				}
			}
		}
		return null;
	}
	
	protected Map<String, Collection> groupByModule(Collection<MultiValued> inPendingrecords)
	{
		Map<String,Collection> groupbymodule = new HashMap();
		for (Iterator iterator = inPendingrecords.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			String moduleid = data.get("entitysourcetype");
			Collection tosave = groupbymodule.get(moduleid);
			if ( tosave ==  null)
			{
				tosave = new ArrayList();
				groupbymodule.put(moduleid,tosave);
			}
			tosave.add(data);
		}
		return groupbymodule;
	}
	
	protected Collection<PropertyDetail> populateFields(String inModuleId, MultiValued inData, Collection<PropertyDetail> inExcludeFields)
	{
		Collection<PropertyDetail> detailsfields = loadActiveDetails(inModuleId);

		Collection<PropertyDetail> contextfields = new ArrayList<PropertyDetail>();
		
		Set<String> contextfieldids = new HashSet<String>();
		
		Set<String> excludeids = new HashSet<String>();
		for (Iterator iterator = inExcludeFields.iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			excludeids.add(detail.getId());
		}

		for (Iterator iter = detailsfields.iterator(); iter.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iter.next();
			if(excludeids.contains(detail.getId()) || contextfieldids.contains(detail.getId()))
			{
				continue;
			}
			contextfields.add(detail);
			contextfieldids.add(detail.getId());
		}
		
		if(!inModuleId.equals("asset") && !contextfieldids.contains("fulltext") && inData.get("pagenum") == null  )
		{
			addPrimaryMediaFulltext(inData, contextfields);
		}
		
		return contextfields;
	}
	
	private void addPrimaryMediaFulltext(MultiValued inData, Collection<PropertyDetail> contextfields) {
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
							PropertyDetail fieldMap = new PropertyDetail();
							fieldMap.setName("Parsed Document Content");
							fieldMap.setId("fulltext");
							
							JsonUtil jsonutils = new JsonUtil();
							inData.setValue("fulltext", jsonutils.escape(fulltext));
							
							contextfields.add(fieldMap);
						}
					}
				}
			}
		}
	}

	@Override
	public LlmResponse processMessage(MultiValued inMessage, AgentContext inAgentContext)
	{
		throw new OpenEditException("Not implemented");
	}

}
