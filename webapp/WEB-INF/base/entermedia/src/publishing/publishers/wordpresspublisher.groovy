package publishing.publishers;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.openedit.Data
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.*

import com.openedit.OpenEditException
import com.openedit.page.Page

public class wordpresspublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(wordpresspublisher.class);
	protected ThreadLocal perThreadCache = new ThreadLocal();
	
	public PublishResult publish(MediaArchive mediaArchive,Asset asset, Data inPublishRequest,  Data destination, Data preset)
	{
		PublishResult result = checkOnConversion(mediaArchive,inPublishRequest,asset,preset); 
		if( result != null)
		{
			return result;
		}

		result = new PublishResult();

		String username = destination.get("username");
		String url = destination.get("url");
		
		log.info("Publishing ${asset} to EnterMedia server ${url}, with hash encoded password from ${username}.");
		
		String password = destination.get("accesskey");
		//http://hc.apache.org/httpcomponents-client-4.4.x/httpmime/examples/org/apache/http/examples/entity/mime/ClientMultipartFormPost.java
		HttpPost method = new HttpPost(url);
		
		/* example for adding an image part */
		MultipartEntityBuilder parts = MultipartEntityBuilder.create();
		parts.addPart("accesskey", new StringBody(password)) ;
		parts.addPart("sourcepath", new StringBody(asset.getSourcePath())) ;
		parts.addPart("assetid", new StringBody(asset.getId())) ;
		String exportname = inPublishRequest.get("exportname");
		parts.addPart("exportname", new StringBody(exportname));

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
				parts.addPart("keywords", new StringBody( buffer.toString()));
			}
		}
		Collection libraries =  asset.getLibraries();
		if(  libraries != null && libraries.size() > 0 )
		{
			StringBuffer buffer = new StringBuffer();
			for (Iterator iterator = libraries.iterator(); iterator.hasNext();)
			{
				String id = (String) iterator.next();
				Data library = mediaArchive.getData("library", id);
				if( library != null)
				{
					buffer.append(library.getName());
					if( iterator.hasNext() )
					{
						buffer.append(',');
					}
				}
			}
			if( buffer.length() > 0)
			{
				parts.addPart("libraries", new StringBody( buffer.toString()));
			}
		}
		Page inputpage = findInputPage(mediaArchive,asset,preset);
		File file = new File(inputpage.getContentItem().getAbsolutePath());
		if( !file.exists() )
		{
			throw new OpenEditException("Input file missing " + file.getPath() );
		}
		FileBody fileBody = new FileBody(file);//, "application/octect-stream") ;
		parts.addPart("file", fileBody);
		
		method.setEntity(parts.build());
		
		CloseableHttpClient httpclient = HttpClients.createDefault();  //TODO: Cache this
		CloseableHttpResponse response2 = httpclient.execute(method);
		
		try 
		{
			if( response2.getStatusLine().getStatusCode() != 200 )
			{
				result.setErrorMessage("Wordpress Server error returned ${response2.getStatusLine().getStatusCode()}");
			}
			HttpEntity entity2 = response2.getEntity();
			log.info( "Wordpress Server response: " + EntityUtils.toString(entity2));
			// do something useful with the response body
			// and ensure it is fully consumed
			EntityUtils.consume(entity2);
		}	
		catch( Exception ex)
		{
			throw new OpenEditException(" ${method} Request failed: status code",ex);
		}
		finally 
		{
			response2.close();
		}
		result.setComplete(true);
		return result;
	}
	
}