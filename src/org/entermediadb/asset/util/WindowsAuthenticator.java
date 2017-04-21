/*
 * Created on Jul 10, 2006
 */
package org.entermediadb.asset.util;

import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.users.UserManagerException;
import org.openedit.users.authenticate.AuthenticationRequest;
import org.openedit.users.authenticate.BaseAuthenticator;

import jcifs.UniAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbSession;

public class WindowsAuthenticator extends BaseAuthenticator
{
	private static final Log log = LogFactory.getLog(WindowsAuthenticator.class);

	public boolean authenticate(AuthenticationRequest inAReq) throws UserManagerException
	{
		String inServer = inAReq.get("authenticationserver");
		if (inServer == null)
		{
			return false;
		}
		// http://support.microsoft.com/default.aspx?scid=kb;EN-US;180548
		String inDomainOrBlank = inAReq.get("domain");
		if (inDomainOrBlank == null)
		{
			inDomainOrBlank = "";
		}
		//1433 
		
		
		//I think 445 is the main port we need
		
		NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(inDomainOrBlank, inAReq.getUser().getUserName(), inAReq.getPassword());
		try
		{
			InetAddress ip = null;
			// if( inServer == null)
			// {
			// String address = InetAddress.getLocalHost().getHostAddress();
			// //Localhost needs to use external IP
			// ip = InetAddress.getByName(address);
			// }
			// else
			// {
			ip = InetAddress.getByName(inServer); // ip address of your
			// windows controller
			// ip.get
			// }
			UniAddress controller = new UniAddress(ip);

			//445 is the default port
			String port = inAReq.get("authenticationserverport");
			if (port != null)
			{
				SmbSession.logon(controller, Integer.parseInt(port), auth);
			}
			else
			{
				SmbSession.logon(controller, auth);
			}
			//password may be different than what's in the xml we should set it
			//it will get encrypted and saved after login
			inAReq.getUser().setPassword(inAReq.getPassword());
			return true;
		}
		catch (Exception ex)
		{
			log.error(ex + " " + inAReq.getUserName() + " could not log in ");
			return false;
		}
		/*
		 * // Obtain a LoginContext, needed for authentication. Tell it // to
		 * use the LoginModule implementation specified by the // entry named
		 * "JaasSample" in the JAAS login configuration // file and to also use
		 * the specified CallbackHandler. LoginContext lc = null; try { lc = new
		 * LoginContext("JaasSample", new TextCallbackHandler());
		 * 
		 * LoginContext loginContext = new LoginContext( "Sample", new
		 * UsernamePasswordCallbackHandler (username, password));
		 * 
		 * loginContext.login(); // Now we're logged in, so we can get the
		 * current subject. Subject subject = loginContext.getSubject(); //
		 * Display the subject System.out.println(subject); } catch
		 * (LoginException le) { System.err.println("Cannot create LoginContext. " +
		 * le.getMessage()); System.exit(-1); } catch (SecurityException se) {
		 * System.err.println("Cannot create LoginContext. " + se.getMessage());
		 * System.exit(-1); }
		 * 
		 * try { // attempt authentication lc.login(); } catch (LoginException
		 * le) {
		 * 
		 * System.err.println("Authentication failed:"); System.err.println(" " +
		 * le.getMessage()); System.exit(-1); }
		 * 
		 * System.out.println("Authentication succeeded!");
		 */
	}

}
/*
 * 
 * 
 * Sample { PasswordLoginModule required; };
 * 
 */