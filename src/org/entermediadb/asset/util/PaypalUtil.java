package org.entermediadb.asset.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;

import org.openedit.WebPageRequest;

public class PaypalUtil {

	

	public String handleIPN(WebPageRequest inReq) throws Exception{
		String mode = inReq.findValue("usePaypalSandbox");

		String url = "https://www.paypal.com/cgi-bin/webscr";
		if(Boolean.parseBoolean(mode)){
			url = "https://www.sandbox.paypal.com/cgi-bin/webscr";
		}
		Enumeration en = inReq.getRequest().getParameterNames();
		String str = "cmd=_notify-validate";
		while(en.hasMoreElements()){
		String paramName = (String)en.nextElement();
		String paramValue = inReq.getRequest().getParameter(paramName);
		str = str + "&" + paramName + "=" + URLEncoder.encode(paramValue);
		}

		// post back to PayPal system to validate
		// NOTE: change http: to https: in the following URL to verify using SSL (for increased security).
		// using HTTPS requires either Java 1.4 or greater, or Java Secure Socket Extension (JSSE)
		// and configured for older versions.
		//URL u = new URL("https://www.paypal.com/cgi-bin/webscr");
		URL u = new URL(url);
		URLConnection uc = u.openConnection();
		uc.setDoOutput(true);
		uc.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		PrintWriter pw = new PrintWriter(uc.getOutputStream());
		pw.println(str);
		pw.close();

		BufferedReader in = new BufferedReader(
		new InputStreamReader(uc.getInputStream()));
		String res = in.readLine();
		in.close();

		

		//check notification validation
		if(res.equals("VERIFIED")) {
	
			//getStore().getOrderArchive().
		
		}
		else if(res.equals("INVALID")) {
		

		// log for investigation
		}
		else {
		// error
		}
		return res;
	}

	public String handlePDT(WebPageRequest inReq) throws Exception {
		String mode = inReq.findValue("usePaypalSandbox");

		String url = "https://www.paypal.com/cgi-bin/webscr";
		if(Boolean.parseBoolean(mode)){
			url = "https://www.sandbox.paypal.com/cgi-bin/webscr";
		}
		String token = inReq.findValue("pdtToken");
		Enumeration en = inReq.getRequest().getParameterNames();
		String str = "cmd=_notify-synch";
		while(en.hasMoreElements()){
		String paramName = (String)en.nextElement();
		String paramValue = inReq.getRequest().getParameter(paramName);
		str = str + "&" + paramName + "=" + URLEncoder.encode(paramValue);
		}
		str = str + "&at=" + URLEncoder.encode(token);
		
			
		URL u = new URL(url);
		URLConnection uc = u.openConnection();
		uc.setDoOutput(true);
		uc.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		PrintWriter pw = new PrintWriter(uc.getOutputStream());
		pw.println(str);
		pw.close();
		BufferedReader in = new BufferedReader(
				new InputStreamReader(uc.getInputStream()));
		String res = in.readLine();
		HashMap resultInfo = new HashMap();
		String line = null;
		int rowNum = 0;
	
		while( (line = in.readLine() ) != null)
		{
			String[] param = line.split("=");
			if(param.length == 2){
			resultInfo.put(param[0], param[1]);
			}
		}
		inReq.putPageValue("resultMap", resultInfo);

//		StringWriter out = new StringWriter();
//		InputStream in = uc.getInputStream();
//		byte[] input = new byte[1024];
//		int size = 0;
//		while ((size = in.read(input)) != -1) {
//			out.append(new String(input, 0, size, "UTF-8"));
//		}
//		String responseString = out.toString();
		return res;
		
		
	}
	
	
	
}

