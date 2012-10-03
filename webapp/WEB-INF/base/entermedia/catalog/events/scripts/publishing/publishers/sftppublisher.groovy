package publishing.publishers;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.*
import org.openedit.entermedia.util.ssh.SftpUtil

import com.openedit.OpenEditException
import com.openedit.page.Page
import com.openedit.users.User
import com.openedit.users.UserManager

public class sftppublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(ftppublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive,Asset asset, Data inPublishRequest,  Data destination, Data preset)
	{
		PublishResult result = new PublishResult();

		Page inputpage = findInputPage(mediaArchive,asset,preset);
		String servername = destination.get("server");
		String username = destination.get("username");
		String password = destination.get("password");
		String url = destination.get("url");

		SftpUtil sftp = new SftpUtil();
		sftp.setHost(servername);
		sftp.setUsername(username);
	
		//get password and login
		if(password != null)
		{
			UserManager userManager = mediaArchive.getModuleManager().getBean("userManager");		
			User user = userManager.getUser(username);
			password = userManager.decryptPassword(user);
		}
		sftp.setPassword(password);
		log.info("Publishing ${asset} to sftp server ${servername}, with username ${username}.");
		
		//change paths if necessary
		if(url != null && url.length() > 0)
		{
			sftp.makeDirs(url);
			sftp.cd(url);
		}
		
		String exportname = inPublishRequest.get("exportname");
	
		//sendFileToRemote(local,remote);
		File input = new File(inputpage.getContentItem().getAbsolutePath())))
		sftp.sendFileToRemote(, url+exportname );
		
		result.setComplete(true);
		log.info("publishished  ${asset} to sftp server ${servername}.");
		sftp.disconnect;
		return result;
	}
}
