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


public class youtubepublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(youtubepublisher.class);
	
	public PublishResult publish(MediaArchive mediaArchive, Asset inAsset, Data inPublishRequest, Data inDestination, Data inPreset)
	{
		//setup result object
		PublishResult result = new PublishResult();
		//youtube connection info
		YouTubeClientService yt = new YouTubeClientService();
		yt.setClientId(inDestination.get("bucket"));
		yt.setDevKey(inDestination.get("accesskey"));
		yt.setUsername(inDestination.get("username"));
		yt.setPassword(inDestination.get("password"));
		if (!yt.hasValidCredentials())
		{
			result.setComplete(true);
			result.setErrorMessage("YouTube credentials are incomplete");
			return result;
		}
		String pubstatus = inPublishRequest.get("status");
		if (pubstatus == null || pubstatus.isEmpty())
		{
			result.setComplete(true);
			result.setErrorMessage("Status is not defined");
		}
		else if (pubstatus.equals("new") || pubstatus.equals("retry"))//new, pending, complete, retry, error
		{
			//initial sanity check
			if (inAsset.get("keywords")==null || inAsset.get("keywords").isEmpty())
			{
				result.setComplete(true);
				result.setErrorMessage("Tags need to be defined on an asset for publication to YouTube");
				return result;
			}
			if (inAsset.get("category")==null || inAsset.get("category").isEmpty())
			{
				result.setComplete(true);
				result.setErrorMessage("Categories need to be defined on an asset for publication to YouTube");
				return result;
			}
			if (inAsset.get("assettitle")==null || inAsset.get("assettitle").isEmpty() || inAsset.get("longcaption")==null || inAsset.get("longcaption").isEmpty())
			{
				result.setComplete(true);
				result.setErrorMessage("Title and description need to be defined on an asset for publication to YouTube");
				return result;
			}
			String ytcats = inPublishRequest.get("youtubecategory");
			String assetfield = inPublishRequest.get("assetfield");
			String category = parseCategory(ytcats,assetfield,inAsset.getId());
			if (category == null)
			{
				result.setComplete(true);
				result.setErrorMessage("Unable to extract a valid YouTube category");
				return result;
			}
			String title = inAsset.get("assettitle");
			String description = inAsset.get("longcaption");
			//categories, keywords
			ArrayList<String> keylist = new ArrayList<String>();
			String [] keywords = inAsset.get("keywords").split(" | ");//tags split on |
			for (String keyword:keywords)
			{
				if (keyword.trim().equals("|"))
					continue;
				keylist.add(keyword.trim());
			}
			String categories = inAsset.get("category");
			ArrayList<String> catlist = new ArrayList<String>();
			String [] cats = categories.split(" | ");//categories split on |
			for (String cat:cats)
			{
				if (cat.trim().equals("|"))
					continue;
				String [] toks = cat.split("_");
				for (String tok:toks)
				{
					if (tok.equals("_") || catlist.contains(tok))
						continue;
					catlist.add(tok);
				}
			}
			Page inputpage = findInputPage(mediaArchive,inAsset,inPreset);
			File file = new File(inputpage.getContentItem().getAbsolutePath());
			String mimeType = mediaArchive.getOriginalDocument(inAsset).getMimeType();
			
			//geolocation - optional
			ArrayList<Double> position = new ArrayList<Double>();
			String latstr = inAsset.get("position_lat");
			if (latstr!=null && !latstr.isEmpty())
			{
				try
				{
					position.add(new Double(Double.parseDouble(latstr)));
				}catch (Exception e){}
			}
			String lngstr = inAsset.get("position_lng");
			if (lngstr!=null && !lngstr.isEmpty())
			{
				try
				{
					position.add(new Double(Double.parseDouble(lngstr)));
				}catch (Exception e){}
			}
			//start service and publish
			yt.startService();
			String videoId = yt.publish(file,title,description,mimeType,category,catlist,keylist,position);
			yt.stopService();
			inPublishRequest.setProperty("trackingnumber",videoId);
			result.setPending(true);
		}
		else if (pubstatus.equals("pending"))
		{
			String videoId = inPublishRequest.get("trackingnumber");
			yt.startService();
			yt.updateVideoStatus(videoId, result);
			yt.stopService();
		}
		return result;
	}
	
	protected String parseCategory(String categories, String assets, String assetId)
	{
		if (categories == null || categories.isEmpty() || assets == null || assets.isEmpty())
			return null;
		String [] toks1 = categories.split(" | ");
		String [] toks2 = assets.split(" | ");
		if (toks1.length != toks2.length)
		{
			log.error("parseCategory error: array token lengths of categories and assets are not the same, exiting");
			return null;
		}
		for (int i=0; i < toks2.length; i++)
		{
			if (toks2[i].trim().equals(assetId))
				return toks1[i];
		}
		return null;
	}
}


//move this to a bean!
public class YouTubeClientService
{
	private static final Log log = LogFactory.getLog(YouTubeClientService.class);
	
	public static final String URL_FEED = "http://gdata.youtube.com/feeds/api/users/default/uploads";
	public static final String URL_UPLOAD = "http://uploads.gdata.youtube.com/feeds/api/users/default/uploads";
	
	protected String fieldClientId = null;
	protected String fieldDevKey = null;
	protected String fieldUsername = null;
	protected String fieldPassword = null;
	
	protected YouTubeService fieldService = null;
	
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
	
	public String getUploadUrl()
	{
		return URL_UPLOAD;
	}
	
	public String getFeedUrl()
	{
		return URL_FEED;
	}
	
	public boolean hasValidCredentials()
	{
		return (getClientId()!=null && getDevKey()!=null && getUsername()!=null && getPassword()!=null &&
			!getClientId().isEmpty() && !getDevKey().isEmpty() && !getUsername().isEmpty()&& !getPassword().isEmpty());
	}
	
	public void startService()
	{
		if (fieldService == null)
		{
			fieldService = new YouTubeService(getClientId(),getDevKey());
		}
		try
		{
			fieldService.setUserCredentials(getUsername(),getPassword());
		}
		catch (AuthenticationException e)
		{
			log.error(e.getMessage(), e);
		}
		catch (NullPointerException e)
		{
			log.error(e.getMessage(), e);
		}
	}
	
	public void stopService()
	{
		fieldService = null;//no other way to shutdown client
	}
	
	public String publish(File file, String title, String description, String mimetype, String category, List<String> categories, List<String> keywords, List<Double> position)
	{
		VideoEntry entry = new VideoEntry();
		YouTubeMediaGroup mg = entry.getOrCreateMediaGroup();
		mg.setTitle(new MediaTitle());
		mg.getTitle().setPlainTextContent(title);
		mg.setDescription(new MediaDescription());
		mg.getDescription().setPlainTextContent(description);
		mg.setPrivate(false);
		mg.addCategory(new MediaCategory(YouTubeNamespace.CATEGORY_SCHEME, category));
		for (String cat:categories)
		{
			mg.addCategory(new MediaCategory(YouTubeNamespace.DEVELOPER_TAG_SCHEME,cat));
		}
		mg.setKeywords(new MediaKeywords());
		for (String keyword:keywords)
		{
			mg.getKeywords().addKeyword(keyword);
		}
		if (position.size() == 2)
		{
			entry.setGeoCoordinates(new GeoRssWhere(position.get(0).doubleValue(),position.get(1).doubleValue()));
		}
		MediaFileSource ms = new MediaFileSource(file, mimetype);
		entry.setMediaSource(ms);
		try
		{
			VideoEntry inserted = fieldService.insert(new URL(getUploadUrl()), entry);
			return inserted.getMediaGroup().getVideoId();
		}
		catch (IOException e)
		{
			log.error(e.getMessage(), e);
		}
		catch (ServiceException e)
		{
			log.error(e.getMessage(), e);
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return null;
	}
	
	public void updateVideoStatus(String inVideoId, PublishResult result)
	{
		try
		{
			VideoFeed vf = fieldService.getFeed(new URL(getFeedUrl()), VideoFeed.class);
			for (VideoEntry ve:vf.getEntries())
			{
				if (ve.getMediaGroup().getVideoId().equals(inVideoId))
				{
					if (ve.isDraft())
					{
						if (ve.getPublicationState().getState() == YtPublicationState.State.PROCESSING)
						{
							result.setPending(true);
						}
						else if (ve.getPublicationState().getState() == YtPublicationState.State.INCOMPLETE)
						{
							result.setComplete(true);
							result.setErrorMessage(ve.getPublicationState().getDescription());
						}
						else if (ve.getPublicationState().getState() == YtPublicationState.State.REJECTED)
						{
							result.setComplete(true);
							result.setErrorMessage(ve.getPublicationState().getDescription());
						}
						else if (ve.getPublicationState().getState() == YtPublicationState.State.FAILED)
						{
							result.setComplete(true);
							result.setErrorMessage(ve.getPublicationState().getDescription());
						}
						else if (ve.getPublicationState().getState() == YtPublicationState.State.DELETED)
						{
							result.setComplete(true);
							result.setErrorMessage(ve.getPublicationState().getDescription());
						}
						else
						{
							result.setComplete(true);
							result.setErrorMessage(ve.getPublicationState().getDescription());
						}
					}
					else
					{
						result.setComplete(true);
					}
					return;
				}
			}
			result.setComplete(false);
			result.setErrorMessage("Unable to find tracking number ("+inVideoId+")");
		}
		catch (MalformedURLException e)
		{
			log.error(e.getMessage(), e);
			result.setComplete(false);
			result.setErrorMessage(e.getMessage());
		}
		catch (IOException e)
		{
			log.error(e.getMessage(), e);
			result.setComplete(false);
			result.setErrorMessage(e.getMessage());
		}
		catch (ServiceException e)//for service unavailable, need to do retry!
		{
			log.error(e.getMessage(), e);
			result.setComplete(false);
			result.setErrorMessage(e.getMessage());
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
			result.setComplete(false);
			result.setErrorMessage(e.getMessage());
		}
	}
}