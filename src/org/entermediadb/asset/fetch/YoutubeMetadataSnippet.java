package org.entermediadb.asset.fetch;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class YoutubeMetadataSnippet extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;
	
	protected Map<String, Object>  snippet;
	
	public void setSnippet(Map<String, Object> inSnippet) {
		snippet = inSnippet;
	}
	
	public String getVideoId() {
		return (String) snippet.get("id");
	}
	
	public String getTitle() {
		return (String) snippet.get("title");
	}
	
	public String getDescription() {
		return (String) snippet.get("description");
	}
	
	public String getPublishedAt() {
		return (String) snippet.get("publishedAt");
	}
	
	public String getThumbnail() {
		return (String) snippet.get("thumbnail");
	}
	
	public String getChannelTitle() {
		return (String) snippet.get("channelTitle");
	}
	
	public String getWebviewLink() {
		return "https://youtu.be/" + getVideoId();
	}
	
	@SuppressWarnings("unchecked")
	public Collection<String> getTags() {
		Object tags = snippet.get("tags");
		if ( tags instanceof Collection) {
			return (Collection<String>) tags;
		}
		return null;
	}
	
	@Override
	public String toString() {
		return "YoutubeMetadataSnippet [id=" + getVideoId() + ", title=" + getTitle() + ", channelTitle=" + getChannelTitle() + ", publishedAt=" + getPublishedAt() + "]";
	}
	
}
