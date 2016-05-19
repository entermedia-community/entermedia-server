package org.entermediadb.authenticate;

import org.openedit.WebPageRequest;

public interface AutoLoginProvider
{

	public boolean autoLogin(WebPageRequest inReq);
	
}
