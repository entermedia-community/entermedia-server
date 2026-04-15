package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Collection;

import org.entermediadb.ai.automation.agents.ToolsCallingAgent;
import org.entermediadb.ai.classify.EmbeddingManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class QuestionManagerAgent extends ToolsCallingAgent
{
    @Override
    public void process(AgentContext inContext)
    {
        AgentEnabled currentEnabled = inContext.getCurrentAgentEnable();
        JSONObject params = currentEnabled.getAgentParameterValues();

        if (params != null)
        {

            String name = (String) params.get("name");
            String email = (String) params.get("email");
            String content = (String) params.get("content");
            String emailtype = (String) params.get("email_type");

            inContext.put("name", name);
            inContext.put("email", email);
            inContext.put("emailcontent", content);
            inContext.put("email_type", emailtype);

            if ("question".equals(emailtype) || "query".equals(emailtype))
            {
                inContext.info("Processing query from " + name + " with email " + email);

                inContext.put("query", content);
                EmbeddingManager embeddings = (EmbeddingManager) getMediaArchive().getBean("embeddingManager");

                JSONArray embeddingids = (JSONArray) inContext.getParentContext().getContext().get("embeddings");
                LlmResponse response = embeddings.findAnswer(inContext, embeddingids, content);

                JSONObject answerraw = response.getRawResponse();
                String answer = (String) answerraw.get("answer");
                inContext.addContext("answer", answer);

                LlmConnection llmConnection = getMediaArchive().getLlmConnection("agentemailanswer");
                response = llmConnection.callStructure(inContext, "agentemailanswer");
                JSONObject raw = response.getMessageStructured();
                String reply = (String) raw.get("reply_email");
                inContext.addContext("reply", reply);
            }
            else
            {
                LlmConnection llmConnection = getMediaArchive().getLlmConnection("agentemailgreeting");
                LlmResponse response = llmConnection.callToolsFunction(inContext, "agentemailgreeting");
                JSONObject raw = response.getMessageStructured();
                String reply = (String) raw.get("greeting_email");
                inContext.addContext("reply", reply);
            }
        }

        super.process(inContext);
    }

}
