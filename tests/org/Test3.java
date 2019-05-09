package org;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.message.BasicHeader;


/**
 * This utility class provides an abstraction layer for sending multipart HTTP
 * POST requests to a web server. 
 * @author www.codejava.net
 *
 */



class MultipartUtility {
	private final String boundary;
	private static final String LINE_FEED = "\r\n";
	private HttpURLConnection httpConn;
	private String charset;
	private OutputStream outputStream;
	private PrintWriter writer;

	/**
	 * This constructor initializes a new HTTP POST request with content type
	 * is set to multipart/form-data
	 * @param requestURL
	 * @param charset
	 * @throws IOException
	 */
	public MultipartUtility(String requestURL, String charset)
			throws IOException {
		this.charset = charset;
		
		// creates a unique boundary based on time stamp
		boundary = "===" + System.currentTimeMillis() + "===";
		
		URL url = new URL(requestURL);
		httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setRequestMethod("POST");
		httpConn.setUseCaches(false);
		httpConn.setDoOutput(true);	// indicates POST method
		httpConn.setDoInput(true);
		
		httpConn.setRequestProperty("Content-Type",
				"multipart/form-data; boundary=" + boundary);
		//httpConn.setRequestProperty("User-Agent", "CodeJava Agent");
		//httpConn.setRequestProperty("Test", "Bonjour");
		outputStream = httpConn.getOutputStream();
		writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
				true);
	}

	/**
	 * Adds a form field to the request
	 * @param name field name
	 * @param value field value
	 */
	public void addFormField(String name, String value) {
		writer.append("--" + boundary).append(LINE_FEED);
		writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
				.append(LINE_FEED);
		writer.append("Content-Type: text/plain; charset=" + charset).append(
				LINE_FEED);
		writer.append(LINE_FEED);
		writer.append(value).append(LINE_FEED);
		writer.flush();
	}

	/**
	 * Adds a upload file section to the request 
	 * @param fieldName name attribute in <input type="file" name="..." />
	 * @param uploadFile a File to be uploaded 
	 * @throws IOException
	 */
	public void addFilePart(String fieldName, File uploadFile)
			throws IOException {
		String fileName = uploadFile.getName();
		writer.append("--" + boundary).append(LINE_FEED);
		writer.append(
				"Content-Disposition: form-data; name=\"" + fieldName
						+ "\"; filename=\"" + fileName + "\"")
				.append(LINE_FEED);
		writer.append(
				"Content-Type: "
						+ URLConnection.guessContentTypeFromName(fileName))
				.append(LINE_FEED);
		writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
		writer.append(LINE_FEED);
		writer.flush();

		FileInputStream inputStream = new FileInputStream(uploadFile);
		byte[] buffer = new byte[4096];
		int bytesRead = -1;
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}
		outputStream.flush();
		inputStream.close();
		
		writer.append(LINE_FEED);
		writer.flush();		
	}

	/**
	 * Adds a header field to the request.
	 * @param name - name of the header field
	 * @param value - value of the header field
	 */
	public void addHeaderField(String name, String value) {
		writer.append(name + ": " + value).append(LINE_FEED);
		writer.flush();
	}
	
	/**
	 * Completes the request and receives response from the server.
	 * @return a list of Strings as response in case the server returned
	 * status OK, otherwise an exception is thrown.
	 * @throws IOException
	 */
	public List<String> finish() throws IOException {
		List<String> response = new ArrayList<String>();

		writer.append(LINE_FEED).flush();
		writer.append("--" + boundary + "--").append(LINE_FEED);
		writer.close();

		// checks server's status code first

		int status = httpConn.getResponseCode();
		if (status == HttpURLConnection.HTTP_OK) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					httpConn.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				response.add(line);
			}
			reader.close();
			httpConn.disconnect();
		} else {
			throw new IOException("Server returned non-OK status: " + status);
		}

		return response;
	}
	
	private static final String clientId = "77cb9279-94ab-45a0-99f1-fefe3f7bf51c";
	private static final String clientSecret = "1bf39680-f4aa-48ab-b4fa-6a2d70f9fedf";
	//private static final String tokenUrl = "https://api-gw-dev-v3.radio-canada.ca/auth/oauth/v2/token";
	private static final String tokenUrl = "https://dev-services.radio-canada.ca/auth/oauth/v2/token";
	private static final String auth = clientId + ":" + clientSecret;
	private static final String authentication = Base64.getEncoder().encodeToString(auth.getBytes());
	
	private static final Pattern pat = Pattern.compile(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*");
	

    static String getClientCredentials() {
	    String content = "grant_type=client_credentials";
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

}
public class Test3 {
	
	public static void main(String[] args) throws IOException {
		BufferedReader reader = null;
	    HttpsURLConnection connection = null;
	    String returnValue = "";
	    
		String urlpoint = "https://pp-services.radio-canada.ca/picto/api/v3/Upload";
			
	    String charset = "UTF-8";
	      
	    String access_token = "d08d47e2-84d8-4878-8452-dec58ae5e6e5";
	    //access_token = MultipartUtility.getClientCredentials();
	    System.out.println(access_token);
		File binaryFile = new File("/Users/hassanmrad/Downloads/test1.jpg");
		String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"; //Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
		String CRLF = "\r\n"; // Line separator required by multipart/form-data.

		URL url = new URL(urlpoint);
        connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        
		connection.setRequestProperty("content-Type", "multipart/form-data; boundary=" + boundary);
		//connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Authorization", "Bearer " + access_token);
		connection.setRequestProperty("Cache-Control", "no-cache");
		
		try (
		    OutputStream output = connection.getOutputStream();
		    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
		) {
			//Binary
			writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"source\"; filename=\"" + binaryFile.getName() + "\"").append(CRLF);
		    writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(binaryFile.getName())).append(CRLF);
		    writer.append("Content-Transfer-Encoding: binary").append(CRLF);
		    writer.append(CRLF).flush();
		    

		    
		    // Send normal param.
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"directory\"").append(CRLF);
		    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		    writer.append(CRLF).append("info-ici").append(CRLF).flush();
		    
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"destination\"").append(CRLF);
		    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		    writer.append(CRLF).append("dossier").append(CRLF).flush();
		    
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"overwrite\"").append(CRLF);
		    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		    writer.append(CRLF).append("true").append(CRLF).flush();
		    
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"legend\"").append(CRLF);
		    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		    writer.append(CRLF).append("test").append(CRLF).flush();
		    
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"alt\"").append(CRLF);
		    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		    writer.append(CRLF).append("alt").append(CRLF).flush();
		    
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"autoDeclinaison\"").append(CRLF);
		    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		    writer.append(CRLF).append("true").append(CRLF).flush();
		   
		    // End of multipart/form-data.
		    writer.append("--" + boundary + "--").append(CRLF).flush();
		    
		    Files.copy(binaryFile.toPath(), output);
		    output.flush(); // Important before continuing with writer!
		    writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.
		    
		    
		}

		// Request is lazily fired whenever you need to obtain information about response.
		int responseCode = ((HttpURLConnection) connection).getResponseCode();
		System.out.println(responseCode); // Should be 200
	}
	
	public static void main__3(String[] args) throws IOException {
		String url = "https://pp-services.radio-canada.ca/picto/api/v3/Upload";
			
	    String charset = "UTF-8";
	    charset = "";
	    //File uploadFile1 = new File("/Users/hassanmrad/Downloads/test.jpg");
	        
	    String access_token = "c453e10a-872a-4900-9681-1acf8b883828";
	    access_token = MultipartUtility.getClientCredentials();
	    System.out.println(access_token);
	    // System.exit(0);
		//String url = "http://example.com/upload";
		//String charset = "UTF-8";
		//String param = "value";
		//File textFile = new File("/path/to/file.txt");
		File binaryFile = new File("/Users/hassanmrad/Downloads/test1.jpg");
		String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
		String CRLF = "\r\n"; // Line separator required by multipart/form-data.

		URLConnection connection = new URL(url).openConnection();
		connection.setDoOutput(true);
		connection.setRequestProperty("content-Type", "multipart/form-data; boundary=" + boundary);
		//connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Authorization", "Bearer " + access_token);
		connection.setRequestProperty("Cache-Control", "no-cache");

		try (
		    OutputStream output = connection.getOutputStream();
		    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output), true);
		) {
		    // Send normal param.
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"directory\"").append(CRLF);
		    //writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		    writer.append(CRLF).append("info-ici").append(CRLF).flush();
		    
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"destination\"").append(CRLF);
		    //writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		    writer.append(CRLF).append("dossier").append(CRLF).flush();
		    
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"overwrite\"").append(CRLF);
		    //writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		    writer.append(CRLF).append("true").append(CRLF).flush();
		    
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"legend\"").append(CRLF);
		    //writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		    writer.append(CRLF).append("test").append(CRLF).flush();
		    
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"alt\"").append(CRLF);
		    //writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		    writer.append(CRLF).append("alt").append(CRLF).flush();
		    
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"autoDeclinaison\"").append(CRLF);
		    //writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
		    writer.append(CRLF).append("true").append(CRLF).flush();
		    

		    // Send text file.
		    //writer.append("--" + boundary).append(CRLF);
		    //writer.append("Content-Disposition: form-data; name=\"textFile\"; filename=\"" + textFile.getName() + "\"").append(CRLF);
		    //writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
		    //writer.append(CRLF).flush();
		    //Files.copy(textFile.toPath(), output);
		    //output.flush(); // Important before continuing with writer!
		    //writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

		    // Send binary file.
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"source\"; filename=\"" + binaryFile.getName() + "\"").append(CRLF);
		    writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(binaryFile.getName())).append(CRLF);
		    writer.append("Content-Transfer-Encoding: binary").append(CRLF);
		    writer.append(CRLF).flush();
		    Files.copy(binaryFile.toPath(), output);
		    output.flush(); // Important before continuing with writer!
		    writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

		    // End of multipart/form-data.
		    writer.append("--" + boundary + "--").append(CRLF).flush();
		}

		// Request is lazily fired whenever you need to obtain information about response.
		int responseCode = ((HttpURLConnection) connection).getResponseCode();
		System.out.println(responseCode); // Should be 200
	}
	public static void main_2(String[] args) {
		
		 String url = "https://dev-services.radio-canada.ca/picto/api/v3";
		
        String charset = "UTF-8";
        File uploadFile1 = new File("/Users/hassanmrad/Downloads/test.jpg");
        
        String access_token = MultipartUtility.getClientCredentials();
		System.out.println(access_token);
		//String addr = url + "/Upload?access_token="+access_token;

		String addr = url + "/Upload";
		
        //String requestURL = "http://localhost:8080/FileUploadSpringMVC/uploadFile.do";
		MultipartUtility multipart = null;
        try {
            multipart = new MultipartUtility(addr, charset);
            /*
            new BasicHeader("content-type", ctValue),
			new BasicHeader("Content-Type", "multipart/form-data"),
			new BasicHeader("Connection", "Keep-Alive"),
			new BasicHeader("Authorization", "Bearer "+access_token),
			new BasicHeader("Cache-Control", "no-cache"),
			*/
            String boundary = "---------------"+UUID.randomUUID().toString();
            multipart.addHeaderField("Authorization", "Bearer "+access_token);
            multipart.addHeaderField("content-Type", "multipart/form-data; boundary="+boundary);
            multipart.addHeaderField("Content-Type", "application/x-www-form-urlencoded");
            multipart.addHeaderField("Cache-Control", "no-cache");

            multipart.addFormField("destination", "dossier");
            multipart.addFormField("directory", "ici-info");
            multipart.addFormField("alt", "alt");
            multipart.addFormField("overwrite", "true");
            multipart.addFormField("autoDeclinaison", "true");
            multipart.addFormField("legend", "test_imagerie001");
            
            
            
            multipart.addFilePart("source", uploadFile1);
            

            List<String> response = multipart.finish();
             
            System.out.println("SERVER REPLIED:");
             
            for (String line : response) {
                System.out.println(line);
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
}
