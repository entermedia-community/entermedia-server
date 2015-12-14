package org.entermediadb.modules.workflow;

import org.openedit.page.Page;

public interface WorkFlowListener
{
	public void pageApproved(Page inPage);
	public void pageDeleted(Page inPage);
}
