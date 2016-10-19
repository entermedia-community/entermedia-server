/*
 * Created on May 19, 2006
 */
package org.entermediadb.modules.workflow;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.page.Page;
import org.openedit.page.PageAction;
import org.openedit.users.User;
import org.openedit.util.PathUtilities;

public class WorkFlowModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(WorkFlowModule.class);
	
	protected WorkFlow fieldWorkFlow;

	public List listDrafts(WebPageRequest inReq) throws OpenEditException
	{
		//search the entire site looking for drafts
		List drafts = getWorkFlow().listAllDrafts();
		inReq.putPageValue("drafts", drafts);
		inReq.putPageValue("workflow", getWorkFlow());
		return drafts;
	}
//	public void viewLiveMode(WebPageRequest inReq) 
//	{
//		inReq.getUser().put("oe_edit_mode","live");
//		redirectBack(inReq);		
//	}
	public void viewEditingMode(WebPageRequest inReq) 
	{
		inReq.getUser().setValue("oe_edit_mode","editing");
		inReq.getUser().setValue("showdebug","false");
		inReq.putSessionValue("oe_edit_mode", "editing");
		getUserManager(inReq).saveUser(inReq.getUser());
		redirectBack(inReq);		
	}
	public void viewDebugMode(WebPageRequest inReq) 
	{
		inReq.getUser().setValue("oe_edit_mode","debug");
		inReq.getUser().setValue("showdebug","true");
		inReq.putSessionValue("oe_edit_mode","debug");
		getUserManager(inReq).saveUser(inReq.getUser());
		redirectBack(inReq);		
	}
	public void viewPreviewMode(WebPageRequest inReq) 
	{
		inReq.getUser().setValue("oe_edit_mode","preview");
		inReq.getUser().setValue("showdebug","false");
		inReq.putSessionValue("oe_edit_mode","preview");

		//inReq.getUser().put("openadmintoolbar","false");
		getUserManager(inReq).saveUser(inReq.getUser());
		redirectBack(inReq);		
	}

	public void approveAll(WebPageRequest inReq) throws Exception
	{
		User user = inReq.getUser();
		getWorkFlow().approveAll(user);
		redirectBack(inReq);
	}
	public void approve(WebPageRequest inReq) throws Exception
	{
		String path = inReq.getRequestParameter("editPath");
		if(!path.contains(".draft.")){
			path = path.replace(".html", ".draft.");
		}

		if ( path.indexOf(".draft.") > -1)
		{
			getWorkFlow().approve(path, inReq.getUser() );
			
			redirectBack(inReq);
		}
	}
	public void deleteDraft(WebPageRequest inReq) throws OpenEditException
	{
		String path = inReq.getRequestParameter("editPath");

		if ( path.indexOf(".draft.") > -1)
		{
			getWorkFlow().deleteDraft(path, inReq.getUser() );
		}
		redirectBack(inReq);
	}
	protected void redirectBack(WebPageRequest inReq)
	{
		String redirect = inReq.getRequestParameter("origURL");
		if ( redirect != null)
		{
			if( redirect.indexOf("?") > -1)
			{
				inReq.redirect(redirect);
			}
			else
			{
				inReq.redirect(redirect + "?cache=false");
			}
		}
	}
	public WorkFlow getWorkFlow()
	{
		return fieldWorkFlow;
	}
	public void setWorkFlow(WorkFlow inWorkFlow)
	{
		fieldWorkFlow = inWorkFlow;
	}
	
	//Gets the pages for a comparison between the original page and the draft page.
	public void showDraftComparison(WebPageRequest inReq) throws OpenEditException
	{
		String draftpath = inReq.getRequestParameter("draftpath");
		String livepath = PathUtilities.createLivePath(draftpath);
		Page livepage = getPageManager().getPage(livepath);
		Page draftpage = getPageManager().getPage(draftpath);
		inReq.putPageValue("livepage", livepage);
		inReq.putPageValue("draftpage", draftpage);
		inReq.putPageValue("draftcontent", draftpage.getContentItem());
		//inReq.putPageValue("origcontent", origpage.getContentItem()	);
		inReq.putPageValue("parentName", inReq.getRequestParameter("parentName"));
		inReq.putPageValue("origURL", inReq.getRequestParameter("origURL"));
	}
	
	public void canApprove(WebPageRequest inReq) throws OpenEditException
	{
		Page editPage = (Page)inReq.getPageValue("editPage");
		//this is a draft I assume
		boolean can = getWorkFlow().canApprove(inReq.getUser(), editPage);
		inReq.putPageValue("canapprove", Boolean.valueOf(can));
	}
	
	public void runPath(WebPageRequest inReq) throws OpenEditException
	{
		PageAction action = inReq.getCurrentAction();
		String path = action.getConfig().getChildValue("path");
		Page page = getPageManager().getPage(path);
		if( page.exists() )
		{
			getModuleManager().executePathActions(page, inReq);
			getModuleManager().executePageActions(page, inReq);
			//TODO Now check for redirects?
		}
		else
		{
			log.error("No such page to run actions on " + path);
		}
	}
	public void addWorkFlowListener(WorkFlowListener inListener){
	getWorkFlow().addWorkFlowListener(inListener);
	}
	
	
	
}
