package org.entermediadb.asset.modules;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.entermediadb.asset.MediaArchive;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.generators.Output;
import org.openedit.page.Page;
import org.openedit.page.PageRequestKeys;
import org.openedit.page.PageSettings;
import org.openedit.repository.filesystem.StringItem;
import org.openedit.util.RequestUtils;
import org.openedit.util.URLUtilities;

public class ThemeModule extends BaseMediaModule {

	protected RequestUtils fieldRequestUtils;
	protected SearcherManager fieldSearcherManager;

	public RequestUtils getRequestUtils() {
		return fieldRequestUtils;
	}

	public void setRequestUtils(RequestUtils inRequestUtils) {
		fieldRequestUtils = inRequestUtils;
	}

	// public void loadPortfolio(WebPageRequest inReq){
	// String catalogid = inReq.findValue("catalogid");
	// Searcher searcher = getSearcherManager().getSearcher(get, inFieldName);
	// }
	//
	public void saveCSS(WebPageRequest inReq) throws Exception {
		String inputfile = inReq.findValue("templatecss");
		String outputfile = inReq.findValue("outputcss");

		Data theme = loadTheme(inReq);

		Page page = getPageManager().getPage(inputfile);

		
		URLUtilities urlUtil = (URLUtilities) inReq
				.getPageValue(PageRequestKeys.URL_UTILITIES);

		// WebPageRequest req =
		// getRequestUtils().createVirtualPageRequest(page.getPath(),
		// inReq.getUser(), urlUtil);
		WebPageRequest req = getRequestUtils().createPageRequest(
				page,
				inReq.getRequest(),
				inReq.getResponse(),
				inReq.getUser(),
				(URLUtilities) inReq
						.getPageValue(PageRequestKeys.URL_UTILITIES));
		Page outputpage = getPageManager().getPage(outputfile);
		getPageManager().putPage(outputpage);
		//loadTheme(req);
		req.putPageValue("theme", theme);

		
		MediaArchive archive = getMediaArchive(inReq);
		
		req.putPageValue("mediaarchive", archive);
		//req.putPageValue("numberutils", new NumberUtils());
		
		req.putProtectedPageValue(PageRequestKeys.HOME,
				urlUtil.relativeHomePrefix());
		ByteArrayOutputStream scapture = new ByteArrayOutputStream();
		Writer capture = null;
		capture = new OutputStreamWriter(scapture, page.getCharacterEncoding());
		Output out = new Output(capture, outputpage.getContentItem()
				.getOutputStream());
		
		page.generate(req, out);
		String output = scapture.toString();
		StringItem revision = new StringItem(outputpage.getPath(), output,
				outputpage.getCharacterEncoding());
		revision.setAuthor(inReq.getUserName());
		
		revision.setMessage("updated by  user");
		outputpage.setContentItem(revision);
		getPageManager().putPage(outputpage);
		getPageManager().clearCache(outputpage);
		
		String appid = inReq.findValue("applicationid");
		PageSettings xconf = getPageManager().getPageSettingsManager().getPageSettings("/" + appid + "/_site.xconf");

		String title = theme.get("title");
		if( title !=  null )
		{
			xconf.setProperty("title",title);
		}
		String systemfromemail = theme.get("systemfromemail");
		if( systemfromemail !=  null )
		{
			xconf.setProperty("systemfromemail",systemfromemail);
		}
		String systemfromemailname = theme.get("systemfromemailname");
		if( systemfromemailname !=  null )
		{
			xconf.setProperty("systemfromemailname",systemfromemailname);
		}
		// <property name="systemfromemail">noreply@entermediasoftware.com</property> 
		
		xconf.setProperty("themeid",theme.getId());
		getPageManager().getPageSettingsManager().saveSetting(xconf);
		
	}

	public void saveTheme(WebPageRequest inReq) {
		String catalogid = inReq.findValue("catalogid");
		Searcher themeSearcher = getSearcherManager().getSearcher(catalogid,
				"theme");
		String owner = inReq.findValue("applicationid");
		if (owner != null) {
			Data theme = (Data) themeSearcher.searchById(owner
					+ "theme");
			if (theme == null) {
				theme = themeSearcher.createNewData();
				theme.setId(owner + "theme");
				theme.setSourcePath("themes" );
			}
			String[] fields = inReq.getRequestParameters("field");
			themeSearcher.updateData(inReq, fields, theme);
			theme.setId(owner + "theme");
			themeSearcher.saveData(theme, inReq.getUser());
			inReq.putPageValue("theme", theme);
		}
	}

	public Data loadTheme(WebPageRequest inReq) {
		String catalogid = inReq.findValue("catalogid");
		Searcher themeSearcher = getSearcherManager().getSearcher(catalogid,
				"theme");
		String themeid = inReq.getRequestParameter("themeid");
		Data theme = (Data) themeSearcher.searchById(themeid);
			if (theme != null) {
				inReq.putPageValue("theme", theme);
			}
		return theme;
	}


}
