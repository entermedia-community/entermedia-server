package org.entermediadb.email;

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

public class ImapInbox {


    //TODO: Cache these values and only check for new messages after a certain time has passed
    String host;
    String port;
    String inboxfoldername;
    Date lastcheckdate;
    boolean useSsl = true;
    String username;
    String password;

    public Collection<Message> checkForNewMessages(String host, int port, String username, String password, boolean useSsl) 
    {
        Properties props = new Properties();
        String protocol = useSsl ? "imaps" : "imap";
        props.put("mail." + protocol + ".host", host);
        props.put("mail." + protocol + ".port", port);

        try
        {

            Session session = Session.getInstance(props);
            Store store = session.getStore(protocol);
            store.connect(host, username, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Fetch only unseen (new) messages
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            // Process messages here before closing...

            inbox.close(false);
            store.close();
            return java.util.Arrays.asList(messages);
         }
         catch ( Exception ex )
         {
            throw new OpenEditException("Error checking email: " + ex.getMessage(), ex);           
         }
    }

}
