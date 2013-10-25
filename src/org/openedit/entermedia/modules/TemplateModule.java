package org.openedit.entermedia.modules;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import org.entermedia.upload.FileUpload;
import org.entermedia.upload.FileUploadItem;
import org.entermedia.upload.UploadRequest;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.modules.BaseMediaModule;
import org.openedit.entermedia.search.AssetSearcher;
import org.openedit.repository.filesystem.StringItem;

import com.openedit.WebPageRequest;
import com.openedit.generators.Output;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.page.PageRequestKeys;
import com.openedit.users.User;
import com.openedit.util.RequestUtils;
import com.openedit.util.URLUtilities;

import org.apache.commons.lang.math.NumberUtils;

public class TemplateModule extends BaseMediaModule {

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
		loadTheme(req);

		loadTemplate(req);
		
		MediaArchive archive = getMediaArchive(inReq);
		req.putPageValue("mediaarchive", archive);
		req.putPageValue("numberutils", new NumberUtils());
		
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

	}

	public void saveTemplate(WebPageRequest inReq) {
		String catalogid = inReq.findValue("catalogid");
		Searcher templateSearcher = getSearcherManager().getSearcher(catalogid,
				"template");
		String owner = inReq.findValue("applicationid");
		if (owner != null) {
			Data template = (Data) templateSearcher.searchById(owner
					+ "template");
			if (template == null) {
				template = templateSearcher.createNewData();
				template.setId(owner + "template");
				template.setSourcePath("templates" );
			}
			String[] fields = inReq.getRequestParameters("field");
			templateSearcher.updateData(inReq, fields, template);
			template.setId(owner + "template");
			templateSearcher.saveData(template, inReq.getUser());
			inReq.putPageValue("template", template);
		}
	}

	public void loadTemplate(WebPageRequest inReq) {
		String catalogid = inReq.findValue("catalogid");
		Searcher templateSearcher = getSearcherManager().getSearcher(catalogid,
				"portfolio");
		
		String owner = inReq.findValue("owner");
		if (owner != null) {
			Data template = (Data) templateSearcher.searchById(owner);
			if (template != null) {
				inReq.putPageValue("portfolio", template);
			}
		}
	}


	public void loadTheme(WebPageRequest inReq) {
		String catalogid = inReq.findValue("catalogid");
		Searcher templateSearcher = getSearcherManager().getSearcher(catalogid,
				"theme");
			String themeid = inReq.findValue("themeid");
			if(themeid == null){
				themeid = "theme";
			}
			Data template = (Data) templateSearcher.searchById(themeid);
			if (template != null) {
				inReq.putPageValue("theme", template);
			}
		
	}

}
