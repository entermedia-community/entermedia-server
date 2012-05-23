package com.openedit.entermedia.scripts;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.util.GroovyScriptEngine;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.runtime.InvokerHelper;

import com.openedit.ModuleManager;
import com.openedit.OpenEditException;
import com.openedit.config.Configuration;
import com.openedit.page.Page;
import com.openedit.page.PageSettings;
import com.openedit.page.manage.PageManager;

public class GroovyScriptRunner implements ScriptRunner
{
	private static Log log = LogFactory.getLog(GroovyScriptRunner.class);
	protected PageManager fieldPageManager;
	protected ModuleManager fieldModuleManager;
	
	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager) {
		fieldModuleManager = inModuleManager;
	}

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
				Collection folders = loadPackages(page);
				//Need an array of classpaths for all the parent fallback folders
				
				GroovyScriptEngine engine = new GroovyScriptEngine((String[])folders.toArray(new String[folders.size()]));
				variableMap.put("engine", engine);
				
				GroovyClassLoader loader = engine.getGroovyClassLoader();

				Object returned = null;
				log.info(variableMap.keySet());
				if( inScript.getMethod() != null )
				{
					File file = new File(page.getContentItem().getAbsolutePath());
					//String text = DefaultGroovyMethods.getText(new FileInputStream(file), "UTF-8");
					Class scriptClass = loader.parseClass(file);
					
					 final GroovyObject object = (GroovyObject) scriptClass.newInstance();
					 InvokerHelper.setProperties(object,variableMap);
					
					 
					 for (Iterator iterator = inScript.getConfiguration().getChildIterator("property"); iterator.hasNext();) {
							Configuration beanconfig = (Configuration) iterator.next();
//							<!--
//							<property name="cookieEncryption">
//							<ref bean="stringEncryption" />
//						</property>
//							-->

							String name = beanconfig.getAttribute("name");
							Configuration ref = beanconfig.getChild("ref");
							if(ref != null){
								String beanid = ref.getAttribute("bean");
								Object bean = getModuleManager().getBean(beanid);
								object.setProperty(name, bean);		
							}
						
						}
					 
					returned = object.invokeMethod(inScript.getMethod(), variableMap.get("context"));
										  
				}
				else
				{
					Binding binding = new Binding();

					for (Iterator iterator = variableMap.keySet().iterator(); iterator.hasNext();)
					{
						String key = (String) iterator.next();
						binding.setProperty(key,variableMap.get(key));
					}
					String filename = page.getPath();
					String packageroot = page.getProperty("packageroot");
					if( packageroot == null )
					{
						packageroot = page.getDirectory();
					}
					filename = filename.substring(packageroot.length() + 1);
					log.info("Running " + filename);
					returned = engine.run(filename, binding); //Pass in only the script file name i.e. conversion/
				}
				return returned;
			}
			catch (OpenEditException e) 
			{
				throw (OpenEditException)e;
			} 
			catch (Exception e) 
			{
				throw new OpenEditException(e, inScript.getPage().getPath() );
			}
		
	}

	private Collection loadPackages(Page page)
	{
		String packageroot = page.getProperty("packageroot");
		//File parent = new File().getParentFile();
		if( packageroot == null)
		{
			log.info("Could not find package root on " + page.getPageSettings().getPath());
			packageroot = page.getDirectory();
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
		return folders;
	}

	public Object newInstance(Script inScript)
	{
		Page page = inScript.getPage();
		Collection folders = loadPackages(page);

		try
		{
			GroovyScriptEngine engine = new GroovyScriptEngine((String[])folders.toArray(new String[folders.size()]));
			
			GroovyClassLoader loader = engine.getGroovyClassLoader();
			
			Object returned = null;
			
			File file = new File(page.getContentItem().getAbsolutePath());
			//String text = DefaultGroovyMethods.getText(new FileInputStream(file), "UTF-8");
			Class scriptClass = loader.parseClass(file);
				
			GroovyObject object = (GroovyObject) scriptClass.newInstance(); //This may not be a real object if the script does not define a public class
			if(inScript.getConfiguration() != null){
			for (Iterator iterator = inScript.getConfiguration().getChildIterator("property"); iterator.hasNext();) {
				Configuration beanconfig = (Configuration) iterator.next();
//				<!--
//				<property name="cookieEncryption">
//				<ref bean="stringEncryption" />
//			</property>
//				-->

				String name = beanconfig.getAttribute("name");
				Configuration ref = beanconfig.getChild("ref");
				if(ref != null){
					String bean = beanconfig.getAttribute("bean");
					
				}
				Object bean = getModuleManager().getBean("bean");
				object.setProperty(name, bean);
			}
			}
			
			return object;
		}
		catch (Exception ex)
		{
			throw new OpenEditException(ex);
		}
	}

}
