package publishing.publishers;

import jcifs.smb.SmbFile

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.Publisher

import com.openedit.OpenEditException
import com.openedit.page.Page
import com.openedit.users.User
import com.openedit.users.UserManager
import com.openedit.util.FileUtils
import com.openedit.util.OutputFiller

public class smbpublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(smbpublisher.class);
	
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
			
			log.info("Publishing ${asset} to smb server ${servername}, with username ${username}.");
			
			//get password and login
			UserManager userManager = mediaArchive.getModuleManager().getBean("userManager");
			User user = userManager.getUser(username);
			if(user == null)
			{
				publishFailure(mediaArchive, inOrderItem, "Unknown user, ${username}");
				return;
			}
			
			String password = userManager.decryptPassword(user);
			
			Page inputpage = findInputPage(mediaArchive,asset,presetid);
			
			String exportname= inOrderItem.get("filename");
			
			OutputStream output = null;
			InputStream input = null;
			try
			{
				//smb://[[[domain;]username[:password]@]server[:port]/[[share/[dir/]file]]][?[param=value[param2=value2[...]]]
				//http://jcifs.samba.org/src/docs/api/
				
				if( !url.endsWith("/"))
				{
					url = url + "/";
				}
				String sambaurl = "smb://" + username + ":" + password + "@" + servername + url;
				SmbFile export = new SmbFile( sambaurl + exportname);
				
				output = export.getOutputStream();
				input = inputpage.getInputStream();
				new OutputFiller().fill( input, output );
			}
			finally
			{
				FileUtils.safeClose(input);
				FileUtils.safeClose(output);
			}
			
			log.info("publishished  ${asset} to SMB server ${servername}");
			inOrderItem.setProperty("status", "complete");
			Searcher itemsearcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "orderitem");
			itemsearcher.saveData(inOrderItem, null);
		}
	}
	
}