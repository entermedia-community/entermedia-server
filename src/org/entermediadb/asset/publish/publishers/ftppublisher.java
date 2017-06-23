package org.entermediadb.asset.publish.publishers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.users.User;
import org.openedit.users.UserManager;

public class ftppublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(ftppublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive,Asset asset, Data inPublishRequest,  Data destination, Data preset)
	{
		try
		{
			PublishResult result = checkOnConversion(mediaArchive,inPublishRequest,asset,preset);
			if( result != null)
			{
				return result;
			}
			
			result = new PublishResult();

			Page inputpage = findInputPage(mediaArchive,asset,preset);
			String servername = destination.get("server");
			String username = destination.get("username");
			String url = destination.get("url");
			
			log.info("Publishing ${asset} to ftp server ${servername}, with username ${username}.");
			
			FTPClient ftp = new FTPClient();
			
			ftp.connect(servername);
			ftp.enterLocalPassiveMode();
			
			//check to see if connected
			int reply = ftp.getReplyCode();
			if(!FTPReply.isPositiveCompletion(reply))
			{
				result.setErrorMessage("Unable to connect to ${servername}, error code: ${reply}");
				ftp.disconnect();
				return result;
			}
			String password = destination.get("password");
			//get password and login
			if(password == null)
			{
				UserManager userManager = mediaArchive.getUserManager();
				User user = userManager.getUser(username);
				password = userManager.decryptPassword(user);
			}
				
			ftp.login(username, password);
			reply = ftp.getReplyCode();
			if(!FTPReply.isPositiveCompletion(reply))
			{
				result.setErrorMessage("Unable to login to ${servername}, error code: ${reply}");
				ftp.disconnect();
				return result;
			}
			ftp.setFileTransferMode(FTPClient.BINARY_FILE_TYPE);
			
			//change paths if necessary
			if(url != null && url.length() > 0)
			{
				ftp.makeDirectory(url);
				ftp.changeWorkingDirectory(url);
				reply = ftp.getReplyCode();
				if(!FTPReply.isPositiveCompletion(reply))
				{
					result.setErrorMessage("Unable to to cd to ${url}, error code: ${reply}");
					ftp.disconnect();
					return result;
				}
			}
			
			String exportname = inPublishRequest.get("exportname");

			ftp.storeFile(exportname, inputpage.getInputStream());
			reply = ftp.getReplyCode();
			if(!FTPReply.isPositiveCompletion(reply))
			{
				result.setErrorMessage("Unable to to send file, error code: ${reply}");
				ftp.disconnect();
				return result;
			}
			
			if(ftp.isConnected())
			{
				ftp.disconnect();
			}
			result.setComplete(true);
			log.info("publishished  ${asset} to FTP server ${servername}");
			return result;
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		
	}
}