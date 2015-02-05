package publishing.publishers;

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.mozilla.javascript.tools.idswitch.FileBody
import org.openedit.Data
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.publishing.*

import com.openedit.OpenEditException
import com.openedit.page.Page
import com.openedit.users.User
import com.openedit.users.UserManager

public class entermediapublisher extends basepublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(entermediapublisher.class);
	protected ThreadLocal perThreadCache = new ThreadLocal();
	
	public PublishResult publish(MediaArchive mediaArchive,Asset asset, Data inPublishRequest,  Data destination, Data preset)
	{
		PublishResult result = new PublishResult();

		String username = destination.get("username");
		String url = destination.get("url");
		
		log.info("Publishing ${asset} to EnterMedia server ${url}, with hash encoded password from ${username}.");
		
		UserManager userManager = mediaArchive.getModuleManager().getBean("userManager");
		User user = userManager.getUser(username);
		if(user == null)
		{
			result.setErrorMessage("Unknown user, ${username}");
			return result;
		}
		String password = user.password;
		//http://hc.apache.org/httpcomponents-client-4.4.x/httpmime/examples/org/apache/http/examples/entity/mime/ClientMultipartFormPost.java
		HttpPost method = new HttpPost(url);
		
		/* example for adding an image part */
		MultipartEntityBuilder parts = MultipartEntityBuilder.create()
		.addPart("bin", bin)
		.addPart("comment", comment)
		//.build();

			
		parts.addPart("accesskey", new StringBody(password)) ;
		parts.addPart("sourcepath", new StringBody(asset.getSourcePath())) ;
		parts.addPart("assetid", new StringBody(asset.getId())) ;
		
		File file = new File(inputpage.getContentItem().getAbsolutePath());
		FileBody fileBody = new FileBody(file, "application/octect-stream") ;
		parts.addPart("file", fileBody) ;
		
		Page inputpage = findInputPage(mediaArchive,asset,preset);
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
					buffer.append('|');
				}
			}
			parts.addPart("keywords", new StringBody( buffer.toString()));
		}
		Collection libraries =  asset.getLibraries();
		if(  libraries != null && libraries.size() > 0 )
		{
			StringBuffer buffer = new StringBuffer();
			for (Iterator iterator = asset.getLibraries().iterator(); iterator.hasNext();)
			{
				String keyword = (String) iterator.next();
				buffer.append( keyword );
				if( iterator.hasNext() )
				{
					buffer.append('|');
				}
			}
			parts.addPart("libraries", new StringBody( buffer.toString()));
		}
		method.setEntity(parts.build());
		
		CloseableHttpResponse response2 = httpclient.execute(httpPost);
		
		try {
			System.out.println(response2.getStatusLine());
			HttpEntity entity2 = response2.getEntity();
			// do something useful with the response body
			// and ensure it is fully consumed
			EntityUtils.consume(entity2);
		catch( Exception ex)
		{
			throw new OpenEditException(" ${method} Request failed: status code",ex);
		}		
		} finally {
			response2.close();
		}

		
//			if (status != 200)
//			{
//				throw new Exception(" ${method} Request failed: status code ${status}");
//			}
//			String returned = method.getResponseBodyAsString(); 
//			log.debug(returned);//for debug purposes only
//		}		
		result.setComplete(true);
		return result;
	}
	protected HttpClient getClient()
	{
		HttpClient ref = (HttpClient) perThreadCache.get();
		if( ref != null)
		{
			
//Look into connection manager and check state of connection? 
//			if( ref.getState())
//			{
//				perThreadCache.remove();
//				ref = null;	
//			}
		}
		
		if (ref == null)
		{
			ref = new HttpClient();
			//Make sure client is up?				
			// use weak reference to prevent cyclic reference during GC
			perThreadCache.set(ref);
		}
		return ref;
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