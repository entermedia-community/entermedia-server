package org.entermediadb.email;

import java.util.Collection;

import javax.mail.Message;

import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;

public class ImapMailAgent extends BaseAgent
{
    public void process(AgentContext inContext)
	{
		AgentEnabled enabled = inContext.getCurrentAgentEnable();

        String server = (String)inContext.getContextValue("mailserver");
        String username = (String)inContext.getContextValue("mailusername");
        String password = (String)inContext.getContextValue("mailpassword");

        ImapInbox inbox = new ImapInbox(); //TODO: Cache these?
        Collection<Message> messages =  inbox.checkForNewMessages(server, 0, username, password, true);

        //New messages
        if (messages != null && messages.size() > 0)
        {
            inContext.put("newmessages", messages);
            inContext.info("Found " + messages.size() + " new messages");
              super.process(inContext);
        }
        else 
        {
            inContext.info("No messages");
        }

        ///String serverUrl = getServerUrl();
      // Implement email checking logic here using the serverUrl
      // For example, connect to the IMAP server and check for new emails
    
   }
    
}
