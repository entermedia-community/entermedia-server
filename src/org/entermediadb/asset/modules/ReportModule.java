package org.entermediadb.asset.modules;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.modules.scriptrunner.ScriptModule;
import org.entermediadb.scripts.Script;
import org.entermediadb.scripts.ScriptLogger;
import org.entermediadb.scripts.ScriptManager;
import org.entermediadb.scripts.TextAppender;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.page.Page;

public class ReportModule extends DataEditModule
{

	private static Log log = LogFactory.getLog(ScriptModule.class);
	protected ScriptManager fieldScriptManager;
	
	
	public ScriptManager getScriptManager()
	{
		if( fieldScriptManager == null)
		{
			fieldScriptManager = (ScriptManager)getModuleManager().getBean("scriptManager");
		}
		return fieldScriptManager;
	}

	public void setScriptManager(ScriptManager inScriptManager)
	{
		fieldScriptManager = inScriptManager;
	}

	public void loadReport(WebPageRequest inReq){
		MediaArchive archive = getMediaArchive(inReq);
		
		String reportid = inReq.findValue("reportid");
		if (reportid == null){
			return;
		}
		
		SearcherManager manager = getSearcherManager();
		Data report = manager.getData("system",  "reports",  reportid);
		
		inReq.putPageValue("report", report);
		inReq.putPageValue("data", report);

		String searchtype = report.get("searchtype");
		//String catalogid = inReq.getUserProfileValue("reportcatalogid");
		String catalogid = archive.getCatalogId();
		if(catalogid == null){
			catalogid = "media/catalogs/public"; //Maybe should be last accessed catalog?  I'll pass in the profile value from the emshare app...
		}
	
		inReq.putPageValue("reportcatalogid", catalogid);

		if(searchtype != null){
			Searcher searcher = archive.getSearcherManager().getSearcher(catalogid, searchtype);
			inReq.putPageValue("reportsearcher", searcher);
			

		}
	}
	
	
	
	public void runReport(WebPageRequest inReq) throws Exception{
		
		MediaArchive archive = getMediaArchive(inReq);

		String reportid = inReq.findValue("reportid");
		if (reportid == null){
			return;
		}
		SearcherManager manager = getSearcherManager();
		Data report = manager.getData("system",  "reports",  reportid);
		
		inReq.putPageValue("report", report);
		String script = report.get("script");
		String searchtype = report.get("searchtype");
		
		//String catalogid = inReq.getUserProfileValue("reportcatalogid");
		String catalogid = archive.getCatalogId();
		
		if(catalogid == null){
			catalogid = "media/catalogs/public"; //Maybe should be last accessed catalog?  I'll pass in the profile value from the emshare app...
		}
		if(script == null){
			script = "reporting/" +  searchtype + ".groovy";
		} 
		inReq.putPageValue("reportcatalogid", catalogid);
		Page page = getPageManager().getPage("/system/events/scripts/" + script);
		
		if(!page.exists()){
			log.info("No script, running standard search");
			inReq.setRequestParameter("searchtype", searchtype);
			inReq.setRequestParameter(searchtype + "includefacets", "true");

		//	page =  getPageManager().getPage("/" + archive.getCatalogId() + "/events/scripts/reports/default.groovy");
			search(inReq);
			return;
		}
		
		Script reportscript = getScriptManager().loadScript(page.getPath());

		final StringBuffer output = new StringBuffer();
		TextAppender appender = new TextAppender()
		{
			public void appendText(String inText)
			{
				output.append(inText);
				output.append("<br>");
			}
		};
		
		ScriptLogger logs = new ScriptLogger();
		logs.setPrefix(reportscript.getType());
		logs.setTextAppender(appender);
		try
		{
			logs.startCapture();
			Map variableMap = inReq.getPageMap();
			variableMap.put("context", inReq );
			variableMap.put("log", logs );
			
			Object returned = getScriptManager().execScript(variableMap, reportscript);
			if( returned != null)
			{
				output.append("returned: " + returned);
			}
		}
		finally
		{
			logs.stopCapture();
		}
		inReq.putPageValue("output",output);
		
	}
	
	public void selectCatalog(WebPageRequest inReq){
		String catalogid = inReq.getRequestParameter("id");
		MediaArchive archive = getMediaArchive(inReq);
		log.info("why no breakpoint");
		if(catalogid != null){
			inReq.getUserProfile().setValue("reportcatalogid", catalogid);
			archive.getSearcher("userprofile").saveData(inReq.getUserProfile(), inReq.getUser());
			
		}
				

	}
	
	
	
	
}
