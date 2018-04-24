package org.entermediadb.scripts;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.openedit.OpenEditException;
import org.openedit.util.Exec;
import org.openedit.util.ExecResult;

public class ShellScriptRunner implements ScriptRunner
{
	private static final Log log = LogFactory.getLog(ShellScriptRunner.class);

	
	protected Exec fieldExec;

	
	
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
		
		ExecResult result = getExec().runExec(path, new ArrayList(), true);
		if(result.isRunOk()) {
			log.info("ran " + inScript.getPage());
			log.info(result.getStandardOut());
		}
		
		return result;
	}


}
