package org.openedit.entermedia.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.entermedia.upload.FileUpload;
import org.entermedia.upload.FileUploadItem;
import org.entermedia.upload.UploadRequest;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.xml.ElementData;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.page.Page;
import com.openedit.users.UserManager;

public class SyncModule extends BaseMediaModule {

	protected SearcherManager fieldSearcherManager;
	protected UserManager fieldUserManager;

	public UserManager getUserManager() {
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager) {
		fieldUserManager = inUserManager;
	}

	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}

	protected HttpClient fieldClient;
	private SAXReader reader = new SAXReader();

	public void acceptPush(WebPageRequest inReq) {
		FileUpload command = new FileUpload();
		command.setPageManager(getPageManager());
		UploadRequest properties = command.parseArguments(inReq);

		String sourcepath = inReq.getRequestParameter("sourcepath");
		String original = inReq.getRequestParameter("original");
		MediaArchive archive = getMediaArchive(inReq);
		Asset target = archive.getAssetBySourcePath(sourcepath);
		if (target == null) {
			target = archive.createAsset(sourcepath);
		}
		String[] fields = inReq.getRequestParameters("field");
		archive.getAssetSearcher().updateData(inReq, fields, target);
		archive.saveAsset(target, inReq.getUser());
		List<FileUploadItem> uploadFiles = properties.getUploadItems();
		if (uploadFiles != null) {
			Iterator<FileUploadItem> iter = uploadFiles.iterator();
			while (iter.hasNext()) {
				FileUploadItem fileItem = iter.next();
				String generatedroot = "/WEB-INF/data/"
						+ archive.getCatalogId() + "/generated/" + sourcepath
						+ "/";
				String originalsroot = "/WEB-INF/data/"
						+ archive.getCatalogId() + "/originals/" + sourcepath
						+ "/";

				String filename = fileItem.getName();
				if (filename.equals(original)) {
					properties.saveFileAs(fileItem, originalsroot + "/"
							+ filename, inReq.getUser());

				} else {
					properties.saveFileAs(fileItem, generatedroot + "/"
							+ filename, inReq.getUser());

				}

			}
		}

	}

	public void processPushQueue(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);

		Searcher pushsearcher = archive.getSearcherManager().getSearcher(
				archive.getCatalogId(), "pushrequest");
		Searcher hot = archive.getSearcherManager().getSearcher(
				archive.getCatalogId(), "hotfolder");
		HitTracker hits = pushsearcher.fieldSearch("status", "pending");

		for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
			Data hit = (Data) iterator.next();
			Data real = (Data) pushsearcher.searchById(hit.getId());

			Asset target = archive.getAssetBySourcePath(hit.getSourcePath());
			ElementData hotfolder = (ElementData) hot.searchById(hit
					.get("hotfolder"));
			boolean oktosend = isOkToSend(hotfolder);
			if (oktosend) {
				List presets = (List) hotfolder.getValues("convertpreset");
				ArrayList filestosend = new ArrayList();
				for (Iterator iterator2 = presets.iterator(); iterator2
						.hasNext();) {
					String presetid = (String) iterator2.next();
					Page tosend = findInputPage(archive, target, presetid);
					if (tosend.exists()) {
						File file = new File(tosend.getContentItem()
								.getAbsolutePath());
						filestosend.add(file);
					}
				}
				try {
					upload(target, archive, filestosend);
					real.setProperty("status", "complete");
				} catch (Exception e) {
					real.setProperty("status", "error");

					real.setProperty("errordetails", e.toString());
				}

				
				pushsearcher.saveData(hit, inReq.getUser());

			}
		}
	}

	private boolean isOkToSend(Data inHotfolder) {
		boolean active = Boolean.parseBoolean(inHotfolder.get("auto"));
		// TODO: check dates, times.
		return active;
	}

	public Map<String, String> upload(Asset inAsset, MediaArchive inArchive,
			List inFiles) {
		String server = getSearcherManager().getData(inArchive.getCatalogId(),
				"catalogsettings", "push_server_url").get("value");
		String account = getSearcherManager().getData(inArchive.getCatalogId(),
				"catalogsettings", "push_server_username").get("value");
		String password = getUserManager().decryptPassword(
				getUserManager().getUser(account));

		String url = server + "/media/services/rest/"
				+ "handlesync.xml?catalogid=" + inArchive.getCatalogId();
		PostMethod method = new PostMethod(url);

		try {
			List<Part> parts = new ArrayList();
			int count = 0;
			for (Iterator iterator = inFiles.iterator(); iterator.hasNext();) {
				File file = (File) iterator.next();
				FilePart part = new FilePart("file." + count, file.getName(),
						file);
				parts.add(part);
				count++;
			}
			parts.add(new StringPart("sourcepath", inAsset.getSourcePath()));
			parts.add(new StringPart("original", inAsset.getName()));

			Part[] arrayOfparts = parts.toArray(new Part[] {});

			method.setRequestEntity(new MultipartRequestEntity(arrayOfparts,
					method.getParams()));

			Element root = execute(inArchive.getCatalogId(), method);
			Map<String, String> result = new HashMap<String, String>();
			for (Object o : root.elements("asset")) {
				Element asset = (Element) o;
				result.put(asset.attributeValue("id"),
						asset.attributeValue("sourcepath"));
			}
			return result;
		} catch (Exception e) {
			throw new OpenEditException(e);
		}
	}

	protected Page findInputPage(MediaArchive mediaArchive, Asset asset,
			Data inPreset) {

		if (inPreset.get("type") == "original") {
			return mediaArchive.getOriginalDocument(asset);

		}
		String input = "/WEB-INF/data/${mediaArchive.catalogId}/generated/${asset.sourcepath}/${inPreset.outputfile}";
		Page inputpage = mediaArchive.getPageManager().getPage(input);
		return inputpage;

	}

	protected Page findInputPage(MediaArchive mediaArchive, Asset asset,
			String presetid) {
		if (presetid == null || presetid.equals("original")) {
			return mediaArchive.getOriginalDocument(asset);
		}
		Data preset = mediaArchive.getSearcherManager().getData(
				mediaArchive.getCatalogId(), "convertpreset", presetid);
		return findInputPage(mediaArchive, asset, (Data) preset);
	}

	protected Element execute(String inCatalogId, HttpMethod inMethod) {
		try {
			int status = getClient(inCatalogId).executeMethod(inMethod);
			if (status != 200) {
				throw new Exception("Request failed: status code " + status);
			}
			Element result = reader.read(inMethod.getResponseBodyAsStream())
					.getRootElement();
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * The web services API require a client to log first. The login is the same
	 * as one used within the EnterMedia usermanager There are two Cookies that
	 * need to be passed in on subsequent requests 1. JSESSIONID - This is used
	 * by resin or similar Java container. Enables short term sessions on the
	 * server 2. entermedia.key - This allows the user to be auto-logged in.
	 * Useful for long term connections. If the web server is restarted then
	 * clients don't need to log in again
	 */
	public HttpClient getClient(String inCatalogId) {
		if (fieldClient == null) {
			fieldClient = new HttpClient();
			String server = getSearcherManager().getData(inCatalogId,
					"catalogsettings", "push_server_url").get("value");
			String account = getSearcherManager().getData(inCatalogId,
					"catalogsettings", "push_server_username").get("value");
			String password = getUserManager().decryptPassword(
					getUserManager().getUser(account));
			PostMethod method = new PostMethod(server
					+ "/media/services/rest/login.xml");

			method.addParameter("accountname", account);
			method.addParameter("password", password);
			execute(inCatalogId, method);
		}
		return fieldClient;
	}

}
