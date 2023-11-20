package org.entermediadb.google;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONObject;
import org.openedit.users.User;

public class FireBase {

	private static final Log log = LogFactory.getLog(FireBase.class);

	public void notifyTopic(String firebaseid, String inToken, String inChannel, User inUser, String inSubject, String inMessage, Map extraData)
	{
		HttpSharedConnection connection = new HttpSharedConnection();

		//https://console.firebase.google.com/u/2/project/openinstitute-27575/notification/reporting
		
		//https://medium.com/@ThatJenPerson/authenticating-firebase-cloud-messaging-http-v1-api-requests-e9af3e0827b8
		
		//https://firebase.google.com/docs/cloud-messaging/ios/topic-messaging
		
		//project-1099360417953
		//https://fcm.googleapis.com/v1/projects/myproject-b5ae1/messages
		
		//https://firebase.google.com/docs/cloud-messaging/http-server-ref
		
		//---oAuth Permissions---
		//Edit Permissions at /app/view/settings/google/connect.xconf
		//Add: https://www.googleapis.com/auth/firebase.messaging
		//Re-connect to Google oAuth (Server/Connect with Google)
		 
		
		
		//TODO: Add to catalog settings
		HttpPost post = new HttpPost("https://fcm.googleapis.com/v1/projects/" + firebaseid +  "/messages:send"); //entermediadb-177816
		post.setHeader("Content-type", "application/json");
		post.setHeader("Authorization", "Bearer " + inToken); //"AIzaSyBSxxxxsXevRq0trDbA9mhnY_2jqMoeChA"

		JSONObject root = new JSONObject();
		JSONObject message = new JSONObject();
		root.put("message",message);
		//message.put("to", "dBbB2BFT-VY:APA91bHrvgfXbZa-K5eg9vVdUkIsHbMxxxxxc8dBAvoH_3ZtaahVVeMXP7Bm0iera5s37ChHmAVh29P8aAVa8HF0I0goZKPYdGT6lNl4MXN0na7xbmvF25c4ZLl0JkCDm_saXb51Vrte");
		//message.put("priority", "high");
		//message.put("topic", "my_channel_id");
		//message.put("from", inUser.getEmail());
		message.put("topic", inChannel);
		
		//message.put("channel_id","my_channel_id");
		JSONObject data = new JSONObject();
		data.put("collectionid",inChannel);  //TODO: This does not seem correct
		if(inUser != null)
		{
			data.put("userid",inUser.getId()); 
			data.put("useremail",inUser.getEmail()); 
			data.put("userlabel",inUser.getScreenName()); 
		}
		for (Iterator iterator = extraData.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			Object value = extraData.get(key);
			//data.put("chattopic",inTopic);
			data.put(key,value);
		}
		data.put("title", inSubject);
		data.put("body", inMessage);
		data.put("notificationtype", "chat"); //TODO: Add more
		

//		 intent.putExtra("collectionid",inCollectionId);
//        intent.putExtra("userlabel",inUserLabel);
//        intent.putExtra("message",messageBody);
		
		message.put("data", data);
	
		JSONObject notification = new JSONObject();
		notification.put("title", inSubject);
		notification.put("body", inMessage);
		message.put("notification", notification);

		String tosend = root.toJSONString();
		post.setEntity(new StringEntity(tosend, "UTF-8"));

		CloseableHttpResponse resp = null;
	 	try
	 	{
	 		resp = connection.sharedPost(post);
	
			if (resp.getStatusLine().getStatusCode() != 200)
			{
				log.info("Google Server error returned " + resp.getStatusLine().getStatusCode() + ":" + resp.getStatusLine().getReasonPhrase());
				String returned = EntityUtils.toString(resp.getEntity());
				log.error(returned);
			}
			else
			{
//				String content = IOUtils.toString(resp.getEntity().getContent());
				log.info("Google message sent " +  message);
			}
//			JsonParser parser = new JsonParser();
//			JsonElement elem = parser.parse(content);
	    }
	 	catch( Throwable ex)
	 	{
	 		log.error("Could not send",ex);
	 	}
	 	finally 
	 	{
	 		connection.release(resp);
        }
	}	
}
