package org.entermediadb.modules;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.entermediadb.asset.modules.AutoCompleteModule;
import org.junit.Test;
import org.openedit.BaseWebPageRequest;
import org.openedit.WebPageRequest;
import org.openedit.hittracker.HitTracker;
import org.openedit.users.BaseUser;
import org.openedit.users.User;

public class AutoCompleteModuleTest extends BaseEnterMediaTest
{

	@Test
	public void testMyGroupSuggestions()
	{
		AutoCompleteModule module = new AutoCompleteModule(){

//			@Override
//			protected Collection<String> extractDuplicates(WebPageRequest inReq)
//			{
//				return Collections.EMPTY_LIST;
//			}
			
		};
		assertNotNull("NULL module", module);
		HitTracker hit = null;
		WebPageRequest inReq = new BaseWebPageRequest(){

			@Override
			public User getUser()
			{
				User user = new BaseUser();
				return user;
			}

			@Override
			public String findValue(String inName)
			{
				return "foo";
			}
			
		};
		hit = module.myGroupSuggestions(inReq);
		assertNotNull("NULL hit result", hit);
	}
	
	public void terstNoDuplicateGroups() throws Exception
	{
		WebPageRequest req = getFixture().createPageRequest("/entermedia/index.html");
		AutoCompleteModule module = new AutoCompleteModule();
		HitTracker hit = module.myGroupSuggestions(req);
		assertNotNull("NULL hit result", hit);
	}

}
