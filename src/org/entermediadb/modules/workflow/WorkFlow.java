/*
 * Created on May 19, 2006
 */
package org.entermediadb.modules.workflow;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.OpenEditException;
import org.openedit.page.FileFinder;
import org.openedit.page.Page;
import org.openedit.page.PageProperty;
import org.openedit.page.manage.PageManager;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.XmlUtil;

public class WorkFlow
{
	protected File fieldRoot;
	protected PageManager fieldPageManager;
	protected UserManager fieldUserManager;
	protected int fieldLevelCount = 0;
	private static final Log log = LogFactory.getLog(WorkFlow.class);
	protected long fieldLastModified = -2;
	protected ArrayList fieldWorkFlowListeners;
	
	public List listAllDrafts() throws OpenEditException
	{
		FileFinder finder = new FileFinder();
		finder.setRoot(getRoot());
		finder.setPageManager(getPageManager());
		finder.addSkipFileName("*.xconf");
		finder.addSkipFileName("WEB-INF");
		finder.addSkipFileName("*/WEB-INF/*");
		return finder.findPages("*.draft.*");
	}
	public String getUserDescription( String inUserName )
	{
		if ( inUserName == null || inUserName.length() == 0)
		{
			return "";
		}
		User user = getUserManager().getUser(inUserName);
		if ( user != null)
		{
			return user.getShortDescription();
		}
		return "";
	}
	public Page getOriginalPage(Page inDraft) throws OpenEditException
	{
		String dpath = inDraft.getPath();
		int index = dpath.indexOf(".draft.");
		String path = dpath.substring(0,index);
		path = path + dpath.substring(index+6);
		
		Page org = getPageManager().getPage(path);
		return org;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	public File getRoot()
	{
		return fieldRoot;
	}
	public void setRoot(File inRoot)
	{
		fieldRoot = inRoot;
	}
	public UserManager getUserManager()
	{
		return fieldUserManager;
	}
	public void setUserManager(UserManager inUserManager) 
	{
		fieldUserManager = inUserManager;
	}

	private void saveLevel(Page draft, int newLevel) throws OpenEditException
	{
		PageProperty property = new PageProperty("approve.level");
		property.setValue(String.valueOf(newLevel ));
		draft.getPageSettings().putProperty(property);
		//save
		getPageManager().getPageSettingsManager().saveSetting(draft.getPageSettings());
	}
	protected void loadLevelCount() throws OpenEditException
	{
		Page level = getPageManager().getPage("/openedit/components/html/workflow/settings.xml");
		if( level.getLastModified().getTime() != fieldLastModified)
		{
			fieldLastModified = level.getLastModified().getTime();
			Element root = new XmlUtil().getXml(level.getReader(), "UTF-8");
			String text = root.elementText("levels");
			if( text != null)
			{
				setLevelCount(Integer.parseInt(text));
			}
		}
	}
	public int findExistingLevel( Page inDraft)
	{
		String existingLevel = inDraft.get("approve.level");
		int oldLevel = 0;
		if( existingLevel != null)
		{
			oldLevel = Integer.parseInt(existingLevel);
		}
		return oldLevel;
	}
	public boolean canApprove( User inUser, Page inDraft) throws OpenEditException
	{
		if( inDraft.isDraft() && !inDraft.exists())
		{
			return false;
		}
		
		if (inUser == null)
		{
			return false;
		}
		
		loadLevelCount();
		if( getLevelCount() == 0)
		{
			if( inUser.hasPermission("oe_edit_approves") )
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		int power = findHighestApproval(inUser);
		if( power == 0)
		{
			return false;
		}
		int existing = findExistingLevel(inDraft);
		if( power > existing)
		{
			return true;
		}
		return false;
	}
	
	public int findHighestApproval(User inUser )
	{
		int level = 0;
		for (int i = 1; i < getLevelCount()+1; i++)
		{
			if( inUser.hasPermission("oe_edit_approve_level" + i) )
			{
				level = i;
			}
		}
		return level;
	}
	
	public void deleteDraft(String inPath, User inUser) throws OpenEditException
	{
		Page page = getPageManager().getPage(inPath);
		page.getContentItem().setAuthor(inUser.getUserName());
		page.getContentItem().setMessage("Deleted Draft");
		getPageManager().removePage(page);
		firePageDeleted(page);
	}
	public int getLevelCount()
	{
		return fieldLevelCount;
	}
	public void setLevelCount(int inLevelCount)
	{
		fieldLevelCount = inLevelCount;
	}
	public ArrayList getWorkFlowListeners()
	{
	if (fieldWorkFlowListeners == null)
	{
		fieldWorkFlowListeners = new ArrayList();
		
	}

	return fieldWorkFlowListeners;
	}
	public void setWorkFlowListeners(ArrayList inWorkFlowListeners)
	{
		fieldWorkFlowListeners = inWorkFlowListeners;
	}
	protected void firePageApproved( Page inPage )
	{
		for ( Iterator iter = getWorkFlowListeners().iterator(); iter.hasNext(); )
		{
			WorkFlowListener listener = (WorkFlowListener) iter.next();
			listener.pageApproved(inPage);
		}
	}
	protected void firePageDeleted( Page inPage )
	{
		for ( Iterator iter = getWorkFlowListeners().iterator(); iter.hasNext(); )
		{
			WorkFlowListener listener = (WorkFlowListener) iter.next();
			listener.pageDeleted(inPage);
		}
	}
	
	public void addWorkFlowListener(WorkFlowListener inListener){
		getWorkFlowListeners().add(inListener);
	}
}
