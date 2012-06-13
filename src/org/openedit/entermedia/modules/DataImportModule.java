package org.openedit.entermedia.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.openedit.WebPageRequest;
import com.openedit.entermedia.scripts.Script;
import com.openedit.entermedia.scripts.ScriptLogger;
import com.openedit.entermedia.scripts.ScriptManager;
import com.openedit.page.Page;

public class DataImportModule extends DataEditModule
{
	protected ScriptManager fieldScriptManager;
	
	public ScriptManager getScriptManager()
	{
		return fieldScriptManager;
	}

	public void setScriptManager(ScriptManager inScriptManager)
	{
		fieldScriptManager = inScriptManager;
	}
	public List listImportScripts(WebPageRequest inReq) throws Exception
	{
		String dataroot = inReq.findValue("dataroot");
		List scripts = getPageManager().getChildrenPaths(dataroot + "/import/scripts/",true);
		List pages = new ArrayList();
		Set dups = new HashSet();
		for (Iterator iterator = scripts.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			Page page = getPageManager().getPage(path);
			if( !dups.contains(page.getName()) )
			{
				pages.add(page);
				dups.add(page.getName());
			}
		}
		
		inReq.putPageValue("scripts", pages);
		return scripts;
	}
	public void importData(WebPageRequest inReq) throws Exception
	{
		String dataroot = inReq.findValue("dataroot");

		String filename = inReq.findValue("scriptname");
		Script script = getScriptManager().loadScript( dataroot + "/import/scripts/" + filename);
		
		Map variables = new HashMap();
		variables.put("context", inReq);
		ScriptLogger logger = new ScriptLogger();
		logger.startCapture();
		variables.put("log", logger);
		try
		{
			getScriptManager().execScript(variables, script);
		}
		finally
		{
			logger.stopCapture();
		}
	}
	
	public void saveScript(WebPageRequest inReq) throws Exception
	{
		String dataroot = inReq.findValue("dataroot");
		String filename = inReq.findValue("filename");
		String code = inReq.findValue("scriptcode");
		
		Page page = getPageManager().getPage(dataroot + "/import/scripts/" + filename);
		getPageManager().saveContent(page, inReq.getUser(), code, "web edit");
		
	}
}
