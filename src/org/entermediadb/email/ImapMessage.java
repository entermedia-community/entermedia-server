package org.entermediadb.email;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;

public class ImapMessage
{
    private Message message;

    public ImapMessage(Message inMessage) {
        message = inMessage;
    }

    public Message getMessage()
    {
        return message;
    }

    // Getters (or use Lombok if available)
    public String getMessageId()
    {
        String messageId = null;
        try
        {
            String[] header = message.getHeader("Message-ID");
            if (header != null && header.length > 0)
            {
                messageId = header[0];
            }
        }
        catch (Exception ex)
        {
            // Ignore and return null
        }

        return messageId;
    }

    public String getSubject()
    {
        try
        {
            return message.getSubject();
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public String getFrom()
    {
        try
        {
            return message.getFrom()[0].toString();
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public String getBody()
    {
        try
        {
            return getTextContent(message);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public String getDate()
    {
        try
        {
            return message.getSentDate().toString();
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private String getTextContent(Message message) throws Exception
    {
        Object content = message.getContent();
        if (content == null)
        {
            return "";
        }
        if (content instanceof String)
        {
            return (String) content;
        }
        else
            if (content instanceof Multipart)
            {
                Multipart multipart = (Multipart) content;
                for (int i = 0; i < multipart.getCount(); i++)
                {
                    BodyPart part = multipart.getBodyPart(i);
                    if (part.isMimeType("text/plain"))
                    {
                        try
                        {
                            return (String) part.getContent();
                        }
                        catch (Exception ex)
                        {
                            // Ignore and try next part
                        }
                    }
                }
                return "No text content found";
            }
        return content.toString();
    }

}
