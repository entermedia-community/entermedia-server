package org.entermediadb.modules.publishing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.dom4j.Element;
import org.entermediadb.ai.ChatMessageHandler;
import org.entermediadb.ai.assistant.AiCreation;
import org.entermediadb.ai.assistant.SemanticAction;
import org.entermediadb.ai.informatics.SemanticTableManager;
import org.entermediadb.ai.llm.AgentContext;
import org.entermediadb.ai.llm.LlmConnection;
import org.entermediadb.ai.llm.LlmResponse;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.importer.DitaImporter;
import org.entermediadb.asset.util.JsonUtil;
import org.entermediadb.find.EntityManager;
import org.entermediadb.net.HttpSharedConnection;
import org.entermediadb.scripts.ScriptLogger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.PropertyDetail;
import org.openedit.data.Searcher;
import org.openedit.data.ViewFieldList;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.repository.InputStreamItem;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.Exec;
import org.openedit.util.JSONParser;
import org.openedit.util.PathUtilities;
import org.openedit.util.XmlUtil;

public class ContentManager implements CatalogEnabled, ChatMessageHandler
{

	private static final String INPUTDIR = "/DITA/"; // /Inputs/";
	private static final String RENDERED = "/DITA/"; // Rendered/";
	private static final Log log = LogFactory.getLog(ContentManager.class);
	protected XmlUtil fieldXmlUtil;
	protected ModuleManager fieldModuleManager;

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}

	public XmlUtil getXmlUtil()
	{
		return fieldXmlUtil;
	}

	public void setXmlUtil(XmlUtil inXmlUtil)
	{
		fieldXmlUtil = inXmlUtil;
	}

	protected Exec fieldExec;

	public Exec getExec()
	{
		return fieldExec;
	}

	public void setExec(Exec inExec)
	{
		fieldExec = inExec;
	}

	protected HttpSharedConnection fieldHttpSharedConnection;
	protected MediaArchive fieldMediaArchive;
	protected String fieldCatalogId;
	protected String fieldsavedapikey = "null";

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public MediaArchive getMediaArchive()
	{

		if (fieldMediaArchive == null)
		{
			fieldMediaArchive = (MediaArchive) getModuleManager().getBean(getCatalogId(), "mediaArchive");
		}

		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	public HttpSharedConnection getSharedConnection()
	{
		String api = getMediaArchive().getCatalogSettingValue("apikeyoneliveweb");

		if (fieldHttpSharedConnection == null || !fieldsavedapikey.equals(api))
		{
			HttpSharedConnection connection = new HttpSharedConnection();
			connection.addSharedHeader("X-tokentype", "entermedia");
			connection.addSharedHeader("X-token", api);
			fieldHttpSharedConnection = connection;
		}

		return fieldHttpSharedConnection;
	}
	
	

	public void createDitaEntityFromAI(String inModuleid, String inEntityid, String inTargetentity)
	{

		Data entity = getMediaArchive().getData(inModuleid, inEntityid);

		// https://oneliveweb.com/oneliveweb/ditachat/llm/api/ditapayload.json?inputdata=Fish%20Recipie&entermedia.key=adminmd5420b06b0ea0d5066b0bb413837460f409108a0be38tstampeMxOa62cNXmuVomBh0oFNw==

		Map inputdata = new HashMap();

		Data targetmodule = getMediaArchive().getCachedData("module", inTargetentity);
		String extra = entity.get("lastprompt");

		if (extra == null)
		{
			extra = "Create a new " + targetmodule.getName();
		}
		inputdata.put("directions", extra);

		// Loop over all the tabs on the UI

		Collection existingdata = new ArrayList();
		Map entitymetadata = new HashMap();

		Collection views = getMediaArchive().query("view").exact("moduleid", inModuleid).exact("systemdefined", false).search();
		for (Iterator iterator = views.iterator(); iterator.hasNext();)
		{
			Data viewdata = (Data) iterator.next();
			// See if its data or lookup
			String render = viewdata.get("rendertype");
			if (render == null)
			{
				JsonUtil util = new JsonUtil();
				// get fields
				ViewFieldList viewfields = (ViewFieldList) getMediaArchive().getSearcher(inModuleid).getDetailsForView(viewdata, null);
				// Copy fields
				for (Iterator iterator2 = viewfields.iterator(); iterator2.hasNext();)
				{
					PropertyDetail detail = (PropertyDetail) iterator2.next();
					Object value = entity.getValue(detail.getId());
					if (value != null)
					{
						// TODO: use SeacherMaager.getValue
						if (value instanceof Date)
						{
							value = util.formatDateObj(value);
						}
						entitymetadata.put(detail.getId(), value);
					}
				}
			}
			else if (render.equals("table"))
			{
				// get children such as recipies

			}
			else if (render.equals("asset"))
			{
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
		resp = getSharedConnection().sharedPostWithJson(url + "/llm/api/ditapayload.json?entermedia.key=adminmd5420b06b0ea0d5066b0bb413837460f409108a0be38tstampeMxOa62cNXmuVomBh0oFNw==", obj);
		if (resp.getStatusLine().getStatusCode() != 200)
		{
			// error
			log.info("Remote Error: " + resp.getStatusLine().toString());
			getSharedConnection().release(resp);
			return;
		}
		
		/*
		JSONObject json = getSharedConnection().parseMap(resp);
		log.info("Received: \n" + json.toJSONString());
		// Pare DITA xml stuff
		// SECURITY BUG!!
		Data child = getMediaArchive().getSearcher(inTargetentity).createNewData();
		Map<String, Object> returned = (Map) json.get("metadata");

		for (Map.Entry<String, Object> entry : returned.entrySet())
		{
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
		if (item.exists())
		{
			// getMediaArchive().getPageManager().getRepository().saveVersion(item); //About
			// to replace it
		}
		getXmlUtil().saveXml(root, item.getOutputStream(), "UTF-8");

		Page outdirectory = getMediaArchive().getPageManager().getPage(rootpath + folder.getCategoryPath() + "/AI/");
		Collection assetids = getMediaArchive().getAssetImporter().processOn(outdirectory.getPath(), outdirectory.getPath(), true, getMediaArchive(), null);

		// Save to Question Area? Or parent or both
		Asset asset = getMediaArchive().getAssetBySourcePath(basesourcepath);
		if (asset != null)
		{
			asset.addCategory(folder);
			getMediaArchive().saveData("asset", asset);
			child.setValue("primarymedia", asset.getId());
		}

		// send Thumbnail?
		// Needed?
		getMediaArchive().saveData(inTargetentity, child);
		 */
	}

	protected String processDitaXml(JSONObject json, Element root, Data child)
	{

		try
		{
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
			 * The correct answer is D) All of the above, as aspirin is commonly
			 * used for pain relief, reducing inflammation, and preventing heart
			 * attacks.
			 * </p>
			 * </section>
			 */
			/*
			 * <section> <title>Multiple Choice Question</title> <ul> <li>What
			 * is the primary benefit of taking aspirin?</li> <li>A. To relieve
			 * pain and reduce inflammation</li> <li>B. To cure bacterial
			 * infections</li> <li>C. To lower blood pressure</li> <li>D. To
			 * increase energy levels</li> </ul> </section> <section>
			 * <title>Correct Answer</title> <p>The correct answer is A. Aspirin
			 * is primarily used to relieve pain and reduce inflammation.</p>
			 * </section>
			 */
			Collection sections = (Collection) json.get("content_sections");

			for (Iterator iterator = sections.iterator(); iterator.hasNext();)
			{
				JSONObject section = (JSONObject) iterator.next();
				String sectitle = (String) section.get("section_title");
				if (sectitle != null)
				{
					if (sectitle.equals("Introduction"))
					{
						String text = getText(section, "content");
						child.setValue("userrationale", text);
					}
					else if (sectitle.equals("Multiple Choice Question"))
					{
						Collection<String> options = (Collection) section.get("content");
						for (Iterator iterator2 = options.iterator(); iterator2.hasNext();)
						{
							String val = (String) iterator2.next();
							if (child.getValue("userstem") == null)
							{
								child.setValue("userstem", val);
							}
							else
							{
								String letter = val.substring(0, 1).toLowerCase();
								child.setValue("useranswer_option_" + letter, val.substring(2));
							}
						}
					}
					else if (sectitle.contains("Question"))
					{
						String text = getText(section, "content");
						child.setValue("userstem", text);
					}
					else if (sectitle.equals("Options"))
					{
						Collection<String> options = (Collection) section.get("content");
						for (Iterator iterator2 = options.iterator(); iterator2.hasNext();)
						{
							String val = (String) iterator2.next();
							String letter = val.substring(0, 1).toLowerCase();
							child.setValue("useranswer_option_" + letter, val.substring(2));
						}
					}
					else if (sectitle.endsWith("Answer"))
					{
						String text = getText(section, "content");
						child.setValue("usercorrect_answer", text);
					}
				}
			}
			return title;
		}
		catch (Throwable ex)
		{
			log.error("Cant process ", ex);
		}
		return null;
	}

	protected String getText(JSONObject section, String inField)
	{
		Object obj = (Object) section.get(inField);
		String text = null;
		if (obj instanceof Collection)
		{
			text = (String) ((Collection) obj).iterator().next();
		}
		else if (obj instanceof String)
		{
			text = (String) obj;
		}
		return text;
	}

	public String loadVisual(Data inBookEntity, String inFormat, Data inDitaAsset)
	{
		ContentItem item = getMediaArchive().getOriginalContent(inDitaAsset);
		if (!item.exists())
		{
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

		Category cat = getMediaArchive().getEntityManager().createDefaultFolder(inBookEntity, null);

		String root = "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/originals/";
		String outputbasefolder = cat.getCategoryPath() + RENDERED + PathUtilities.extractPageName(inDitaAsset.getName());
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
		Collection assetids = getMediaArchive().getAssetImporter().processOn(outdirectory.getPath(), outdirectory.getPath(), true, getMediaArchive(), null);

		// Load all the HTML?
		return finaloutputpage;
	}

	protected String findOutputSourcePath(ContentItem item, String outputbasefolder, String inType)
	{
		File xml = new File(item.getAbsolutePath());
		Element input = getXmlUtil().getXml(xml, "UTF-8");
		String finalpage = null;

		if (inType.equals("pdf"))
		{
			finalpage = outputbasefolder + "/" + PathUtilities.extractPageName(item.getName()) + ".pdf";
		}
		else
		{
			if ("bookmap".equals(input.getName()))
			{
				finalpage = outputbasefolder + "/index.html";
			}
			else if ("chapters".equals(input.getName()))
			{
				finalpage = outputbasefolder + "/chapters/" + PathUtilities.extractPageName(item.getName()) + ".html";
			}
		}
		return finalpage;
	}

	public Collection findDitaAssets(Data inEntity)
	{
		if (inEntity == null)
		{
			log.error("missing enityty");
			return null;
		}
		Category cat = getMediaArchive().getEntityManager().createDefaultFolder(inEntity, null);
		Collection assets = getMediaArchive().query("asset").exact("category", cat).orgroup("fileformat", "ditamap").not("editstatus", "7").search();
		return assets;
	}

	public void loadTree(String inModuleId, Data inEntity, Asset inDita) throws Exception
	{

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

	public String saveImage(Page inputdirectory, String inAssetId, String size)
	{
		Asset asset = getMediaArchive().getAsset(inAssetId);
		String rel = saveImage(inputdirectory, asset, size);
		return rel;
	}

	public String saveImage(Page inputdirectory, Data inAsset, String size)
	{
		// Copy the file to a location

		String format = inAsset.get("fileformat");
		String extention = "jpg";
		if ("png".equals(format))
		{
			// extention = "png";
		}
		String path = "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/generated/" + inAsset.getSourcePath() + "/" + size + "." + extention;
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

	// 1. Loop over all child chapter and search for their children data in order

	// 2. Each chapter picks a dita template. So learning or topics. Not both. The
	// template goes and load its data and loops it as needed

	// 3. The book mapping file always looks for chapters renders them as dita and
	// combines the chapters mapping files and stores

	protected void renderDita(WebPageRequest inReq, String entitymoduleid, Data entity, String chaptermoduleid)
	{
		HitTracker chapters = getMediaArchive().query(chaptermoduleid).exact(entitymoduleid, entity.getId()).sort("userchapter_number").sort("ordering").search();

		Category cat = getMediaArchive().getEntityManager().createDefaultFolder(entity, null);
		// Render DITAS for each question and a map
		String edithome = inReq.findPathValue("edithome");
		// TODO: These files should be in the catalog in my opinion so they can be
		// consistently accessed from mediadb etc
		Data module = getMediaArchive().getCachedData("module", entitymoduleid);

		String root = "/WEB-INF/data/" + getMediaArchive().getCatalogId() + "/originals/";

		String basemapsourcepath = cat.getCategoryPath() + INPUTDIR;

		String exportname = inReq.getRequestParameter("exportname");
		if (exportname == null)
		{
			exportname = entity.getName() + ".ditamap";
			exportname = exportname.replace('/', '-');
		}
		String finalmapsourcepath = basemapsourcepath + PathUtilities.extractPageName(exportname) + "/" + exportname;
		Page inputdirectory = getMediaArchive().getPageManager().getPage(root + basemapsourcepath + PathUtilities.extractPageName(exportname) + "/");
		Page mapoutputpage = getMediaArchive().getPageManager().getPage(root + finalmapsourcepath);

		log.info("Creating DITAMAP" + finalmapsourcepath);
		int it = 0;

		Set submodules = new HashSet();
		Map<String, String> submoduletotemplate = new HashMap();

		Collection<Data> children = getMediaArchive().query("ditatemplate").named("ditatemplates").all().search(inReq);
		for (Iterator iterator = children.iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			String submodid = data.get("targetsubmoduleid");
			submodules.add(submodid);
			submoduletotemplate.put(submodid, data.get("filename"));
		}

		Map<String, Collection> groupsoftopics = new HashMap();
		Collection chapterfilenames = new ArrayList();
		for (Iterator iterator = chapters.iterator(); iterator.hasNext();)
		{
			MultiValued onechapter = (MultiValued) iterator.next();

			Integer chapternumber = onechapter.getInt("userchapter_number");
			if (chapternumber == null)
			{
				chapternumber = 1;
			}

			Collection<String> savedtopics = new ArrayList();

			HitTracker allcontents = getMediaArchive().query("modulesearch").put("searchtypes", submodules).exact(chaptermoduleid, onechapter.getId()).sort("useritem_number").sort("ordering").search();
			for (Iterator iterator2 = allcontents.iterator(); iterator2.hasNext();)
			{
				Data somecontent = (Data) iterator2.next();
				String template = submoduletotemplate.get(somecontent.get("entitysourcetype"));

				// Combine lots of little renderings
				Page ditatemplatepage = getMediaArchive().getPageManager().getPage(edithome + "/templates/" + template);
				WebPageRequest newcontext = inReq.copy(ditatemplatepage);
				newcontext.putPageValue("contentmanager", this);
				newcontext.putPageValue("inputdirectory", inputdirectory);

				// proccess chapter
				newcontext.putPageValue("entity", somecontent);
				newcontext.putPageValue("chapter", onechapter);

				StringWriter output = new StringWriter();
				ditatemplatepage.generate(newcontext, output);

				String id = PathUtilities.extractId(somecontent.getName(), true);
				String ending = String.format("chapters/%03d-%s.dita", chapternumber, id);

				String ditabasesourcepath = cat.getCategoryPath() + INPUTDIR + mapoutputpage.getDirectoryName() + "/" + ending;
				Page outputfile = getMediaArchive().getPageManager().getPage(root + ditabasesourcepath);
				getMediaArchive().getPageManager().saveContent(outputfile, inReq.getUser(), output.toString(), "Generated DITA");
				log.info("Saved DITA: " + outputfile);
				savedtopics.add(ending);

			}
			// Save an intro to each chapter
			String id = PathUtilities.extractId(onechapter.getName(), true);
			String ending = String.format("chapters/%03d-%s.dita", chapternumber, id);
			groupsoftopics.put(ending, savedtopics);
			chapterfilenames.add(ending);

			String template = "chapterintro.dita";
			Page ditatemplatepage = getMediaArchive().getPageManager().getPage(edithome + "/templates/" + template);
			WebPageRequest newcontext = inReq.copy(ditatemplatepage);
			newcontext.putPageValue("contentmanager", this);
			newcontext.putPageValue("inputdirectory", inputdirectory);

			// proccess chapter
			newcontext.putPageValue("entity", onechapter);
			newcontext.putPageValue("chapter", onechapter);

			StringWriter output = new StringWriter();
			ditatemplatepage.generate(newcontext, output);

			String ditabasesourcepath = cat.getCategoryPath() + INPUTDIR + mapoutputpage.getDirectoryName() + "/" + ending;
			Page outputfile = getMediaArchive().getPageManager().getPage(root + ditabasesourcepath);
			getMediaArchive().getPageManager().saveContent(outputfile, inReq.getUser(), output.toString(), "Generated DITA");
			log.info("Saved DITA: " + outputfile);

		}

		Page ditatemplatemap = getMediaArchive().getPageManager().getPage(edithome + "/templates/templatecreatebook.ditamap");

		StringWriter output = new StringWriter();
		WebPageRequest newcontext = inReq.copy(ditatemplatemap);
		newcontext.putPageValue("entity", entity);
		newcontext.putPageValue("chapters", chapterfilenames);
		newcontext.putPageValue("groupsoftopics", groupsoftopics);

		ditatemplatemap.generate(newcontext, output);
		// Get Names
		// Save content
		getMediaArchive().getPageManager().saveContent(mapoutputpage, inReq.getUser(), output.toString(), "Generated DITAMMAP");
		log.info("Saved DMAP: " + mapoutputpage);

		// Page outdirectory = mediaArchive.getPageManager().getPage(root +
		// cat.getCategoryPath() +RENDERED + mapoutputpage.getDirectoryName() +"/");
		// mediaArchive.getPageManager().removePage(outdirectory); //Assets will still
		// be linked?

		Collection assetids = getMediaArchive().getAssetImporter().processOn(inputdirectory.getPath(), inputdirectory.getPath(), true, getMediaArchive(), null);

		// Save to Question Area? Or parent or both
		Asset asset = getMediaArchive().getAssetBySourcePath(finalmapsourcepath);
		if (asset != null)
		{
			loadVisual(entity, "xhtml", asset);
			loadVisual(entity, "pdf", asset);
		}
	}

	//    public Data createFromLLM(WebPageRequest inReq, LlmConnection inManager, String inModel, String inModuleid,
	//	    String inEntityid, String inTargetentity) throws Exception {
	//
	//	Data entity = getMediaArchive().getData(inModuleid, inEntityid);
	//
	//	// https://oneliveweb.com/oneliveweb/ditachat/llm/api/ditapayload.json?inputdata=Fish%20Recipie&entermedia.key=adminmd5420b06b0ea0d5066b0bb413837460f409108a0be38tstampeMxOa62cNXmuVomBh0oFNw==
	//	MediaArchive archive = getMediaArchive();
	//	Map inputdata = new HashMap();
	//	Data parentmodule = getMediaArchive().getCachedData("module", inModuleid);
	//	Data targetmodule = getMediaArchive().getCachedData("module", inTargetentity);
	//	Searcher targetsearcher = getMediaArchive().getSearcher(inTargetentity);
	//
	//	String extra = entity.get("lastprompt");
	//
	//	if (extra == null) {
	//	    extra = "Create a new " + targetmodule.getName();
	//	}
	//	inputdata.put("directions", extra);
	//	// Loop over all the tabs on the UI
	//	inputdata.put("metadata", inputdata);
	//	inReq.putPageValue("parentmodule", parentmodule);
	//	inReq.putPageValue("targetmodule", targetmodule);
	//	inReq.putPageValue("parent", entity);
	//
	//	// This is the "Message" to the LLM - it can be verbose and uses velocity, can
	//	// access anything.
	//	inReq.putPageValue("inputdata", inputdata);
	//	String template = inManager.loadInputFromTemplate(inReq,
	//		"/" + archive.getMediaDbId() + "/gpt/templates/create_entity.html");
	//
	//	JSONObject results = inManager.callFunction(inReq, "create_entity", template, 0, 5000);
	//
	//	Data child = getMediaArchive().getSearcher(inTargetentity).createNewData();
	//
	//	targetsearcher.updateData(child, results);
	//
	//	child.setValue("entity_date", new Date());
	//	child.setValue(inModuleid, inEntityid); // Lookup
	//	child.setValue("ai-functioncall", results.toJSONString());
	//	// No assets being created in this one.
	//	archive.saveData(inTargetentity, child);
	//	// Category folder =
	//	// getMediaArchive().getEntityManager().createDefaultFolder(entity, null);
	//	return child;
	//
	//    }

//	public Asset createAssetFromLLM(WebPageRequest inReq, String inSourcepath, String inStructions)
//	{
//
//		// TODO: This is all hardcoded to use OpenAI - need to change this to lookup
//		// once we have StableDiffusion or some other open soruce thing
//		MediaArchive archive = getMediaArchive();
//		Map inputdata = new HashMap();
//
//		String type = "gptManager";
//		LlmConnection manager = (LlmConnection) archive.getBean(type);
//		String model = "dall-e-3";
//		String prompt = inReq.findValue("llmprompt.value");
//
//		inReq.putPageValue("inputdata", inputdata);
//		inReq.putPageValue("prompt", prompt);
//
//		String imagestyle = inReq.findValue("llmimagestyle.value");
//		if (imagestyle == null)
//		{
//			imagestyle = "vivid";
//		}
//		LlmResponse results = manager.createImage(inReq, 1, "1024x1024", imagestyle, inStructions);
//		String[] fields = inReq.getRequestParameters("field");
//		ArrayList assets = new ArrayList();
//		for (Iterator iterator = results.getImageUrls().iterator(); iterator.hasNext();)
//		{
//
//			String url = (String) iterator.next();
//			String filename = getMediaArchive().getUserManager().getStringEncryption().generateHashFromString(url, 15);
//			filename = filename + ".png";
//			String uploadsourcepath = inSourcepath + "/" + filename;
//			AssetImporter importer = getMediaArchive().getAssetImporter();
//			Asset asset = importer.createAssetFromFetchUrl(archive, url, inReq.getUser(), uploadsourcepath, filename, null);
//			getMediaArchive().getAssetSearcher().updateData(inReq, fields, asset);
//
//			assets.add(asset);
//		}
//		getMediaArchive().saveAssets(assets);
//		archive.fireSharedMediaEvent("importing/assetscreated");
//
//		return (Asset) assets.get(0);
//
//	}

	public Asset createAssetFromLLM(Map params, Data contentrequest)
	{
		String prompt = (String) contentrequest.get("llmprompt");
		
		if (prompt == null)
		{
			return null;
		}
		
		MediaArchive archive = getMediaArchive();
		Asset asset = archive.getAsset(contentrequest.get("primarymedia"));
		if(asset == null) {
			return null;
		}


		try 
		{
			LlmConnection llmconnection = archive.getLlmConnection("createImage");
			
			LlmResponse results = llmconnection.createImage(prompt);

			for (Iterator iterator = results.getImageBase64s().iterator(); iterator.hasNext();)
			{
				String base64 = (String) iterator.next();

	
				asset.setValue("importstatus", "created");
				asset.setValue("assetaddeddate", new Date());
				
				String filename = results.getFileName();

				String path = "/WEB-INF/data/" + asset.getCatalogId() + "/originals/" + asset.getSourcePath();
				ContentItem saveTo = archive.getPageManager().getPage(path).getContentItem();
				
				try
				{
					InputStreamItem revision = new InputStreamItem();
					
					revision.setAbsolutePath(saveTo.getAbsolutePath());
					revision.setPath(saveTo.getPath());
					revision.setType( ContentItem.TYPE_ADDED );
					revision.setMessage( saveTo.getMessage());
					
					revision.setPreviewImage(saveTo.getPreviewImage());
					revision.setMakeVersion(false);
					
					log.info("Saving image -> " + path + "/" + filename);
					
					InputStream input = null;
					
					String code = base64.substring(base64.indexOf(",") +1, base64.length());
					byte[] tosave = Base64.getDecoder().decode(code);
					input = new ByteArrayInputStream(tosave);
					
					revision.setInputStream(input);
					
					archive.getPageManager().getRepository().put( revision );
					
					asset.setName(filename);

					archive.saveAsset(asset);

					archive.fireSharedMediaEvent("importing/assetscreated");
					contentrequest.setValue("status", "complete");
					archive.saveData("contentcreator", contentrequest);
				}
				catch (Exception ex)
				{
					asset.setProperty("importstatus", "error");
					log.error(ex);
					archive.saveAsset(asset);
				}
			} 
		}
		catch (Exception e)
		{
			log.error(e);
			contentrequest.setValue("status", "error");
			contentrequest.setValue("errormessage", e.toString());
			archive.saveData("contentcreator", contentrequest);
		}
		return asset;

	}

	public Data createFromLLM(Map params, LlmConnection inLlm, Data inContentrequest) throws Exception
	{
		MediaArchive archive = getMediaArchive();

		String entityid = inContentrequest.get("entityid");

		String view = inContentrequest.get("entitymoduleviewid");
		if (view == null)
		{
			return null;
		}

		Data entitypartentview = archive.getCachedData("view", view);

		String moduleid = entitypartentview.get("moduleid");
		Searcher targetsearcher = null;
		Data child = null;
		// If there is a view we're creating a child based on the view
		if (entitypartentview.get("rendertable") == null)
		{

			targetsearcher = getMediaArchive().getSearcher(moduleid);
			Data targetmodule = getMediaArchive().getCachedData("module", moduleid);// Chapter

			params.put("targetmodule", targetmodule);

			params.put("contentrequest", inContentrequest);

			LlmResponse results = inLlm.callCreateFunction(params, "create_entity");

			child = targetsearcher.createNewData();
			JSONObject args = results.getMessageStructured();
			if (args != null) {
				for (Iterator iterator = args.keySet().iterator(); iterator.hasNext();) {
					Object key = (Object) iterator.next();
					Object val = args.get(key);
					if(val instanceof String) 
					{
						String str = (String)val;
						str = str.split("\\|")[0];
						args.put(key, str);
					}
					else if(val instanceof Collection) {
						JSONArray arr = (JSONArray)val;
						if(arr.size() > 0) {
							for(int i = 0; i < arr.size(); i++) {
								String str = (String)arr.get(i);
								str = str.split("\\|")[0];
								arr.set(i, str);
							}
							args.put(key, arr);
						}
					}
					
				}
				targetsearcher.updateData(child, args);
			}
			child.setValue("entity_date", new Date());
			child.setValue("ai-functioncall", results.getFunctionName());
			child.setValue("owner", inContentrequest.get("owner"));
			targetsearcher.saveData(child);

		}
		else
		{
			String submodsearchtype = entitypartentview.get("rendertable");
			Data targetmodule = getMediaArchive().getCachedData("module", submodsearchtype);// Adding a new chapter

			targetsearcher = getMediaArchive().getSearcher(submodsearchtype);
			Searcher parentsearcher = getMediaArchive().getSearcher(moduleid);//

			Data directparent = getMediaArchive().getCachedData(moduleid, entityid);

			params.put("parentmodule", moduleid); //Book
			params.put("parententity", directparent); //Which book
			params.put("parentsearcher", parentsearcher);
			params.put("parentdetails", parentsearcher.getPropertyDetails());

			params.put("targetmodule", targetmodule); //Chapter
			params.put("targetsearcher", targetmodule);

			params.put("contentrequest", inContentrequest);

			LlmResponse results = inLlm.callCreateFunction(params, "create_entity");
			
			JSONObject args = results.getMessageStructured();
			for (Iterator iterator = args.keySet().iterator(); iterator.hasNext();) {
				Object type = (Object) iterator.next();
				Object val = args.get(type);
				if(val instanceof String) 
				{
					String str = (String)val;
					str = str.split("|")[0]; //TODO
					args.put(type, str);
				}
				else if(val instanceof Collection) {
					JSONArray arr = (JSONArray)val;
					if(arr.size() > 0) {
						String str = (String)arr.get(0);
						str = str.split("|")[0]; //TODO
						args.put(type, str);
					}
				}
				else if(val instanceof Boolean) {
					// TODO
				} 
			}

			child = targetsearcher.createNewData();
			targetsearcher.updateData(child, args);
			child.setValue("entity_date", new Date());
			child.setValue(moduleid, entityid); // Lookup
			child.setValue("ai-functioncall", args);
			// No assets being created in this one.
			child.setValue("owner", inContentrequest.get("owner"));

			archive.saveData(submodsearchtype, child);
			
			log.info("New record created for: " + moduleid + " Name: " + child.getName());
			// Category folder =
			// getMediaArchive().getEntityManager().createDefaultFolder(entity, null);

		}
		// TODO: Create some assets?
		return child;

	}
	
	@Override
	public LlmResponse processMessage(AgentContext inAgentContext, MultiValued inAgentMessage, MultiValued inAiFunction)
	{
		
		if("createImage".equals(inAgentContext.getFunctionName()))
		{
			
			MultiValued usermessage = (MultiValued)getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
			LlmResponse result = createImage(usermessage, inAgentContext);
			return result;
		}
		else if("renderImage".equals(inAgentContext.getFunctionName()))
		{
			String assetid = inAgentContext.get("assetid");
			
			Asset asset = getMediaArchive().getAsset(assetid);
			inAgentContext.addContext("asset", asset); //Get the updated asset

			inAgentContext.addContext("refreshing", "true");
			
			LlmConnection llmconnection = getMediaArchive().getLlmConnection("renderImage");
			
			LlmResponse result = llmconnection.renderLocalAction(inAgentContext);
			
			log.info("Next function: " + inAgentContext.getNextFunctionName());
			
			
			return result;
			
		}
		else if ("createEntity".equals(inAgentContext.getFunctionName()))
		{
			MultiValued usermessage = (MultiValued)getMediaArchive().getCachedData("chatterbox", inAgentMessage.get("replytoid"));
			
			LlmResponse result = createEntity(usermessage, inAgentContext);
			
			return result;
		}
		
		throw new OpenEditException("Unknown function name: " + inAgentContext.getFunctionName());
	}
	
	public LlmResponse createImage(MultiValued usermessage, AgentContext inAgentContext) 
	{
		MediaArchive archive = getMediaArchive();
		
		AiCreation aiCreation = inAgentContext.getAiCreationParams();

		LlmConnection llmconnection = archive.getLlmConnection("createImage");
		
		JSONObject imagefields = (JSONObject) aiCreation.getCreationFields();
		
		String prompt = (String) imagefields.get("prompt");

		if (prompt == null)
		{
			return null;
		}
		
		String filename = (String) imagefields.get("image_name");

		LlmResponse results = llmconnection.createImage(prompt);
		

		for (Iterator iterator = results.getImageBase64s().iterator(); iterator.hasNext();)
		{
			String base64 = (String) iterator.next();

			Asset asset = (Asset) archive.getAssetSearcher().createNewData();

			asset.setValue("importstatus", "created");

			if( filename == null || filename.length() == 0)
			{
				filename = "aiimage_" + System.currentTimeMillis() ;
			}
			
			asset.setName(filename + ".png");
			asset.setValue("assettitle", filename);
			asset.setValue("assetaddeddate", new Date());
			
			String sourcepath = "Channels/" + usermessage.get("user") + "/" + DateStorageUtil.getStorageUtil().getTodayForDisplay() + "/" + filename;
			asset.setSourcePath(sourcepath);

			String path = "/WEB-INF/data/" + asset.getCatalogId() + "/originals/" + asset.getSourcePath();
			ContentItem saveTo = archive.getPageManager().getPage(path).getContentItem();
			
			
			try
			{
				InputStreamItem revision = new InputStreamItem();
				
				revision.setAbsolutePath(saveTo.getAbsolutePath());
				revision.setPath(saveTo.getPath());
				revision.setAuthor( usermessage.get("user") );
				revision.setType( ContentItem.TYPE_ADDED );
				revision.setMessage( saveTo.getMessage());
				
				revision.setPreviewImage(saveTo.getPreviewImage());
				revision.setMakeVersion(false);
				
				log.info("Saving image -> " + path + "/" + filename);
				
				InputStream input = null;
				
				String code = base64.substring(base64.indexOf(",") +1, base64.length());
				byte[] tosave = Base64.getDecoder().decode(code);
				input = new ByteArrayInputStream(tosave);
				
				revision.setInputStream(input);
				
				archive.getPageManager().getRepository().put( revision );
				asset.setProperty("importstatus", "created");
				archive.saveAsset(asset);
			}
			catch (Exception ex)
			{
				asset.setProperty("importstatus", "error");
				log.error(ex);
				archive.saveAsset(asset);
			}
			
//			inReq.putPageValue("asset", asset);
			inAgentContext.addContext("asset", asset);
			inAgentContext.setNextFunctionName("renderImage");
			inAgentContext.setValue("assetid", asset.getId());
			inAgentContext.setValue("wait", 1000);
		}
		
		
		archive.fireSharedMediaEvent("importing/assetscreated");
		
		return results;
	}
	
	public LlmResponse createEntity(MultiValued usermessage, AgentContext inAgentContext) 
	{
		MediaArchive archive = getMediaArchive();
		
		AiCreation aiCreation = inAgentContext.getAiCreationParams();
		
		JSONObject entityfields = (JSONObject) aiCreation.getCreationFields();
		String entityname = (String) entityfields.get("entity_name");

		if (entityname == null)
		{
			inAgentContext.addContext("error", "Please provide a name for the new entity");
			return null;
		}
		
		String moduleid = (String) entityfields.get("module_id");
		if(moduleid == null)
		{
			inAgentContext.addContext("error", "Could not find module. Please provide an existing module name or id");
			return null;
		}
		
		
		moduleid = moduleid.split("\\|")[0];

		Data module = archive.getCachedData("module", moduleid);
		
		if(module == null)
		{
			inAgentContext.addContext("error", "Could not find module. Please provide an existing module name or id");
			return null;
		}
		
		Searcher searcher = archive.getSearcher(module.getId());
		
		Data entity = searcher.createNewData();
		entity.setName(entityname);

		searcher.saveData(entity);
		
		inAgentContext.addContext("entity", entity);
		inAgentContext.addContext("module", module);
		
		
		LlmConnection llmconnection = getMediaArchive().getLlmConnection("createEntity");
		
		LlmResponse result = llmconnection.renderLocalAction(inAgentContext);
		
		return result;
		
	}
	
	public void updateEntity(WebPageRequest inReq) throws Exception 
	{
		MediaArchive archive = getMediaArchive();

		String args = inReq.getRequestParameter("arguments");
		if(args == null)
		{
			log.warn("No arguments found in request");
			return;
		}
		JSONObject arguments = new JSONParser().parse( args );
		
		String entityid = (String) arguments.get("entityId");
		String moduleid = (String) arguments.get("moduleId");
		
		if (entityid == null)
		{
			inReq.putPageValue("error", "Please provide the entity id for the entity to update");
			return;
		}
		if (moduleid == null)
		{
			inReq.putPageValue("error", "Please provide the module id for the entity to update");
			return;
		}
		
		Data module = archive.getCachedData("module", moduleid);
		if(module == null)
		{
			inReq.putPageValue("error", "Could not find module. Please provide the module id of the entity to update");
			return;
		}
		Searcher searcher = archive.getSearcher(module.getId());
		Data entity = searcher.query().id(entityid).cachedSearchOne();
		if(entity == null)
		{
			inReq.putPageValue("error", "Could not find entity. Please provide an existing entity id to update");
			return;
		}
		
		String newmoduleid = (String) arguments.get("newModuleId");
		if(newmoduleid != null)
		{
			Data newmodule = archive.getCachedData("module", newmoduleid);
			if(newmodule == null)
			{
				inReq.putPageValue("error", "Could not find new module. Please provide an existing module id to update");
				return;
			}
			EntityManager entityManager = archive.getEntityManager();
			String modulechangemethod = (String) arguments.get("moduleChangeMethod");
			if("copy".equals(modulechangemethod))
			{
				Data newentity = entityManager.copyEntity(inReq, module.getId(), newmodule.getId(), entity);
				entity = newentity;
				module = newmodule;
				inReq.putPageValue("changemethod", "copy");
			}
			else if("move".equals(modulechangemethod))
			{
				Data newentity = entityManager.copyEntity(inReq, module.getId(), newmodule.getId(), entity);
				if( newentity == null)
				{
					inReq.putPageValue("error", "Could not copy entity. Please try again");
					return;
				}
				entityManager.deleteEntity(inReq, module.getId(), entity.getId());
				entity = newentity;
				module = newmodule;
				inReq.putPageValue("changemethod", "move");
			}
			else
			{
				inReq.putPageValue("error", "Please specify whether to copy or move the entity to the new module");
				return;
			}
			
		}
		
		String primaryImage = (String) arguments.get("primaryImageId");
		String newName = (String) arguments.get("newName");
		
		if(newName != null && newName.length() > 0)
		{
			entity.setName(newName);
			inReq.putPageValue("newname", newName);
		}
		if(primaryImage != null && primaryImage.length() > 0)
		{
			Asset asset = archive.getAsset(primaryImage);
			if(asset != null)
			{
				EntityManager entityManager = getMediaArchive().getEntityManager();
				String destinationcategorypath = inReq.getRequestParameter("destinationcategorypath");
				Category destinationCategory = null;
				if(destinationcategorypath!= null)
				{
					destinationCategory = archive.getCategorySearcher().createCategoryPath(destinationcategorypath);
				}
				else {
					destinationCategory = entityManager.loadDefaultFolder(module, entity, inReq.getUser());
				}
				entityManager.addAssetToEntity(inReq.getUser(), module, entity, asset, destinationCategory);
				entity.setValue("primaryimage", primaryImage);
				inReq.putPageValue("primaryimage", asset);
			}
		}
		searcher.saveData(entity);
		
		inReq.putPageValue("entity", entity);
		inReq.putPageValue("module", module);
	}

	@Override
	public Collection<SemanticAction> createPossibleFunctionParameters(ScriptLogger inLog) {
		// TODO Auto-generated method stub
		return null;
	}


}
