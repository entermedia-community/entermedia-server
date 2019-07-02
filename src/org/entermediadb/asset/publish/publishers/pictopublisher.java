package org.entermediadb.asset.publish.publishers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.hittracker.SearchQuery;
import org.openedit.page.Page;
import org.openedit.users.UserManager;
import org.openedit.hittracker.HitTracker;

public class pictopublisher extends BasePublisher implements Publisher
{

	private static final Log log = LogFactory.getLog(pictopublisher.class);
	private static final Pattern pat = Pattern.compile(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*");
	
	private static Map<String, String[]> UserRestInstruction = new HashMap<String, String[]>(); //rightsusageinstructions 
	
	static {
		UserRestInstruction.put("1", new String[] {"nouvelle"});
		UserRestInstruction.put("2", new String[] {"exclusif"});
		UserRestInstruction.put("3", new String[] {"emission"});
		UserRestInstruction.put("4", new String[] {"equitable"});
		UserRestInstruction.put("5", new String[] {"autre", "Utilisation éditoriale seulement"});
		UserRestInstruction.put("6", new String[] {"autre", "Respecter le contexte d'origine"});
	}
	
	//private static String RESTRICTIONS = "[{\"author\":\"{username}\",\"RestrictionType\":\"{restrictionType}\", \"description\":\"{restrinctionOtherSpecs}\", \"active\":true}]";
	private static String RESTRICTIONS = "{\"author\":\"{username}\",\"RestrictionType\":\"{restrictionType}\", \"description\":\"{restrinctionOtherSpecs}\", \"active\":true}";
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
				publish(inMediaArchive, inDestination, inAsset, f, accessToken);
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
	private void publish(MediaArchive inMediaArchive, Data inDestination, Asset inAsset, File f, String accessToken) 
			throws ClientProtocolException, IOException, HttpException {
		/*
		 * restrictions = $"[{{\"author\":\"{sender.DisplayName}\",\"RestrictionType\":\"autre\", \"description\":\"{mediaGroupParent.TermsOfUse}\", \"active\":true}}]";
           content.Add(new StringContent(restrictions), "\"restrictions\"");
		 */
		String addr = inDestination.get("url");
		String filePath = f.getAbsolutePath();
		log.info("**** publishAPicto publish filePath "+filePath + " to "+ addr);
		
		HttpClient httpClient = HttpClientBuilder.create().build();
		
		String legend = getParam(inAsset, "name", "N/A");
		String alt = getParam(inAsset, "longcaption", "N/A");
		
		//String legend = getParam(inAsset, "longcaption", "N/A");
		String credit = getParam(inAsset, "creator", "");	
		String source = getParam(inAsset, "source", "");	
		String agency = getParam(inAsset, "copyrightnotice", "");	
		if (agency.length() > 0) {
			if (agency.equalsIgnoreCase("Autre")) {
				agency = "Aucune";
				if (source.length() > 0) {
					credit = source + (credit.length() > 0 ? (" / " + credit) : "");
				}
			} else {
				SearcherManager fieldSearcherManager = inMediaArchive.getSearcherManager();
				Searcher searcher = fieldSearcherManager.getSearcher(inMediaArchive.getCatalogId(), "copyrightnotice");
				SearchQuery query = searcher.createSearchQuery();
				
				query.setHitsPerPage(1);
				query.addExact("id", agency);
			    HitTracker hits = searcher.search(query);
			    Data d = searcher.loadData((Data) hits.first());
		            if (d != null) {
		            	agency = d.getName("fr");
		            	//log.info("\t &&&&&&& agency: "+agency);
			    }  
			}
		}
		
		
		
		String destination = getParam(inAsset, "name", "");
		String username = getParam(inAsset, "username", "").trim().replaceAll("( )+", " "); //trim then replace middle spaces with a single space
		String keywords = getParam(inAsset, "keywords", "").replace('|', '-');
		if (keywords.length() == 0) {
			keywords = alt;
		}
		String directory = "ici-info";	
		String sub_directory = "Imagerie";	
		String tmp_sd = inDestination.get("sub_directory");
		if (tmp_sd != null && tmp_sd.length() > 0) {
			sub_directory = tmp_sd;
		} else {
			sub_directory = null;
		}
		/*
		boolean isRestricted = false;
		String restriction = (String)inAsset.get("restrictions");
		if (restriction != null && restriction.equals("2")) {
			isRestricted = true;
		}
		*/
		String restrinctionType = null;
		String restrinctionOtherSpecs = "";
		String rightsusageinstructions = inAsset.get("rightsusageinstructions");
		
		String[] rightsusageinstructionsList = null;
		if (rightsusageinstructions != null) {
			rightsusageinstructionsList = rightsusageinstructions.split("\\|");
			
		}
		String termVal = (String)inAsset.get("rightsusageterms");
		
		log.info("\t **** rightsusageinstructionsList: "+rightsusageinstructionsList);
		
		String restrictions = "[";
		if (rightsusageinstructionsList != null) {
			//Special case, when 5 and 6 options are selectd, we should send "autre" value to true with both text.
			if (rightsusageinstructionsList.length == 2 
					&& rightsusageinstructionsList[0].trim().equals("5") 
					&& rightsusageinstructionsList[1].trim().equals("6") ) {
				String rightUsage1 = (UserRestInstruction.get(rightsusageinstructionsList[0]))[1];
				String rightUsage2 = (UserRestInstruction.get(rightsusageinstructionsList[1]))[1];
				String tmp = RESTRICTIONS.replace("{username}", username)
						.replace("{restrictionType}", "autre")
						.replace("{restrinctionOtherSpecs}", rightUsage1+", "+rightUsage2);
				restrictions = restrictions + tmp + ",";
			} else {
				for (int idx = 0; idx< rightsusageinstructionsList.length; idx++) {
					String[] rightUsage = UserRestInstruction.get(rightsusageinstructionsList[idx]);
					if (rightUsage != null) {
						restrinctionType = rightUsage[0];
						if (rightUsage.length == 2) {
							restrinctionOtherSpecs = rightUsage[1];
						}
					}
					//if (termVal != null && termVal.length() > 0) {
					//	restrinctionType = "autre";
					//	restrinctionOtherSpecs = termVal;
					//}
					
					if (restrinctionType != null) {
						String tmp = RESTRICTIONS.replace("{username}", username)
								.replace("{restrictionType}", restrinctionType)
								.replace("{restrinctionOtherSpecs}", restrinctionOtherSpecs);
						restrictions = restrictions + tmp + ",";
						restrinctionOtherSpecs = "";
					}
					//log.info("\t **** idx: "+idx + " restrictions " + restrictions + " val "+rightsusageinstructionsList[idx]);
				} 
			}
			restrictions = restrictions.substring(0, restrictions.length()-1);
		}
		if (restrictions.length() > 10) {
			restrictions += "]";
		} else {
			restrictions = null;
		}
		
		
			
		/*
		String[] rightUsage = (String[])UserRestInstruction.get(inAsset.get("rightsusageinstructions"));
		if (rightUsage != null) {
			restrinctionType = rightUsage[0];
			if (rightUsage.length == 2) {
				restrinctionOtherSpecs = rightUsage[1];
			}
		}
		String termVal = (String)inAsset.get("rightsusageterms");
		if (termVal != null && termVal.length() > 0) {
			//isRestricted = true;
			restrinctionType = "autre";
			restrinctionOtherSpecs = termVal;
		}*/
		//RESTRICTIONS = "[{\"author\":\"{username}\",\"RestrictionType\":\"{restrictionType}\", \"description\":\"{restrinctionOtherSpecs}\", \"active\":true}]";
		/*
		if (restrinctionType != null) {
			restrictions = RESTRICTIONS.replace("{username}", username)
					.replace("{restrictionType}", restrinctionType)
					.replace("{restrinctionOtherSpecs}", restrinctionOtherSpecs);
			
			
			//restrictions = "\"["{"author":"YAHIA HARKATI","RestrictionType":"equitable", "description":"un dernier exemple equitable", "active":true}]";
		}*/
		
		
		
		log.info("\t legend: "+legend);
		log.info("\t agency: "+agency);
		log.info("\t credit: "+credit);
		log.info("\t destination: "+keywords);
		log.info("\t sub_directory: "+sub_directory);
		log.info("\t directory: "+directory);
		log.info("\t alt: "+alt);
		log.info("\t language: "+"fr");
		//log.info("\t commonName: "+keywords);
		log.info("\t restrictions: "+restrictions);
		log.info("\t username: "+username);
		
		
		MultipartEntityBuilder entity = MultipartEntityBuilder
			    .create()
			    .addTextBody("destination", destination)
			    .addTextBody("directory", directory)
			    .addTextBody("alt", alt)
			    .addTextBody("language", "fr")
			    .addTextBody("overwrite", "false")
			    .addTextBody("autoDeclinaison", "true")
			    .addTextBody("legend", legend)
			    .addTextBody("agency", agency)
			    .addTextBody("commonName", keywords) 
			    .addTextBody("credit", credit)
			    .addTextBody("username", username)
			  //.addTextBody("subdirectory", sub_directory)
			    .addBinaryBody("source", f/*new File(filePath)*/, ContentType.create("image/jpeg"), f.getName());
		if (restrictions != null) {
			entity.addTextBody("restrictions", restrictions);
		}
		if (sub_directory != null) {
			entity.addTextBody("subdirectory", sub_directory);
		}
			    
			    
			    
			
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
	private String getParam(Asset inAsset, String id, String defaultValue) {
		String tmp = inAsset.get(id);
		return tmp == null ? defaultValue: tmp;
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


