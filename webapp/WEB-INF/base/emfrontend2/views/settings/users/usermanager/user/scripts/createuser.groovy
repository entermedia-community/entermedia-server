import org.openedit.Data
import org.openedit.OpenEditException
import org.openedit.data.Searcher
import org.openedit.users.Group
import org.openedit.users.User
import org.openedit.users.UserManager

import org.openedit.page.manage.*


public User createUser()
{
	String email = context.getRequestParameter("email.value");
	UserManager um = userManager;
	
	User newuser = userManager.getUserByEmail(email);
	if( newuser == null)
	{
		newuser = userManager.createUser( null, null);
		newuser.setEmail(email);
		newuser.setVirtual(false);
		userManager.saveUser(newuser);
	}
	context.putPageValue("userName",newuser.getId());
	context.putPageValue("selectedUser",newuser);
	return newuser;
}

createUser();
