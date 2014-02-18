package publishing.publishers;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.dom4j.io.SAXReader
import org.openedit.Data
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.*

import com.openedit.page.Page
import com.openedit.users.User
import com.openedit.users.UserManager

import em.model.push.MediaUploader

public class entermediapublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(entermediapublisher.class);
	private SAXReader reader = new SAXReader();
	
	public PublishResult publish(MediaArchive mediaArchive,Asset asset, Data inPublishRequest,  Data destination, Data preset)
	{
		PublishResult result = new PublishResult();

		String servername = destination.get("server");
		String username = destination.get("username");
		//String url = destination.get("url");
		
		log.info("Publishing ${asset} to EnterMedia server ${servername}, with username ${username}.");
		
		UserManager userManager = mediaArchive.getModuleManager().getBean("userManager");
		User user = userManager.getUser(username);
		if(user == null)
		{
			result.setErrorMessage("Unknown user, ${username}");
			return result;
		}
		
		String password = userManager.decryptPassword(user);
		
		MediaUploader uploader = (MediaUploader)mediaArchive.getModuleManager().getBean("mediaUploader");
		
		//always send generated media. sometimes add in the original file as well
		//if preset is original then send all the generated media as well
		//if preset id is all generated then send all
		//if preset is a specific generated then just send that

		//upload original and all generated
		
		//upload one
		
		try
		{
			//MediaArchive archive, Asset inAsset, Data inPublishDestination, User inUser )
			uploader.uploadOriginal(mediaArchive,asset, destination, user);
			result.setComplete(true);
			log.info("publishished  ${asset} to EnterMedia server ${servername}");
		}
		catch ( Exception ex)
		{
			//inPublishDestination(searcher, savequeue, target, "complete", inUser);
			result.setErrorMessage(ex.toString());
		}
		return result;
	}
	
	/*
	 * public Map<String, String> upload(String server, String inCatalogId, String inSourcePath, File inFile)
	 
	{
		String url =server + "/media/services/" + "/uploadfile.xml?catalogid=" + inCatalogId;
		PostMethod method = new PostMethod(url);

		try
		{
			 def parts =[new FilePart("file", inFile.getName(), inFile),	new StringPart("sourcepath", inSourcePath)] as Part[];
			
			method.setRequestEntity( new MultipartRequestEntity(parts, method.getParams()) );
	
			Element root = execute(method);
			Map<String, String> result = new HashMap<String, String>();
			for(Object o: root.elements("asset"))
			{
				Element asset = (Element)o;
				result.put(asset.attributeValue("id"), asset.attributeValue("sourcepath"));
			}
			return result;
		}
		catch( Exception e )
		{
			return null;
		}
	}
	*/
	
	
	protected Page findInputPage(MediaArchive mediaArchive, Asset asset, String presetid)
	{
		if( presetid == null)
		{
			return mediaArchive.getOriginalDocument(asset);
		}
		Data preset = mediaArchive.getSearcherManager().getData( mediaArchive.getCatalogId(), "convertpreset", presetid);
		return findInputPage(mediaArchive,asset,(Data)preset);
	}
	
	
}