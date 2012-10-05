package org.openedit.entermedia.model;

import org.openedit.entermedia.BaseEnterMediaTest;

import com.openedit.WebPageRequest;

public class PermissionTest extends BaseEnterMediaTest
{
	public void testPermissionLoading() throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/testcatalog/components/upload/types/html5/finish.html");
		getFixture().getEngine().executePathActions(req);
		assertFalse( req.hasRedirected() );
		assertTrue( (Boolean)req.getPageValue("canview") );
	}
}
