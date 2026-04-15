package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Collection;

import org.entermediadb.ai.automation.agents.ToolsCallingAgent;
import org.entermediadb.ai.classify.EmbeddingManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class QuestionManagerAgent extends ToolsCallingAgent
{
    @Override
    public void process(AgentContext inContext)
    {
        AgentEnabled currentEnabled = inContext.getCurrentAgentEnable();
        JSONObject params = currentEnabled.getParentAgentEnabled().getAgentParameterValues();

        if (params != null)
        {

            String name = (String) params.get("name");
            String email = (String) params.get("email");
            String question = (String) params.get("question_or_issue");
            inContext.put("question", question);
            EmbeddingManager embeddings = (EmbeddingManager) getMediaArchive().getBean("embeddingManager");

            JSONArray embeddingids = (JSONArray) inContext.getContext("embeddings");
            LlmResponse response = embeddings.findAnswer(inContext, embeddingids, question);

            inContext.addContext("answer", response.getRawResponse());
        }

        super.process(inContext);
    }

}
