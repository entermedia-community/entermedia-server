import javax.servlet.http.HttpServletRequest;
import com.entermedia.sso.OracleSSO;

protected void verifyHeaders()
{
	String pid = context.getRequestParameter("HBS_PERSON_ID");
	if( pid == null)
	{
		HttpServletRequest requestHeader = context.getRequest();
		pid = requestHeader.getHeader("HBS_PERSON_ID");
	}
	//check if matches logged in user
	String loggedinid = context.getUser().getId();
	if(pid != loggedinid && loggedinid != 'admin')
	{
		autologin = new OracleSSO();
		autologin.setUserManager(userManager);
		autologin.setModuleManager(moduleManager);
		autologin.oracleSsoLogin(context);
		log.info("You were not who you said you were.");
	}
}

verifyHeaders();