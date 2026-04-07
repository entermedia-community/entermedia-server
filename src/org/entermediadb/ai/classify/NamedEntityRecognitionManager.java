package org.entermediadb.ai.classify;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.informatics.InformaticsContext;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.data.PropertyDetail;

public class NamedEntityRecognitionManager extends ClassifyManager {
	private static final Log log = LogFactory.getLog(NamedEntityRecognitionManager.class);

	@Override
	public LlmConnection getLlmNamingServer() {
		return getMediaArchive().getLlmConnection("namedEntityRecognition");
	}

	@Override
	protected void processOneAsset(InformaticsContext inContext, MultiValued inAsset) throws Exception {
		processOneEntity(inContext, inAsset, "asset");
	}

	@Override
	// protected void processOneEntity(MultiValued inConfig, MultiValued inData,
	// String inModuleId)
	protected void processOneEntity(InformaticsContext inContext, MultiValued inData, String inModuleId) {
		Collection<PropertyDetail> autocreatefields = getMediaArchive().getSearcher(inModuleId).getPropertyDetails()
				.findAiAutoCreatedProperties();

		// TODO: Load the right AgentEnabled and config for this context
		MultiValued inConfig = inContext.getCurrentAgentEnable().getAgentData();
		// Validate tables
		if (autocreatefields.isEmpty()) {
			log.info(inConfig.get("bean") + " No fields to create in " + inData.getId() + " " + inData.getName());
			return;
		}
		Collection<PropertyDetail> contextfields = populateFields(inModuleId, inData, autocreatefields);

		if (contextfields.isEmpty()) {
			log.info(inConfig.get("bean") + " No fields to check for names in " + inData.getId() + " " + inData.getName());
			return;
		}

		AgentContext agentcontext = new AgentContext();
		agentcontext.put("data", inData);
		agentcontext.put("fieldparams", inConfig);

		RenderValues rendervalues = new RenderValues();
		rendervalues.setMediaArchive(getMediaArchive());
		rendervalues.setData(inData);
		rendervalues.setInFields(contextfields);
		agentcontext.put("rendervalues", rendervalues);
		agentcontext.put("contextfields", contextfields);

		agentcontext.put("autocreatefields", autocreatefields);

		try {
			LlmConnection llmconnection = getLlmNamingServer();
			LlmResponse results = llmconnection.callStructure(agentcontext, "namedEntityRecognition");
			Map categories = (Map) results.getMessageStructured().get("categories");
			if (categories != null) {
				for (Iterator iterator = categories.keySet().iterator(); iterator.hasNext();) {
					String sourcetype = (String) iterator.next();
					Collection values = (Collection) categories.get(sourcetype);
					if (values == null || values.isEmpty()) {
						continue;
					}
					PropertyDetail detail = getMediaArchive().getSearcher(inModuleId).getDetail(sourcetype);
					if (detail != null) {
						if (detail.isList()) {
							for (Iterator iterator2 = values.iterator(); iterator2.hasNext();) {
								String value = (String) iterator2.next();
								if (value == null || value.isEmpty()) {
									continue;
								}
								Data savedrecord = saveIfNeeded(inConfig, detail, value);
								if (savedrecord != null) {
									inData.addValue(detail.getId(), savedrecord.getId());
								}
							}

						}
					}

				}
			}
		} catch (Exception e) {
			log.info("Error running NER on: " + inData + " " + e.getMessage());
			inData.setValue("llmerror", true);
		}

		return;

	}

	protected Data saveIfNeeded(MultiValued inConfig, PropertyDetail inDetail, String inlabel) {
		String listid = inDetail.getId();
		Data found = getMediaArchive().query(listid).match("name", inlabel).searchOne();
		if (found == null) {
			found = getMediaArchive().getSearcher(listid).createNewData();
			found.setName(inlabel);
			getMediaArchive().saveData(listid, found);
		}
		return found;
	}

}
