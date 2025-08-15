package org.entermediadb.mcp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.ai.llm.VelocityRenderUtil;
import org.entermediadb.asset.MediaArchive;
import org.openedit.Generator;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.generators.Output;
import org.openedit.page.Page;

/**
 * Basic SSE-compatible generator that bypasses Velocity to stream live events.
 */
public class McpGenerator implements Generator
{
	protected ModuleManager fieldModuleManager;
	protected VelocityRenderUtil fieldRender;
	protected String fieldName;
	private static final Log log = LogFactory.getLog(McpGenerator.class);

	public VelocityRenderUtil getRender()
	{
		return fieldRender;
	}

	public void setRender(VelocityRenderUtil inRender)
	{
		fieldRender = inRender;
	}

	@Override
	public void generate(WebPageRequest inReq, Page inPage, Output inOut) throws OpenEditException {
		String catalogid = inReq.findPathValue("catalogid");
		MediaArchive archive = (MediaArchive) getModuleManager().getBean(catalogid, "mediaArchive");
    	McpManager manager = (McpManager) archive.getBean("mcpManager");
        String sessionId = inReq.getRequest().getSession().getId();
		try {
	        String method = inReq.getRequest().getMethod();
	        
	        // Handle SSE connect (GET)
	        if ("GET".equalsIgnoreCase(method)) {
	        	// /sse/somekey
	        	
		       
	        	 
	        	
	        	McpConnection connection = manager.createConnection(archive,inReq);
	        	
	        	//This won't ever return - it stays alive indefinitely
	        	
	        	
	        	
	        	
	            return;    

	         }
	        


	    }
	    catch (Exception ex) {
	    	manager.removeConnection(sessionId);
	        log.error("Error in MCP stream- Client likely closed" );
	        ex.printStackTrace();
	     
	      //  throw new OpenEditException("Error in MCP stream", ex);
	    }
	}

	@Override
	public boolean canGenerate(WebPageRequest inReq)
	{
		return true;
	}

	@Override
	public String getName()
	{
		return fieldName;
	}

	@Override
	public void setName(String inName)
	{
		fieldName = inName;
	}

	@Override
	public boolean hasGenerator(Generator inChild)
	{
		return false;
	}

	public ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inModuleManager)
	{
		fieldModuleManager = inModuleManager;
	}
}
