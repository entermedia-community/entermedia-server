package org.entermediadb.llm;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.events.PathEventManager;
import org.json.simple.JSONObject;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;
import org.openedit.page.PageRequestKeys;
import org.openedit.page.PageStreamer;
import org.openedit.page.manage.PageManager;
import org.openedit.servlet.OpenEditEngine;
import org.openedit.users.User;
import org.openedit.util.OutputFiller;
import org.openedit.util.RequestUtils;
import org.openedit.util.URLUtilities;

public abstract class BaseLLMManager implements LLMManager {
	private static Log log = LogFactory.getLog(LLMManager.class);

	protected ModuleManager fieldModuleManager;
	protected PageManager fieldPageManager;
	protected RequestUtils fieldRequestUtils;
	OutputFiller filler = new OutputFiller();
	protected OpenEditEngine fieldEngine;

	public ModuleManager getModuleManager() {
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager) {
		fieldModuleManager = inModuleManager;
	}

	public RequestUtils getRequestUtils() {
		return fieldRequestUtils;
	}

	public void setRequestUtils(RequestUtils inRequestUtils) {
		fieldRequestUtils = inRequestUtils;
	}

	public BaseLLMManager() {
		super();
	}

	public PageManager getPageManager() {
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager) {
		fieldPageManager = inPageManager;
	}

	public OpenEditEngine getEngine() {
		if (fieldEngine == null) {
			fieldEngine = (OpenEditEngine) getModuleManager().getBean("OpenEditEngine");

		}

		return fieldEngine;
	}

	public String loadInputFromTemplate(WebPageRequest inReq, String inTemplate) {
		return loadInputFromTemplate(inReq, inTemplate, new HashMap());
	}

	public String loadInputFromTemplate(WebPageRequest inReq, String inTemplate, Map inMap) {
		if(inTemplate == null) {
			throw new OpenEditException("Cannot load input, template is null" + inReq);
		}
		try {
			URLUtilities urlUtil = (URLUtilities) inReq.getPageValue(PageRequestKeys.URL_UTILITIES);

			User user = inReq.getUser();
			Page template = getPageManager().getPage(inTemplate);
			log.info("Loading input: " + inTemplate);
			WebPageRequest	request = inReq.copy(template);
			PathEventManager manager = (PathEventManager) getModuleManager().getBean( "pathEventManager");
			
			StringWriter output = new StringWriter();
			request.setWriter(output);
			PageStreamer streamer = getEngine().createPageStreamer(template, request);
				getEngine().executePathActions(request);
				if( !request.hasRedirected())
				{
					getModuleManager().executePageActions( template,request );
				}
				if( request.hasRedirected())
				{
					log.info("action was redirected");
				}
			
			streamer.include(template, request);
			String string = output.toString();
			//log.info(inTemplate +" Output: " + string);
			return string;
		} catch (OpenEditException e) {
			throw e;
		} 
	}
	
	
	public LLMResponse callFunction(WebPageRequest inReq, String inModel, String inFunction, String inQuery, int temp, int maxtokens) throws Exception {
		return callFunction(inReq, inModel, inFunction, inQuery, temp, maxtokens, null);
	}
	
	
	public int copyData(JSONObject source, Data data)
	{
		int i = 0; 
		Map metadata =  (Map) source.get("metadata");
		for (Iterator iterator = metadata.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			Object value = metadata.get(key);
			
			if (key.equals("googlekeywords") ||  data.getValue(key) == null )
			{
				data.setValue(key, value);
				i ++;
			}
		}
		return i;
	}
	
	
}