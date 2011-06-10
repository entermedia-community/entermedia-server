package com.openedit.entermedia.scripts;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.TargetError;

import com.openedit.OpenEditException;
import com.openedit.page.Page;

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
