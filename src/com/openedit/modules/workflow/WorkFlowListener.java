package com.openedit.modules.workflow;

import com.openedit.page.Page;

public interface WorkFlowListener
{
	public void pageApproved(Page inPage);
	public void pageDeleted(Page inPage);
}
