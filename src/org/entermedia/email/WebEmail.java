package org.entermedia.email;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.page.PageRequestKeys;
import com.openedit.users.User;

public abstract class WebEmail {
	public static final String EMAIL_TEMPLATE_REQUEST_PARAMETER = "emaillayout";
	public static final String OLDEMAIL_TEMPLATE_REQUEST_PARAMETER = "e-mail_layout";

	protected List fieldRecipients;
	protected String fieldFrom;
	protected String fieldFromName;
	protected Map fieldProperties;
	protected String fieldSubject;
	protected String fieldWebServerName;
	protected WebPageRequest fieldWebPageContext;
	protected String fieldId;
	protected User fieldUser;
	protected String fieldMessage;
	protected String fieldAlternativeMessage;
	protected PostMail fieldPostMail;

	public User getUser() {
		return fieldUser;
	}

	public void setUser(User inUser) {
		fieldUser = inUser;
	}

	public String getId() {
		return fieldId;
	}

	public void setId(String inId) {
		fieldId = inId;
	}

	public String getFromName() {
		return fieldFromName;
	}

	public void setFromName(String inFromName) {
		fieldFromName = inFromName;
	}

	public Map getProperties() {
		if (fieldProperties == null) {
			fieldProperties = new HashMap();

		}

		return fieldProperties;
	}

	public void setProperty(String inKey, String inValue) {
		getProperties().put(inKey, inValue);
	}

	public void setProperties(Map inProperties) {
		fieldProperties = inProperties;
	}

	public String getProperty(String inKey) {
		if("from".equals(inKey)){
			return getFrom();
		}
		if("to".equals(inKey)){
			StringBuffer prop = new StringBuffer();
			for (int i = 0; i < getTo().length; i++) {
				String to = getTo()[i];
				prop.append(to);
				prop.append(" ");
				
			}
		}
		if("fromname".equals(inKey)){
			return getFromName();
		}
		if("subject".equals(inKey)){
			return getSubject();
		}
		if("user".equals(inKey)){
			return getUser().getUserName();
		}
		

	
		return (String) getProperties().get(inKey);
	}

	public PostMail getPostMail() {
		return fieldPostMail;
	}

	public void setPostMail(PostMail postMail) {
		this.fieldPostMail = postMail;
	}

	protected WebEmail() {

	}

	public WebPageRequest getWebPageContext() {
		return fieldWebPageContext;
	}

	public void loadSettings(WebPageRequest inContext) throws OpenEditException {
		if (getFrom() == null) {
			String from = inContext.findValue("from");
			setFrom(from);
		}
		if (getFrom() == null)
		{
			setFrom(inContext.getContentProperty("systemfromemail"));
			setFromName(inContext.getContentProperty("systemfromemailname"));
		}



		if (getSubject() == null) {
			String subject = inContext.findValue("subject");
			if( subject == null)
			{
				subject = (String)inContext.getPageValue("subject");
			}
			setSubject(subject);
		}
		if (getRecipients() == null || getRecipients().size() == 0) {
			String to = inContext.findValue("to");
			// If you set the property sendCustomerCopy to true, then the form
			// will also be emailed to the value of property email
			String copy = inContext.findValue("sendCustomerCopy");
			if (Boolean.parseBoolean(copy)
					&& inContext.findValue("email") != null) {
				to = to + ", " + inContext.findValue("email");
			}
			setTo(to);
		}

		// TODO: remove this since you can get it from url_utils
		if (getWebServerName() == null) {
			setWebServerName((String) inContext
					.getPageValue(PageRequestKeys.WEB_SERVER_PATH));
		}

		if (getWebPageContext() == null) {
			setWebPageContext(inContext);
		}
	}

	public List getRecipients() {

		return fieldRecipients;
	}

	public String[] getTo() {
		if (getRecipients() == null) {
			return null;
		}
		String[] tos = new String[getRecipients().size()];
		for (int i = 0; i < getRecipients().size(); i++) {
			Recipient rec = (Recipient) getRecipients().get(i);
			tos[i] = rec.getEmailAddress();
		}
		return tos;
	}

	
	
	public void setTo(String inTo) {
		if (inTo == null) {
			return;
		}
		List recipients = new ArrayList();
		String[] emails = inTo.split(",");
		for (int i = 0; i < emails.length; i++) {
			String value = emails[i].trim();
			Recipient recipient = parseRecipient(value);

			recipients.add(recipient);
		}
		setRecipients(recipients);
	}
	
	

	private Recipient parseRecipient(String inValue) {
		Recipient rec = new Recipient();
		inValue = inValue.trim();
		//get the name out?
		rec.setEmailAddress(inValue);

		return rec;
	}

	public String getWebServerName() {
		return fieldWebServerName;
	}

	public void setWebServerName(String inWebServerName) {
		fieldWebServerName = inWebServerName;
	}

	public String getFrom() {
		return fieldFrom;
	}

	public void setFrom(String inFrom) {
		fieldFrom = inFrom;
	}

	public String getSubject() {
		return fieldSubject;
	}

	public void setSubject(String inSubject) {
		fieldSubject = inSubject;
	}

	public void setWebPageContext(WebPageRequest inWebPageContext) {
		fieldWebPageContext = inWebPageContext;
	}

	public void setRecipients(List inList) {
		fieldRecipients = inList;
	}
	
	public List setRecipientsFromUnknown(String inAddresses)
	{
		String[] splitaddresses = null;
		//check if the string contains a semicolon if so split on the semicolon
		if(inAddresses.indexOf(";") > -1)
		{
			splitaddresses = inAddresses.split(";");
		}
		
		//no semicolons treat as all comma seperated list
		if(splitaddresses == null)
		{
			return setRecipientsFromCommas(inAddresses);
		}
		else	//check each split section for commas if so add split results to results
		{
			ArrayList<Recipient> results = new ArrayList<Recipient>();
			for (int i = 0; i < splitaddresses.length; i++)
			{
				String addresses = splitaddresses[i].trim();
				if(addresses.indexOf(",") > -1)
				{
					results.addAll(createRecipientsFromSplit(addresses, ","));
				}
				else if(addresses.length() > 0)
				{
					Recipient one = parseRecipient(addresses);
					results.add(one);
				}
			}
			setRecipients(results);
			return results;
		}
	}
	public List setRecipientsFromCommas(String inAddresses)
	{
		List recipients = createRecipientsFromSplit(inAddresses, ",");
		setRecipients(recipients);
		return recipients;	
	}
	public List setRecipientsFromSemicolon(String inAddresses)
	{
		List recipients = createRecipientsFromSplit(inAddresses, ";");
		setRecipients(recipients);
		return recipients;	
	}
	public List<Recipient> createRecipientsFromSplit(String inAddresses, String inSplit)
	{
		ArrayList<Recipient> list = new ArrayList<Recipient>();
		String[] all = inAddresses.split(inSplit);
		for (int i = 0; i < all.length; i++)
		{
			if(all[i].length() > 0)
			{
				Recipient one = parseRecipient(all[i]);
				list.add(one);
			}
		}
		return list;
	}

	public void setRecipientsFromStrings(List inToAddresses) {
		List all = new ArrayList();
		for (Iterator iter = inToAddresses.iterator(); iter.hasNext();) {
			String email = (String) iter.next();
			Recipient one = parseRecipient(email);
			all.add(one);
		}
		setRecipients(all);
	}

	public void setRecipient(Recipient inRecipient) {
		List one = new ArrayList(1);
		one.add(inRecipient);
		setRecipients(one);
	}

	public String getMessage() {
		return fieldMessage;
	}

	public void setMessage(String inMessage) {
		fieldMessage = inMessage;
	}

	public String getAlternativeMessage() {
		return fieldAlternativeMessage;
	}

	public void setAlternativeMessage(String inAlternativeMessage) {
		fieldAlternativeMessage = inAlternativeMessage;
	}

	public abstract void send() throws OpenEditException, MessagingException;
}
