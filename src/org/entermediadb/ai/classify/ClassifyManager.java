package org.entermediadb.ai.classify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.BaseAiManager;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONObject;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.users.User;

public class ClassifyManager extends BaseAiManager
{
	private static final Log log = LogFactory.getLog(ClassifyManager.class);

	public LlmConnection getLlmNamingServer()
	{
		return getMediaArchive().getLlmConnection("classifyAsset");
	}

	public LlmConnection getEntityClassificationLlmConnection()
	{
		return getMediaArchive().getLlmConnection("classifyEntity");
	}

	// public void processInformaticsOnAssets(ScriptLogger inContext, MultiValued inConfig,
	// Collection<MultiValued> assets)
	public void processAssets(InformaticsContext inContext)
	{
		Collection pageofhits = inContext.getAssetsToProcess();
		int count = 1;

		if (pageofhits == null || pageofhits.isEmpty())
		{
			return;
		}

		inContext.headline("Classifying: " + pageofhits.size() + " Assets");

		for (Iterator iterator = pageofhits.iterator(); iterator.hasNext();)
		{
			MultiValued asset = (MultiValued) iterator.next();

			String mediatype = getMediaArchive().getMediaRenderType(asset);
			if (mediatype.equals("default"))
			{
				// inContext.info(inConfig.get("bean") + " - Skipping asset " + asset);
				continue;
			}

			try
			{
				long startTime = System.currentTimeMillis();

				// inContext.info(inConfig.get("bean") + " - Analyzing asset ("+count+"/"+assets.size()+")" +
				// asset.getName());
				count++;

				processOneAsset(inContext, asset);

				long duration = (System.currentTimeMillis() - startTime);
				inContext.info("" + asset.getName() + ": took " + (duration > 1000L ? duration / 1000L + "s" : duration + "ms"));
			}
			catch (Exception e)
			{
				inContext.error("LLM Error", e);
				asset.setValue("llmerror", true);
				continue;
			}
		}
	}

	protected void processOneAsset(InformaticsContext inContext, MultiValued asset) throws Exception
	{
		Collection allaifields = getMediaArchive().getAssetPropertyDetails().findAiCreationProperties();
		Collection<PropertyDetail> aifields = new ArrayList();

		String mediatype = getMediaArchive().getMediaRenderType(asset);

		for (Iterator iterator2 = allaifields.iterator(); iterator2.hasNext();)
		{
			PropertyDetail aifield = (PropertyDetail) iterator2.next();
			// if( mediatype.equals("document") )
			// {
			// TODO: add better way to have media type specific fields
			// continue;
			// }
			if (!asset.hasValue(aifield.getId()))
			{
				if (aifield.getId().equals("documenttype") && !mediatype.equals("document"))
				{
					continue;
				}
				aifields.add(aifield);
			}
		}

		if (!aifields.isEmpty())
		{
			InformaticsContext agentcontext = new InformaticsContext(inContext);
			agentcontext.addContext("asset", asset);
			agentcontext.addContext("data", asset);
			agentcontext.addContext("aifields", aifields);

			LlmConnection llmconnection = getLlmNamingServer();

			String functionname = "classifyAsset";

			String base64EncodedString = null;

			String textContent = null;

			Collection<PropertyDetail> contextFields = new ArrayList<PropertyDetail>();

			Searcher assetsearcher = getMediaArchive().getAssetSearcher();
			contextFields.add(assetsearcher.getDetail("name"));
			contextFields.add(assetsearcher.getDetail("assettype"));

			if (asset.hasValue("longcaption") && asset.get("longcaption").length() > 0)
			{
				contextFields.add(assetsearcher.getDetail("longcaption"));
			}
			if (asset.hasValue("keywords") && asset.getValues("keywords").size() > 0)
			{
				contextFields.add(assetsearcher.getDetail("keywords"));
			}

			if (!aifields.isEmpty())
			{
				if (mediatype.equals("text"))
				{
					String fulltext = getMediaArchive().getAssetSearcher().getFulltext(asset);
					if (fulltext == null)
					{
						inContext.error("Text has no text: " + asset);
						asset.setValue("llmerror", true);
						return;
					}
					if (fulltext.length() > 4000)
					{
						fulltext = fulltext.substring(0, Math.min(fulltext.length(), 4000));
					}
					asset.setValue("markdowncontent", fulltext);
					mediatype = "document";
				}

				if (mediatype.equals("image"))
				{
					base64EncodedString = loadBase64Image(asset, "image3000x3000");

					if (base64EncodedString == null)
					{
						inContext.error("Image missing for asset: " + asset);
						asset.setValue("llmerror", true);
						return;
					}
					functionname = functionname + "_image";

				}
				else
					if (mediatype.equals("document"))
					{
						textContent = asset.get("markdowncontent");

						if (textContent == null || textContent.trim().length() == 0)
						{
							textContent = (String) asset.getValue("fulltext");
						}

						if (textContent == null || textContent.trim().length() == 0)
						{
							log.error("Document has no text: " + asset);
							asset.setValue("llmerror", true);
							return;
						}
						functionname = functionname + "_document";
					}
					else
						if (mediatype.equals("video") || mediatype.equals("audio"))
						{
							textContent = loadTranscript(asset);

							if (textContent == null)
							{
								log.error("Video missing for asset: " + asset);
								asset.setValue("llmerror", true);
								return;
							}
							functionname = functionname + "_transcript";
						}
						else
						{
							/// Check for text type
							log.info("Skipping media type: " + mediatype + " for asset: " + asset);
							asset.setValue("llmerror", true);
							return;
						}
			}

			if (textContent != null && textContent.length() > 4000)
			{
				textContent = textContent.substring(0, Math.min(4000, textContent.length()));
			}

			RenderValues rendervalues = new RenderValues();
			rendervalues.setData(asset);
			rendervalues.setInFields(contextFields);
			rendervalues.setMediaArchive(getMediaArchive());
			agentcontext.put("rendervalues", rendervalues);
			agentcontext.addContext("contextfields", contextFields);

			LlmResponse results = llmconnection.callClassifyFunction(agentcontext, functionname, base64EncodedString, textContent);

			if (results != null)
			{
				JSONObject arguments = results.getMessageStructured();
				if (arguments != null)
				{

					Map metadata = (Map) arguments.get("metadata");
					if (metadata == null || metadata.isEmpty())
					{
						log.error("No metadata return on asset: " + asset.getName());
						return;
					}
					Map datachanges = new HashMap();
					for (Iterator iterator2 = metadata.keySet().iterator(); iterator2.hasNext();)
					{
						String inKey = (String) iterator2.next();
						PropertyDetail detail = getMediaArchive().getAssetPropertyDetails().getDetail(inKey);
						if (detail != null)
						{
							String value = (String) metadata.get(inKey);
							if (detail.isMultiValue())
							{
								Collection<String> values = Arrays.asList(value.split(","));
								datachanges.put(detail.getId(), values);
								asset.addValues(detail.getId(), values);
							}
							else
								if (detail.isList())
								{
									String listId = value.split("\\|")[0];
									datachanges.put(detail.getId(), listId);
									asset.setValue(detail.getId(), listId);
								}
								else
								{
									datachanges.put(detail.getId(), value);
									asset.setValue(detail.getId(), value);
								}
						}
					}

					// Save change event
					User agent = getMediaArchive().getUser("agent");
					if (agent != null)
					{
						getMediaArchive().getEventManager().fireDataEditEvent(getMediaArchive().getAssetSearcher(), agent, "assetgeneral", asset, datachanges);
					}
				}
				else
				{
					log.info("Asset " + asset.getId() + " " + asset.getName() + " - Nothing Detected.");
				}
			}
		}

	}

	public void processRecords(InformaticsContext inContext)
	{
		Collection<MultiValued> hits = inContext.getRecordsToProcess();
		inContext.headline("Classifying " + hits.size() + " entities");

		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			MultiValued entity = (MultiValued) iterator.next();

			String moduleid = entity.get("entitysourcetype");
			if (moduleid == null)
			{
				inContext.info("Skipping entity with no source type: " + entity.getId() + " " + entity.getName());
				continue;
			}

			try
			{
				long startTime = System.currentTimeMillis();

				// inContext.info("Classiffying :" + entity.getName());

				processOneEntity(inContext, entity, moduleid);

				long duration = (System.currentTimeMillis() - startTime);
				inContext.info(entity.getName() + ": took " + (duration > 1000L ? duration / 1000L + "s" : duration + "ms") + "ms");

			}
			catch (Exception e)
			{
				inContext.error("LLM Error ", e);
				entity.setValue("llmerror", true);
			}
		}
	}

	protected void processOneEntity(InformaticsContext inContext, MultiValued inEntity, String inModuleId) throws Exception
	{
		Collection detailsfields = getMediaArchive().getSearcher(inModuleId).getDetailsForView(inModuleId + "general");

		Collection<PropertyDetail> fieldsToFill = new ArrayList<PropertyDetail>();
		Collection<PropertyDetail> contextFields = new ArrayList<PropertyDetail>();

		for (Iterator iterator = detailsfields.iterator(); iterator.hasNext();)
		{
			PropertyDetail field = (PropertyDetail) iterator.next();
			if (inEntity.hasValue(field.getId()))
			{
				contextFields.add(field);
			}
			else
				if (field.get("aicreationcommand") != null)
				{
					fieldsToFill.add(field);
				}
		}

		InformaticsContext agentcontext = new InformaticsContext(inContext);

		if (contextFields.size() == 0)
		{
			log.info("No context fields found for entity: " + inEntity.getId() + " " + inEntity.getName());
			return;
		}

		if (fieldsToFill.isEmpty())
		{
			log.info("No fields to fill for entity: " + inEntity.getId() + " " + inEntity.getName());
			return;
		}
		else
		{
			agentcontext.put("entity", inEntity);
			agentcontext.put("data", inEntity);

			RenderValues rendervalues = new RenderValues();
			rendervalues.setData(inEntity);
			rendervalues.setInFields(contextFields);
			rendervalues.setMediaArchive(getMediaArchive());
			agentcontext.put("rendervalues", rendervalues);
			agentcontext.put("contextfields", contextFields);

			agentcontext.put("fieldstofill", fieldsToFill);

			boolean isDocPage = inEntity.get("entitydocument") != null;
			if (isDocPage)
			{
				agentcontext.put("docpage", isDocPage);
				// base64EncodedString = loadImageContent(inEntity);
			}

			try
			{
				LlmConnection llmconnection = getEntityClassificationLlmConnection();

				LlmResponse results = llmconnection.callClassifyFunction(agentcontext, "classifyEntity", null);

				if (results != null)
				{
					JSONObject arguments = results.getMessageStructured();
					if (arguments != null)
					{

						Map metadata = (Map) arguments.get("metadata");
						if (metadata == null || metadata.isEmpty())
						{
							inEntity.setValue("llmerror", true);
							return;
						}
						Map datachanges = new HashMap();

						Searcher searcher = getMediaArchive().getSearcher(inModuleId);
						for (Iterator iterator2 = metadata.keySet().iterator(); iterator2.hasNext();)
						{
							String inKey = (String) iterator2.next();
							PropertyDetail detail = searcher.getDetail(inKey);
							if (detail != null)
							{
								String value = (String) metadata.get(inKey);
								if (detail.isList())
								{
									String listId = value.split("\\|")[0];
									datachanges.put(detail.getId(), listId);
								}
								else
									if (detail.isMultiValue())
									{
										Collection<String> values = Arrays.asList(value.split(","));
										datachanges.put(detail.getId(), values);
									}
									else
									{
										datachanges.put(detail.getId(), value);
									}
							}
						}

						for (Iterator iterator2 = datachanges.keySet().iterator(); iterator2.hasNext();)
						{
							String inKey = (String) iterator2.next();
							Object value = datachanges.get(inKey);

							inEntity.setValue(inKey, value);
							log.info("AI updated field " + inKey + ": " + metadata.get(inKey));
						}
					}
					else
					{
						log.info("Entity " + inEntity.getId() + " " + inEntity.getName() + " - Nothing Detected.");
					}
				}
			}
			catch (Exception e)
			{
				log.error("Error generating metadata for entity " + inEntity, e);
				inEntity.setValue("llmerror", true);
			}
		}

	}

}
