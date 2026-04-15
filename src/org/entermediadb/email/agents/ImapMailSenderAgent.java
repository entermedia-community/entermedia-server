package org.entermediadb.email.agents;

import javax.mail.MessagingException;
import org.entermediadb.ai.BaseAgent;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.AgentEnabled;
import org.entermediadb.email.PostMail;

public class ImapMailSenderAgent extends BaseAgent
{
    public void process(AgentContext inContext)
    {
        AgentEnabled currentEnabled = inContext.getCurrentAgentEnable();

        String server = (String) inContext.getContextValue("mailserver");
        int serverport = ((Long) inContext.getContextValue("mailport")).intValue();
        String username = (String) inContext.getContextValue("mailusername");
        String password = (String) inContext.getContextValue("mailpassword");

        // TODO Send email
        String to = (String) inContext.getContextValue("email");
        String subject = "Re: " + inContext.getContextValue("emailcontent");
        String content = (String) inContext.getContextValue("reply");
        if (content == null)
        {
            inContext.error("No reply content found in context for email response");
            return;
        }

        try
        {
            PostMail postMail = new PostMail();
            postMail.setSmtpServer(server);
            postMail.setPort(serverport);
            postMail.setSmtpUsername(username);
            postMail.setSmtpPassword(password);
            postMail.postMail(to, subject, content, username);
        }
        catch (MessagingException e)
        {
            inContext.error("Failed to send email response: " + e.getMessage());
        }

        super.process(inContext);

    }

}
