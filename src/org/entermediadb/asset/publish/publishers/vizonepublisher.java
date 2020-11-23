package org.entermediadb.asset.publish.publishers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.tree.DefaultElement;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.data.PropertyDetail;
import org.openedit.page.Page;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.XmlUtil;

public class vizonepublisher extends BasePublisher implements Publisher
{

	private static final Log log = LogFactory.getLog(vizonepublisher.class);
	protected XmlUtil fieldXmlUtil;
	private static final String CACHE = "VIZ_Cookies";
	private static final String COOKIES = "Cookies";
	
	
	//vpm.restrictions should contain the values of UserRestrictions and UserRestInstruction seperated by ','
	//
	private static Map<String, String> UserRestInstruction = new HashMap<String, String>(); //rightsusageinstructions 
	private static Map<String, String> UserRestrictions = new HashMap<String, String>();   //restrictions
	private static Map<String, String> RightCodes = new HashMap<String, String>();  
	static {
		UserRestInstruction.put("1", "Nouvelles seulement");
		UserRestInstruction.put("2", "Utilisation unique");
		UserRestInstruction.put("3", "Émission spécifique");
		UserRestInstruction.put("4", "Utilisation équitable seulement");
		UserRestInstruction.put("5", "Utilisation éditoriale seulement");
		UserRestInstruction.put("6", "Respecter le contexte d'origine");
		
		UserRestrictions.put("1", "Aucune");
		UserRestrictions.put("2", "Oui");
		
		RightCodes.put("1", "G");
		RightCodes.put("2", "Y");
	}

	public PublishResult publish(MediaArchive inMediaArchive, Asset inAsset, Data inPublishRequest, Data inDestination, Data inPreset)
	{
		

		try
		{
			PublishResult result = checkOnConversion(inMediaArchive, inPublishRequest, inAsset, inPreset);
			if (result != null)
			{
				return result;
			}

			result = new PublishResult();

			Page inputpage = findInputPage(inMediaArchive, inAsset, inPreset);
			String servername = inDestination.get("server");
			String username = inDestination.get("username");
			String url = inDestination.get("url");
			
			log.info("Publishing ${asset} to ftp server ${servername}, with username ${username}.");

			String password = inDestination.get("password");
			//get password and login
			if (password == null)
			{
				UserManager userManager = inMediaArchive.getUserManager();
				User user = userManager.getUser(username);
				password = userManager.decryptPassword(user);
			}
			String enc = username + ":" + password;
			byte[] encodedBytes = Base64.encodeBase64(enc.getBytes());
			String authString = new String(encodedBytes);

			String vizid = inAsset.get("vizid");

			if (true) //vizid == null (Marie-Eve asked to re-push the asset as if it was a new image 
			{
				Element results = createAsset(inMediaArchive, inDestination, inAsset, authString);
				String id = results.element("id").getText();
				String[] splits = id.split(":");

				inAsset.setProperty("vizid", splits[4]);
				inAsset.setValue("fromfiz", false);
				String identifier = results.element("identifier").getText();
				inAsset.setValue("vizidentifier", identifier);

				inMediaArchive.saveAsset(inAsset, null);
			}

			uploadAsset(inMediaArchive, result, inAsset, inDestination, inPreset, authString);
			Thread.sleep(5000);
			//http://vizmtlvamf.media.in.cbcsrc.ca/api/asset/item/2101604250011569821/metadata
			setMetadata(inMediaArchive, inDestination.get("url"), inAsset, authString);
			setAcl(inMediaArchive, inDestination.get("url"), inAsset, authString);
			result.setComplete(true);
			log.info("publishished  ${asset} to FTP server ${servername}");
			return result;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new OpenEditException(e);
		}

	}

	private void storeCookies(MediaArchive inArchive, HttpResponse response) {
		//log.info("*** storeCookies");
		Header[] headers = response.getHeaders("Set-Cookie");
		if (headers != null && headers[0].getValue() != null) {
			inArchive.getCacheManager().put(CACHE, COOKIES, headers[0].getValue());
			//log.info("*** storeCookies: "+headers[0].getValue());
		}
		
	}
	
	private void setCookies(MediaArchive inArchive, HttpMessage method) {
		//log.info("*** setCookies");
		Object header = inArchive.getCacheManager().get(CACHE, COOKIES);
		if (header != null) {
			method.setHeader("Cookie", header.toString());
			//log.info("*** setCookies: "+header.toString());
		}
	}
	
	
	public void updateAsset(MediaArchive inArchive, String servername, Asset inAsset, String inAuthString) throws Exception
	{

		//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"

		String addr = servername + "api/asset/item/" + inAsset.get("vizid") + "/metadata";

		//Change URL - 
		//String data = "<payload xmlns='http://www.vizrt.com/types' model=\"http://vizmtlvamf.media.in.cbcsrc.ca/api/metadata/form/vpm-item/r1\"><field name='asset.title'><value>${title}</value></field> <field name='asset.owner'><value>Img</value></field>  <field name='asset.retentionPolicy'>    <value>${policy}</value>  </field>     </payload>";

		HttpGet get = new HttpGet(addr);

		get.setHeader("Content-Type", "application/vnd.vizrt.payload+xml;charset=utf-8");
		get.setHeader("Authorization", "Basic " + inAuthString);
		get.setHeader("Expect", "");
		get.setHeader("Accept", "*/*");
		setCookies(inArchive, get);
		
		HttpResponse response = getClient().execute(get);
		
		StatusLine sl = response.getStatusLine();
		int status = sl.getStatusCode();
		if (status >= 400)
		{
			throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
		}

		Element elem = getXmlUtil().getXml(response.getEntity().getContent(), "UTF-8");

		for (Iterator iterator = elem.elementIterator("field"); iterator.hasNext();)
		{
			Element field = (Element) iterator.next();
			String name = field.attributeValue("name");
			Element value = field.element("value");
			if (value != null)
			{
				String current = value.getText();

				PropertyDetail detail = inArchive.getAssetSearcher().getPropertyDetails().getDetailByProperty("vizonefield", name);
				if (detail != null)
				{
					inAsset.setValue(detail.getId(), current);
				}
			}
		}

		//	Accept: application/atom+xml;type=feed" "https://vmeserver/thirdparty/asset/item?start=1&num=20&sort=-search.modificationDate&q=breakthrough
	}

	public Element setMetadata(MediaArchive inArchive, String servername, Asset inAsset, String inAuthString) throws Exception
	{

		//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"

		String addr = servername + "api/asset/item/" + inAsset.get("vizid") + "/metadata";
		log.info("Updating metadata at " + addr);
		String vizoneretention = inAsset.get("vizoneretention");
		if(vizoneretention == null){
			vizoneretention = "oneweek";
			inAsset.setValue("vizoneretention", vizoneretention);
			inArchive.saveAsset(inAsset);
		}
		
		
		//Change URL - 
		//String data = "<payload xmlns='http://www.vizrt.com/types' model=\"http://vizmtlvamf.media.in.cbcsrc.ca/api/metadata/form/vpm-item/r1\"><field name='asset.title'><value>${title}</value></field> <field name='asset.owner'><value>Img</value></field>  <field name='asset.retentionPolicy'>    <value>${policy}</value>  </field>     </payload>";

		HttpGet get = new HttpGet(addr);

		get.setHeader("Content-Type", "application/vnd.vizrt.payload+xml;charset=utf-8");
		get.setHeader("Authorization", "Basic " + inAuthString);
		get.setHeader("Expect", "");
		get.setHeader("Accept-Charset", "UTF-8");
		get.setHeader("Accept", "*/*");
		setCookies(inArchive, get);

		HttpResponse response = getClient().execute(get);
		StatusLine sl = response.getStatusLine();
		int status = sl.getStatusCode();
		if (status >= 400)
		{
			throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
		}
		//Element elem = getXmlUtil().getXml(response.getEntity().getContent(), "UTF-8");
		//ArrayList done = new ArrayList();
        
		Element elem = getXmlUtil().getXml(response.getEntity().getContent(), "UTF-8");
		ArrayList done = new ArrayList();
		/**
		 * or each element in the response, check for vizonefield locally and if it's found then force its value instead
		 * of the received one.
		 */
		for (Iterator iterator = elem.elementIterator("field"); iterator.hasNext();)
		{
			Element field = (Element) iterator.next();
			String name = field.attributeValue("name");
			Element value = field.element("value");

			if (value != null)
			{
				String current = value.getText();

				PropertyDetail detail = inArchive.getAssetSearcher().getPropertyDetails().getDetailByProperty("vizonefield", name);
				if (detail != null)
				{
					String assetvalue = inAsset.get(detail.getId());
					if(assetvalue != null){
						log.info("***Already DONE assetvalue: "+assetvalue +" for "+name);
					    value.setText(assetvalue);
					    done.add(detail.getId());
					}
				}
			}
		}
		
		for (Iterator iterator = inArchive.getAssetSearcher().getPropertyDetails().iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			if(done.contains(detail.getId())){
				continue;
			}
			String vizfield = detail.get("vizonefield");
			if(vizfield != null){		
				String assetvalue = inAsset.get(detail.getId());
				if(assetvalue != null){
					Element field = elem.addElement("field");
					field.addAttribute("name", vizfield);
					Element value = field.addElement("value");
					value.setText(assetvalue);		
					//log.info("*** value.setText: "+assetvalue +" for "+vizfield);
					
				}
			}
		}
		//the date saved in emshare should be formatted for viz
		setAssetDate(inArchive, inAsset, elem);
		setField(inAsset, elem, "asset.retentionPolicy", vizoneretention);
		setField(inAsset, elem, "asset.owner", "Img");
		
		String restrictionVal = getRestrictionValue(inAsset);
		if (restrictionVal != null) {
			setField(inAsset, elem, "vpm.restrictions", restrictionVal);
		}
		
        String rightsCode = (String)RightCodes.get(inAsset.get("restrictions"));
		if (rightsCode != null) {
			setField(inAsset, elem, "asset.rightsCode", rightsCode);
		}

		String rightsComment = (String)getRightsComment(inAsset);
		if (rightsComment != null) {
			setField(inAsset, elem, "asset.rightsComment", rightsComment);
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 7);  // number of days to add
        String dt = sdf.format(c.getTime());
        setField(inAsset, elem, "asset.retentionDate", dt);
        
		HttpPut method = new HttpPut(addr);
		method.setHeader("Content-Type", "application/vnd.vizrt.payload+xml;charset=utf-8");
		method.setHeader("Authorization", "Basic " + inAuthString);
		method.setHeader("Expect", "");
		method.setHeader("Accept-Charset", "UTF-8");
		method.setHeader("Accept", "*/*");
		setCookies(inArchive, method);
		
		
		
		StringEntity params = new StringEntity(elem.asXML(), "UTF-8");
		
		log.info("*** value.setAll "+elem.asXML());
		
		method.setEntity(params);

		HttpResponse response2 = getClient().execute(method);
		sl = response2.getStatusLine();
		status = sl.getStatusCode();
		if (status >= 400)
		{
			throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
		}
		Element response3 = getXmlUtil().getXml(response2.getEntity().getContent(), "UTF-8");
		//String xml = response.asXML();
		return response3;

		//	Accept: application/atom+xml;type=feed" "https://vmeserver/thirdparty/asset/item?start=1&num=20&sort=-search.modificationDate&q=breakthrough
	}

	private void setField(Asset inAsset, Element elem, String key, String val) {
		//check if key exists
		boolean isKeyExists = false;
		for (Iterator iterator = elem.elementIterator(); iterator.hasNext();) {
			Element _elem = (Element) iterator.next();
			Attribute att_v = _elem.attribute("name");
			if (att_v != null && att_v.getStringValue().equals(key)) {
				Element _elem_value = _elem.element("value");
				if (_elem_value != null) {
					_elem_value.setText(val);
				} else {
					//asset.owner present but empty
					Element value = _elem.addElement("value");
					value.setText(val);
				}
				//log.info("*** value.setText: "+val +" for "+key);
				isKeyExists = true;
			} 
		}
		if (!isKeyExists) {
			Element field = elem.addElement("field");
			field.addAttribute("name", key);
			Element value = field.addElement("value");
			value.setText(val);
			//log.info("*** value.setText: "+val +" for "+key);
		}
	}
	
	private void setAssetDate(MediaArchive inArchive, Asset inAsset, Element elem) throws Exception {
		for (Iterator iterator = inArchive.getAssetSearcher().getPropertyDetails().iterator(); iterator.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) iterator.next();
			
			String vizfield = detail.get("vizonefield");
			if(vizfield != null && vizfield.equals("news.eventDate")){		
				String assetvalue = inAsset.get(detail.getId());
				if(assetvalue != null){
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	                Date date = sdf.parse(assetvalue);
	                assetvalue = sdf.format(date);
	                setField(inAsset, elem, "news.eventDate", assetvalue);
	                //log.info("***setAssetDate value.setText: "+assetvalue +" for "+vizfield);
	                return;
				}
			}
		}
	}
	
	
	private String getRestrictionValue(Asset inAsset) {
		String rest_val = UserRestrictions.get(inAsset.get("restrictions"));
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
			}
			
		}
		String term_val = (String)inAsset.get("rightsusageterms");
		//log.info("*** getRestrictionValue: "+rest_val +" , "+right_val +" , "+term_val);
		
		String ret = "";
		if (rest_val != null) 
			ret = rest_val + "-";
		if (right_val.length() > 0) 
			ret = ret + right_val + "-";
		if (term_val != null) 
			ret = ret + term_val + "-";
		
		if (ret.length() > 0) {
			ret = ret.substring(0, ret.length()-1);
			return ret;
		}
		/*
		if (rest_val != null && right_val != null) {
			return rest_val + " - " + right_val;
		} else if (rest_val != null) {
			return rest_val;
		} else if (right_val != null) {
			return right_val;
		}*/
		return null;
	}
	
	private String getRightsComment(Asset inAsset) {
		String cr_val = (String)inAsset.get("copyrightnotice");
		String source_val = (String)inAsset.get("source");
		log.info("*** getRightsComment cr_val: "+cr_val +" , source_val: "+source_val);
		
		String ret = "";
		if (cr_val != null) 
			ret = cr_val + " - ";
		if (source_val != null) 
			ret = ret + source_val + " - ";
		
		
		if (ret.length() > 3) {
			ret = ret.substring(0, ret.length()-3);
			return ret;
		}
		
		return null;
	}

	private boolean updateAcl(Element elemRoot, QName qname) {
		Element elemAcl = elemRoot.element(qname);
		boolean ret = false;
		if (elemAcl != null) {
			
			ret = updateAclElement(elemAcl, "External applications", new String[] {"1","1","0"});
			ret = updateAclElement(elemAcl, "Newsroom Plug-In", new String[] {"1","1","0"}) || ret;
			ret = updateAclElement(elemAcl, "Administrators", new String[] {"1","1","0"}) || ret;
			//ret = updateAclElement(elemAcl, "vizrtadmins", new String[] {"1","1","0"}) || ret;
			
			return ret;
			
		}
		return false;
	}
	
	private boolean  updateAclElement(Element elemAcl, String elementName, String[] rra) {
		boolean externalAppElementFound = false;
		//acl:media element found. search for "External applications" element
		for (Iterator iterator = elemAcl.elementIterator(); iterator.hasNext();) {
			Element _elem = (Element) iterator.next();
			//System.out.println(_elem);
			Attribute att_v = _elem.attribute("name");
			//System.out.println(att_v);
			if (att_v != null && att_v.getStringValue().equals(elementName)) {
				externalAppElementFound = true;
				
			} 
		}
		if (!externalAppElementFound) {
			Element nElement = new DefaultElement("acl:group");
			nElement.addAttribute("name", elementName);
			nElement.addAttribute("read", rra[0]);
			nElement.addAttribute("write", rra[1]);
			nElement.addAttribute("admin", rra[2]);
			//log.info("***updateAclElement: read: "+rra[0] +" write: "+rra[1] +" admin: " +rra[2] +" for "+elementName);
			elemAcl.add(nElement);
			return true;
		}
		return false;
	}
	
	public Element setAcl(MediaArchive inArchive, String servername, Asset inAsset, String inAuthString) throws Exception
	{

		//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"

		String addr = servername + "api/asset/item/" + inAsset.get("vizid");
		log.info("Updating Acl at " + addr);
		
		HttpGet get = new HttpGet(addr);

		get.setHeader("Content-Type", "application/atom+xml;type=entry;charset=utf-8");
		get.setHeader("Authorization", "Basic " + inAuthString);
		get.setHeader("Expect", "");
		get.setHeader("Accept", "*/*");
		setCookies(inArchive, get);
		
		HttpResponse response = getClient().execute(get);
		StatusLine sl = response.getStatusLine();
		int status = sl.getStatusCode();
		if (status >= 400)
		{
			throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
		}

		Element elemRoot = getXmlUtil().getXml(response.getEntity().getContent(), "UTF-8");
		
		QName qname = new QName("asset", new Namespace("acl", "http://www.vizrt.com/2012/acl"), "acl:asset");
		boolean doPut = updateAcl(elemRoot, qname);
		qname = new QName("media", new Namespace("acl", "http://www.vizrt.com/2012/acl"), "acl:media");
		doPut = updateAcl(elemRoot, qname) || doPut;
		
		if (doPut) {
			HttpPut method = new HttpPut(addr);
			method.setHeader("Content-Type", "application/atom+xml;type=entry;charset=utf-8");
			method.setHeader("Authorization", "Basic " + inAuthString);
			method.setHeader("Expect", "");
			method.setHeader("Accept-Charset", "UTF-8");
			method.setHeader("Accept", "*/*");
			setCookies(inArchive, method);
			
			StringEntity params = new StringEntity(elemRoot.asXML(), "UTF-8");
			method.setEntity(params);

			HttpResponse response2 = getClient().execute(method);
			sl = response2.getStatusLine();
			status = sl.getStatusCode();
			if (status >= 400)
			{
				throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
			}
			Element response3 = getXmlUtil().getXml(response2.getEntity().getContent(), "UTF-8");
			//String xml = response.asXML();
			return response3;
		}
		return null;
	}
	
	public Element createAsset(MediaArchive inArchive, Data inDestination, Asset inAsset, String inAuthString) throws Exception
	{

		//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"

		String servername = inDestination.get("url");
		String addr = servername + "thirdparty/asset/item";
		//				urn:vme:default:dictionary:retentionpolicy:oneweek

		String data = "<atom:entry xmlns:atom='http://www.w3.org/2005/Atom'>  <atom:title>Title: ${title}</atom:title>" + "<identifier>${name}</identifier>"

				+ "</atom:entry>";
		Map metadata = inAsset.getProperties();
		Calendar now = new GregorianCalendar();
		now.add(now.DAY_OF_YEAR, 7);
		Date oneweek = now.getTime();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		String date = format.format(oneweek);

		metadata.put("${date}", date);

		data = inArchive.getReplacer().replace(data, metadata);

		HttpPost method = new HttpPost(addr);

		//method.setEntity(new ByteArrayEntity(data.bytes));
		StringEntity params = new StringEntity(data, "UTF-8");
		//				method.setEntity(params);

		//HttpEntity entity = new ByteArrayEntity(data.getBytes("UTF-8"));
		//StringEntity xmlEntity = new StringEntity(data, "application/atom+xml;type=entry","UTF-8");

		method.setEntity(params);
		method.setHeader("Authorization", "Basic " + inAuthString);
		method.setHeader("Expect", "");
		method.setHeader("Content-Type", "application/atom+xml;type=entry;charset=utf-8");
		method.setHeader("Accept", "*/*");
		
		setCookies(inArchive, method);
		
		HttpResponse response2 = getClient().execute(method);
		storeCookies(inArchive, response2);

		
		StatusLine sl = response2.getStatusLine();
		int status = sl.getStatusCode();
		if (status >= 400)
		{
			throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
		}

		Element response = getXmlUtil().getXml(response2.getEntity().getContent(), "UTF-8");
		String xml = response.asXML();
		return response;

		//	Accept: application/atom+xml;type=feed" "https://vmeserver/thirdparty/asset/item?start=1&num=20&sort=-search.modificationDate&q=breakthrough
	}

	public void uploadAsset(MediaArchive archive, PublishResult inResult, Asset inAsset, Data inDestination, Data inPreset, String inAuthString)
	{
		try
		{
			Page inputpage = findInputPage(archive, inAsset, inPreset);

			String servername = inDestination.get("url");
			String addr = servername + "api/asset/item/" + inAsset.get("vizid") + "/upload";

			HttpPut method = new HttpPut(addr);
			method.setHeader("Authorization", "Basic " + inAuthString);
			method.setHeader("Expect", "");
			method.setHeader("Accept", "*/*");
			setCookies(archive, method);
			
			HttpResponse response2 = getClient().execute(method);

			String addr2 = response2.getFirstHeader("Location").getValue();

			HttpPut upload = new HttpPut(addr2);
			upload.setHeader("Authorization", "Basic " + inAuthString);
			upload.setHeader("Expect", "");
			upload.setHeader("Accept", "*/*");
			setCookies(archive, upload);
			
			FileEntity entity = new FileEntity(new File(inputpage.getContentItem().getAbsolutePath()));
			upload.setEntity(entity);

			HttpResponse response3 = getClient().execute(upload);

			StatusLine sl = response3.getStatusLine();
			int status = sl.getStatusCode();
			if (status >= 400)
			{
				throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
			}

		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}

	}

	
	
	
	
	public void deleteAsset(MediaArchive inArchive, String servername, Asset inAsset, String inAuthString) throws Exception
	{

		//	curl --insecure --user "$VMEUSER:$VMEPASS" --include --header "Accept: application/opensearchdescription+xml" "https://vmeserver/thirdparty/asset/item?format=opensearch"

		String addr = servername + "api/asset/item/" + inAsset.get("vizid");
		
		
		
		//Change URL - 
		//String data = "<payload xmlns='http://www.vizrt.com/types' model=\"http://vizmtlvamf.media.in.cbcsrc.ca/api/metadata/form/vpm-item/r1\"><field name='asset.title'><value>${title}</value></field> <field name='asset.owner'><value>Img</value></field>  <field name='asset.retentionPolicy'>    <value>${policy}</value>  </field>     </payload>";

		HttpDelete get = new HttpDelete(addr);

		get.setHeader("Content-Type", "application/atom+xml;type=entry;charset=utf-8");
		get.setHeader("Authorization", "Basic " + inAuthString);
		get.setHeader("Expect", "");
		get.setHeader("Accept", "*/*");
		setCookies(inArchive, get);
		
		HttpResponse response = getClient().execute(get);
		StatusLine sl = response.getStatusLine();
		int status = sl.getStatusCode();
		if (status >= 400)
		{
			throw new OpenEditException("error from server " + status + "  " + sl.getReasonPhrase());
		}

		
	}
	
	
	
	
	
	public HttpClient getClient()
	{

		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();

		return httpClient;
	}

	public XmlUtil getXmlUtil()
	{
		if (fieldXmlUtil == null)
		{
			fieldXmlUtil = new XmlUtil();
		}
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}

}

