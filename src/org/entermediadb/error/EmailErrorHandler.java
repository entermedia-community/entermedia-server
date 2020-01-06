/*
 * Created on Jul 1, 2004
 */
package org.entermediadb.error;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.email.PostMail;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.config.Configuration;
import org.openedit.config.XMLConfiguration;
import org.openedit.error.ErrorHandler;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.PageRequestKeys;
import org.openedit.page.manage.PageManager;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.users.UserManager;

/**
 * @author Matthew Avery, mavery@einnovation.com
 */
public class EmailErrorHandler implements ErrorHandler
{
	protected String fieldRecipientList;
	protected String fieldServer;
	protected String fieldFrom;
	protected String fieldFromName;
	
	protected boolean fieldConsumeErrors;
	protected PageManager fieldPageManager;
	protected UserManager fieldUserManager;
	protected PostMail postMail;
	private static Log log = LogFactory.getLog(EmailErrorHandler.class);
	
	public boolean handleError(Throwable error, WebPageRequest inRequest)
	//public boolean handleError( Exception error, String inPath, WebPageRequest inContext )
	{
		try
		{
			if ( fieldRecipientList == null)
			{
				configure();
			}
			Set emails = findNotificationEmailAddresses();
			if( emails.size() == 0)
			{
				log.error("No users configured to receive email error notifications");
				return false;
			}
			String server = (String) inRequest.getPageValue(PageRequestKeys.WEB_SERVER_PATH);
			
			if( server == null )
			{
				server = (String) inRequest.getPageValue("hostName");
			}
			String message = createMessage(error, inRequest, server);

			String subject = "Open Edit error: ";
			if ( server != null )
			{
				subject = "Open Edit error on " + server + ": ";
			}
			if ( error.getMessage() != null )
			{
				subject =  subject + error.getMessage();
			}
			//Loop over the groups and fill a list of users
			
			String[] recepients = (String[])emails.toArray(new String[emails.size()]);
			
			postMail.postMail(recepients,subject,null,message,getFrom(), getFromName());
		}
		catch (Exception e)
		{
			log.error( "Email Error Sender Failed: " + e );
			return false;
		}
		return isConsumeErrors();
	}

	/**
	 * Determine all the email addresses to notify for all the groups the given
	 * user is in. TODO: Move this to a business object
	 * 
	 * @param inGroups
	 *            The user for which to determine the email addresses
	 * 
	 * @return All the email addresses
	 */
	public Set findNotificationEmailAddresses() throws OpenEditException
	{
		Set emailAddresses = new HashSet();

		for (Iterator groupIter = getUserManager().getGroups().iterator(); groupIter.hasNext();)
		{
			Group group = (Group) groupIter.next();

			if (group.hasPermission("oe.error.notify"))
			{
				// Collect the email addresses of all the users in this group.
				HitTracker list = getUserManager().getUsersInGroup(group);
				for (Iterator userIter = list.iterator(); userIter.hasNext();)
				{
					User user = (User) userIter.next();
					String email = user.getEmail();

					if ((email != null) && (email.length() > 0))
					{
						emailAddresses.add(email);
					}
				}
			}
		}

		return emailAddresses;
	}

	protected String createMessage(Throwable error, WebPageRequest inRequest, String server)
	{
		StringWriter writer = new StringWriter();
		writer.write("<pre>");
		writer.write( "Requested Path: " + inRequest.getPath() + "\n\n");

		HttpServletRequest req = inRequest.getRequest();
		if ( req != null)
		{
			String header = req.getHeader("Referer");
			if ( header != null)
			{
				writer.write( "Referer: "+ header);
				writer.write( "\n");
			}
			String ipaddress = req.getRemoteAddr();
			if ( ipaddress != null)
			{
				writer.write( "Remote IP address: "+ ipaddress);
				writer.write( "\n");
			}
			String userAgent = req.getHeader("User-Agent");
			if ( userAgent != null)
			{
				writer.write( "User agent: "+ userAgent);
				writer.write( "\n");
			}
		}

		if ( server == null && req != null )
		{
			server = req.getServerName();
			if ( server != null)
			{
				writer.write( "Server: "+ server);
				writer.write( "\n");
			}
			else
			{
				server = "unknown";
			}
		}

		try
		{
			String serverIP = InetAddress.getLocalHost().getHostAddress();
			writer.write( "Server Id: " + serverIP );
			writer.write( "\n");
		} catch (UnknownHostException e)
		{
			log.error(e);
		}

		String version = (String)inRequest.getPageValue("version");
		if ( version != null)
		{
			writer.write("OE Core Version: " + version);
			writer.write("\n");
		}
		//writer.write( "Recipients: " + getRecipientList() + "\n\n");
		User user = inRequest.getUser();
		if ( user != null )
		{
			writer.write( "User: " + user.getUserName() + "\n\n" );
		}
		else
		{
			writer.write( "User: none\n\n" );
		}
		
		if ( error.getMessage() != null )
		{
			writer.write( "Error Message: " + error.getMessage() + "\n\n" );
		}
		writer.write( "Detail:\n\n" );
		error.printStackTrace( new PrintWriter( writer ) );
		writer.write("<pre>");

		
		//String message =  URLUtilities.xmlEscape( writer.toString() ); //Dont allow HTML in here

		return writer.toString();
	}

	protected boolean isConsumeErrors()
	{
		return fieldConsumeErrors;
	}
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	protected void configure() throws OpenEditException
	{
		//read in the configuration file
		//TODO: Look for a config file on the root level first
		Page config = getPageManager().getPage("/WEB-INF/errorsettings.xml");
		if ( !config.exists() )
		{
			config = getPageManager().getPage("/openedit/notification/errorsettings.xml");
		}
		if( config.exists() )
		{
			XMLConfiguration notificationConfig = new XMLConfiguration();
			notificationConfig.readXML(config.getReader());
			Configuration configuration = notificationConfig.getChild("emailsettings");
			String sender = configuration.getChildValue( "from" );
			//setsetsender );
			setFrom(sender);
			setServer( configuration.getChildValue( "smtp-server" ) );
			
			/*
			String consumeError = configuration.getChildValue("consume-error");
			if ( consumeError != null )
			{
				setConsumeErrors( Boolean.valueOf( consumeError ).booleanValue() );
			}
			*/
		}
		else
		{
			log.error("No error settings page available " );
		}
	}

	public String getServer()
	{
		return fieldServer;
	}

	public void setServer(String inServer)
	{
		fieldServer = inServer;
	}

	public String getFrom()
	{
		return fieldFrom;
	}
	
	public void setFrom(String inFrom)
	{
		fieldFrom = inFrom;
	}
	
	public String getFromName()
	{
		return fieldFromName;
	}
	
	public void setFromName(String inFromName)
	{
		fieldFromName = inFromName;
	}

	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

	public PostMail getPostMail() {
		return postMail;
	}

	public void setPostMail(PostMail postMail) {
		this.postMail = postMail;
	}

	public void sendNotification(String inSubject, String inMessage) {
		try
		{
			if ( fieldRecipientList == null)
			{
				configure();
			}
			Set emails = findNotificationEmailAddresses();
			if( emails.size() == 0)
			{
				log.error("No users configured to receive email error notifications");
				return;
			}
			
			
			String[] recepients = (String[])emails.toArray(new String[emails.size()]);
			
			postMail.postMail(recepients,inSubject,null,inMessage,getFrom(), getFromName());
		}
		catch (Exception e)
		{
			log.error( "Email Error Sender Failed: " + e );
			return ;
		}
		
		
	}

}
