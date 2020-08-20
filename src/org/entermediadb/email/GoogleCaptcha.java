package org.entermediadb.email;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openedit.OpenEditException;

public class GoogleCaptcha {

	
	private static final String RECAPTCHA_SERVICE_URL = "https://www.google.com/recaptcha/api/siteverify";
	public static String secretKey = "x123"; //Move to Catalog Settings?
	
	public static String getSecretKey() {
		return secretKey;
	}

	public static void setSecretKey(String secretKey) {
		GoogleCaptcha.secretKey = secretKey;
	}

	public static boolean isValid(String clientRecaptchaResponse) throws OpenEditException {
		if (clientRecaptchaResponse == null || "".equals(clientRecaptchaResponse)) {
			return false;
		}
		
		Boolean success = false;
		Double score = 0.0;
		
		try {
		URL obj = new URL(RECAPTCHA_SERVICE_URL);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		con.setRequestMethod("POST");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		//add client result as post parameter
		String postParams =
				"secret=" + getSecretKey() +
				"&response=" + clientRecaptchaResponse;

		// send post request to google recaptcha server
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(postParams);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();

		//System.out.println("Post parameters: " + postParams);
		//System.out.println("Response Code: " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(
				con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		System.out.println(response.toString());

		//Parse JSON-response
		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(response.toString());


		success = (Boolean) json.get("success");
		score = (Double) json.get("score");

		//System.out.println("success : " + success);
		//System.out.println("score : " + score);

		//result should be sucessfull and spam score above 0.5
		
		}catch (Exception e)
		{
			e.printStackTrace();
		}
		//return (success && score >= 0.5); //score is for v3
		return (success);
	}
}
