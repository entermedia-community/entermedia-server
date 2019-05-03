/*
 * Created on May 12, 2006
 */
package org.entermediadb.modules.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.openedit.OpenEditException;
import org.openedit.util.FileUtils;
import org.openedit.util.OutputFiller;

public class Downloader
{

	public void download(String inUrl, String inAbsoluteFilePath) throws OpenEditException
	{
		download(inUrl, new File(inAbsoluteFilePath));
	}

	public void ftpDownload(String inServer,String path,String filename, String downloadfilename, String inUsername, String inPassword)
	{
		FTPClient client = new FTPClient();
		FileOutputStream fos = null;
		try
		{

			client.connect(inServer);
			if(inUsername != null && inUsername.length() != 0) {
				client.login(inUsername, inPassword);
			} else {
				client.login("anonymous", "");

			}


			fos = new FileOutputStream(downloadfilename);
			client.changeWorkingDirectory(path);
			// Fetch file from server 
			
//			 FTPFile[] ftpFiles = client.listFiles();
//
//	            if (ftpFiles != null && ftpFiles.length > 0) {
//	                //loop thru files
//	                for (FTPFile file : ftpFiles) {
//	                    if (!file.isFile()) {
//	                        continue;
//	                    }
//	                    System.out.println("File is " + file.getName());
//	                    //get output stream
//	                   // OutputStream output;
//	                //    output = new FileOutputStream("FtpFiles" + "/" + file.getName());
//	                    //get the file from the remote system
//	                    //client.retrieveFile(file.getName(), output);
//	                    //close output stream
//	              //      output.close();
//
//	                    //delete the file
//	                    // ftp.deleteFile(file.getName());
//
//	                }
//	            }
//			
			
			
			client.retrieveFile( filename, fos);

		}
		catch (Exception e)
		{

			throw new OpenEditException(e);

		}
		finally
		{
			try
			{
				if (fos != null)
				{
					fos.close();
				}
				client.disconnect();
			}
			catch (IOException e)
			{

				throw new OpenEditException(e);

			}

		}

	

	}

	public void download(String inStrUrl, File outputFile) throws OpenEditException
	{
		FileOutputStream out = null;
		InputStream in = null;
		HttpGet method = null;
		try
		{

			RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
			HttpClient client = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();

			method = new HttpGet(inStrUrl);

			//  HttpRequestBuilder builder = new HttpRequestBuilder();

			// method.setEntity(builder.build());

			HttpResponse response2 = client.execute(method);
			StatusLine sl = response2.getStatusLine();
			//int status = client.executeMethod(method);
			if (sl.getStatusCode() != 200)
			{
				throw new Exception(method + " Request failed: status code " + sl.getStatusCode());
			}

			//this helps prevent 403 errors.
			//			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB;     rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 (.NET CLR 3.5.30729)");

			//*** create new output file
			//*** make a growable storage area to read into 
			outputFile.getParentFile().mkdirs();
			out = new FileOutputStream(outputFile);
			//*** read in url connection stream into input stream
			HttpEntity httpentity = response2.getEntity();
			in = httpentity.getContent();
			//*** fill output stream
			//log.info("downloading " + inStrUrl);
			new OutputFiller().fill(in, out);
			//EntityUtils.consume(httpentity);
		}
		catch (Throwable ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			//*** close output stream
			FileUtils.safeClose(out);
			//*** close input stream
			FileUtils.safeClose(in);
			if (method != null)
			{
				method.releaseConnection();
			}
		}
	}

	public String downloadToString(String inUrl)
	{
		StringWriter out = null;
		InputStream in = null;
		try
		{
			URL url = new URL(inUrl);
			URLConnection con = url.openConnection();
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB;     rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 (.NET CLR 3.5.30729)");

			con.setConnectTimeout(15 * 1000);
			con.setReadTimeout(15 * 1000);
			con.setUseCaches(false);
			con.connect();

			//*** create new output file
			//*** make a growable storage area to read into 
			out = new StringWriter();
			//*** read in url connection stream into input stream
			in = con.getInputStream();
			//*** fill output stream
			new OutputFiller().fill(new InputStreamReader(in), out);
			return out.toString();
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
		finally
		{
			//*** close output stream
			FileUtils.safeClose(out);
			//*** close input stream
			FileUtils.safeClose(in);
		}
	}

	public File download(URL url, File dstFile)
	{
		CloseableHttpClient httpclient = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()) // adds HTTP REDIRECT support to GET and POST methods 
				.build();
		try
		{
			HttpGet get = new HttpGet(url.toURI()); // we're using GET but it could be via POST as well
			File downloaded = httpclient.execute(get, new FileDownloadResponseHandler(dstFile));
			return downloaded;
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
		finally
		{
			//IOUtils.closeQuietly(httpclient);
		}
	}

	static class FileDownloadResponseHandler implements ResponseHandler<File>
	{

		private final File target;

		public FileDownloadResponseHandler(File target)
		{
			this.target = target;
		}

		@Override
		public File handleResponse(HttpResponse response) throws ClientProtocolException, IOException
		{
			InputStream source = response.getEntity().getContent();

			org.apache.commons.io.FileUtils.copyInputStreamToFile(source, this.target);
			return this.target;
		}

	}

}
