package publishing.publishers;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import com.google.gdata.util.ServiceException;
import com.google.gdata.data.youtube.VideoFeed;
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
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.Person;
import com.google.gdata.data.extensions.Rating;
import com.google.gdata.data.geo.impl.GeoRssWhere;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.media.mediarss.MediaCategory;
import com.google.gdata.data.media.mediarss.MediaDescription;
import com.google.gdata.data.media.mediarss.MediaKeywords;
import com.google.gdata.data.media.mediarss.MediaPlayer;
import com.google.gdata.data.media.mediarss.MediaThumbnail;
import com.google.gdata.data.media.mediarss.MediaTitle;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.VideoFeed;
import com.google.gdata.data.youtube.YouTubeMediaContent;
import com.google.gdata.data.youtube.YouTubeMediaGroup;
import com.google.gdata.data.youtube.YouTubeMediaRating;
import com.google.gdata.data.youtube.YouTubeNamespace;
import com.google.gdata.data.youtube.YtPublicationState;
import com.google.gdata.data.youtube.YtStatistics;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import java.net.URL;
import com.google.gdata.data.youtube.VideoEntry;
import com.openedit.page.Page
import com.openedit.util.FileUtils


public class fatwirepublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(fatwirepublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive, Asset inAsset, Data inPublishRequest, Data inDestination, Data inPreset)
	{
		
		System.out.println(" &&& fatwire publish request ");
		
		//setup result object
		PublishResult result = new PublishResult();
		
		FatwireClientService fw = new FatwireClientService();
		//setup fw object
		if (!fw.hasValidCredentials())
		{
			result.setComplete(true);
			result.setErrorMessage("FatWire credentials are incomplete");
		}
		String pubstatus = inPublishRequest.get("status");
		if (pubstatus == null || pubstatus.isEmpty())
		{
			result.setComplete(true);
			result.setErrorMessage("Status is not defined");
		}
		else if (pubstatus.equals("new") || pubstatus.equals("retry"))//new, pending, complete, retry, error
		{
			//prepare asset to be published
			Page inputpage = findInputPage(mediaArchive,inAsset,inPreset);
			File file = new File(inputpage.getContentItem().getAbsolutePath());
			String mimeType = mediaArchive.getOriginalDocument(inAsset).getMimeType();
			
			//publish via fatwire
			fw.startService();
			String trackingId = fw.publish(file);
			fw.stopService();
			inPublishRequest.setProperty("trackingnumber",trackingId);
			result.setPending(true);
		}
		else if (pubstatus.equals("pending"))
		{
			String trackingId = inPublishRequest.get("trackingnumber");
			//do stuff here with fatwireclient
			fw.startService();
			fw.updateStatus(trackingId, result);
			fw.stopService();
		}
		return result;
	}
}


//move this to a bean!
public class FatwireClientService
{
	private static final Log log = LogFactory.getLog(FatwireClientService.class);
	
	
	
	protected String fieldClientId = null;
	protected String fieldDevKey = null;
	protected String fieldUsername = null;
	protected String fieldPassword = null;
	
	//instance of service or whatever required for fatwire communication
	
	
	public void setClientId(String inClientId)
	{
		fieldClientId = inClientId;
	}
	
	public String getClientId()
	{
		return fieldClientId;
	}
	
	public void setDevKey(String inDevKey)
	{
		fieldDevKey = inDevKey;
	}
	
	public String getDevKey()
	{
		return fieldDevKey;
	}
	
	public void setUsername(String inUsername)
	{
		fieldUsername = inUsername;
	}
	
	public String getUsername()
	{
		return fieldUsername;
	}
	
	public void setPassword(String inPassword)
	{
		fieldPassword = inPassword;
	}
	
	public String getPassword()
	{
		return fieldPassword;
	}
	
	public boolean hasValidCredentials()
	{
		//whatever makes sense here
		return true;
	}
	
	public void startService()
	{
		//code to start client service
	}
	
	public void stopService()
	{
		//code to stop client service
	}
	
	public String publish(File file)//include arguments...
	{
		String trackingId = null;
		//do stuff
		trackingId = "testId";
		return trackingId;
	}
	
	public void updateStatus(String trackingId, PublishResult result)
	{
		//do stuff here
		result.setComplete(true);
	}
}