package org.entermediadb.email.agents;

import java.util.Collection;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.automation.agents.ToolsCallingAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.email.ImapInbox;
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

    ImapInbox inbox = new ImapInbox(server, serverport, username, password, true);
    Collection<ImapMessage> messages = inbox.getInboxUnread();

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

    // A fake message for testing
    /*
     * try {
     * 
     * Message fakeMessage = new javax.mail.internet.MimeMessage((javax.mail.Session) null);
     * fakeMessage.setSubject("Hello!"); fakeMessage.setFrom(new InternetAddress("test@example.com"));
     * 
     * fakeMessage.setText("What is your opening hours?\\n\\n-Regards,\\nJohn Doe"); Collection<Message>
     * messages = new ArrayList<Message>(); messages.add(fakeMessage);
     * 
     * // New messages if (messages != null && messages.size() > 0) { inContext.put("newmessages",
     * messages);
     * 
     * inContext.info("Found " + messages.size() + " new messages");
     * 
     * inContext.info("Multiple child agents, invoking decision agent"); AgentContext subContext = new
     * AgentContext(inContext); subContext.put("previousagent", currentEnabled.getAgentData().getId());
     * for (Message message : messages) { subContext.put("previousoutput", "From: " +
     * message.getFrom()[0].toString() + "\\nSubject: " + message.getSubject() + "\\nMessage: " +
     * message.getContent().toString()); super.process(subContext); } } else {
     * inContext.info("No messages found"); super.process(inContext); } } catch (Exception e) {
     * inContext.error("Error processing email messages: " + e.getMessage()); }
     */

  }
}
