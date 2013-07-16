package publishing.publishers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.*
import com.openedit.hittracker.SearchQuery;
import java.net.URL;

import com.openedit.page.Page
import com.openedit.util.FileUtils

import com.openedit.util.RequestUtils
import com.openedit.users.UserManager
import com.openedit.users.User

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply

import org.apache.commons.io.IOUtils

import org.apache.commons.net.io.Util;



public class fatwirepublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(fatwirepublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive, Asset inAsset, Data inPublishRequest, Data inDestination, Data inPreset)
	{
		//setup result object
		PublishResult result = new PublishResult();
		
		String exportname = inPublishRequest.get("exportname");
		String urlHome = inPublishRequest.get("homeurl");
		String username =  inPublishRequest.get("username");
		String outputfile = inPublishRequest.get("convertpresetoutputfile");
		Searcher presetsearch = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "convertpreset");
		
		if (outputfile == null || outputfile.isEmpty())
		{
			//search for presetid
			String presetid = inPublishRequest.get("presetid");
			//find outputfile
			Data d = (Data) presetsearch.searchById(presetid);
			outputfile = d.get("outputfile");
			if (outputfile != null && outputfile.isEmpty()) outputfile = null;
		}
		Data thumbpreset = presetsearch.searchById("thumbimage");//get thumbnail data
		
		UserManager usermanager = (UserManager) mediaArchive.getModuleManager().getBean("userManager");
		User inUser = usermanager.getUser(username);
		String copyrightstatus = inAsset.get("copyrightstatus");
		Searcher searcher = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "copyrightstatus");
		String usage = null;
		if (copyrightstatus!=null)
		{
			Data data = searcher.searchById(copyrightstatus);
			usage = data.get("name");
		}
		//failsafe
		if (exportname == null || urlHome == null || username == null || outputfile == null)
		{
			log.info("internal error: unable to publish to fatwire (exportname=${exportname} urlHome=${urlHome} username=${username} outputfile=${outputfile}");
			result.setComplete(true);
			result.setErrorMessage("Error publishing to FatWire: variables have not been set");
			return result;
		}
		//this does the actual publishing
		Object fatwireManager = mediaArchive.getModuleManager().getBean( "fatwireManager");
		try {
			fatwireManager.setMediaArchive(mediaArchive);
			Object assetBean = fatwireManager.pushAsset(inAsset, inUser, urlHome, usage, exportname, outputfile);
			if (assetBean != null)
			{
				String newId = assetBean.getId();
				inPublishRequest.setProperty("trackingnumber",newId);
				log.info("response from publishing request to FatWire: newId ${newId}");
				
				//ftp images to fatwire server
				Searcher publishdestinationsearch = mediaArchive.getSearcherManager().getSearcher(mediaArchive.getCatalogId(), "publishdestination");
				SearchQuery fatwirequery = publishdestinationsearch.createSearchQuery().append("name", "FatWire");
				Data fatwireData = publishdestinationsearch.searchByQuery(fatwirequery);
				
				String ftpServer = fatwireData.get("ftpserver");
				String ftpUsername = fatwireData.get("ftpusername");
				User ftpUser = usermanager.getUser(ftpUsername);
				String ftpPwd = usermanager.decryptPassword(ftpUser);
				
				Page original = findInputPage(mediaArchive,inAsset,inPreset);
				Page thumb = findInputPage(mediaArchive,inAsset,thumbpreset);
				
				ArrayList<String> images = new ArrayList<String>();
				ArrayList<Page> pages = new ArrayList<Page>();
				
				Iterator itr = assetBean.getAttributes().iterator();
				while(itr.hasNext())
				{
					Object att = itr.next();
					if (att.getName() != null && att.getName().equals("thumbnailurl"))
					{
						Object attdata = att.getData();
						String to = (attdata!=null ? attdata.getStringValue() : null);
						if (to!=null)
						{
							if (to.startsWith("/image/EM/"))
							{
								to = to.substring("/image/EM/".length());
							}
							pages.add(thumb);
							images.add(to);
						}
					}
					else if (att.getName() != null && att.getName().equals("imageurl"))
					{
						Object attdata = att.getData();
						String to = (attdata!=null ? attdata.getStringValue() : null);
						if (to!=null)
						{
							if (to.startsWith("/image/EM/"))
							{
								to = to.substring("/image/EM/".length());
							}
							pages.add(original);
							images.add(to);
						}
					}
				}
				
				ftpPublish(ftpServer, ftpUsername, ftpPwd, pages, images, result);
//				result.setComplete(true);
			}
			else 
			{
				log.info("Error publishing asset: asset bean is NUll");
				result.setComplete(true);
				result.setErrorMessage("Error publishing to FatWire: unable to publish asset");
			}
		}
		catch (IOException e)
		{
			log.error(e.getMessage(), e);
//			result.setComplete(true);
			result.setErrorMessage(e.getMessage());
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
//			result.setComplete(true);
			result.setErrorMessage(e.getMessage());
		}
		return result;
	}
	
	public void ftpPublish(String servername, String username, String password, ArrayList<Page> from, ArrayList<String> to, PublishResult result)
	{
		
		log.info("ftpPublish ${servername} ${username} ${to}");
		FTPClient ftp = new FTPClient();
		
		ftp.connect(servername,21);
		ftp.enterLocalPassiveMode();
		
		//check to see if connected
		int reply = ftp.getReplyCode();
		if(!FTPReply.isPositiveCompletion(reply))
		{
			result.setErrorMessage("Unable to connect to ${servername}, error code: ${reply}")
			ftp.disconnect();
			return;
		}	
		ftp.login(username, password);
		reply = ftp.getReplyCode();
		if(!FTPReply.isPositiveCompletion(reply))
		{
			result.setErrorMessage("Unable to login to ${servername}, error code: ${reply}");
			ftp.disconnect();
			return;
		}
		ftp.setFileTransferMode(FTPClient.BINARY_FILE_TYPE);
		
		//change paths if necessary
//		String url = "/images/EM/";
//		ftp.changeWorkingDirectory(url);
//		reply = ftp.getReplyCode();
//		if(!FTPReply.isPositiveCompletion(reply))
//		{
//			result.setErrorMessage("Unable to to cd to ${url}, error code: ${reply}");
//			ftp.disconnect();
//			return result;
//		}
		
//		String exportname = inPublishRequest.get("exportname");
		
		for (int i=0; i < from.size(); i++){
			Page page = from.get(i);
			String export = to.get(i);
			
			ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
			
			OutputStream os = null;
			try
			{
//				ftp.storeFile(export, page.getInputStream());
				os  = ftp.storeFileStream(export);
				long responsecode = Util.copyStream(page.getInputStream(), os);
				
				log.info("&&&& response from server ${responsecode}");
				
//				IOUtils.copy(page.getInputStream(),os);
			}
			finally
			{
				try
				{
					if (os!=null)
					{
						os.flush();
						os.close();
					}
				}
				catch (Exception e){}
				try
				{
					if (os!=null)
					{
						os.close();
					}
				}
				catch (Exception e){}
				try
				{
					page.getInputStream().close();
				}
				catch (Exception e){}
			}
			
			ftp.completePendingCommand();
//			
//			reply = ftp.getReplyCode();
//			if(!FTPReply.isPositiveCompletion(reply))
//			{
//				result.setErrorMessage("Unable to to send file to ${export}, error code: ${reply}");
//				ftp.disconnect();
//				return;
//			}
		}
		

		
		if(ftp.isConnected())
		{
			ftp.disconnect();
		}
		result.setComplete(true);
	}
}