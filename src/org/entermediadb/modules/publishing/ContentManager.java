package org.entermediadb.modules.publishing;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.dom4j.Element;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.importer.DitaImporter;
import org.entermediadb.asset.util.JsonUtil;
import org.entermediadb.llm.GptManager;
import org.entermediadb.net.HttpSharedConnection;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.data.ViewFieldList;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.util.Exec;
import org.openedit.util.PathUtilities;
import org.openedit.util.XmlUtil;

import com.google.gson.JsonObject;

public class ContentManager implements CatalogEnabled {

	private static final String INPUTDIR = "/DITA/"; // /Inputs/";
	private static final String RENDERED = "/DITA/"; // Rendered/";
	private static final Log log = LogFactory.getLog(ContentManager.class);
	protected XmlUtil fieldXmlUtil;
	protected ModuleManager fieldModuleManager;

	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager) {
		fieldModuleManager = inModuleManager;
	}

	public XmlUtil getXmlUtil() {
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil) {
		fieldXmlUtil = inXmlUtil;
	}

	protected Exec fieldExec;

	public Exec getExec() {
		return fieldExec;
	}

	public void setExec(Exec inExec) {
		fieldExec = inExec;
	}

	protected HttpSharedConnection fieldHttpSharedConnection;
	protected MediaArchive fieldMediaArchive;
	protected String fieldCatalogId;
	protected String fieldsavedapikey = "null";

	public String getCatalogId() {
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId) {
		fieldCatalogId = inCatalogId;
	}

	public MediaArchive getMediaArchive() {

		if (fieldMediaArchive == null) {
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}

		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive) {
		fieldMediaArchive = inMediaArchive;
	}

	public HttpSharedConnection getSharedConnection() {
		String api = getMediaArchive().getCatalogSettingValue("apikeyoneliveweb");

		if (fieldHttpSharedConnection == null || !fieldsavedapikey.equals(api)) {
			HttpSharedConnection connection = new HttpSharedConnection();
			connection.addSharedHeader("X-tokentype", "entermedia");
			connection.addSharedHeader("X-token", api);
			fieldHttpSharedConnection = connection;
		}

		return fieldHttpSharedConnection;
	}

	public void createDitaEntityFromAI(String inModuleid, String inEntityid, String inTargetentity) {

		Data entity = getMediaArchive().getData(inModuleid, inEntityid);

		// https://oneliveweb.com/oneliveweb/ditachat/llm/api/ditapayload.json?inputdata=Fish%20Recipie&entermedia.key=adminmd5420b06b0ea0d5066b0bb413837460f409108a0be38tstampeMxOa62cNXmuVomBh0oFNw==

		Map inputdata = new HashMap();

		Data targetmodule = getMediaArchive().getCachedData("module", inTargetentity);
		String extra = entity.get("lastprompt");

		if (extra == null) {
			extra = "Create a new " + targetmodule.getName();
		}
		inputdata.put("directions", extra);

		// Loop over all the tabs on the UI

		Collection existingdata = new ArrayList();
		Map entitymetadata = new HashMap();

		Collection views = getMediaArchive().query("view").exact("moduleid", inModuleid).exact("systemdefined", false)
				.search();
		for (Iterator iterator = views.iterator(); iterator.hasNext();) {
			Data viewdata = (Data) iterator.next();
			// See if its data or lookup
			String render = viewdata.get("rendertype");
			if (render == null) {
				JsonUtil util = new JsonUtil();
				// get fields
				ViewFieldList viewfields = (ViewFieldList) getMediaArchive().getSearcher(inModuleid).getDetailsForView(viewdata, null);
				//Copy fields
				for (Iterator iterator2 = viewfields.iterator(); iterator2.hasNext();) {
					PropertyDetail detail = (PropertyDetail) iterator2.next();
					Object value = entity.getValue(detail.getId());
					if (value != null) {
						// TODO: use SeacherMaager.getValue
						if (value instanceof Date) {
							value = util.formatDateObj(value);
						}
						entitymetadata.put(detail.getId(), value);
					}
				}
			} else if (render.equals("table")) {
				// get children such as recipies

			} else if (render.equals("asset")) {
				// get a list of files as URLs
			}
		}
		inputdata.put("metadata", entitymetadata);

		// =Fish%20Recipie&
		// entermedia.key=adminmd5420b06b0ea0d5066b0bb413837460f409108a0be38tstampeMxOa62cNXmuVomBh0oFNw==

		String url = "https://oneliveweb.com/oneliveweb/ditachat";
		CloseableHttpResponse resp = null;

		JSONObject obj = new JSONObject();
		obj.put("inputdata", inputdata);
		log.info("Sending: \n" + obj.toJSONString());
		resp = getSharedConnection().sharedPostWithJson(url
				+ "/llm/api/ditapayload.json?entermedia.key=adminmd5420b06b0ea0d5066b0bb413837460f409108a0be38tstampeMxOa62cNXmuVomBh0oFNw==",
				obj);
		if (resp.getStatusLine().getStatusCode() != 200) {
			// error
			log.info("Remote Error: " + resp.getStatusLine().toString());
			getSharedConnection().release(resp);
			return;
		}
		JSONObject json = getSharedConnection().parseJson(resp);
		log.info("Received: \n" + json.toJSONString());
		// Pare DITA xml stuff
		// SECURITY BUG!!
		Data child = getMediaArchive().getSearcher(inTargetentity).createNewData();
		Map<String, Object> returned = (Map) json.get("metadata");

		for (Map.Entry<String, Object> entry : returned.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();
			child.setValue("user" + key, val);
		}
		String xml = (String) json.get("xml");

		child.setValue("entity_date", new Date());
		child.setValue(inModuleid, inEntityid); // Lookup

		Element root = getXmlUtil().getXml(xml, "UTF-8");

		String name = processDitaXml(json, root, child);

		String id = PathUtilities.makeId(name);

		// log.info("Xml: "+ root.asXML());

		Category folder = getMediaArchive().getEntityManager().createDefaultFolder(entity, null);

		String basesourcepath = folder.getCategoryPath() + "/AI/" + id + ".dita";
		String rootpath = "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/originals/";
		String path = rootpath + basesourcepath;

		ContentItem item = getMediaArchive().getContent(path);

		// Version control?
		if (item.exists()) {
			// getMediaArchive().getPageManager().getRepository().saveVersion(item); //About
			// to replace it
		}
		getXmlUtil().saveXml(root, item.getOutputStream(), "UTF-8");

		Page outdirectory = getMediaArchive().getPageManager().getPage(rootpath + folder.getCategoryPath() + "/AI/");
		Collection assetids = getMediaArchive().getAssetImporter().processOn(outdirectory.getPath(),
				outdirectory.getPath(), true, getMediaArchive(), null);

		// Save to Question Area? Or parent or both
		Asset asset = getMediaArchive().getAssetBySourcePath(basesourcepath);
		if (asset != null) {
			asset.addCategory(folder);
			getMediaArchive().saveData("asset", asset);
			child.setValue("primarymedia", asset.getId());
		}

		// send Thumbnail?
		// Needed?
		getMediaArchive().saveData(inTargetentity, child);

	}

	protected String processDitaXml(JSONObject json, Element root, Data child) {

		try {
			String title = (String) json.get("title");
			child.setName(title);

			child.setValue("ditatopic", root.asXML());
			log.info(root.asXML());

			/**
			 * <body> <section> <title>Question</title>
			 * <p>
			 * Which of the following is a common use of aspirin?
			 * </p>
			 * </section> <section> <title>Options</title>
			 * <ul>
			 * <li>A) Pain relief</li>
			 * <li>B) Reducing inflammation</li>
			 * <li>C) Preventing heart attacks</li>
			 * <li>D) All of the above</li>
			 * </ul>
			 * </section> <section> <title>Answer</title>
			 * <p>
			 * The correct answer is D) All of the above, as aspirin is commonly used for
			 * pain relief, reducing inflammation, and preventing heart attacks.
			 * </p>
			 * </section>
			 */
			/*
			 * <section> <title>Multiple Choice Question</title> <ul> <li>What is the
			 * primary benefit of taking aspirin?</li> <li>A. To relieve pain and reduce
			 * inflammation</li> <li>B. To cure bacterial infections</li> <li>C. To lower
			 * blood pressure</li> <li>D. To increase energy levels</li> </ul> </section>
			 * <section> <title>Correct Answer</title> <p>The correct answer is A. Aspirin
			 * is primarily used to relieve pain and reduce inflammation.</p> </section>
			 */
			Collection sections = (Collection) json.get("content_sections");

			for (Iterator iterator = sections.iterator(); iterator.hasNext();) {
				JSONObject section = (JSONObject) iterator.next();
				String sectitle = (String) section.get("section_title");
				if (sectitle != null) {
					if (sectitle.equals("Introduction")) {
						String text = getText(section, "content");
						child.setValue("userrationale", text);
					} else if (sectitle.equals("Multiple Choice Question")) {
						Collection<String> options = (Collection) section.get("content");
						for (Iterator iterator2 = options.iterator(); iterator2.hasNext();) {
							String val = (String) iterator2.next();
							if (child.getValue("userstem") == null) {
								child.setValue("userstem", val);
							} else {
								String letter = val.substring(0, 1).toLowerCase();
								child.setValue("useranswer_option_" + letter, val.substring(2));
							}
						}
					} else if (sectitle.contains("Question")) {
						String text = getText(section, "content");
						child.setValue("userstem", text);
					} else if (sectitle.equals("Options")) {
						Collection<String> options = (Collection) section.get("content");
						for (Iterator iterator2 = options.iterator(); iterator2.hasNext();) {
							String val = (String) iterator2.next();
							String letter = val.substring(0, 1).toLowerCase();
							child.setValue("useranswer_option_" + letter, val.substring(2));
						}
					} else if (sectitle.endsWith("Answer")) {
						String text = getText(section, "content");
						child.setValue("usercorrect_answer", text);
					}
				}
			}
			return title;
		} catch (Throwable ex) {
			log.error("Cant process ", ex);
		}
		return null;
	}

	protected String getText(JSONObject section, String inField) {
		Object obj = (Object) section.get(inField);
		String text = null;
		if (obj instanceof Collection) {
			text = (String) ((Collection) obj).iterator().next();
		} else if (obj instanceof String) {
			text = (String) obj;
		}
		return text;
	}

	public String loadVisual(String inModuleId, Data inEntity, String inFormat, Data inDitaAsset) {
		ContentItem item = getMediaArchive().getOriginalContent(inDitaAsset);
		if (!item.exists()) {
			log.info("No such assset");
			return null;
		}
		// bin/dita -i
		// ../90130_SPC_C-EFM/DITA-OUTPUT/90130_SPC_C-EFM/90130_SPC_C-EFM.ditamap -o out
		// -f html
		// output a folder of HTML and read it in
		Collection<String> args = new ArrayList();
		args.add("-i");
		args.add(item.getAbsolutePath());

		Category cat = getMediaArchive().getEntityManager().createDefaultFolder(inEntity, null);

		String root = "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/originals/";
		String outputbasefolder = cat.getCategoryPath() + RENDERED
				+ PathUtilities.extractPageName(inDitaAsset.getName());
		String finaloutputpage = findOutputSourcePath(item, outputbasefolder, inFormat);

		Page finalpage = getMediaArchive().getPageManager().getPage(root + finaloutputpage); // mkdir

		if (finalpage.exists()) // Reload?
		{
			log.error("Already done " + finalpage);
			return finaloutputpage;
		}
		// getMediaArchive().getPageManager().putPage(finalpage); //mkdir
		Page outdirectory = getMediaArchive().getPageManager().getPage(finalpage.getDirectory());

		args.add("-o");
		args.add(outdirectory.getContentItem().getAbsolutePath());

		args.add("-f");
		args.add(inFormat);

		getExec().runExec("dita", args);
		getMediaArchive().getPageManager().clearCache();
		// Now import assets like crazy?
		Collection assetids = getMediaArchive().getAssetImporter().processOn(outdirectory.getPath(),
				outdirectory.getPath(), true, getMediaArchive(), null);

		// Load all the HTML?
		return finaloutputpage;
	}

	protected String findOutputSourcePath(ContentItem item, String outputbasefolder, String inType) {
		File xml = new File(item.getAbsolutePath());
		Element input = getXmlUtil().getXml(xml, "UTF-8");
		String finalpage = null;

		if (inType.equals("pdf")) {
			finalpage = outputbasefolder + "/" + PathUtilities.extractPageName(item.getName()) + ".pdf";
		} else {
			if ("bookmap".equals(input.getName())) {
				finalpage = outputbasefolder + "/index.html";
			} else if ("chapters".equals(input.getName())) {
				finalpage = outputbasefolder + "/chapters/" + PathUtilities.extractPageName(item.getName()) + ".html";
			}
		}
		return finalpage;
	}

	public Collection findDitaAssets(Data inEntity) {
		if (inEntity == null) {
			log.error("missing enityty");
			return null;
		}
		Category cat = getMediaArchive().getEntityManager().createDefaultFolder(inEntity, null);
		Collection assets = getMediaArchive().query("asset").exact("category", cat).orgroup("fileformat", "ditamap")
				.not("editstatus", "7").search();
		return assets;
	}

	public void loadTree(String inModuleId, Data inEntity, Asset inDita) throws Exception {

		// See if we have data already. If not check on version?

		ContentItem item = getMediaArchive().getOriginalContent(inDita);
		DitaImporter oniximporter = new DitaImporter();
		oniximporter.setMediaArchive(getMediaArchive());
		oniximporter.setMakeId(false);

		Data module = getMediaArchive().getCachedData("module", inModuleId);
		oniximporter.setModule(module);
		oniximporter.setAsset(inDita);
		oniximporter.setEntity(inEntity);
		oniximporter.importData();

		// Search using jquery
	}

	public String saveImage(Page inputdirectory, String inAssetId, String size) {
		Asset asset = getMediaArchive().getAsset(inAssetId);
		String rel = saveImage(inputdirectory, asset, size);
		return rel;
	}

	public String saveImage(Page inputdirectory, Data inAsset, String size) {
		// Copy the file to a location

		String format = inAsset.get("fileformat");
		String extention = "jpg";
		if ("png".equals(format)) {
			// extention = "png";
		}
		String path = "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/generated/" + inAsset.getSourcePath()
				+ "/" + size + "." + extention;
		ContentItem inputpage = getMediaArchive().getPageManager().getRepository().getStub(path);
		// Copy it no version
		String ending = "generated/" + inAsset.getSourcePath() + "/" + inputpage.getName();

		String outputpath = inputdirectory.getPath() + ending;
		ContentItem outputcontent = getMediaArchive().getPageManager().getRepository().getStub(outputpath);
		outputcontent.setMakeVersion(false);
		getMediaArchive().getPageManager().getRepository().copy(inputpage, outputcontent);

		String relativepath = "../" + ending;
		return relativepath;

	}

	protected void renderDita(WebPageRequest inReq, String parentmodule, Data entity, String targetmodule,
			HitTracker children, MediaArchive mediaArchive) {
		Category cat = mediaArchive.getEntityManager().createDefaultFolder(entity, null);
		// Render DITAS for each question and a map
		String searchhome = inReq.findPathValue("edithome");
		//TODO: These files should be in the catalog in my opinion so they can be consistently accessed from mediadb etc
		
		Page ditatemplate = mediaArchive.getPageManager().getPage(	searchhome + "/renderdita/templatedita.dita");
		PropertyDetail detail = mediaArchive.getSearcher(targetmodule).getDetail("name");

		WebPageRequest newcontext = inReq.copy(ditatemplate);
		newcontext.putPageValue("contentmanager", this);

		Collection savedtopics = new ArrayList();
		String root = "/WEB-INF/data/" + mediaArchive.getCatalogId() + "/originals/";

		String basemapsourcepath = cat.getCategoryPath() + INPUTDIR;

		String exportname = inReq.getRequestParameter("exportname");
		if (exportname == null) {
			exportname = entity.getName() + ".ditamap";
			exportname = exportname.replace('/', '-');
		}
		String finalmapsourcepath = basemapsourcepath + PathUtilities.extractPageName(exportname) + "/" + exportname;
		Page inputdirectory = mediaArchive.getPageManager()
				.getPage(root + basemapsourcepath + PathUtilities.extractPageName(exportname) + "/");
		newcontext.putPageValue("inputdirectory", inputdirectory);
		Page mapoutputpage = mediaArchive.getPageManager().getPage(root + finalmapsourcepath);

		log.info("Creating DITAMAP" + finalmapsourcepath);
		int it = 0;

		long currenchapter = -1;
		Collection tosave = new ArrayList();
		Collection onechapter = new ArrayList();

		for (Iterator iterator = children.iterator(); iterator.hasNext();) {
			Data subentity = (Data) iterator.next();

			Long thischapter = (Long) Long.valueOf(subentity.get("userchapter_number"));

			if (currenchapter == -1) {
				currenchapter = thischapter;
			}

			if (currenchapter != thischapter || !iterator.hasNext()) {
				if (!iterator.hasNext()) {
					onechapter.add(subentity);
				}
				// proccess chapter
				newcontext.putPageValue("chapter", currenchapter);
				newcontext.putPageValue("onechapter", onechapter);

				StringWriter output = new StringWriter();
				ditatemplate.generate(newcontext, output);

				// subentity.setValue("ditatopic",output.toString());
				// tosave.add(subentity);
				// Save content
				// String.format("%03d", a);
				String ending = String.format("chapters/%03d.dita", currenchapter);
				String ditabasesourcepath = cat.getCategoryPath() + INPUTDIR + mapoutputpage.getDirectoryName() + "/"
						+ ending;
				Page outputfile = mediaArchive.getPageManager().getPage(root + ditabasesourcepath);
				mediaArchive.getPageManager().saveContent(outputfile, inReq.getUser(), output.toString(),
						"Generated DITA");
				log.info("Saved DITA: " + outputfile);

				// TODO: Put the image files inside .images

				savedtopics.add(ending);
				// clear
				currenchapter = thischapter;
				onechapter.clear();

			}
			onechapter.add(subentity);

		}

		mediaArchive.saveData("targetmodule", tosave);

		Page ditatemplatemap = mediaArchive.getPageManager().getPage( searchhome  +"/renderdita/templateditamap.ditamap");

		StringWriter output = new StringWriter();
		newcontext = inReq.copy(ditatemplatemap);
		newcontext.putPageValue("exportname", entity.getName());
		newcontext.putPageValue("savedtopics", savedtopics);
		ditatemplatemap.generate(newcontext, output);
		// Get Names
		// Save content
		mediaArchive.getPageManager().saveContent(mapoutputpage, inReq.getUser(), output.toString(),
				"Generated DITAMMAP");
		log.info("Saved DITA MAP: " + mapoutputpage);

//		Page outdirectory = mediaArchive.getPageManager().getPage(root + cat.getCategoryPath() +RENDERED + mapoutputpage.getDirectoryName() +"/");
//		mediaArchive.getPageManager().removePage(outdirectory); //Assets will still be linked?

		Collection assetids = mediaArchive.getAssetImporter().processOn(inputdirectory.getPath(),
				inputdirectory.getPath(), true, mediaArchive, null);

		// Save to Question Area? Or parent or both
		Asset asset = mediaArchive.getAssetBySourcePath(finalmapsourcepath);
		if (asset != null) {
			loadVisual(parentmodule, entity, "xhtml", asset);
			loadVisual(parentmodule, entity, "pdf", asset);
		}
	}

	public void createFromLLM(WebPageRequest inReq, String inModuleid, String inEntityid, String inTargetentity) throws Exception {

		Data entity = getMediaArchive().getData(inModuleid, inEntityid);

		// https://oneliveweb.com/oneliveweb/ditachat/llm/api/ditapayload.json?inputdata=Fish%20Recipie&entermedia.key=adminmd5420b06b0ea0d5066b0bb413837460f409108a0be38tstampeMxOa62cNXmuVomBh0oFNw==
		MediaArchive archive = getMediaArchive();
		Map inputdata = new HashMap();
		Data parentmodule = getMediaArchive().getCachedData("module", inModuleid);
		Data targetmodule = getMediaArchive().getCachedData("module", inTargetentity);
		Searcher targetsearcher = getMediaArchive().getSearcher(inTargetentity);
		
		String extra = entity.get("lastprompt");

		if (extra == null) {
			extra = "Create a new " + targetmodule.getName();
		}
		inputdata.put("directions", extra);
		// Loop over all the tabs on the UI
		inputdata.put("metadata", inputdata);
		inReq.putPageValue("parentmodule", parentmodule);
		inReq.putPageValue("targetmodule", targetmodule);
		inReq.putPageValue("parent", entity);

		
		GptManager manager = (GptManager) archive.getBean("gptManager");
		String model = inReq.findValue("model.value");
		if(model == null) {
			model = archive.getCatalogSettingValue("gpt-model");
		}
		if(model == null) {
			model = "gpt-4o";
		}
		//This is the "Message" to the LLM - it can be verbose and uses velocity, can access anything.
		inReq.putPageValue("inputdata", inputdata);
		String template = manager.loadInputFromTemplate(inReq,archive.getCatalogId() + "/gpt/templates/create_entity.html");

		JsonObject results = manager.callFunction(inReq, model, "create_entity", template, 0, 5000	);
		
		Data child = getMediaArchive().getSearcher(inTargetentity).createNewData();
		JsonObject returned = (JsonObject) results.get("metadata");
		
		
		manager.updateData(returned, child);
		

		child.setValue("entity_date", new Date());
		child.setValue(inModuleid, inEntityid); // Lookup
		//No assets being created in this one.
		archive.saveData(inTargetentity, child);
		//Category folder = getMediaArchive().getEntityManager().createDefaultFolder(entity, null);


	}

}
