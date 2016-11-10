package org.entermediadb.asset.modules;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.modules.BaseModule;
import org.openedit.modules.translations.Language;
import org.openedit.modules.translations.Translation;
import org.openedit.modules.translations.TranslationSearcher;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.page.PageRequestKeys;
import org.openedit.util.FileUtils;
import org.openedit.web.Browser;

public class TranslationModule extends BaseModule {

	String[] languages = new String[] { "af", "sq", "ar-DZ", "ar", "hy", "az",
			"eu", "bs", "bg", "ca", "zh-HK", "zh-CN", "zh-TW", "hr", "cs",
			"da", "nl-BE", "nl", "en-AU", "en-NZ", "en-GB", "eo", "et", "fo",
			"fa", "fi", "fr", "fr-CH", "gl", "ge", "de", "el", "he", "hi",
			"hu", "is", "id", "it", "ja", "kk", "km", "ko", "lv", "lt", "lb",
			"mk", "ml", "ms", "no", "pl", "pt", "pt-BR", "rm", "ro", "ru",
			"sr", "sr-SR", "sk", "sl", "es", "sv", "ta", "th", "tj", "tr",
			"uk", "vi", "cy-GB" };
	Collection list = Arrays.asList(languages);

	public void listFilesInBase(WebPageRequest inReq) throws Exception{
		// get a list
		String path = "/WEB-INF/base";
		String lang = inReq.getRequestParameter("lang");
		List translations = gatherTranslations(path, lang);
		StringWriter out = new StringWriter();
		for (Iterator iterator = translations.iterator(); iterator.hasNext();) {
			path = (String) iterator.next();
			Page page = getPageManager().getPage(path);
			Properties props = new Properties();
			String content = page.getContent();
			
			Reader reader = page.getReader();
			try
			{
				props.load(reader);
			}
			catch( Throwable ex)
			{
				FileUtils.safeClose(reader);
			}
			out.append(path);
			out.append("\n");
			//props.list(new PrintWriter(out));  //This doesn't have the / in it - it needs the escape values.
			props.store(out, null);
			out.append("\n===\n");
		}
		String text = out.toString();
		text = text.replaceAll("-- listing properties --\n", "");
		inReq.putPageValue("translations", text);
	}

	public List gatherTranslations(String inPath, String inlang) {
		
		List translations = new ArrayList();
		List children = getPageManager().getChildrenPaths(inPath);
		for (Iterator iterator = children.iterator(); iterator.hasNext();) {
			String path = (String) iterator.next();
			if (path.contains("/.versions")) {
				continue;
			}
			if (path.contains("/WEB-INF/data/")) {
				continue;
			}
			if (path.endsWith("_text_" + inlang + ".txt")) {
				translations.add(path);
				continue;
			}
			// System.out.println("Trying to get page for path: " + path);
			Page page = getPageManager().getPage(path);
			if (page.isFolder()) {
				translations.addAll(gatherTranslations(path, inlang));
			}
		}
		return translations;
	}

	public void saveFiles(WebPageRequest inReq) throws Exception {
		// take the text and save it
		String text = inReq.getRequestParameter("translations");
		if (text != null) {
			boolean addnew = Boolean.parseBoolean(inReq
					.findValue("mergenewcontent"));

			BufferedReader read = new BufferedReader(new StringReader(text));
			String line = null;
			String filepath = read.readLine();
			StringBuffer textout = new StringBuffer();
			int c = 0;
			while ((line = read.readLine()) != null) {
				if (line.equals("===")) {
					// save off the last stuff and get ready for next file
					Page page = getPageManager().getPage(filepath);
					// dont save to folders that do not exists
					Page parent = getPageManager().getPage(page.getParentPath());
					if (parent == null || !parent.exists()) {
						line = read.readLine();
						filepath = line;
						continue;
					}
					
					String existing = "";
					if (page.exists()) {
						Reader reader = page.getReader();
						Properties existingprops = new Properties();
						existingprops.load(reader);
						StringWriter out = new StringWriter();
						existingprops.store(out, null);
						existing = out.toString();
					}
					String newcontent = textout.toString();
					
					
					
					if (!newcontent.equals(existing)) {
						if (addnew) {
							// merge together old and new
							Properties existingprops = new Properties();
							existingprops.load(new StringReader(existing));
							existingprops.load(new StringReader(newcontent));
							StringWriter out = new StringWriter();
							existingprops.store(out, null);
							newcontent = out.toString();
						}
						getPageManager().saveContent(page, inReq.getUser(),
								newcontent, null);
						c++;
					}
					textout.setLength(0);
					line = read.readLine();
					if (line == null) {
						break;
					}
					filepath = line;
				} else {
					textout.append(line);
					textout.append('\n');
				}
			}
			inReq.putPageValue("count", String.valueOf(c));
		}
	}

	public static final String PROPERTIES_FROM_MARKUP = "properties_from_markup";

	public Translation getTranslations(WebPageRequest inReq)
			throws OpenEditException {
		Translation trans = new Translation();

		// get the languages
		init(inReq, trans);

		inReq.putPageValue("pageManager", getPageManager());
		inReq.putPageValue("translations", trans);
		return trans;
	}

	protected void init(WebPageRequest inReq, Translation inTrans)
			throws OpenEditException {
		// #set( $languages = $page.getPageSettings().getProperty("languages") )
		PageProperty prop = inReq.getPage().getPageSettings()
				.getProperty("languages");

		if (prop != null) {
			for (Iterator iter = prop.getValues().keySet().iterator(); iter
					.hasNext();) {
				String locale = (String) iter.next();
				String name = (String) prop.getValues().get(locale);
				Language lang = new Language();
				lang.setPageManager(getPageManager());
				if (locale.length() == 0) {
					lang.setId("default");
					lang.setRootDirectory("");
				} else {
					lang.setId(locale);
					lang.setRootDirectory("/translations/" + locale);
				}
				lang.setName(name);
				inTrans.addLanguage(lang);
			}
			inTrans.sort();
			Language browser = createBrowserLang(inReq);
			inTrans.getLanguages().add(0, browser);
		}
		// This is for transition for people who do not have languages setup yet
		// or upgrades
		if (inTrans.getLanguages().size() == 0) {
			Language browser = createBrowserLang(inReq);
			inTrans.getLanguages().add(browser);
			Language lang = new Language();
			lang.setPageManager(getPageManager());
			lang.setId("default");
			lang.setName("Language: Use Default");
			lang.setRootDirectory("");
			inTrans.addLanguage(lang);
			// TODO: remove this section
			String done = (String) inReq.getSessionValue("defaultset");
			if (done == null) {
				inReq.putSessionValue("sessionlocale", "default");
				inReq.putSessionValue("defaultset", "true");
			}
		}
		String selectedLang = inReq.getLanguage();
		inTrans.setSelectedLang(selectedLang);
	}

	protected Language createBrowserLang(WebPageRequest inReq) {
		Language lang = new Language();
		lang.setPageManager(getPageManager());
		lang.setId("browser");
		Browser browser = (Browser) inReq.getPageValue("browser");
		if (browser != null) {
			lang.setName("Language: " + browser.getLocale());
		}
		lang.setRootDirectory("");
		return lang;
	}

	public void changeLanguage(WebPageRequest inReq) throws Exception {
		String newlang = inReq.getRequestParameter("newlang");
		getTranslations(inReq);
		if (newlang != null) {
			if (newlang.equals("locale_browser")) {
				inReq.removeSessionValue("sessionlocale");
			} else {
				String locale = newlang.substring("locale_".length());
				inReq.putSessionValue("sessionlocale", locale);
			}
			String orig = inReq.findValue("origURL");
			if (orig != null) {
				inReq.redirect(orig);
			}
		}
	}

	// for editing
	protected Language getEditingLanguage(WebPageRequest inReq) {
		String id = (String) inReq.getSessionValue("editinglanguage");

		Translation trans = (Translation) inReq.getPageValue("translations");

		return trans.getLanguage(id);
	}

	public void selectElement(WebPageRequest inReq) throws Exception {
		String eid = inReq.getRequestParameter("elementid");
		if (eid != null) {
			Translation trans = (Translation) inReq
					.getPageValue("translations");
			Language lang = trans.getLanguage(eid);
			inReq.putSessionValue("editinglanguage", lang.getId());
		}
	}

	public void loadTranslations(WebPageRequest inReq) {
		String translationid = inReq.findValue("translationsid");
		TranslationSearcher searcher = (TranslationSearcher) getSearcherManager()
				.getSearcher(translationid, "translation");
		inReq.putPageValue("translations", searcher);
		String locale = inReq.getLocale();
		if (locale != null) {
			if (locale.contains("_")) {

				locale = locale.substring(0, locale.indexOf("_"));

			}
			inReq.putPageValue("lang", locale);
		}

	}

	public void loadBrowserLanguage(WebPageRequest inReq) {
		HttpServletRequest req = inReq.getRequest();
		
		if (req != null) {
			Browser browser = new Browser(req.getHeader("User-Agent"));
			browser.setHttpServletRequest(req);
			browser.setLocale(req.getLocale());
			inReq.putPageValue(PageRequestKeys.BROWSER, browser);
		}

		if (inReq.getLocale() != null && req != null && req.getLocale() != null) {
			String temp = req.getLocale().toString();
			temp = temp.replace("_", "-").toLowerCase();
			String[] split = temp.split("-");
			String browserlanguage = null;
			if (split.length == 1) {

				browserlanguage = split[0];

			} else {
				browserlanguage = split[0] + "-" + split[1].toUpperCase();

			}
			if (list.contains(browserlanguage)) {
				inReq.putPageValue("browserlanguage", browserlanguage);
			}
		}

	}

}
