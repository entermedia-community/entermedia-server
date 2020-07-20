import org.entermediadb.email.PostMail
import org.entermediadb.email.TemplateWebEmail
import org.openedit.*
import org.openedit.data.Searcher
import org.openedit.users.*
import org.openedit.util.DateStorageUtil

import org.openedit.BaseWebPageRequest
import org.openedit.hittracker.*
import org.openedit.users.authenticate.PasswordGenerator
import org.openedit.util.Exec
import org.openedit.util.ExecResult
import org.openedit.util.RequestUtils
public void init() 
{
	String catalogid = "sitemanager/catalog";

	String email = context.getRequestParameter("email.value");
	
	
	User teamuser = mediaarchive.getUserManager().getUserByEmail(email);
	if( teamuser == null) 
	{
		context.putPageValue("result","true");
	}
	else {
		context.putPageValue("result","false");  //not valid
	}
	
}
init();