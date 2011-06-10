package org.entermedia.email;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.modules.BaseModule;
import com.openedit.page.Page;

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
