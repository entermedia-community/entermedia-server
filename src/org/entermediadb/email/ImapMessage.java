package org.entermediadb.email;

public class ImapMessage
{
    private String messageId;
    private String subject;
    private String from;
    private String body;
    private String date;

    public ImapMessage(String messageId, String subject, String from, String body, String date) {
        this.messageId = messageId;
        this.subject = subject;
        this.from = from;
        this.body = body;
        this.date = date;
    }

    // Getters (or use Lombok if available)
    public String getMessageId()
    {
        return messageId;
    }

    public String getSubject()
    {
        return subject;
    }

    public String getFrom()
    {
        return from;
    }

    public String getBody()
    {
        return body;
    }

    public String getDate()
    {
        return date;
    }

}
