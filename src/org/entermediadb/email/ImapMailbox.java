package org.entermediadb.email;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import javax.mail.*;
import javax.mail.search.FlagTerm;
import org.openedit.OpenEditException;

public class ImapMailbox
{
    private String host;
    private int port;
    private final String inboxFolderName = "INBOX";
    public Date lastCheckedDate;
    private boolean useSsl = true;
    private String username;
    private String password;

    private Store store;

    public ImapMailbox() {}

    public ImapMailbox(String inHost, int inPort, String inUsername, String inPassword, boolean inUseSsl) {
        host = inHost;
        port = inPort;
        username = inUsername;
        password = inPassword;
        useSsl = inUseSsl;

        try
        {
            Properties props = new Properties();
            String protocol = useSsl ? "imaps" : "imap";
            props.put("mail." + protocol + ".host", host);
            props.put("mail." + protocol + ".port", port);

            Session session = Session.getInstance(props);
            store = session.getStore(protocol);
        }
        catch (Exception ex)
        {
            throw new OpenEditException("Error checking email: " + ex.getMessage(), ex);
        }
    }

    public void connect()
    {
        try
        {
            if (store == null || !store.isConnected())
            {
                Properties props = new Properties();
                String protocol = useSsl ? "imaps" : "imap";
                props.put("mail." + protocol + ".host", host);
                props.put("mail." + protocol + ".port", port);

                Session session = Session.getInstance(props);
                store = session.getStore(protocol);
                store.connect(host, port, username, password);
            }
        }
        catch (Exception ex)
        {
            throw new OpenEditException("Error connecting to email server: " + ex.getMessage(), ex);
        }
    }

    public void disconnect()
    {
        try
        {
            if (store != null && store.isConnected())
            {
                store.close();
            }
            store = null;
        }
        catch (Exception ex)
        {
            throw new OpenEditException("Error disconnecting from email server: " + ex.getMessage(), ex);
        }
    }

    public Collection<ImapMessage> getInboxUnread()
    {
        try
        {
            connect();

            Folder inbox = store.getFolder(inboxFolderName);
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            Collection<ImapMessage> results = new ArrayList<>();

            for (Message message : messages)
            {
                results.add(new ImapMessage(message));
            }

            inbox.close(false);
            disconnect();
            lastCheckedDate = new Date();

            return results;
        }
        catch (Exception ex)
        {
            throw new OpenEditException("Error checking email: " + ex.getMessage(), ex);
        }
    }

    public Collection<ImapMessage> moveToInProgress(Collection<ImapMessage> messages)
    {
        connect();

        try
        {
            Collection<ImapMessage> movedMessages = moveEmail(messages, "INBOX", "InProgress");

            return movedMessages;
        }
        catch (Exception ex)
        {
            throw new OpenEditException("Error moving email: " + ex.getMessage(), ex);
        }
        finally
        {
            disconnect();
        }

    }

    public Collection<ImapMessage> moveToResolved(Collection<ImapMessage> messages)
    {
        connect();

        try
        {
            Collection<ImapMessage> movedMessages = moveEmail(messages, "InProgress", "Resolved");

            return movedMessages;
        }
        catch (Exception e)
        {
            throw new OpenEditException("Error moving email: " + e.getMessage(), e);
        }
        finally
        {
            disconnect();
        }

    }

    public Collection<ImapMessage> moveEmail(Collection<ImapMessage> messages, String sourceFolderName, String targetFolderName) throws Exception
    {
        connect();

        Folder sourceFolder = store.getFolder(sourceFolderName);
        Folder targetFolder = store.getFolder(targetFolderName);

        if (!targetFolder.exists())
        {
            throw new OpenEditException("Target folder does not exist: " + targetFolderName);
        }

        sourceFolder.open(Folder.READ_WRITE);
        targetFolder.open(Folder.READ_WRITE);

        Message[] sourceMessages = sourceFolder.getMessages();

        for (ImapMessage message : messages)
        {
            for (Message sourceMessage : sourceMessages)
            {
                String sourceMessageId = null;
                String[] header = sourceMessage.getHeader("Message-ID");
                if (header != null && header.length > 0)
                {
                    sourceMessageId = header[0];
                }
                if (sourceMessageId == null)
                {
                    continue;
                }
                if (sourceMessageId.equals(message.getMessageId()))
                {
                    sourceMessage.setFlag(Flags.Flag.DELETED, true);
                    targetFolder.appendMessages(new Message[] {sourceMessage});
                }
            }
        }

        sourceFolder.expunge();
        sourceFolder.close(false);
        targetFolder.close(false);

        disconnect();

        return messages;
    }

}
