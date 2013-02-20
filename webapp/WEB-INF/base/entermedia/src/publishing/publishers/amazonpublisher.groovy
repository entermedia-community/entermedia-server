package publishing.publishers;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.entermedia.amazon.S3Repository
import org.openedit.Data
import org.openedit.data.*
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.*
import org.openedit.repository.filesystem.StringItem

import com.openedit.page.Page

public class amazonpublisher extends basepublisher implements Publisher 
{
	private static final Log log = LogFactory.getLog(amazonpublisher.class);

	public PublishResult publish(MediaArchive mediaArchive,Asset asset, Data inPublishRequest,  Data destination, Data inPreset)
	{
		S3Repository repo = (S3Repository)mediaArchive.getModuleManager().getBean("S3Repository");
		log.info("Publish asset to Amazon ${asset} for on server: ${destination}" );

		repo.setBucket(destination.bucket);
		repo.setAccessKey(destination.accesskey);
		repo.setSecretKey(destination.secretkey);

		PublishResult result = new PublishResult();
		//open the file and send it
		Page inputpage = findInputPage(mediaArchive,asset,inPreset);
		String exportname = inPublishRequest.get("exportname");
		if( !exportname.startsWith("/"))
		{
			exportname = "/" + exportname;
		}
		StringItem item = new StringItem();
		item.setPath( exportname);
		item.setAbsolutePath(inputpage.getContentItem().getAbsolutePath());

		repo.put(item); //copy the file
		log.info("published  ${exportname} to Amazon s3");
		result.setComplete(true);
		return result;
	}
}

