package org.entermediadb.email;

import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.modules.BaseModule;
import org.openedit.page.Page;

public class MailTestModule  extends BaseModule{
	
	private PostMail postMail;
	
	protected void sendEmail(WebPageRequest inReq) throws OpenEditException
	{
		System.out.println("Send Email being accessed");
		Page page = inReq.getPage();
		
	}

	public PostMail getPostMail() {
		return postMail;
	}

	public void setPostMail(PostMail postMail) {
		this.postMail = postMail;
	}
}
