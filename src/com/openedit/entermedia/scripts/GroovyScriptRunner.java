package com.openedit.entermedia.scripts;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.util.GroovyScriptEngine;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.page.PageSettings;
import com.openedit.page.manage.PageManager;

public class GroovyScriptRunner implements ScriptRunner
{
	private static Log log = LogFactory.getLog(GroovyScriptRunner.class);
	protected PageManager fieldPageManager;
	
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	@Override
	public Object exec(final Script inScript, Map variableMap) throws OpenEditException
	{
			Page page = inScript.getPage();
			try {
				String packageroot = page.getProperty("packageroot");
				//File parent = new File().getParentFile();
				if( packageroot == null)
				{
					packageroot = inScript.getPage().getDirectory();
				}
				Page proot = getPageManager().getPage(packageroot);
				List folders = new ArrayList();
				folders.add(proot.getContentItem().getAbsolutePath() + "/");
				for( Object parent: proot.getPageSettings().getFallBacks() )
				{
					String path = ((PageSettings)parent).getPath();
					path = path.replace("/_site.xconf", "");
					path = path.replace(".xconf", "/");
					
					path = getPageManager().getPage(path).getContentItem().getAbsolutePath();
					if( !folders.contains(path))
					{
						folders.add(path + "/");
					}
				}
				//Need an array of classpaths for all the parent fallback folders
				
				GroovyScriptEngine engine = new GroovyScriptEngine((String[])folders.toArray(new String[folders.size()]));
				variableMap.put("engine", engine);
				
				String filename = page.getPath();
				filename = filename.substring(packageroot.length() + 1);
				
				GroovyClassLoader loader = engine.getGroovyClassLoader();

				Object returned = null;
				
				if( inScript.getMethod() != null )
				{
					File file = new File(page.getContentItem().getAbsolutePath());
					//String text = DefaultGroovyMethods.getText(new FileInputStream(file), "UTF-8");
					Class scriptClass = loader.parseClass(file);
					
					 final GroovyObject object = (GroovyObject) scriptClass.newInstance();
					 InvokerHelper.setProperties(object,variableMap);
					
					returned = object.invokeMethod(inScript.getMethod(), null);
										  
				}
				else
				{
					Binding binding = new Binding();

					for (Iterator iterator = variableMap.keySet().iterator(); iterator.hasNext();)
					{
						String key = (String) iterator.next();
						binding.setProperty(key,variableMap.get(key));
					}

					returned = engine.run(filename, binding);
				}
				return returned;
			}
			catch (OpenEditException e) 
			{
				throw (OpenEditException)e;
			} 
			catch (Exception e) 
			{
				throw new OpenEditException(e);
			}
		
	}


}
