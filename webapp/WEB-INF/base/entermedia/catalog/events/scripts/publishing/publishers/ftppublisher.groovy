package publishing.publishers;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.Publisher

import com.openedit.OpenEditException
import com.openedit.page.Page
import com.openedit.users.User
import com.openedit.users.UserManager

public class ftppublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(ftppublisher.class);
	
	public void publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest,  Data inDestination, Data inPreset)
	{
		throw new OpenEditException("Not implemented");	
	}
	
	public void publish(MediaArchive mediaArchive,Data inOrder, Data inOrderItem, Asset asset)
	{
		String publishdestination = inOrderItem.get("publishdestination");
		if( publishdestination == null)
		{
			publishdestination = inOrder.get("publishdestination");
		}
		String presetid = inOrderItem.get("presetid");
		if( presetid == null)
		{
			presetid = inOrder.get("presetid");
		}
		
		if(publishdestination != null)
		{
			Data destination = mediaArchive.getSearcherManager().getData( mediaArchive.getCatalogId(), "publishdestination", publishdestination);
			String servername = destination.get("server");
			String username = destination.get("username");
			String url = destination.get("url");
			
			log.info("Publishing ${asset.sourcepath} to ftp server ${servername}, with username ${username}.  Order ID was ${inOrder.id} and item id was ${inOrderItem.id}");
			
			FTPClient ftp = new FTPClient();
			
			ftp.connect(servername);
			
			//check to see if connected
			int reply = ftp.getReplyCode();
			if(!FTPReply.isPositiveCompletion(reply))
			{
				publishFailure(mediaArchive, inOrderItem, "Unable to connect to ${servername}, error code: ${reply}")
				ftp.disconnect();
				return;
			}
			
			//get password and login
			UserManager userManager = mediaArchive.getModuleManager().getBean("userManager");
			User user = userManager.getUser(username);
			if(user == null)
			{
				publishFailure(mediaArchive, inOrderItem, "Unknown user, ${username}");
				ftp.disconnect();
				return;
			}
			
			String password = userManager.decryptPassword(user);
			
			ftp.login(username, password);
			reply = ftp.getReplyCode();
			if(!FTPReply.isPositiveCompletion(reply))
			{
				publishFailure(mediaArchive, inOrderItem, "Unable to login to ${servername}, error code: ${reply}");
				ftp.disconnect();
				return;
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
					publishFailure(mediaArchive, inOrderItem, "Unable to to cd to ${url}, error code: ${reply}");
					ftp.disconnect();
					return;
				}
			}
			
			Page inputpage = findInputPage(mediaArchive,asset,presetid);
			
			String exportname= inOrderItem.get("filename");
			
			ftp.storeFile(exportname, inputpage.getInputStream());
			reply = ftp.getReplyCode();
			if(!FTPReply.isPositiveCompletion(reply))
			{
				publishFailure(mediaArchive, inOrderItem, "Unable to to send file, error code: ${reply}");
				ftp.disconnect();
				return;
			}
			
			if(ftp.isConnected())
			{
				ftp.disconnect();
			}
			
			log.info("publishished  ${asset} to FTP server ${servername}");
			inOrderItem.setProperty("status", "complete");
			Searcher itemsearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "orderitem");
			itemsearcher.saveData(inOrderItem, null);
		}
	}
}