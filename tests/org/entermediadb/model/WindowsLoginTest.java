package org.entermediadb.model;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.openedit.users.Authenticator;
import org.openedit.users.BaseUser;
import org.openedit.users.User;
import org.openedit.users.authenticate.AuthenticationRequest;


public class WindowsLoginTest extends BaseEnterMediaTest 
{
	public WindowsLoginTest(String inName) {
		super(inName);
		// TODO Auto-generated constructor stub
	}

	public void testWindows() throws Exception
	{
		//WindowsAuthentication auth = new WindowsAuthentication();
		
		Authenticator authen = (Authenticator)getFixture().getModuleManager().getBean("authenticator");
		
		AuthenticationRequest aReq = new AuthenticationRequest();
		
		User user = getFixture().getUserManager().getUser("TesterTwo");
		if( user == null)
		{
			user = new BaseUser();
			user.setUserName("TesterTwo");
		}
		aReq.setUser(user);
		aReq.setPassword("test");
		aReq.putProperty("authenticationdomain","Mshome");
		
		aReq.putProperty("authenticationserver","192.168.1.6");
		
		boolean ok = authen.authenticate(aReq);
		assertTrue( ok );
	}
}
