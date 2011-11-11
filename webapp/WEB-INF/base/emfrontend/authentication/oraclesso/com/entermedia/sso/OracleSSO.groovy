package com.entermedia.sso

import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Iterator

import javax.servlet.http.HttpServletRequest

import org.openedit.data.Searcher
import org.openedit.data.SearcherManager

import com.entermedia.soap.SoapUserManager
import com.google.gson.Gson
import com.openedit.ModuleManager
import com.openedit.WebPageRequest
import com.openedit.modules.update.Downloader
import com.openedit.users.Group
import com.openedit.users.User
import com.openedit.users.UserManager

public class Student
{
	public String lastName;
	public String email;
	public String personId;
	public String firstName;
}

public class ExerciseVO
{
	public String startDate;
	public String endDate;
	public String revealDate;
}

public class Team
{
	public String id;
	public String location;
	public String publicName;
	public String prettyPublicName;
	public List<Student> studentList;
	public ExerciseVO exerciseVO;
}

public class Status
{
	public String desc;
	public String ok;
}

public class TeamInfo
{
	public List<Team> teams;
	public Status status;
}

public class OracleSSO
{
	//2011-09-23T16:00:00-04:00
	protected String fieldDateFormat = "yyyy-MM-dd'T'HH:mm:ssZ";
	protected SimpleDateFormat fieldFormatter;
	protected UserManager fieldUserManager;
	protected ModuleManager fieldModuleManager;
	
	protected UserManager getUserManager() 
	{
		return fieldUserManager;
	}
	
	protected ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}
	
	protected void setModuleManager(ModuleManager um)
	{
		fieldModuleManager = um;
	}
	
	protected void setUserManager(UserManager um)
	{
		fieldUserManager = um;
	}
	
	protected SearcherManager getSearcherManager()
	{
		SearcherManager sm = (SearcherManager)getModuleManager().getBean("searcherManager");
		return sm;
	}
	
	protected SimpleDateFormat getFormatter()
	{
		if(fieldFormatter == null)
		{
			fieldFormatter = new SimpleDateFormat(fieldDateFormat);
		}
		return fieldFormatter;
	}
	
	protected Date parseTeamDate(String inDate)
	{
		//we need to remove the colon, simple date formatter doesn't handle a colon in the timezone properly
		int lastcolon = inDate.lastIndexOf(":");
		StringBuffer buf = new StringBuffer( inDate.length() - 1 );
		buf.append(inDate.substring(0, lastcolon)).append(inDate.substring(lastcolon + 1));
		Date date = getFormatter().parse(buf.toString());
		return date;
	}

	//go through list of teams in teaminfo
	protected void generateTeams(User inUser, TeamInfo inTeamInfo)
	{
		Searcher groupSearcher = getSearcherManager().getSearcher("system", "group");
		Searcher userSearcher = getSearcherManager().getSearcher("system", "user");
		Calendar today = new GregorianCalendar();
		for (Iterator iterator = inTeamInfo.teams.iterator(); iterator.hasNext();) 
		{
			Team team = (Team) iterator.next();
			//look up the group by name
			UserManager um = getUserManager();
			Group group = um.getGroup(team.id);
			if(group == null)
			{
				group = um.createGroup(team.id, team.publicName);
				
			}
			//check if the group should be enabled
			Date endDate = parseTeamDate(team.exerciseVO.endDate);
			Date revealDate = parseTeamDate(team.exerciseVO.revealDate);
			if(today.after(endDate) || today.before(revealDate))
			{
				group.setProperty("enabled", "false");
			}
			else
			{
				group.setProperty("enabled", "true");
			}
			groupSearcher.saveData(group, null);
			
			//add the user to the group
			inUser.addGroup(group);
			
			//create users for the team members if necessary
			for (Iterator iterator2 = team.studentList.iterator(); iterator2.hasNext();)
			{
				Student student = (Student) iterator2.next();
				//try to load the user
				User member = um.getUser(student.personId);
				if(member == null)
				{
					member = um.createUser(student.personId, null);
					member.setEmail(student.email);
					member.setFirstName(student.firstName);
					member.setLastName(student.lastName);
					member.addGroup(group);
					userSearcher.saveData(member, null);
				}
				else if( !member.isInGroup(group))
				{
					member.addGroup(group);
					userSearcher.saveData(member, null);
				}
			}
		}
	}

	protected String getContent(String inUrl)
	{
		Downloader dl = new Downloader();
		return dl.downloadToString(inUrl);
	}

	protected void oracleSsoLogin(WebPageRequest context)
	{
		
		String pid = context.getRequestParameter("HBS_PERSON_ID");
		if( pid == null)
		{
			HttpServletRequest requestHeader = context.getRequest();
			pid = requestHeader.getHeader("HBS_PERSON_ID");
		}
			//pid = "641292" cburkey;
		//pid = "615538";
		context.putPageValue("personid", pid);
		//String tid = requestHeader.getHeader("teamid");  //could be multiple teamids, guess need to split these
		//String teaminfo = "http://pine-stage.hbs.edu/teamMgmt/internal/ws.htm?action=getTeamInfo&teamId=498211";
		String personinfo = context.getPageProperty("teaminfourl") + "?action=getTeamInfo&personId=";
		
		String jsonteaminfo;
		try
		{
			jsonteaminfo = getContent(personinfo + pid);
		}
		catch(Exception e)
		{
			context.putPageValue("ssoerror", "Failed to logon using SSO");
		}
		
		context.putPageValue("jsonresponse", jsonteaminfo);
		
		Gson gson = new Gson();
		TeamInfo teams = gson.fromJson(jsonteaminfo, TeamInfo.class);
			
		//search for a user
		UserManager um = getUserManager();
		User user = um.getUser(pid);
		if(user == null)
		{
			user = um.createUser(pid, null);
			Group guestgroup = um.getGroup("guest");
			user.addGroup(guestgroup);
		}
		
		generateTeams(user, teams);
		saveUserData(pid, context);
		//um.saveUser(user);
		
		//auto login user
		context.putSessionValue("user", user);
		context.redirect("/hbs/index.html");
	}

	protected void saveUserData(String id, WebPageRequest context){
	//	GroovyClassLoader loader = engine.getGroovyClassLoader();
	//	Class groovyClass = loader.loadClass("com.entermedia.soap.SoapUserManager");
		SoapUserManager mgr =   new SoapUserManager();// groovyClass.newInstance();	
		mgr.setUserManager(getUserManager());
		mgr.setSearcherManager( getSearcherManager() );
		mgr.setXmlUtil(getModuleManager().getBean("xmlUtil"));
		mgr.setContext( context );
		mgr.updateUserByPersonId(id);
	}
}


