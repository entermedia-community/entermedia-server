package org.entermediadb.scripts;

import java.util.Iterator;
import java.util.Map;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.openedit.OpenEditException;

public class BsfScriptRunner implements ScriptRunner
{

	@Override
	public Object exec(Script inScript, Map context) throws OpenEditException
	{
		BSFManager bsfManager = new BSFManager();

		try
		{
			// expose standard items in the context
			for (Iterator iter = context.keySet().iterator(); iter.hasNext();)
			{
				String element = (String) iter.next();
				Object val = context.get(element);
				if( val != null)
				{
					bsfManager.declareBean(element, val, val.getClass());
				}
			}
	
			bsfManager.exec(
				BSFManager.getLangFromFilename(inScript.getDescription()),
				inScript.getDescription(),
				0,
				0,
				inScript.getScriptText());
		}
		catch( BSFException ex)
		{
			Throwable ext = ex.getTargetException();
			if( ext instanceof OpenEditException)
			{
				throw (OpenEditException)ext;
			}
			throw new OpenEditException(ext);
		}
		return null;
		
		
	}


}
