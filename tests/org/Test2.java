package org;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;

public class Test2 {

	
	private static final String clientId = "77cb9279-94ab-45a0-99f1-fefe3f7bf51c";
	private static final String clientSecret = "1bf39680-f4aa-48ab-b4fa-6a2d70f9fedf";
	//private static final String tokenUrl = "https://api-gw-dev-v3.radio-canada.ca/auth/oauth/v2/token";
	private static final String tokenUrl = "https://dev-services.radio-canada.ca/auth/oauth/v2/token";
	private static final String auth = clientId + ":" + clientSecret;
	private static final String authentication = Base64.getEncoder().encodeToString(auth.getBytes());
	
	private static final String url = "https://dev-services.radio-canada.ca/picto/api/v3";
	
	private static final Pattern pat = Pattern.compile(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*");
	
	

	
	private static String getClientCredentials() {
	    String content = "grant_type=client_credentials&scope=picto.write";
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
	        connection.setRequestProperty("Scope", "application/json");
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
	        System.out.println("Error : " + e.getMessage());
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

	public static void __main(String[] args) throws ClientProtocolException, IOException {
		String access_token = getClientCredentials();
		System.out.println(access_token);
		String addr = url + "/Upload?access_token="+access_token;
		String filePath = "/Users/hassanmrad/Downloads/test.jpg";
		HttpClient httpClient = HttpClientBuilder.create().build();
		
		//HttpPost method = new HttpPost(addr);
		//method.setHeader("Content-Type", "multipart/form-data");
		
		FileBody bin = new FileBody(new File(filePath));
		System.out.println(bin.getContentType().getMimeType());
		HttpEntity entity = MultipartEntityBuilder
			    .create()
			    .addTextBody("destination", "dossier")
			    .addTextBody("directory", "ici-info")
			    .addTextBody("alt", "alt")
			    .addTextBody("legend", "legend_Imagerie")
			    .addTextBody("overwrite", "true")
			    .addTextBody("autoDeclinaison", "true")
			    //.addPart("source", bin)
			    .addBinaryBody("source", new File(filePath), ContentType.create("image/jpeg"), "test_imagerie001")
			    .build();

		HttpPost httpPost = new HttpPost(addr);
		httpPost.setHeader("Content-Type", "multipart/form-data");
		httpPost.setEntity(entity);
		HttpResponse response = httpClient.execute(httpPost);
		System.out.println(response);
		HttpEntity result = response.getEntity();

	}
	
	public static void main(String[] args) throws ClientProtocolException, IOException {
		String access_token = getClientCredentials();
		System.out.println(access_token);
		//String addr = url + "/Upload?access_token="+access_token;
		String addr = "https://pp-services.radio-canada.ca/picto/api/v3/Upload";
		String filePath = "/Users/hassanmrad/Downloads/test_imagerie001.jpg";
		HttpClient httpClient = HttpClientBuilder.create().build();
		
		//HttpPost method = new HttpPost(addr);
		//method.setHeader("Content-Type", "multipart/form-data");
		
		//FileBody bin = new FileBody(new File(filePath));
		//System.out.println(bin.getContentType().getMimeType());
		
		//MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		//builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		//String boundary = "---------------"+UUID.randomUUID().toString();=
		
		
		
		MultipartEntityBuilder entity = MultipartEntityBuilder
			    .create()
			    .addTextBody("destination", "dossier")
			    .addTextBody("directory", "ici-info")
			    .addTextBody("alt", "alt")
			    .addTextBody("overwrite", "true")
			    .addTextBody("autoDeclinaison", "true")
			    .addTextBody("legend", "test_imagerie001")
			    .addBinaryBody("source", new File(filePath), ContentType.create("image/jpeg"), "test_imagerie001");
			    //.addPart("source", bin);
			    //.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			
		
		
		HttpEntity httpEntithy = entity.build();
		
		HttpPost httpPost = new HttpPost(addr);
	
		httpPost.setEntity(httpEntithy);
		//httpEntithy.getContentType().
	    //httpPost.setHeader(httpEntithy.getContentType());
		String ctValue = httpEntithy.getContentType().getValue();
		//httpPost.setHeader("content-type", ctValue);
		//httpPost.setHeader("Content-Type", "multipart/form-data");
	    //httpPost.setHeader("Connection", "Keep-Alive");
	    //httpPost.setHeader("Authorization", "Bearer "+access_token);
	    //httpPost.setHeader("Cache-Control", "no-cache");
	    //BasicNameValuePair be;
	    httpPost.setHeaders(
	    		new Header[] {
	    				new BasicHeader("content-type", ctValue),
	    				new BasicHeader("Content-Type", "application/x-www-form-urlencoded"),
	    				new BasicHeader("Connection", "Keep-Alive"),
	    				new BasicHeader("Authorization", "Bearer "+access_token),
	    				new BasicHeader("Cache-Control", "no-cache"),
	    				
	    		}
	    		);
	    //httpEntithy.addBinaryBody("source", new File(filePath), ContentType.APPLICATION_OCTET_STREAM, "Test_Imagerie12");
	    
		HttpResponse response = httpClient.execute(httpPost);
		System.out.println(response);
		HttpEntity result = response.getEntity();
	    
	    		

	}
	
}
