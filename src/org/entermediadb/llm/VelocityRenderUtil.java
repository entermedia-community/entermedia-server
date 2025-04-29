package org.entermediadb.llm;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.events.PathEventManager;
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

public class VelocityRenderUtil
{
	protected ModuleManager fieldModuleManager;
	protected PageManager fieldPageManager;
	protected RequestUtils fieldRequestUtils;
	protected OpenEditEngine fieldEngine;
	private static Log log = LogFactory.getLog(VelocityRenderUtil.class);
	

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
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

	public RequestUtils getRequestUtils()
	{
		return fieldRequestUtils;
	}

	public void setRequestUtils(RequestUtils inRequestUtils)
	{
		fieldRequestUtils = inRequestUtils;
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
	
	
}