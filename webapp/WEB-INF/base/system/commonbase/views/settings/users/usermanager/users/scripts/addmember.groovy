import org.openedit.Data
import org.openedit.OpenEditException
import org.openedit.data.Searcher
import org.openedit.users.Group
import org.openedit.users.User
import org.openedit.users.UserManager

import org.openedit.page.manage.*

public Group getGroup()
{
	String groupid = context.getRequestParameter("groupid");
	if( groupid == null)
	{
		return null
	}
	Group group = userManager.getGroup(groupid);
	if (group == null)
	{
		group = userManager.createGroup(groupid, catalogid);
	}
	
	return group;
}

public User createUser()
{
	String email = context.getRequestParameter("email.value");
	String emailcheck = context.getRequestParameter("emailmatch.value");
	
	if ((email != null && emailcheck != null) && !email.equals(emailcheck))
	{
		throw new OpenEditException("E-mail addresses don't match.");
	}
	UserManager um = userManager;
	
	User newuser = userManager.getUserByEmail(email);
	
	String password = context.getRequestParameter("password.value");
	String passwordcheck = context.getRequestParameter("passwordmatch.value");
	
	if( newuser == null)
	{
		//create new
		if( !password.equals(passwordcheck))
		{
			throw new OpenEditException("passwords don't match");
		}
			
	}
	String username = context.getRequestParameter("id.value");

	newuser = userManager.createUser( username, password);
	newuser.setVirtual(false);
	
	return newuser;
}

public Data saveUserProfile(String inUserId)
{
	Searcher userprofilesearcher = searcherManager.getSearcher(catalogid,"userprofile");
	
	hits = userprofilesearcher.fieldSearch("id", inUserId);
	def userprofile;
	if (hits.size() == 1)
	{
		userprofilehit = hits.get(0);
		userprofile = userprofilesearcher.searchById(userprofilehit.getId());
	}
	else
	{
		userprofile = userprofilesearcher.createNewData();
		userprofile.setId(inUserId);
		userprofile.setSourcePath(inUserId);
	}
		
	details = userprofilesearcher.getDetailsForView("userprofileedit", context.getPageValue("userprofile"));
	
	fieldlist = []
	
	details.each {
		fieldlist << it.id;
	}
	
	fields = fieldlist as String[]
	
	userprofilesearcher.saveDetails(context,fields,userprofile,userprofile.getId());
}

public void addUser()
{
	User newuser = createUser();
	Searcher usersearcher = searcherManager.getSearcher(catalogid,"user");
	
	def fields = context.getRequestParameters("field");//fieldlist as String[]
	
	usersearcher.saveDetails(context,fields,newuser,newuser.getId());
	
	saveUserProfile(newuser.getId());
	
	context.putPageValue("userName",newuser.getId());
	context.putPageValue("selectedUser",newuser);
	
//	MediaArchive
//	mediaArchive.fire
}

String method = context.getRequestParameter("method");
if (method == "adduser")
{
	addUser();
}
