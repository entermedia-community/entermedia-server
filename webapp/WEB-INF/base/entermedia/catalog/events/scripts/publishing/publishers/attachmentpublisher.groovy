package publishing.publishers;

import java.io.File;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.Publisher

import com.openedit.page.Page
import com.openedit.util.FileUtils

public class attachmentpublisher extends filecopypublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(attachmentpublisher.class);
	
	public void publish(MediaArchive mediaArchive,Asset inAsset, Data inPublishRequest,  Data inDestination, Data inPreset)
	{
		//make the asset folder based
		mediaArchive.getAssetEditor().makeFolderAsset(inAsset, null);
		//modify the destination url
		inDestination.setProperty("url", "webapp/WEB-INF/data/" + mediaArchive.getCatalogId() + "/originals/" + inAsset.getSourcePath() + "/");
		super.publish(mediaArchive, inAsset, inPublishRequest, inDestination, inPreset);
	}
	
	public void publish(MediaArchive mediaArchive,Data inOrder, Data inOrderItem, Asset asset)
	{
		super.publish(mediaArchive, inOrder, inOrderItem, asset);
	}
}