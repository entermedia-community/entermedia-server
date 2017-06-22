package org.entermediadb.asset.publish.publishers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.openedit.Data;



public class smbpublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(smbpublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive,Asset asset, Data inPublishRequest,  Data destination, Data inPreset)
	{
		PublishResult result = checkOnConversion(mediaArchive,inPublishRequest,asset,inPreset);
		if( result != null)
		{
			return result;
		}

		result = new PublishResult();
		
//		Page inputpage = findInputPage(mediaArchive,asset,inPreset);
//		String servername = destination.get("server");
//		String username = destination.get("username");
//		String url = destination.get("url");
//			
//		log.info("Publishing ${asset} to smb server ${servername}, with username ${username}.");
//			
//		//get password and login
//		UserManager userManager = (UserManager) mediaArchive.getModuleManager().getBean("userManager");
//		User user = userManager.getUser(username);
//		if(user == null)
//		{
//			publishFailure(mediaArchive, inOrderItem, "Unknown user, ${username}");
//			
//		}
//		
//		String password = userManager.decryptPassword(user);
//			
//			
//		String exportname= inOrderItem.get("filename");
//		
//		OutputStream output = null;
//		InputStream input = null;
//		try
//		{
//			//smb://[[[domain;]username[:password]@]server[:port]/[[share/[dir/]file]]][?[param=value[param2=value2[...]]]
//			//http://jcifs.samba.org/src/docs/api/
//			
//			if( !url.endsWith("/"))
//			{
//				url = url + "/";
//			}
//			String sambaurl = "smb://" + username + ":" + password + "@" + servername + url;
//			SmbFile export = new SmbFile( sambaurl + exportname);
//			
//			output = export.getOutputStream();
//			input = inputpage.getInputStream();
//			new OutputFiller().fill( input, output );
//		}
//		finally
//		{
//			FileUtils.safeClose(input);
//			FileUtils.safeClose(output);
//		}
//		
//		log.info("publishished  ${asset} to SMB server ${servername}");
//		result.setComplete(true);
		return result;
	}	
}