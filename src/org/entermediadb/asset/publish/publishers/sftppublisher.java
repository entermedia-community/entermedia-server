package org.entermediadb.asset.publish.publishers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.orders.Order;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.entermediadb.asset.util.ssh.SftpUtil;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.users.User;
import org.openedit.users.UserManager;

public class sftppublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(sftppublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive,Order inOrder, Data inOrderItem, Data inDestination, Data inPreset, Asset inAsset)
	{
		
		try
		{
			PublishResult result = checkOnConversion(mediaArchive,inOrderItem,inAsset,inPreset);
			if( result != null)
			{
				return result;
			}

			result = new PublishResult();

			Page inputpage = findInputPage(mediaArchive,inAsset,inPreset);
			String servername = inDestination.get("server");
			String username = inDestination.get("username");
			String password = inDestination.get("password");
			String path = inDestination.get("url");

			SftpUtil sftp = new SftpUtil();
			sftp.setHost(servername);
			sftp.setUsername(username);

			//get password and login
			if(password == null)
			{
				UserManager userManager = mediaArchive.getUserManager();		
				User user = userManager.getUser(username);
				password = userManager.decryptPassword(user);
			}
			sftp.setPassword(password);
			log.info("Publishing ${asset} to sftp server ${servername}, with username ${username}.");
			
			//change paths if necessary
			if(path != null && path.length() > 0)
			{
				sftp.makeDirs(path);
				//sftp.cd(path);
			}
			else
			{
				path = "";
			}
			String exportname = inOrderItem.get("itemexportname");
			//export name should have a leading /
			if( !exportname.startsWith("/") )
			{
				exportname = "/" + exportname;
			}
			sftp.sendFileToRemote(inputpage.getInputStream(), path + exportname );
			
			result.setComplete(true);
			log.info("publishished  ${asset} to sftp server ${servername}.");
			sftp.disconnect();
			return result;
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		
	}
}
