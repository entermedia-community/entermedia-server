package org.entermediadb.scripts;

import java.util.Iterator;
import java.util.Map;

import org.openedit.OpenEditException;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.TargetError;

public class BshScriptRunner implements ScriptRunner
{

	@Override
	public Object exec(Script inScript, Map variableMap) throws OpenEditException
	{
  	String path = inScript.getPage().getPath();
		
		try {
			Interpreter i = new Interpreter(); 
			for (Iterator iterator = variableMap.keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				i.set(key,variableMap.get(key));
			}
            Object obj = i.source(inScript.getPage().getContentItem().getAbsolutePath());
            return obj;
        } catch ( TargetError e ) {
        	throw new OpenEditException("line: " + e.getErrorLineNumber()+ " " + path + " " + e.getTarget(), e.getTarget(), path);
        } catch ( EvalError e2 )    {
        	//throw new OpenEditException("line: " + e2.getErrorLineNumber()+ " " + e2.getMessage(), e2, path);
        	throw new OpenEditException(e2.getMessage(), e2, path);
        }
        catch ( OpenEditException ex)
        {
        	throw ex;
        }
        catch ( Exception ex)
        {
        	throw new OpenEditException(ex);
        }
		
	}


}
