package org.entermediadb.asset.modules;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;

import org.entermediadb.asset.Asset;
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
	
	public void saveCSS(WebPageRequest inReq) throws Exception 
	{
		saveAllCustomThemes(inReq);	

		String appid = inReq.findValue("applicationid");
		PageSettings xconf = getPageManager().getPageSettingsManager().getPageSettings("/" + appid + "/_site.xconf");
		Data theme = loadTheme(inReq);
		if (theme != null) 
		{
			xconf.setProperty("themeid",theme.getId()); //Default
				
			getPageManager().getPageSettingsManager().saveSetting(xconf);
			getPageManager().clearCache();
		}
	}

	protected void saveAllCustomThemes(WebPageRequest inReq) throws UnsupportedEncodingException
	{
		MediaArchive archive = getMediaArchive(inReq);
		//Process all the themes
		String catalogid = inReq.findPathValue("catalogid");
		Collection themes = getSearcherManager().query(catalogid, "theme").all().search();
		String appid = inReq.findValue("applicationid");

		for (Iterator iterator = themes.iterator(); iterator.hasNext();)
		{
			Data theme = (Data) iterator.next();
			
			String inputfile = theme.get("templatecss");
			if( inputfile == null)
			{
				inputfile="/${applicationid}/theme/styles/overridestemplate.css";
			}
			inputfile = inReq.getContentPage().replaceProperty(inputfile);
			
			String outputfile = "/" + appid + "/theme/" + theme.getId() + "/custom.css";
	
			Page page = getPageManager().getPage(inputfile);
			
			URLUtilities urlUtil = (URLUtilities) inReq.getPageValue(PageRequestKeys.URL_UTILITIES);
	
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
		}
	}
	
	public void saveLogo(WebPageRequest inReq) {
		MediaArchive archive = getMediaArchive(inReq);
		String applicationid = inReq.findValue("applicationid");
		Data theme = loadTheme(inReq);
		if(theme == null) {
			return;
		}
		String logoassetid = theme.get("logoasset");
		if (logoassetid != null) {
			Asset logoasset = archive.getAsset(logoassetid);
			if(logoasset != null) {
				Page logopage = archive.getOriginalDocument(logoasset);
				if(logopage != null) {
					Page destpage = getPageManager().getPage("/"+ applicationid + "/theme/" + theme.getId() + "/logo.png");
					if( !destpage.getPath().equals(logopage.getPath()) )
					{
						getPageManager().copyPage(logopage, destpage);
						if( theme.getValue("logowith") == null)
						{
							theme.setValue("logowith",logoasset.get("width"));
							theme.setValue("logoheight",logoasset.get("height"));
							archive.saveData("theme",theme);
						}
					}
				}
			}
		}
	}

	public void saveTheme(WebPageRequest inReq) {
		String catalogid = inReq.findPathValue("catalogid");
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
		String catalogid = inReq.findPathValue("catalogid");
		Searcher themeSearcher = getSearcherManager().getSearcher(catalogid,
				"theme");
		String themeid = inReq.getRequestParameter("themeid");
		Data theme = (Data) themeSearcher.searchById(themeid);
			if (theme != null) {
				inReq.putPageValue("theme", theme);
			}
		return theme;
	}

	public void changeTheme(WebPageRequest inReq) {
		String appid = inReq.findValue("applicationid");
		String themeid = inReq.getRequestParameter("themeid");
		PageSettings xconf = getPageManager().getPageSettingsManager().getPageSettings("/" + appid + "/_site.xconf");
		
		xconf.setProperty("themeid",themeid);
		getPageManager().getPageSettingsManager().saveSetting(xconf);
		getPageManager().clearCache();
		
		try {
			saveCSS(inReq);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void resetThemes(WebPageRequest inReq) {
		String catalogid = inReq.findPathValue("catalogid");
		Searcher themeSearcher = getSearcherManager().getSearcher(catalogid,
				"theme");
		String themeid = inReq.getRequestParameter("themeid");
		themeSearcher.restoreSettings();
		themeSearcher.reindexInternal();
		inReq.putPageValue("message","Theme reset");
		changeTheme(inReq);
//		Data theme = (Data) themeSearcher.searchById(themeid);
//			if (theme != null) {
//				inReq.putPageValue("theme", theme);
//			}
//		return theme;
	}

}
