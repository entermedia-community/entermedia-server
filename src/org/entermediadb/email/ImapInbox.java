package org.entermediadb.email;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;

import org.openedit.OpenEditException;

public class ImapInbox
{


    // TODO: Cache these values and only check for new messages after a certain time has passed
    String host;
    String port;
    String inboxfoldername;
    Date lastcheckdate;
    boolean useSsl = true;
    String username;
    String password;

    public Collection<ImapMessage> checkForNewMessages(String host, int port, String username,
            String password, boolean useSsl)
    {
        Properties props = new Properties();
        String protocol = useSsl ? "imaps" : "imap";
        props.put("mail." + protocol + ".host", host);
        props.put("mail." + protocol + ".port", port);

        try
        {
            Session session = Session.getInstance(props);
            Store store = session.getStore(protocol);
            store.connect(host, port, username, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Fetch only unseen (new) messages
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            Collection<ImapMessage> results = new ArrayList<>();
            // Process messages here before closing...
            for (Message message : messages)
            {
                String subject = message.getSubject();

                String from = "";
                if (message.getFrom() != null && message.getFrom().length > 0)
                {
                    from = message.getFrom()[0].toString();
                }

                String messageId = "";
                String[] header = message.getHeader("Message-ID");
                if (header != null && header.length > 0)
                {
                    messageId = header[0];
                }

                String body = getTextContent(message);

                Date date = message.getSentDate();

                results.add(new ImapMessage(messageId, subject, from, body, date.toString()));
            }



            inbox.close(false);
            store.close();
            // return java.util.Arrays.asList(messages);
            return results;
        }
        catch (Exception ex)
        {
            throw new OpenEditException("Error checking email: " + ex.getMessage(), ex);
        }
    }

    private String getTextContent(Message message) throws Exception
    {
        Object content = message.getContent();
        if (content instanceof String)
        {
            return (String) content;
        }
        else
            if (content instanceof javax.mail.Multipart)
            {
                javax.mail.Multipart multipart = (javax.mail.Multipart) content;
                for (int i = 0; i < multipart.getCount(); i++)
                {
                    javax.mail.BodyPart part = multipart.getBodyPart(i);
                    if (part.isMimeType("text/plain"))
                    {
                        return (String) part.getContent();
                    }
                }
                return "No text content found";
            }
        return content.toString();
    }

}
