package org.entermediadb.asset.publish.publishers;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.entermediadb.projects.LibraryCollection;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.util.HttpRequestBuilder;

public class wordpresspublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(wordpresspublisher.class);
	protected ThreadLocal perThreadCache = new ThreadLocal();
	
	public PublishResult publish(MediaArchive mediaArchive,Asset asset, Data inPublishRequest,  Data destination, Data preset)
	{
		try
		{
			PublishResult result = checkOnConversion(mediaArchive,inPublishRequest,asset,preset); 
			if( result != null)
			{
				return result;
			}

			result = new PublishResult();

			String url = destination.get("url");
			
			String password = destination.get("accesskey");
			log.info("Publishing " + asset + " to EnterMedia server " + url);
			
			//http://hc.apache.org/httpcomponents-client-4.4.x/httpmime/examples/org/apache/http/examples/entity/mime/ClientMultipartFormPost.java
			HttpPost method = new HttpPost(url);
			
			/* example for adding an image part */
			HttpRequestBuilder builder = new HttpRequestBuilder();

			//TODO: Use HttpRequestBuilder.addPart()
			
			builder.addPart("accesskey", password) ;
			builder.addPart("sourcepath", asset.getSourcePath());
			builder.addPart("assetid", asset.getId());
			String exportname = inPublishRequest.get("exportname");
			builder.addPart("exportname", exportname);
			builder.addPart("title", asset.toString());
			builder.addPart("caption", asset.get("headline"));
			builder.addPart("description", asset.get("longcaption")); 
			builder.addPart("uploadby", "entermedia");

			if( asset.getKeywords().size() > 0 )
			{
				StringBuffer buffer = new StringBuffer();
				for (Iterator iterator = asset.getKeywords().iterator(); iterator.hasNext();)
				{
					String keyword = (String) iterator.next();
					buffer.append( keyword );
					if( iterator.hasNext() )
					{
						buffer.append(',');
					}
				}
				if( buffer.length() > 0)
				{
					builder.addPart("keywords",  buffer.toString());
				}
			}
			Collection collections =  asset.getCollections();
			if(  collections != null && collections.size() > 0 )
			{
				for (Iterator iterator = collections.iterator(); iterator.hasNext();)
				{
					LibraryCollection librarycollection = (LibraryCollection) iterator.next();
					Data library = (Data)librarycollection.getLibrary();
					if( library != null)
					{
						builder.addPart("library", library.getName());
					}
					builder.addPart("collection", librarycollection.getName());
				}
			}
			Page inputpage = findInputPage(mediaArchive,asset,preset);
			File file = new File(inputpage.getContentItem().getAbsolutePath());
			if( !file.exists() )
			{
				throw new OpenEditException("Input file missing " + file.getPath() );
			}
			builder.addPart("file", file);
			
			method.setEntity(builder.build());
			
			CloseableHttpClient httpclient = HttpClients.createDefault();  //TODO: Cache this

			CloseableHttpResponse response2 = httpclient.execute(method);
			try
			{
				if( response2.getStatusLine().getStatusCode() != 200 )
				{
					result.setErrorMessage("Wordpress Server error returned " + response2.getStatusLine().getStatusCode());
				}
				else
				{
					result.setComplete(true);
				}
				HttpEntity entity2 = response2.getEntity();
				log.info( "Wordpress Server response: " + EntityUtils.toString(entity2));
				// do something useful with the response body
				// and ensure it is fully consumed
				EntityUtils.consume(entity2);
			}	
			finally 
			{
				response2.close();
			}
			return result;
		}
		catch( Exception ex)
		{
			throw new OpenEditException(" Request failed: status code",ex);
		}

	}
	
}