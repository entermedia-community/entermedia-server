package org.entermediadb.asset.publish.publishers;

import java.io.BufferedOutputStream;
import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
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

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.io.FileOutputStream;
import java.io.IOException;
  

public class avidpublisher extends BasePublisher implements Publisher
{
	static String JSON_STR = "{\n" + 
			"    \"Attachments\": [],\n" + 
			"    \"JobName\": \"{Image_Name}\",\n" + 
			"    \"Labels\": [],\n" + 
			"    \"Medias\": [\n" + 
			"        {\n" + 
			"            \"Identifier\": \"abb3f7e5-8d7e-49ba-84c5-674a55edb700\",\n" + 
			"            \"Data\": \"{Data}\",\n" + 
			"            \"Description\": \"{Description}\",\n" + 
			"            \"Files\": [\n" + 
			"                \"{Image_Path}\"\n" + 
			"            ],\n" + 
			"            \"Name\": \"Original\"\n" + 
			"        }\n" + 
			"    ],\n" + 
			"    \"Priority\": 0,\n" + 
			"    \"Variables\": [\n" +
			"                     {\n" +
			"                        \"Identifier\":\"2972dbcb-d724-40c9-9a52-b13467a2983b\",\n" +
			"                        \"DefaultValue\":\"\",\n" +
			"                        \"Description\":\"The full path for XML metadata\",\n" +
			"                        \"Name\":\"XML_Path\",\n" +
			"                        \"TypeCode\":\"Uri\",\n" +
			"                        \"Value\":\"{Xml_Path}\"\n"+
		    "                     }\n]\n" + 
			"}";
	
	static String XML_STR = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<Metadata>\n"
			+ "<Entry>\n"
			+ "<Tag>ClipName</Tag>\n"
			+ "<Value>{ClipName}</Value>\n"
			+ "</Entry>\n"
			+ "<Entry>\n"
			+ "<Tag>Comments</Tag>\n"
			+ "<Value>{Comments}</Value>\n"
			+ "</Entry>\n"
			+ "<Locator>\n"
			+ "<Label>{Label}</Label>\n"
			+ "<Color>White</Color>\n"
			+ "<Timecode>10:00:00:00</Timecode>\n"
			+ "<Username>mtlvantagews</Username>\n"
			+ "<Track>V1</Track>\n"
			+ "</Locator>\n"
			+ "</Metadata>\n"
			+ "";
	
	static final String PREFIX_DEST = "IMG ";
	/*
	static String JSON_STR = "{\n" + 
			"    \"Attachments\": [{Attachments}],\n" + 
			"    \"JobName\": \"{JobName}\",\n" + 
			"    \"Labels\": [{Labels}],\n" + 
			"    \"Medias\": [\n" + 
			"        {\n" + 
			"            \"Identifier\": \"{Identifier}\",\n" + 
			"            \"Data\": \"{Data}\",\n" + 
			"            \"Description\": \"{Description}\",\n" + 
			"            \"Files\": [\n" + 
			"                \"{Files}\"\n" + 
			"            ],\n" + 
			"            \"Name\": \"{Name}\"\n" + 
			"        }\n" + 
			"    ],\n" + 
			"    \"Priority\": 0,\n" + 
			"    \"Variables\": [{},{}]\n" + 
			"}";

	*/
	private static Map<String, String> UserRestrictions = new HashMap<String, String>();   //restrictions
	private static Map<String, String> UserRestInstruction = new HashMap<String, String>(); //rightsusageinstructions 
	static {
		UserRestInstruction.put("1", "Nouvelles seulement");
		UserRestInstruction.put("2", "Utilisation unique");
		UserRestInstruction.put("3", "Émission spécifique");
		UserRestInstruction.put("4", "Utilisation équitable seulement");
		UserRestInstruction.put("5", "Utilisation éditoriale seulement");
		UserRestInstruction.put("6", "Respecter le contexte d'origine");
		
		UserRestrictions.put("1", "Aucune");
		UserRestrictions.put("2", "Oui");
	}
	private static final Log log = LogFactory.getLog(avidpublisher.class);
	
	public PublishResult publish(MediaArchive inMediaArchive, Asset inAsset, Data inPublishRequest, Data inDestination, Data inPreset)
	{
		log.info("inPublishRequest inPublishRequest: " + inPublishRequest.getProperties());
		log.info("inPublishRequest inAsset: " + inAsset.getProperties());
		log.info("inPublishRequest inDestination: " + inDestination.getProperties());
		log.info("inPublishRequest inPreset: " + inPreset.getProperties());
		
		String catalogId = inMediaArchive.getCatalogId();
		ProfileModule module = (ProfileModule)inMediaArchive.getModuleManager().getBean("ProfileModule");
		//inMediaArchive
		//ProfileModule module = (ProfileModule)getFixture().getModuleManager().getBean("ProfileModule");
		
		UserProfileManager upmanager = module.getUserProfileManager();
		UserManager um = upmanager.getUserManager(catalogId);
		log.info("inPublishRequest um.getUsers(): " + um.getUsers());
		
		try
		{
			PublishResult result = checkOnConversion(inMediaArchive, inPublishRequest, inAsset, inPreset);
			if (result != null)
			{
				return result;
			}

			result = new PublishResult();

			Page inputpage = findInputPage(inMediaArchive, inAsset, inPreset);
			
			File source = new File(inputpage.getContentItem().getAbsolutePath());
			String fullName = source.getName();
			int idxP = fullName.lastIndexOf('.');
			if (idxP == -1) {
				throw new OpenEditException("Source file name error "+source.getAbsolutePath());
			}
			String fileName = fullName.substring(0, idxP);
			//String extension = tmp.substring(idxP+1);
			String[] deposit = inDestination.get("url").split(",");
			String vaultDirectory = deposit[0];
			String vaultDirectory_vantage = deposit[1];
			
			//new File(vaultDirectory, file.getName())
			
			//Copy the file first
			//File dest = new File(vaultDirectory, source.getName());
			//if (dest.exists()) {
			//	throw new OpenEditException("Destination File already exist in deposit folder "+dest.getAbsolutePath());
			//}
			String destFilename = null;
			try {
				Path pathSource = FileSystems.getDefault().getPath(inputpage.getContentItem().getAbsolutePath());
				destFilename = PREFIX_DEST + source.getName();
				Path pathDest = FileSystems.getDefault().getPath(vaultDirectory, destFilename);
				log.info("publishAvid copy file "+pathSource+" dest "+pathDest);
				Files.copy(pathSource, pathDest, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ioex) {
				log.error(ioex);
				throw new OpenEditException(ioex);
			}

			//create XML
			File xmlFile = new File(vaultDirectory, fileName+".xml");
			try {
				createXmlFile(destFilename, xmlFile, inDestination, inAsset);
			} catch (IOException ioex) {
				log.error(ioex);
				throw new OpenEditException(ioex);
			}
			
			try {
				//publish(vaultDirectory_vantage, xmlFile, inDestination, inAsset);
				publish(vaultDirectory_vantage, destFilename, xmlFile.getName(), inDestination, inAsset);
			} catch (OpenEditException ex) {
				log.error(ex);
				throw ex;
			}
			
			
			log.info("publishAvid file "+source.getAbsolutePath()+" to Avid");
			result.setComplete(true);
			return result;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new OpenEditException(e);
		}

	}

	private void createXmlFile(String filename,File xmlFile, Data inDestination, Asset inAsset) throws IOException {
		
		String longcaption = getParam(inAsset, "longcaption");
		String ville = getParam(inAsset, "city");
		String etat = getParam(inAsset, "state");
		//String pays = getParam(inAsset, "country");
		String date = getParam(inAsset, "assetcreationdate");
		String credit = getParam(inAsset, "copyrightnotice");	
		String credit_autre = getParam(inAsset, "source");	
		String url = getParam(inAsset, "downloadurl-file");
		String auteur = getParam(inAsset, "creator");	
		//String destination = getParam(inAsset, "name");	
		String restrictions = UserRestrictions.get(getParam(inAsset, "restrictions"));	
		//String conditionsUtilisation = getParam(inAsset, "rightsusageinstructions");	
		String conditionsUtilisation_autre = getParam(inAsset, "rightsusageterms");	
		
		/*
		Document document = DocumentFactory.getInstance().createDocument();
        // Create the root element of xml file
        Element root = document.addElement("Metadata");
        addEntry(root, "ClipName", "IMG "+filename);
        addEntry(root, "Comments", longcaption);
        */
        String data = "Description : " + longcaption+System.lineSeparator();
        data = data + "Ville : "+ville+System.lineSeparator();
        data = data + "Province/État : "+etat+System.lineSeparator();
        //data = data + "Pays : "+pays+System.lineSeparator();
        data = data + "Date : "+date+System.lineSeparator();
        data = data + "Crédits : "+credit+System.lineSeparator();
        data = data + "Crédits - Autre : "+credit_autre+System.lineSeparator();
        data = data + "URL, nom Avid : "+url+System.lineSeparator();
        data = data + "Auteur : "+auteur+System.lineSeparator();
        
        if (restrictions != null) {
        	data = data + "Restrictions : "+restrictions+System.lineSeparator();
        }
        
        String restrictionValue = getRestrictionValue(inAsset);
        if (restrictionValue != null) {
        	data = data + "Conditions d'utilisation : "+restrictionValue+System.lineSeparator();
        }
        data = data + "Autres conditions d'utilisation : "+conditionsUtilisation_autre+System.lineSeparator();
        /*
        Element locator = root.addElement("Locator");
        Element label = locator.addElement("Label");
        label.setText(data);
        Element color = locator.addElement("Color");
        color.setText("YELLOW");
        Element timecode = locator.addElement("Timecode");
        timecode.setText("10:00:00:00");
        Element username = locator.addElement("Username");
        username.setText("mtlvantagews");
        Element track = locator.addElement("Track");
        track.setText("V1");
        */
        String payload = XML_STR.replace("{ClipName}", "IMG "+filename)
				.replace("{Comments}", longcaption)
				.replace("{Label}", data);

        log.info("publishAvid createXmlFile "+xmlFile.getAbsolutePath());
        
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(xmlFile, false));
        bos.write(payload.getBytes());
        bos.flush();
        bos.close();
        bos = null;
        		/*
        // Create the pretty print of xml document.
        OutputFormat format = OutputFormat.createPrettyPrint();//createCompactFormat();
        // Create the xml writer by passing outputstream and format
        XMLWriter writer = new XMLWriter(fos, format);
        // Write to the xml document
        writer.write(document);
        // Flush after done
        writer.flush();
        writer.close();
        writer = null;*/
	}
	
	private String getRestrictionValue(Asset inAsset) {
		String right_val = "";
		String rightsusageinstructions = inAsset.get("rightsusageinstructions");
		if (rightsusageinstructions != null) {
			String[] rightsusageinstructionsList = rightsusageinstructions.split("\\|");
			for (int idx = 0; idx< rightsusageinstructionsList.length; idx++) {
				String rightUsage = UserRestInstruction.get(rightsusageinstructionsList[idx]);
				right_val = right_val + rightUsage + ",";
			}
			if (right_val.length() > 0) {
				right_val = right_val.substring(0, right_val.length()-1);
				return right_val;
			}
		}
		return null;
	}
	/*
	private void addEntry(Element root, String param, String val) {
		Element entry = root.addElement("Entry");
        Element tag = entry.addElement("Tag");
        tag.setText(param);
        Element value = entry.addElement("Value");
        value.setText(val);
	}
	*/
	private void publish(String vault, String filename, String xmlFilename, Data inDestination, Asset inAsset) throws ClientProtocolException, IOException, HttpException {
		String tmp = inDestination.get("server");
		if (tmp == null) {
			throw new OpenEditException("Avid - Vantage publishdestination.server is empty");
		}
		String[] servers = tmp.split(",");
		if (servers.length == 0) {
			throw new OpenEditException("Avid - Vantage publishdestination.server Not found");
		}
		String url = servers[(int)(servers.length-1 * Math.random())];
		String profile = inDestination.get("refresh_token");
		url = url+"/Rest/workflows/"+profile+"/Submit";

		
		String payload = JSON_STR.replace("{Image_Name}", filename)
			.replace("{Data}", "")
			.replace("{Description}", "")
			.replace("{Image_Path}", vault+filename)
			.replace("{Xml_Path}", vault+xmlFilename);
		
		log.info("Call Vantage workflow: " + url + "\n"+payload);
		/*
		String payload = JSON_STR.replace("{Attachments}", attachments)
				.replace("{JobName}", filename)
				.replace("{Labels}", labels)
				.replace("{Identifier}", identifier)
				.replace("{Data}", data)
				.replace("{Description}", description)
				.replace("{Files}", files)
				.replace("{Name}", filename);
		*/
		
		HttpPost method = new HttpPost(url);

		StringEntity params = new StringEntity(payload, "UTF-8");
		method.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		method.setEntity(params);
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpResponse response = httpClient.execute(method);
		log.info(response);
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new HttpException("Error "+response.getStatusLine().getReasonPhrase());
		}
		

	}
	
	private static String replace(String source, String key, String value) {
		return source.replaceFirst(key, value);
	}
	private String getParam(Asset inAsset, String id) {
		String tmp = inAsset.get(id);
		return tmp == null ? "N/A": tmp;
	}
	
	
	
}


