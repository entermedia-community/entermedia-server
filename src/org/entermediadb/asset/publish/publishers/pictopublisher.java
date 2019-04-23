package org.entermediadb.asset.publish.publishers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.ProfileModule;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.entermediadb.users.UserProfileManager;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.users.UserManager;

public class pictopublisher extends BasePublisher implements Publisher
{

	private static final Log log = LogFactory.getLog(pictopublisher.class);
	private static final Pattern pat = Pattern.compile(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*");
	
	public PublishResult publish(MediaArchive inMediaArchive, Asset inAsset, Data inPublishRequest, Data inDestination, Data inPreset)
	{
		//inMediaArchive.getRe
		//ProfileModule module = (ProfileModule)inMediaArchive.getModuleManager().getBean("ProfileModule");
		//inMediaArchive
		//ProfileModule module = (ProfileModule)getFixture().getModuleManager().getBean("ProfileModule");
		
		//UserProfileManager upmanager = module.getUserProfileManager();
		//UserManager um = upmanager.getUserManager("inCatalogId");
		
		try
		{
			PublishResult result = checkOnConversion(inMediaArchive, inPublishRequest, inAsset, inPreset);
			if (result != null)
			{
				return result;
			}

			result = new PublishResult();
			String accessToken = null;
			
			try {
				accessToken = getClientCredentials(inDestination);
				log.info("publishAPicto accessToken "+accessToken);
			} catch (Exception e) {
				e.printStackTrace();
				throw new OpenEditException(e);
			}
			
			Page inputpage = findInputPage(inMediaArchive, inAsset, inPreset);
			
			
			String pictoUrl = inDestination.get("url");

			File f = new File(inputpage.getContentItem().getAbsolutePath());
			
			try {
				publish(inDestination, inAsset, f, accessToken);
			} catch (Exception e) {
				e.printStackTrace();
				throw new OpenEditException(e);
			}

			log.info("publishAPicto file "+f.getAbsolutePath()+" to Picto");
			result.setComplete(true);
			return result;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new OpenEditException(e);
		}

	}
	/*
	 * longcaption -> legend
	 * 
	 */
	private void publish(Data inDestination, Asset inAsset, File f, String accessToken) throws ClientProtocolException, IOException, HttpException {
		
		String addr = inDestination.get("url");
		String filePath = f.getAbsolutePath();
		log.info("**** publishAPicto publish filePath "+filePath + " to "+ addr);
		
		HttpClient httpClient = HttpClientBuilder.create().build();
		String legend = getParam(inAsset, "longcaption");
		String agency = getParam(inAsset, "copyrightnotice");	
		String credit = getParam(inAsset, "creator");	
		String destination = getParam(inAsset, "name");	
		String directory = "ici-info";	
		String sub_directory = "Imagerie";	
		
		log.info("\t legend: "+legend);
		log.info("\t agency: "+agency);
		log.info("\t credit: "+credit);
		log.info("\t destination: "+destination);
		log.info("\t sub_directory: "+sub_directory);
		
		MultipartEntityBuilder entity = MultipartEntityBuilder
			    .create()
			    .addTextBody("destination", destination)
			    .addTextBody("directory", directory)
			    .addTextBody("alt", "alt")
			    .addTextBody("overwrite", "true")
			    .addTextBody("autoDeclinaison", "true")
			    .addTextBody("legend", legend)
			    .addTextBody("agency", agency)
			    .addTextBody("credit", credit)
			    //.addTextBody("subdirectory", sub_directory)
			    .addBinaryBody("source", f/*new File(filePath)*/, ContentType.create("image/jpeg"), f.getName());
			
		HttpEntity httpEntithy = entity.build();
		
		HttpPost httpPost = new HttpPost(addr);
	
		httpPost.setEntity(httpEntithy);
		String ctValue = httpEntithy.getContentType().getValue();
		
	    httpPost.setHeaders(
	    		new Header[] {
	    				new BasicHeader("content-type", ctValue),
	    				new BasicHeader("Content-Type", "application/x-www-form-urlencoded"),
	    				new BasicHeader("Connection", "Keep-Alive"),
	    				new BasicHeader("Authorization", "Bearer "+accessToken),
	    				new BasicHeader("Cache-Control", "no-cache"),
	    				
	    		}
	    		);
	    
		HttpResponse response = httpClient.execute(httpPost);
		log.info(response);
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new HttpException("Error "+response.getStatusLine().getReasonPhrase());
		}
		//if (response.getSt
		//HttpEntity result = response.getEntity();
		//result.
		
	}
	private String getParam(Asset inAsset, String id) {
		String tmp = inAsset.get(id);
		return tmp == null ? "N/A": tmp;
	}
	
	private String getClientCredentials(Data inDestination) {
		String content = "grant_type=client_credentials&scope=picto.write";
		String clientId = inDestination.get("accesskey");//
		String clientSecret = inDestination.get("secretkey");//secret_key
		//String authorizeUrl = inDestination.get("authorize_url");
		String tokenUrl = inDestination.get("access_token"); //access_teken
		
		String auth = clientId + ":" + clientSecret;
		String authentication = Base64.getEncoder().encodeToString(auth.getBytes());
		
	    
	    BufferedReader reader = null;
	    HttpsURLConnection connection = null;
	    String returnValue = "";
	    try {
	        URL url = new URL(tokenUrl);
	        connection = (HttpsURLConnection) url.openConnection();
	        connection.setRequestMethod("POST");
	        connection.setDoOutput(true);
	        connection.setRequestProperty("Authorization", "Basic " + authentication);
	        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	        connection.setRequestProperty("Accept", "application/json");
	        PrintStream os = new PrintStream(connection.getOutputStream());
	        os.print(content);
	        os.close();
	        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        String line = null;
	        StringWriter out = new StringWriter(connection.getContentLength() > 0 ? connection.getContentLength() : 2048);
	        while ((line = reader.readLine()) != null) {
	            out.append(line);
	        }
	        String response = out.toString();
	        Matcher matcher = pat.matcher(response);
	        if (matcher.matches() && matcher.groupCount() > 0) {
	            returnValue = matcher.group(1);
	        }
	    } catch (Exception e) {
	        log.error(e);
	        return null;
	    } finally {
	        if (reader != null) {
	            try {
	                reader.close();
	            } catch (IOException e) {
	            }
	        }
	        connection.disconnect();
	    }
	    return returnValue;
	}
	
	
	
}


