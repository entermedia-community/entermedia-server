/*
 * Created on Oct 3, 2003
 */
package org.entermediadb.email;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.OpenEditRuntimeException;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.PageStreamer;
import org.openedit.page.manage.PageManager;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.PathUtilities;
import org.openedit.util.RequestUtils;

/**
 * @author cburkey
 *
 */
public class TemplateWebEmail extends WebEmail implements Data
{
	public String getName(String inLocale) {
		return getName();
	}
	protected PostMail postMail;
	protected Page fieldMailTemplatePage;
	protected String fieldMailTemplatePath;
	protected PageManager fieldPageManager;
	protected List fieldFileAttachments;
	protected Date fieldSendDate;
	protected String fieldSourcePath;
	protected SearcherManager fieldSearcherManager;
	
	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
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
		
		if("sent".equals(inKey)){
			return Boolean.toString(isSent());
		}
		if("sent-date".equals(inKey)){
			return DateStorageUtil.getStorageUtil().formatForStorage(getSendDate()); //Use the $context.getDate to format
		}
		if("attachments".equals(inKey)){
			StringBuffer attachments = new StringBuffer();
			for (Iterator iterator = getFileAttachments().iterator(); iterator.hasNext();) {
				String filename = (String) iterator.next();
				attachments.append(filename);
				
			}
			return attachments.toString();
		}
		if("htmlmessage".equals(inKey)){
			return getMessage();
		}
		if("textmessage".equals(inKey)){
			return getAlternativeMessage();
		}
		
		
	
		return (String) getProperties().get(inKey);
	}

	public Date getSendDate() {
		return fieldSendDate;
	}

	public void setSendDate(Date inSendDate) {
		fieldSendDate = inSendDate;
		
	}
	public Collection getObjects(String inField)
	{
		Collection values = (Collection)getValue(inField);
		return values;
	}
	public boolean isSent() {
		return fieldSent;
	}

	public void setSent(boolean inSent) {
		fieldSent = inSent;
	}

	protected boolean fieldSent= false;
	
	public List getFileAttachments() {
		if (fieldFileAttachments == null) {
			fieldFileAttachments = new ArrayList();
			
		}

		return fieldFileAttachments;
	}

	public void setFileAttachments(List fileAttachments) {
		fieldFileAttachments = fileAttachments;
	}

	private static final Log log = LogFactory.getLog(TemplateWebEmail.class);
	
	public TemplateWebEmail()
	{
		super();
	}
	
	protected TemplateWebEmail( WebPageRequest inContext, Page inTemplate )
	{
		fieldWebPageContext = inContext;
		setMailTemplatePage( inTemplate );
		String[] attachments = inContext.getRequestParameters("attachment");
		List attachmentList = Arrays.asList(attachments);
		setFileAttachments(attachmentList);
		
	}
	
	public Page getMailTemplatePage() 
	{
		if( fieldMailTemplatePage == null)
		{
			try
			{
				Page templatePage = getPageManager().getPage( getMailTemplatePath()); //home is only needed when dealing with full URL's 			
				fieldMailTemplatePage = templatePage;
			}
			catch ( OpenEditException ex)
			{
				throw new OpenEditRuntimeException(ex);
			}
		}
		return fieldMailTemplatePage;
	}
	
	public void setMailTemplatePage(Page page)
	{
		fieldMailTemplatePage = page;
		if( page != null)
		{
			setMailTemplatePath(page.getPath());
		}
	}

	public String getContentType()
	{
		return getMailTemplatePage().getMimeType();
	}
	
	public String getMailTemplatePath()
	{
		return fieldMailTemplatePath;
	}

	public void setMailTemplatePath(String inMailTemplatePath)
	{
		fieldMailTemplatePath = inMailTemplatePath;
	}
	
	public void loadSettings( WebPageRequest inContext) throws OpenEditException
	{
		super.loadSettings(inContext);
		
		//retrieve system email from database if it hasn't already been specified
		if (getSearcherManager() != null && getFrom() == null)
		{
			SearcherManager sm = getSearcherManager();
			String catalogid = inContext.findValue("catalogid");
			Data setting = sm.getData(catalogid, "catalogsettings", "system_from_email");
			if (setting != null)
			{
				String from = setting.get("value");
				if (from!=null && !from.isEmpty())
				{
					setFrom(from);
				}
			}
			setting = sm.getData(catalogid,  "catalogsettings","system_from_email_name");
			if (setting != null)
			{
				String fromname = setting.get("value");
				if (fromname!=null && !fromname.isEmpty())
				{
					setFromName(fromname);
				}
			}
		}
		
		Page page = inContext.getPage();
		
		String templatePath = page.get( "emailbody");
		if (templatePath == null || templatePath.length() <= 0)
		{
			templatePath = inContext.findValue(EMAIL_TEMPLATE_REQUEST_PARAMETER);
			if(templatePath == null )
			{
				templatePath = inContext.findValue(OLDEMAIL_TEMPLATE_REQUEST_PARAMETER);
			}
		}
		if( templatePath != null)
		{
			templatePath = PathUtilities.buildRelative(templatePath,inContext.getPath());
			setMailTemplatePath(templatePath);
		}
		else
		{
			String body = inContext.getRequestParameter("body"); //Is this SPAM prof? TODO: remove
			if( body!= null && body.indexOf("Message-Id:") > 0)
			{
				throw new OpenEditException("Email message looks like spam");
			}
			setMessage(body);
		}
	}
	
	public String render(Writer outputStream) throws OpenEditException
	{

		if( getWebPageContext() != null &&  getMailTemplatePath() != null)
		{
			PageStreamer streamer = getWebPageContext().getPageStreamer().copy();
			
			Output out = new Output();
			out.setWriter(outputStream);
			streamer.setOutput(out);
	
			WebPageRequest context = getWebPageContext().copy(getMailTemplatePage());
			context.putPageStreamer(streamer);
			Map params = context.getParameterMap();
			Set keys = params.keySet();
			streamer.include(getMailTemplatePage(), context);

		}
		else if ( getMailTemplatePath() != null)
		{
			log.info("No context set. Using raw html");
			try
			{
				outputStream.write(getMailTemplatePage().getContent());
			}
			catch (IOException ex)
			{
				throw new OpenEditException( ex );
			}
		}
		else if ( getMessage() != null)
		{
			try
			{
				outputStream.write(getMessage());
			}
			catch (IOException ex)
			{
				throw new OpenEditException( ex );
			}
		}
		else
		{
			throw new OpenEditException("No template found " + getMailTemplatePath() );
		}
		String result = outputStream.toString();
		setMessage(result);
		return result; 
	}


	public PostMail getPostMail() {
		return postMail;
	}

	public void setPostMail(PostMail postMail) {
		this.postMail = postMail;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public void loadBodyFromForm(WebPageRequest inReq)  throws OpenEditException
	{
		//We are going to load up the body of this email from a form
		String path = inReq.findValue(EMAIL_TEMPLATE_REQUEST_PARAMETER);
		if(path == null )
		{
			 path = inReq.findValue(OLDEMAIL_TEMPLATE_REQUEST_PARAMETER);
		}
		if(path == null )
		{
			path = inReq.getPath();
		}
		setMailTemplatePath(path);

		PageStreamer streamer = getWebPageContext().getPageStreamer().copy();
		Output out = new Output();
		out.setWriter(new StringWriter());
		streamer.setOutput(out);
		WebPageRequest context = getWebPageContext().copy(getMailTemplatePage());
		context.putPageStreamer(streamer);
		streamer.include(getMailTemplatePage(), context);

		String content = out.getWriter().toString();
		setMailTemplatePath(null);
		String done = replaceText( content, inReq, "input");
		done = replaceText( done, inReq, "textarea");
		done = replaceText( done, inReq, "select");
		done = done.replace("<label","<br><label");
		//inReq.getPage().generate(inReq, inOut)
		
		
		setMessage(done);
	}

	private String replaceText(String inContent, WebPageRequest inReq, String inType)
	{
		StringBuffer done = new StringBuffer(inContent.length() + 10);
		//Look for any <input
		String[] inputs = inContent.split("<" + inType);
		for (int i = 0; i < inputs.length; i++)
		{
			String chunk = inputs[i];
			if( i == 0)
			{
				done.append(chunk);
				continue;
			}
//			done.append("<");
			int start = chunk.indexOf("name");
			if( start > -1)
			{
				start = chunk.indexOf("\"",start);
				if( start != -1)
				{
					start++;
					int end = chunk.indexOf("\"",start); //TODO: Use RegEx
					if( end != -1)
					{
						String name = chunk.substring(start,end);
						String value = inReq.getRequestParameter(name);
						if( value != null)
						{
//							done.append(">");
							done.append(value);
						}
					}
				}

				int end = chunk.indexOf("</" + inType);
				if( end == -1)
				{
					end = chunk.indexOf("/>")+2;
				}

				if( end == -1)
				{
					end = chunk.indexOf(">")+1;
				}
				else
				{
					end = chunk.indexOf(">",end)+1;
				}
				done.append(chunk.substring(end));
			}
			else
			{
				done.append(chunk);
			}
			//done.append("<hidden");
		}
		return done.toString();
	}

	public String getFormattedSendDate() {
		if(getSendDate() != null){
			return DateStorageUtil.getStorageUtil().formatForStorage(getSendDate());
		}
		return null;
	}

	
	public String get(String inId) {
		return (String) getProperty(inId);
	}

	
	public String getName()
	{
		return getId();
	}
	public void setName(String inName)
	{
		
	}
	
	public String getSourcePath() {
		return fieldSourcePath;
	}

	public void setSourcePath(String sourcePath) {
		fieldSourcePath = sourcePath;
	}

	public void configureAndSend(WebPageRequest inReq, String inTemplate,InternetAddress inRecipient) throws OpenEditException, MessagingException
	{
		List one = new ArrayList();
		one.add(inRecipient);
		configureAndSend(inReq,inTemplate,one);
	}	

	public void configureAndSend(WebPageRequest inReq, String inTemplate,String inRecipients) throws OpenEditException, MessagingException
	{
		setTo(inRecipients);
		configureAndSend(inReq,inTemplate,getRecipients());
	}	
	public void configureAndSend(WebPageRequest inReq, String inTemplate, List<InternetAddress> inRecipients) throws OpenEditException, MessagingException
	{
		Page emailLayout = getPageManager().getPage(inTemplate);
		if (!emailLayout.exists()) {
			throw new OpenEditException("emailLayout" + emailLayout + "does not exist or is invalid");
		}
		
		String subject = emailLayout.get("subject");
		if( subject == null)
		{
			subject = inReq.findValue("subject");
		}
		String from = emailLayout.get("from");
		if( from == null)
		{
			from = emailLayout.get("systemfromemail");
			if( getFromName() == null)
			{
				this.setFromName(emailLayout.get("systemfromemailname"));
			}
		}
		this.setFrom(from);
		this.setSubject(subject);
		this.setWebPageContext(inReq);
		this.setRecipients(inRecipients);
		
		this.setMailTemplatePage(emailLayout);
		this.send();
	}
	public void send(Recipient inRec)
	{
		setRecipient(inRec);
		send();
		
	}	
	
	public RequestUtils getRequestUtils()
	{
		return fieldRequestUtils;
	}

	public void setRequestUtils(RequestUtils inRequestUtils)
	{
		fieldRequestUtils = inRequestUtils;
	}
	public RequestUtils fieldRequestUtils;
	
	public void send(Map inObjects)
	{
		WebPageRequest req = getRequestUtils().createPageRequest(getMailTemplatePage(), null);
		PageStreamer streamer = getRequestUtils().createPageStreamer(getMailTemplatePage(),req);
		StringWriter outputStream = new StringWriter();
		Output out = new Output();
		out.setWriter(outputStream);

		streamer.setOutput(out);
		for (Iterator iterator = inObjects.keySet().iterator(); iterator.hasNext();)
		{
			String	key = (String) iterator.next();
			req.putPageValue(key, inObjects.get(key));			
		}
		streamer.include(getMailTemplatePage(), req);
		sendText(outputStream.toString());

	}
	public void send()
	{
		if (getFrom() == null)
		{
			throw new OpenEditException("Missing 'From' field.");
		}
		StringWriter out = new StringWriter();
		String output = render(out);
		sendText(output);
	}

	protected void sendText(String output)
	{
		try
		{
			String from = getFrom();
			
			if (getBCCRecipients()==null || getBCCRecipients().isEmpty())
			{
				postMail.postMail(getRecipients(),getSubject(),output,null,from,getFileAttachments(), getProperties());
			}
			else
			{
				InternetAddress target = new InternetAddress();
				target.setAddress(from);
				postMail.postMail(getRecipients(),getBCCRecipients(),getSubject(),output,null,target,getFileAttachments(), getProperties());
			}
		}
		catch ( MessagingException ex)
		{
			throw new OpenEditException(ex);
		}
		setSent(true);
		setSendDate(new Date());
	}
	
//	public void setValues(String inKey, Collection<String> inValues)
//	{
//		StringBuffer values = new StringBuffer();
//		for (Iterator iterator = inValues.iterator(); iterator.hasNext();)
//		{
//			String detail = (String) iterator.next();
//			values.append(detail);
//			if( iterator.hasNext())
//			{
//				values.append(" | ");
//			}
//		}
//		setProperty(inKey,values.toString());
//	}

	@Override
	public Object getValue(String inKey)
	{
		return get(inKey);
	}

	@Override
	public void setValue(String inKey, Object inValue)
	{
		setProperty(inKey, String.valueOf( inValue ) ); 
	}
	
	
	public String get(String inKey, String inLocale)
	{
		// TODO Auto-generated method stub
		return get(inKey + "." + inLocale);
	}
	
	@Override
	public Set keySet()
	{
		return getProperties().keySet();
	}
}
