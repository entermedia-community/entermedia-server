package org.entermediadb.scripts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.config.Configuration;
import org.openedit.config.ScriptPathLoader;
import org.openedit.page.Page;
import org.openedit.page.PageSettings;
import org.openedit.page.manage.PageManager;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.util.GroovyScriptEngine;

public class GroovyScriptRunner implements ScriptRunner
{
	private static Log log = LogFactory.getLog(GroovyScriptRunner.class);
	protected PageManager fieldPageManager;
	protected ModuleManager fieldModuleManager;
	protected File fieldRoot;
	protected Map<String,GroovyScriptEngine> fieldEnginesByPackage;
	
	public void clearCache()
	{
		if( fieldEnginesByPackage != null )
		{
			fieldEnginesByPackage.clear();
		}
	}
	
	public GroovyScriptEngine getEngine(String inPackageRoot)
	{
		if( fieldEnginesByPackage == null )
		{
			fieldEnginesByPackage = new HashMap();
		}
		GroovyScriptEngine engine = fieldEnginesByPackage.get(inPackageRoot);
		if( engine == null )
		{
			Collection folders = loadPackages(inPackageRoot);
			try
			{
				engine = new GroovyScriptEngine((String[])folders.toArray(new String[folders.size()]));
			}
			catch (IOException e)
			{
				throw new OpenEditException(e);
			}
			fieldEnginesByPackage.put(inPackageRoot, engine);
		}

		return engine;
	}
	
	public File getRoot()
	{
		return fieldRoot;
	}

	public void setRoot(File inRoot)
	{
		fieldRoot = inRoot;
	}

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
			if( !page.exists() )
			{
				throw new OpenEditException("Script not found : " + page.getPath() );
			}
			try 
			{
				
				
				
				Object returned = null;

//				removed, use Spring
//				if( inScript.getMethod() != null )
//				{
//					File file = new File(page.getContentItem().getAbsolutePath());
//					//String text = DefaultGroovyMethods.getText(new FileInputStream(file), "UTF-8");
//					Class scriptClass = loader.parseClass(file);
//					
//					 final GroovyObject object = (GroovyObject) scriptClass.newInstance();
//					 InvokerHelper.setProperties(object,variableMap);
//					
//					 
//					 for (Iterator iterator = inScript.getConfiguration().getChildIterator("property"); iterator.hasNext();) {
//							Configuration beanconfig = (Configuration) iterator.next();
////							<!--
////							<property name="cookieEncryption">
////							<ref bean="stringEncryption" />
////						</property>
////							-->
//
//							String name = beanconfig.getAttribute("name");
//							Configuration ref = beanconfig.getChild("ref");
//							if(ref != null){
//								String beanid = ref.getAttribute("bean");
//								Object bean = getModuleManager().getBean(beanid);
//								object.setProperty(name, bean);		
//							}
//						
//						}
//					 
//					returned = object.invokeMethod(inScript.getMethod(), null);
//										  
//				}
//				else
//				{
					Binding binding = new Binding();

					for (Iterator iterator = variableMap.keySet().iterator(); iterator.hasNext();)
					{
						String key = (String) iterator.next();
						Object val = variableMap.get(key);
						if(val != null && key != null){
						binding.setProperty(key,val);
						}
					}
					String filename = page.getPath();
					String packageroot = page.getProperty("packageroot");
					if( packageroot == null )
					{
						packageroot = page.getDirectory();
					}
					filename = filename.substring(packageroot.length() + 1);
					if( log.isDebugEnabled() )
					{
						log.debug("Running " + filename);
					}
					GroovyScriptEngine engine = getEngine(packageroot);
					binding.setProperty("engine", engine);
					
					returned = engine.run(filename, binding); //Pass in only the script file name i.e. conversion/
//				}
				return returned;
			}
			catch (OpenEditException e) 
			{
				throw (OpenEditException)e;
			} 
			catch (Exception e) 
			{
				log.error("Could not run script: " + inScript.getPage().getPath(), e);
				throw new OpenEditException(e, inScript.getPage().getPath() );
			}
		
	}

	protected Collection loadPackages(String inPackageRoot)
	{
		//catalog Events scripts
		Page proot = getPageManager().getPage(inPackageRoot);
		List folders = new ArrayList();
		folders.add(proot.getContentItem().getAbsolutePath() + "/");
		for( Object parent: proot.getPageSettings().getFallBacks() )
		{
			String path = ((PageSettings)parent).getPath();
			path = path.replace("/_site.xconf", "");
			path = path.replace(".xconf", "/");
			
			path = getPageManager().getPage(path).getContentItem().getAbsolutePath();
			path = path + "/";
			if( !folders.contains(path))
			{
				folders.add(path);
			}
		}
		ScriptPathLoader loader = new ScriptPathLoader();
		loader.setRoot(getRoot());
		List basefolders = loader.findPaths();
		folders.addAll(basefolders);
		return folders;
	}

	//Use Spring
	
	public Object newInstance(Script inScript)
	{
		Page page = inScript.getPage();
		Collection folders = loadPackages(page.getPath());

		try
		{
			GroovyScriptEngine engine = new GroovyScriptEngine((String[])folders.toArray(new String[folders.size()]));
			
			GroovyClassLoader loader = engine.getGroovyClassLoader();
			
			Object returned = null;
			
			File file = new File(page.getContentItem().getAbsolutePath());
			//String text = DefaultGroovyMethods.getText(new FileInputStream(file), "UTF-8");
			Class scriptClass = loader.parseClass(file);
			
			if( log.isDebugEnabled() )
			{
				log.debug("Parsing a class " + file);
			}
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
