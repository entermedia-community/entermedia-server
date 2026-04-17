package org.entermediadb.email.agents;

import java.util.Collection;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.automation.agents.ToolsCallingAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.email.ImapMailbox;
import org.entermediadb.email.ImapMessage;
import org.json.simple.JSONObject;

public class ImapMailCheckerAgent extends ToolsCallingAgent
{
  private static final Log log = LogFactory.getLog(ImapMailCheckerAgent.class);

  @Override
  public void process(AgentContext inContext)
  {
    AgentEnabled currentEnabled = inContext.getCurrentAgentEnable();

    String server = (String) inContext.getContextValue("mailserver");
    int serverport = ((Long) inContext.getContextValue("mailport")).intValue();
    String username = (String) inContext.getContextValue("mailusername");
    String password = (String) inContext.getContextValue("mailpassword");

    ImapMailbox mailbox = new ImapMailbox(server, serverport, username, password, true);
    Collection<ImapMessage> messages = mailbox.getInboxUnread();

    log.info(messages);

    if (messages != null && messages.size() > 0)
    {
      inContext.put("newmessages", messages);

      inContext.info("Found " + messages.size() + " new messages");

      AgentContext subContext = createAgentContext(inContext, currentEnabled);

      String agentid = currentEnabled.getAgentData().getId();
      try
      {
        for (ImapMessage message : messages)
        {
          String fromString = message.getFrom();
          String subjectString = message.getSubject();
          String contentString = message.getBody();
          String sentDate = message.getDate();

          JSONObject params = new JSONObject();
          params.put("from", fromString);
          params.put("subject", subjectString);
          params.put("content", contentString);
          params.put("sentdate", sentDate);

          currentEnabled.setAgentParameterValues(params);

          subContext.put("agentid", agentid);
          subContext.put("agentoutput", "From: " + fromString + "\\nSubject: " + subjectString + "\\nMessage: " + contentString + "\\nSentDate: " + sentDate);

          super.process(subContext);
        }

        mailbox.moveToInProgress(messages);
      }
      catch (Exception e)
      {
        inContext.error("Error processing email messages: " + e.getMessage());
      }
    }
    else
    {
      inContext.info("No messages found");
      super.process(inContext);
    }
  }
}
