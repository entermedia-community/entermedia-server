package org.entermediadb.email;

public class ImapMessage {
    private String messageId;
    private String subject;
    private String from;
    private String body;

    public ImapMessage(String messageId, String subject, String from, String body) {
        this.messageId = messageId;
        this.subject = subject;
        this.from = from;
        this.body = body;
    }

    // Getters (or use Lombok if available)
    public String getMessageId() { return messageId; }
    public String getSubject() { return subject; }
    public String getFrom() { return from; }
    public String getBody() { return body; }

    
}
