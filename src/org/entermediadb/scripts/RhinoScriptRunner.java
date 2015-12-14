/*
Copyright (c) 2003 eInnovation Inc. All rights reserved

This library is free software; you can redistribute it and/or modify it under the terms
of the GNU Lesser General Public License as published by the Free Software Foundation;
either version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Lesser General Public License for more details.
*/

package org.entermediadb.scripts;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.openedit.OpenEditException;


/**
 * This implementation of {@link ScriptRunner} uses Mozilla's Rhino JavaScript engine.
 *
 * @author Eric Galluzzo
 */
public class RhinoScriptRunner implements ScriptRunner
{
	private static final Log LOGGER = LogFactory.getLog(RhinoScriptRunner.class);
	protected Scriptable fieldGlobalScope;
	
	/**
	 * ScriptRunner constructor comment.
	 *
	 * @param inScripts DOCUMENT ME!
	 */
	public RhinoScriptRunner()
	{
	}

	/**
	 * @see ScriptRunner#execFunction(String, Object[])
	public Object execFunction(String inFuncName, Object[] inParameters)
		throws OpenEditException
	{
		try
		{
			Context context = Context.enter();

			// See comment above about the context class loader.
			ClassLoader loader  = getClass().getClassLoader();
			if( loader == null)
			{
				loader = ClassLoader.getSystemClassLoader();
			}

			Thread.currentThread().setContextClassLoader(loader);

			Object function = getScope().get(inFuncName, getScope());

			if (function instanceof Function)
			{
				LOGGER.debug(
					"Evaluating the " + inFuncName + "() function on " +
					getScript().getDescription());

				Object result = ((Function) function).call(context, getScope(), null, inParameters);

				return unmarshal(result);
			}
			else
			{
				LOGGER.debug(
					"No " + inFuncName + "() function found on " + getScript().getDescription() +
					", was " + function.toString());
			}
		}
		catch (Exception e)
		{
			LOGGER.error("execFunction(): Error during " + inFuncName + "(): " + e.getMessage());

			//e.printStackTrace();
			throw new OpenEditException(e);
		}
		finally
		{
			Context.exit();
		}

		return null;
	}
	 */

	protected String makeStringFromCode(String inCode)
	{
		if ((inCode.indexOf('"') < 0) && (inCode.indexOf('\\') < 0))
		{
			return "\"" + inCode + "\"";
		}

		StringBuffer sb = new StringBuffer('"');

		for (int i = 0; i < inCode.length(); i++)
		{
			char c = inCode.charAt(i);

			switch (c)
			{
				case '"':
					sb.append("\\\"");

					break;

				case '\\':
					sb.append("\\\\");

					break;

				default:
					sb.append(c);

					break;
			}
		}

		sb.append('"');

		return sb.toString();
	}

	/**
	 * Unmarshal the given JavaScript object into a Java object.
	 *
	 * @param inObj The JavaScript object
	 *
	 * @return The Java object
	 */
	protected Object unmarshal(Object inObj)
	{
		if (inObj instanceof NativeJavaObject)
		{
			return ((NativeJavaObject) inObj).unwrap();
		}

		if (inObj instanceof NativeArray)
		{
			NativeArray array = (NativeArray) inObj;
			Object[] result = new Object[(int) array.jsGet_length()];

			for (int i = 0; i < array.jsGet_length(); i++)
			{
				result[i] = unmarshal(array.get(i, null));
			}

			return result;
		}

		return inObj;
	}

	@Override
	public Object exec(Script inScript, Map inContext) throws OpenEditException
	{
		// TODO Auto-generated method stub
		// Run the main script, which contains the calculate() (and
		// possibly init() ) function definitions.
		try
		{
			// We need to set the context class loader so that Rhino's
			// DefiningClassLoader picks it up.  In reality, we should
			// probably set this elsewhere (i.e. where we create the
			// thread), but we don't really need to right now.
			Context context = Context.enter();
//			if (Thread.currentThread().getContextClassLoader() != getClass().getClassLoader())
//			{
//				//setInitialized(true);
//				Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
//				context.setOptimizationLevel(-1);
//			}

			Scriptable scope = createScope();
		
			if (inContext != null)
			{
				for (Iterator iter = inContext.keySet().iterator(); iter.hasNext();)
				{
					String name = (String) iter.next();
					Object bean = inContext.get(name);
					scope.put(name, scope, Context.toObject(bean, scope));
					//runner.declareBean((String) element, );
				}
			}
			Object returned = context.evaluateString(
				scope, inScript.getScriptText(), inScript.getDescription(),
				inScript.getStartLineNumber(), null);
			return returned;

			//LOGGER.info( "Javascript optimization level: " + context.getOptimizationLevel() );
		}
		catch (Exception e)
		{
			LOGGER.error("init(): " + e.getMessage());

			// LOGGER.error(bse);
			throw new OpenEditException(e);
		}
		finally
		{
			Context.exit();
		}
	}
	public Scriptable getGlobalScope()
	{
		if( fieldGlobalScope == null)
		{
			try
			{
				Context context = Context.enter();
				fieldGlobalScope = new ImporterTopLevel(context);
			}
			catch (Exception e)
			{
				throw new OpenEditException(e);
			}
			finally
			{
				Context.exit();
			}
		}
		return fieldGlobalScope;
	}
	public Scriptable createScope()
	{
		try
		{
			Context context = Context.enter();
			
			Scriptable scope = context.newObject(getGlobalScope());
			scope.setPrototype(getGlobalScope());
			scope.setParentScope(null);
			return scope;
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		finally
		{
			Context.exit();
		}
	}

}
