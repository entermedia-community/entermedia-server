package org.entermediadb.scripts;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.SearcherManager;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;
import org.openedit.util.Replacer;

public class ShellScriptRunner implements ScriptRunner
{
	private static final Log log = LogFactory.getLog(ShellScriptRunner.class);

	
	protected Exec fieldExec;
	protected SearcherManager fieldSearcherManager;
	protected ModuleManager fieldModuleManager;
	
	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}
	
	/*
	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}
	 */


	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}



	public Exec getExec() {
		return fieldExec;
	}



	public void setExec(Exec inExec) {
		fieldExec = inExec;
	}



	@Override
	public Object exec(Script inScript, Map variableMap) throws OpenEditException
	{
		String path = inScript.getPage().getContentItem().getAbsolutePath();
		ArrayList args = new ArrayList();
		WebPageRequest req = (WebPageRequest) variableMap.get("context");
		String catalogid = req.findPathValue("catalogid");
		String mask = req.findValue("scriptargs");
		if(catalogid != null && mask != null) {
			
			//String value =  getSearcherManager().getValue(catalogid, mask, variableMap);
			Replacer replacer = getReplacer(catalogid);
			String value = replacer.replace(mask, variableMap);
			
			if(value != null) {
				args.add(value);
			}
		}
		ExecResult result = getExec().runExec(path, args, true);
		if(result.isRunOk()) {
			log.info("ran " + inScript.getPage());
			log.info(result.getStandardOut());
		}
		
		return result;
	}
	
	public Replacer getReplacer(String inCatalogId)
	{
		return (Replacer)getModuleManager().getBean(inCatalogId, "replacer");
	}		


}
