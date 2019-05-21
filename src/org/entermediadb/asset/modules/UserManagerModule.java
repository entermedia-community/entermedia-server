/*
 * Created on Dec 20, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.entermediadb.asset.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.email.PostMail;
import org.entermediadb.email.TemplateWebEmail;
import org.entermediadb.modules.admin.users.PasswordMismatchException;
import org.entermediadb.modules.admin.users.PropertyContainerManipulator;
import org.entermediadb.modules.admin.users.Question;
import org.entermediadb.modules.admin.users.QuestionArchive;
import org.openedit.BaseWebPageRequest;
import org.openedit.Data;
import org.openedit.MultiValued;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.hittracker.HitTracker;
import org.openedit.page.Page;
import org.openedit.page.PageAction;
import org.openedit.page.Permission;
import org.openedit.page.XconfConfiguration;
import org.openedit.repository.filesystem.StringItem;
import org.openedit.users.Group;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.users.UserManagerException;
import org.openedit.users.authenticate.PasswordGenerator;
import org.openedit.util.PathUtilities;
import org.openedit.util.strainer.Filter;
import org.openedit.util.strainer.FilterReader;
import org.openedit.util.strainer.GroupFilter;
import org.openedit.util.strainer.OrFilter;

/**
 * @author Matthew Avery, mavery@einnovation.com
 *  
 */
public class UserManagerModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog( UserManagerModule.class );

	public static final String GROUPS = "groups";
	public static final String USERNAMES = "usernames";
	public static final String USERMANAGER = "UserManager";
	public static final String GROUP_ID_PARAMETER = "groupid";
	public static final String GROUP_NAME_PARAMETER = "groupname";
	public static final String USERNAME_PARAMETER = "username";

	protected PropertyContainerManipulator fieldPropertyManipulator;
	protected PostMail postMail;
	
	protected QuestionArchive fieldQuestionArchive;
	
	public void createGroup( WebPageRequest inReq ) throws OpenEditException
	{
		checkAdminPermission(inReq);

		String name = inReq.getRequiredParameter( GROUP_NAME_PARAMETER);

		try
		{
			String id = PathUtilities.extractId(name,false);
			inReq.setRequestParameter("groupid", id);
			Group group = getUserManager(inReq).createGroup( id, name );

			//	We no longer have standard properties, now we have standard
			// permissions (e.g. wsp.edit.notify) instead
			for ( Iterator iter = inReq.getParameterMap().entrySet().iterator(); iter.hasNext(); )
			{
				Map.Entry entry = (Map.Entry) iter.next();

				if (entry.getKey().toString().startsWith( "value-" ))
				{
					String propertyName = entry.getKey().toString().substring( 6 );
					group.addPermission( propertyName );
				}
			}
			getGroupSearcher(inReq).saveData(group, inReq.getUser());
		}
		catch( UserManagerException ume )
		{
			throw new OpenEditException( ume );
		}
	}
	public void saveGroupPermissions(WebPageRequest inReq)
	{
		String catalogid = inReq.getRequestParameter("catalogid");
		if(catalogid== null)
		{
			return;
		}
		String groupid = inReq.getRequestParameter("groupid");
		String path = "/" + catalogid + "/_site.xconf";
		Page page = getPageManager().getPage(path);
		List permissions = page.getPermissions();

		List localperms = page.getPageSettings().getFieldPermissions();
		if(localperms == null)
		{
			localperms = new ArrayList();
		}
		
		String[] fields = inReq.getRequestParameters("field");
		//get permissions list
		Searcher permsearcher = getSearcherManager().getSearcher(catalogid, "permissions");
		HitTracker permhits = permsearcher.getAllHits();
		
		for(int i=0;i<fields.length;i++)
		{
			String name = fields[i];
			boolean found = false;
			String permvalue = inReq.getRequestParameter(name+ "_value");
			
			Permission localperm = page.getPageSettings().getLocalPermission(name);
			Filter fil = null;
			if(localperm == null)
			{
				localperm = new Permission();
				localperm.setName(name);
				localperm.setPath(path);
				Permission currentperm = page.getPageSettings().getPermission(name);
				if(currentperm == null)
				{
					//create new permission if it is enabled for this group
					if("true".equals(permvalue))
					{
						OrFilter orf = new OrFilter();
						orf.addFilter(new GroupFilter(groupid));
						localperm.setRootFilter(orf);
						
						//we are done with this permission now.
						continue;
					}
				}
				else
				{
					//copy fallback permission into local permission
					fil = currentperm.getRootFilter();
					if(fil==null)
					{
						fil = new OrFilter();
						localperm.setRootFilter(fil);
					}
					else
					{
						FilterReader reader = (FilterReader) getModuleManager().getBean("filterReader");
						localperm.setRootFilter(fil.copy(reader, name));
						fil = localperm.getRootFilter();
					}
				}
				page.getPageSettings().addPermission(localperm);
			}
			else
			{
				//permission already exists locally
				fil = localperm.getRootFilter();
			}
			//fil should (and MUST) now be an OrFilter that will hold our GroupFilters
			if(!(fil instanceof OrFilter))
			{
				log.info("Trying to save an abnormal permission: " + name);
				continue;
			}
			Filter[] filters = fil.getFilters();
			//if permission box was checked:
			if("true".equals(permvalue))
			{
				//Check if there is already a group filter for this group.
				boolean alreadyset = false;
				boolean abnormal = false;
				if(filters != null)
				{
					for (int j = 0; j < filters.length; j++)
					{
						if(filters[j] instanceof GroupFilter)
						{
							GroupFilter gf = (GroupFilter)filters[j];
							if(gf.getGroupId().equals(groupid))
							{
								alreadyset = true;
								break;
							}
						}
						else
						{
							abnormal = true;
							break;
						}
					}
				}
				if(abnormal)
				{
					log.info("Trying to save an abnormal permission: " + name);
					continue;
				}
				//if not, add a new groupfilter
				if(!alreadyset)
				{
					GroupFilter newfilter = new GroupFilter();
					newfilter.setGroupId(groupid);
					fil.addFilter(newfilter);
				}
			}
			//if permission box was not checked:
			else
			{
				boolean abnormal = false;
				//look for a groupfilter for this group.  if found, remove it.
				if(filters != null)
				{
					for (int j = 0; j < filters.length; j++)
					{
						if(filters[j] instanceof GroupFilter)
						{
							GroupFilter gf = (GroupFilter)filters[j];
							if(gf.getGroupId().equals(groupid))
							{
								fil.removeFilter(filters[j]);
								break;
							}
						}
						else
						{
							abnormal = true;
							break;
						}
					}
				}
				if(abnormal)
				{
					log.info("Trying to save an abnormal permission: " + name);
					continue;
				}
			}
					
		}
		getPageManager().saveSettings(page);
	}
	public void createGroupProperties( WebPageRequest inReq ) throws OpenEditException,
			UserManagerException
	{
		checkAdminPermission(inReq);
		Group group = getGroup( inReq );
		getPropertyContainerManipulator().createProperties( inReq.getParameterMap(),
			group.getProperties() );
		getUserManager(inReq).saveGroup( group );
	}
	public void createGuestAccount(WebPageRequest inReq) throws OpenEditException
	{
		String allow = inReq.getPage().get("allowguestregistration");
		if ( !Boolean.parseBoolean( allow ) )
		{
			throw new OpenEditException("Guest registration not allowed.");
		}
		else
		{
			boolean checkanswer = checkQuestion(inReq);
			if( !checkanswer)
			{
				return;
			}

			User newUser = null;
			
			String email = inReq.getRequestParameter( "email.value" );
			if( email == null)
			{
				email = inReq.getRequestParameter( "value-email" );
			}
			if(newUser == null && email != null)
			{
				newUser = getUserSearcher(inReq).getUserByEmail(email);
			}
			
			if( newUser !=null)
			{
				inReq.putPageValue("newuser", newUser);
				String password = "";
				if (newUser.getPassword().startsWith("DES:"))
				{
					password = getUserManager(inReq).getStringEncryption().decrypt(newUser.getPassword());
				} 
				else
				{
					password = newUser.getPassword();
				}
				inReq.putPageValue("password", password);

				emailPassword(newUser, inReq);
				return; 
			}
			
			String password = new PasswordGenerator().generate();//Integer.toString((int)(100000 + generator.nextDouble() * 899999D));
			
			newUser = getUserManager(inReq).createUser(null, password); //username may be null, in fact it always is
			newUser.setPassword(password);
			getPropertyContainerManipulator().updateProperties( inReq.getParameterMap(),
							newUser.getProperties() );
			inReq.putPageValue("password", password);
			newUser.setValue("refererurl", inReq.getSessionValue("refererurl"));
			
			Group group = getGroupSearcher(inReq).getGroup("guest");
			if ( group == null)
			{
				group = getUserManager(inReq).createGroup("guest", "Guest");
			}
			newUser.addGroup(group);
			
			//add to referral group if the original entry page has the referred parameter
			String entryPage = (String)inReq.getSessionValue("fullOriginalEntryPage");
			if(entryPage != null && entryPage.contains("referred=true"))
			{
				String referredGroupId = inReq.getContentProperty("referredgroup");
				if(referredGroupId != null)
				{
					//search for the media group if it exists add the user
					Group referredGroup = getUserManager(inReq).getGroup(referredGroupId);
					if(referredGroup != null)
					{
						newUser.addGroup(referredGroup);
					}
				}
			}
			
			getUserSearcher(inReq).saveData(newUser,inReq.getUser());
			getUserManager(inReq).saveGroup(group);
			
			inReq.putPageValue("newuser", newUser);
			emailPassword(newUser, inReq);
		}
	}
	
	public void emailPassword(User inNewUser, WebPageRequest inReq) throws OpenEditException
	{
		String email = inReq.getRequestParameter( "email.value" );
		if( email == null)
		{
			email = inReq.getRequestParameter( "value-email" );
		}
		try
		{
			String template = inReq.findValue("email-template");
			if(template == null || template.length() == 0)
			{
				return;
			}
			
			String md5 = getUserManager(inReq).getStringEncryption().getPasswordMd5(inNewUser.getPassword());
			
			inReq.putPageValue("entermediakey", inNewUser.getUserName() + "md542" + md5);

			TemplateWebEmail mailer = postMail.getTemplateWebEmail();
			//String email = inReq.getRequestParameter("value-email");
			mailer.configureAndSend(inReq, template, email);
			
			//email admin to let them know a user has registered
			template = inReq.findValue("admin-email-template");
			if(template == null || template.length() == 0)
			{
				return;
			}
			StringBuffer admins = new StringBuffer();
			String sendtogroup = inReq.findValue("sendnotificationgroup");
			if( sendtogroup == null)
			{
				sendtogroup = "administrators";
			}
			Collection users = getUserManager(inReq).getUsersInGroup(sendtogroup);
			for (Iterator iterator = users.iterator(); iterator.hasNext();)
			{
				User user = (User) iterator.next();
				if( user.getEmail() != null)
				{
					if( admins.length() > 0)
					{
						admins.append(",");
					}
					admins.append(user.getEmail());					
				}
			}
			if( admins.length() == 0)
			{
				return;
			}
			
			mailer.configureAndSend(inReq, template, admins.toString());
		}
		catch ( Exception e )
		{
			log.error( "Could not send email", e );
			
			throw new OpenEditException( e );
		}
	}
	
	public void createUser( WebPageRequest inReq ) throws OpenEditException
	{
		checkAdminPermission(inReq);
		
		String username = inReq.getRequiredParameter( USERNAME_PARAMETER );
		String password = inReq.getRequestParameter( "newpassword" );
		if(password==null)
		{
			password = inReq.getRequestParameter("password");
		}
		String retypedPassword = inReq.getRequestParameter( "retypedPassword" );

		
		if (password==null || password.equals( retypedPassword ))
		{
			try
			{
				if(password==null)
				{
					password = new PasswordGenerator().generate();
				}
				User user = getUserManager(inReq).createUser( username, password );
				user.setPassword(password);
				user.setEnabled(true);
				getPropertyContainerManipulator().updateProperties( inReq.getParameterMap(),
						user.getProperties() );
				//groups
				String groups[] = inReq.getRequestParameters(GROUPS);
				if( groups != null )
				{
					for (int i = 0; i < groups.length; i++)
					{
						Group group = getGroupSearcher(inReq).getGroup( groups[i] );
						user.addGroup(group);
						getUserManager(inReq).saveGroup(group);
					}
				}
				getUserSearcher(inReq).saveData( user ,inReq.getUser());
				inReq.putPageValue( "newUser", user );
			}
			catch( UserManagerException ume )
			{
				throw new OpenEditException( ume );
			}

		}
		else
		{
			throw new PasswordMismatchException( "The two passwords did not match." );
		}
	}
	
	
	//TODO: Delete this method and use the Secured interface instead
	protected void checkAdminPermission(WebPageRequest inReq) throws OpenEditException
	{
		User user = inReq.getUser();
		if ( user == null)
		{
			throw new OpenEditException("Must be logged in");
		}
		
		Object canUpload = inReq.getPageValue("caneditusersgroups");
		
		if ( ! (user.hasPermission("oe.usermanager") || user.hasPermission("oe.administration")  //The second permission is deprecated 
				|| Boolean.parseBoolean(String.valueOf(canUpload)) ) )
		{
			String allowsave = inReq.getContentProperty("allowadmimability");
			if( Boolean.parseBoolean(allowsave))
			{
				return;
			}
			throw new OpenEditException("No Permissions");			
		}
	}

	public void saveUserGroups( WebPageRequest inReq ) throws OpenEditException
	{
		checkAdminPermission(inReq);
		String username = inReq.getRequiredParameter( USERNAME_PARAMETER );

		try
		{
			User user = getUserSearcher(inReq).getUser(username);
			String groups[] = inReq.getRequestParameters(GROUPS);
			Collection groupslist = new ArrayList();
			if( groups != null )
			{
				for (int i = 0; i < groups.length; i++)
				{
					Group group = getGroupSearcher(inReq).getGroup( groups[i] );
					groupslist.add( group );
				}
			}
			user.setGroups(groupslist);
			getUserSearcher(inReq).saveData( user ,inReq.getUser());
		}
		catch( UserManagerException ume )
		{
			throw new OpenEditException( ume );
		}
	}
	public void createUserProperties( WebPageRequest inReq ) throws OpenEditException
	{
		checkAdminPermission(inReq);
		try
		{
			User user = getUser( inReq );
			getPropertyContainerManipulator().createProperties( inReq.getParameterMap(),
				user.getProperties() );
			getUserSearcher(inReq).saveData( user ,inReq.getUser());
		}
		catch( UserManagerException ume )
		{
			throw new OpenEditException( ume );
		}
	}

	protected User getUser( WebPageRequest inReq ) throws OpenEditException
	{
		String username = inReq.getRequiredParameter( USERNAME_PARAMETER );
		User user = null;

		try
		{
			user = getUserSearcher(inReq).getUser( username );

			return user;
		}
		catch( UserManagerException ume )
		{
			throw new OpenEditException( ume );
		}

	}

	public void getGroupsForDeletion( WebPageRequest inReq ) throws OpenEditException
	{
		String[] groups = inReq.getRequestParameters( GROUPS );
		if (groups != null)
		{

			List groupsForDeletion = new ArrayList( groups.length );
			for ( int i = 0; i < groups.length; i++ )
			{
				try
				{
					Group group = getGroupSearcher(inReq).getGroup( groups[i] );
					groupsForDeletion.add( group );
				}
				catch( UserManagerException e )
				{
					throw new OpenEditException( e );
				}
			}
			inReq.putPageValue( GROUPS, groupsForDeletion );
			inReq.putSessionValue( GROUPS, groupsForDeletion );
		}
	}


	public void getUsersForDeletion( WebPageRequest inReq ) throws OpenEditException
	{
		String[] userNames = inReq.getRequestParameters( USERNAMES );
		if (userNames != null)
		{

			List usersForDeletion = new ArrayList( userNames.length );
			for ( int i = 0; i < userNames.length; i++ )
			{
				try
				{
					User user = getUserSearcher(inReq).getUser( userNames[i] );
					if( user != null)
					{
						usersForDeletion.add( user );						
					}
				}
				catch( UserManagerException e )
				{
					throw new OpenEditException( e );
				}
			}
			inReq.putPageValue( USERNAMES, usersForDeletion );
			inReq.putSessionValue( USERNAMES, usersForDeletion );
		}
	}

	public void deleteGroups( WebPageRequest inReq ) throws OpenEditException
	{
		checkAdminPermission(inReq);
		Group group = loadGroup(inReq);
		getUserManager(inReq).deleteGroup(group);
	}

	public void deleteUsers( WebPageRequest inReq ) throws OpenEditException
	{
		checkAdminPermission(inReq);
		List users = (List) inReq.getSessionValue( USERNAMES );
		getUserManager(inReq).deleteUsers( users );
		getUserSearcher(inReq).reIndexAll();
		inReq.removeSessionValue( USERNAMES );
	}
	
	public void deleteUser(WebPageRequest inReq) throws OpenEditException
	{
		checkAdminPermission(inReq);
		String usertodelete = inReq.getRequestParameter("usertodelete");
		if( usertodelete == null)
		{
			usertodelete = inReq.getRequestParameter("id");
		}
		User user = getUserManager(inReq).getUser(usertodelete);
		getUserManager(inReq).deleteUser(user);
		getUserSearcher(inReq).delete(user, inReq.getUser());
	}

	public void deleteGroupProperties( WebPageRequest inReq ) throws OpenEditException,
			OpenEditException
	{
		checkAdminPermission(inReq);
		Group group = getGroup( inReq );
		getPropertyContainerManipulator().deleteProperties( inReq,
			group.getProperties() );
		getUserManager(inReq).saveGroup( group );
	}

	public void deleteUserProperties( WebPageRequest inReq ) throws OpenEditException
	{
		try
		{
			checkAdminPermission(inReq);
			User user = getUser( inReq );
			getPropertyContainerManipulator().deleteProperties( inReq,
				user.getProperties() );
			getUserSearcher(inReq).saveData( user ,inReq.getUser());
		}
		catch( UserManagerException e )
		{
			throw new OpenEditException( e );
		}
	}

	public UserManager getUserManager( WebPageRequest inReq ) throws OpenEditException
	{
		UserManager userManager2 = super.getUserManager(inReq);
		inReq.putPageValue( USERMANAGER, userManager2 );
		inReq.putPageValue( "usermanager", userManager2 ); //No needed
		inReq.putPageValue( "userManager", userManager2 );
		inReq.putPageValue( "usermanagerhome", inReq.getContentProperty("usermanagerhome"));
		return userManager2;
	}

	/**
	 * @param phone1
	 * @see Customer.cleanphone1
	 * @return
	 */
	private String clean( String phone1 )
	{
		if (phone1 == null)
		{
			return null;
		}
		StringBuffer out = new StringBuffer();
		for (int i = 0; i < phone1.length(); i++)
		{
			if ( Character.isDigit(phone1.charAt(i)))
			{
				out.append(phone1.charAt(i));
			}
		}
		phone1 = out.toString();

		return phone1;
	}

	public void enterUserHomePage( WebPageRequest inReq ) throws Exception
	{
		//we need get the authenticated user and copy the blank home page that they will use
		String username = inReq.getRequestParameter("username");
		String lastname = inReq.getRequestParameter(User.LAST_NAME_PROPERTY);
		if ( username == null || lastname == null)
		{
			String error = "Missing information";
			inReq.putPageValue("errorMessage",error);
			log.error( error );
			return;
		}
		
		User user = getUserSearcher(inReq).getUser(username);
		if ( user == null)
		{
			String error = "No such user";
			inReq.putPageValue("errorMessage",error);
			log.error( error );
			return;
		}
		if ( lastname == null || !lastname.equalsIgnoreCase( user.getLastName() ) )
		{
			String error = "User's last name does not match our records";
			inReq.putPageValue("errorMessage",error);
			log.error( error );
			return;
		}
		String directory = inReq.getPath();
		//take off the end
		directory = PathUtilities.extractDirectoryPath(directory);
		String homepage = directory  + "/" + user.getUserName() + "/index.html";
		org.openedit.page.Page homePage = getPageManager().getPage(homepage);
		if ( !homePage.exists() )
		{
			//copy the example page
			Page starter = getPageManager().getPage( directory + "/starterpage.html");
			User admin = getUserSearcher(inReq).getUser("admin");
			WebPageRequest tempContext = new BaseWebPageRequest( inReq);
			tempContext.putPageValue("user",admin); //we need a user with proper permissions
			getPageManager().copyPage(starter,homePage);
			
			//Now save the settings
			XconfConfiguration config = new XconfConfiguration();
			config.setName("page");
			//config.readXML(settings.getReader());
			config.setWritePermissions(	"<or><group name=\"administrators\"/><user name=\"" + 
				username +	"\" /></or>");
			
			Page settings  = getPageManager().getPage( directory + "/" + user.getUserName() + "/_default.xconf" );
			StringItem out = new StringItem(settings.getPath(),config.toXml(settings.getCharacterEncoding()),settings.getCharacterEncoding());
			out.setAuthor(admin.getUserName());
			out.setMessage("Initial copy");
			settings.setContentItem(out);
			getPageManager().putPage(settings);
		}
		
		//See if they want to try to log in
		String password = inReq.getRequestParameter("password");
		if ( password == null || password.length() == 0 )
		{
			String phone = inReq.getRequestParameter("phone1");
			password = clean(phone); //this might be thier default password
		}				
		if ( password != null)
		{
			boolean ok = getUserManager(inReq).authenticate(user, password);
			//Phone1
			if ( ok)
			{
				inReq.putSessionValue("user",user);
			}
			else
			{
				String error = "Incorrect information entered for editing";
				inReq.putPageValue("errorMessage",error);
				log.error( error );
				return;
			}
		}
		inReq.redirect(homePage.getPath());
	}

	public void setUserPassword( WebPageRequest inReq ) throws UserManagerException,
			OpenEditException
	{
		String password = inReq.getRequestParameter( "password" );
		String retypedPassword = inReq.getRequestParameter( "retypedPassword" );
		
		
		if(password == null || retypedPassword == null)
		{
			inReq.putPageValue("errors", "novalues");
			return;
		}
		if (password.equals( retypedPassword ))
		{
		
			MediaArchive archive = getMediaArchive(inReq);
			if(archive != null) {
				Data regex = archive.getCatalogSetting("passwordregex");
				if(regex != null) {
					String value = regex.get("value");
					 if(!password.matches(value)) {
						 String label = regex.getName();
				         inReq.putPageValue("errors", "regex");

						 inReq.putPageValue("regexerror", label);
						 return;
					 }
				}
				
				
			}
			
			
			
			User user = getUser( inReq );
			User target = inReq.getUser();
			if( !user.getId().equals(target.getId()  ))
			{
				checkAdminPermission(inReq);
			}
			user.setPassword( password );
			getUserSearcher(inReq).saveData( user ,target);
			inReq.putPageValue("message", "passwordchanged");
			
		}
		else
		{
			inReq.putPageValue("errors", "mismatch");
			//throw new PasswordMismatchException( "The two passwords do not match." );
		}
	}

	/**
	 * Retrieve a property manipulator.
	 * 
	 * @return PropertyContainerManipulator
	 */
	protected PropertyContainerManipulator getPropertyContainerManipulator()
	{
		if (fieldPropertyManipulator == null)
		{
			fieldPropertyManipulator = new PropertyContainerManipulator();
		}

		return fieldPropertyManipulator;
	}

	/**
	 * @see org.openedit.action.Command#execute(Map, Map)
	 */
	public void updateUserProperties( WebPageRequest inReq ) throws UserManagerException,
			OpenEditException
	{
		User user = getUser( inReq );
		if( !user.getId().equals(  inReq.getUser().getId() ) )
		{
			checkAdminPermission(inReq);
		}
		

		//TODO: Why is this needed? Simplify
		Map params = inReq.getParameterMap();
		String[] fields = (String[]) params.get("field");
		String enabled = inReq.getRequestParameter("enabled.value");
		
			
		
		getUserSearcher(inReq).saveDetails(inReq, fields, user, user.getId());
		if(enabled!= null) {
			user.setEnabled(Boolean.parseBoolean(enabled));
		}
		getUserSearcher(inReq).saveData( user ,inReq.getUser());
		
		
	
		inReq.putPageValue("status","Saved");
		inReq.putPageValue("saved",true);
		
		
		String catalogid = inReq.findValue("catalogid");
		if( user.getId().equals(  inReq.getUser().getId() ) )
		{
			inReq.putSessionValue(catalogid + "user",user);
			inReq.putSessionValue("systemuser",user);
			inReq.putPageValue("user",user);
		}
	}

	/**
	 * @see org.openedit.action.Command#execute(Map, Map)
	 */
	public void updateGroupProperties( WebPageRequest inReq ) throws UserManagerException,
			OpenEditException
	{
		checkAdminPermission(inReq);
		Group group = getGroup( inReq );
		getPropertyContainerManipulator().updateProperties( inReq.getParameterMap(),
				group );
		getGroupSearcher(inReq).saveData(group,inReq.getUser());
	}

	/**
	 * @see org.openedit.action.Command#execute(Map, Map)
	 */
	public void addUsersToGroup( WebPageRequest inReq ) throws UserManagerException,
			OpenEditException
	{
		inReq.removeSessionValue("cachedGroupQuery");
		checkAdminPermission(inReq);
		String[] userNames = inReq.getRequestParameters( "addUsernames" );
		if( userNames == null)
		{
			userNames = inReq.getRequestParameters( "id" );
		}
		if( userNames == null)
		{		
			return;
		}
		Group group = getGroup( inReq );

		if (userNames != null)
		{
			for ( int i = 0; i < userNames.length; i++ )
			{
				User user = getUserSearcher(inReq).getUser( userNames[i] );

				if( user == null)
				{
					user = getUserSearcher(inReq).getUserByEmail(userNames[i]);
				}
				
				if (user == null)
				{
					throw new UserManagerException( "Could not find user " + userNames[i] );
				}
				user.addGroup(group );
				getUserSearcher(inReq).saveData(user,inReq.getUser());
			}
			getGroupSearcher(inReq).saveData(group,null); //This is probably called to update the index
		}
	}

	/**
	 * @see org.openedit.action.Command#execute(Map, Map)
	 */
	protected Group getGroup( WebPageRequest inReq ) throws OpenEditException
	{
		String name = inReq.getRequestParameter( GROUP_ID_PARAMETER );
		
		if ( name == null)
		{
			PageAction action = (PageAction)inReq.getPageValue("exec-action");
			if ( action != null)
			{
				name = action.getChildValue("groupid");
			}

		}
		
		Group group = null;

		try
		{
			group = getGroupSearcher(inReq).getGroup( name );

			return group;
		}
		catch( UserManagerException ume )
		{
			throw new OpenEditException( ume );
		}
	}
	public Group loadGroup( WebPageRequest inReq) throws OpenEditException
	{
		Group group = getGroup(inReq);
		inReq.putPageValue("group",group);
		return group;
	}
	/**
	 * @see org.openedit.action.Command#execute(Map, Map)
	 */
	public void removeUsersFromGroup( WebPageRequest inReq ) throws UserManagerException,
			OpenEditException
	{
		
		checkAdminPermission(inReq);
		inReq.removeSessionValue("cachedGroupQuery");

		String[] userNames = inReq.getRequestParameters( "removeUsernames" );
		if( userNames == null)
		{
			userNames = inReq.getRequestParameters( "id" );
		}
		if (userNames != null)
		{
			Group group = getGroup(inReq);
			if( group == null)
			{
				return;
			}
			for ( int i = 0; i < userNames.length; i++ )
			{
				User user = getUserSearcher(inReq).getUser( userNames[i] );

				if (user != null)
				{
					user.removeGroup(group);
					getUserSearcher(inReq).saveData(user,inReq.getUser());
				}
			}
		}
	}
	public void removeAllUsersFromGroup( WebPageRequest inReq ) throws UserManagerException,	OpenEditException
	{
		checkAdminPermission(inReq); //This is deprecated with new permissions system
		inReq.removeSessionValue("cachedGroupQuery");
		Group group = getGroup(inReq);
		
		List userstosave = new ArrayList();
		HitTracker users = getUserSearcher(inReq).getUsersInGroup(group);
		for (Iterator iter = users.iterator(); iter.hasNext();)
		{
			Object userhit = (Object) iter.next();
			String id = users.getValue(userhit, "id");
			User user = getUserSearcher(inReq).getUser(id);
			user.removeGroup(group);
			userstosave.add(user);
		}
		getUserSearcher(inReq).saveUsers(userstosave,inReq.getUser());
	}
		
	/**
	 * @deprecated use loadData
	 */
	public void loadUserProperties(WebPageRequest inReq) throws Exception
	{		
		//Remove this section on Oct 15 2008
//		Page config = getPageManager().getPage("/openedit/usermanager/usermanagersettings.xml");
//		XMLConfiguration notificationConfig = new XMLConfiguration();
//		notificationConfig.readXML(config.getReader());
//		List children = notificationConfig.getChildren("userproperty");
//		inReq.putPageValue("standarduserproperties",children);
		//end remove
		
		
		inReq.putPageValue("searcher", getUserSearcher(inReq));

		
		User user = null;//inReq.getUser();
		String userName = inReq.getRequestParameter( "username" );
		if( userName != null)
		{
			user = getUserSearcher(inReq).getUser( userName );
		}
		if( user != null)
		{
			inReq.putPageValue( "propertycontainer", user );
			inReq.putPageValue( "selectedUser", user );
			inReq.putPageValue( "userName", user.getUserName());
		}
	}
	
	
	public void findUsers(WebPageRequest inReq)
	{
		HitTracker hits = getUserSearcher(inReq).fieldSearch(inReq);
		inReq.putPageValue("hits", hits);
	
	}
	public void findAllUsers(WebPageRequest inReq)
	{
		HitTracker all = getUserSearcher(inReq).getAllHits();
		all.setHitsName("userTracker");
		all.setCatalogId(getUserSearcher(inReq).getCatalogId());
		inReq.putPageValue(all.getHitsName(), all);
		inReq.putSessionValue(all.getSessionId(), all);
		log.info(all.getHitsName() + " = " + all.size());
		inReq.putPageValue("searcher", getUserSearcher(inReq));
	}
	
	public void findUsersInGroup(WebPageRequest inReq)
	{
		HitTracker all = null;
		String page = inReq.getRequestParameter("page");
		if( page != null)
		{
			all = (HitTracker)inReq.getSessionValue("usergroupsTracker" + getUserSearcher(inReq).getCatalogId());
			if( all != null)
			{
				all.setPage(Integer.parseInt(page));
			}
		}
		
		if( all == null )
		{
			Group group = getGroup(inReq);
			if(group != null)
			{	
				all = getUserSearcher(inReq).getUsersInGroup(group);
			}
		}	
		if(all!= null)
		{
			all.setHitsName("userTracker");
			all.setCatalogId(getUserSearcher(inReq).getCatalogId());
			
			inReq.putPageValue(all.getHitsName(), all);
			inReq.putSessionValue(all.getSessionId(), all);
				
			inReq.putPageValue("searcher", getUserSearcher(inReq));
		}
	}

	public HitTracker getGroupHits(WebPageRequest inReq, String inGroup)
	{
		Group group = getGroupSearcher(inReq).getGroup(inGroup);
		HitTracker users = getUserSearcher(inReq).getUsersInGroup(group);
		return users;
	}

	public PostMail getPostMail() {
		return postMail;
	}

	public void setPostMail(PostMail postMail) {
		this.postMail = postMail;
	}
	
	public boolean checkQuestion(WebPageRequest inReq) throws OpenEditException
	{
		if( inReq.getSessionValue("answer") != null || inReq.getUser() != null)
		{
			return true;
		}
		
		String answer = inReq.getRequestParameter("answerid");
		
		Question q = (Question)inReq.getSessionValue("question");
		boolean passed = false;
		if( q == null)
		{
			q = loadQuestion(inReq);
			inReq.putPageValue("error", "Question has changed. Go back and answer " + q.getDescription());
			passed = false;
		}
		else 
		{
			if ( q.checkAnswer( answer) )
			{
				passed = true;
			}
			else
			{
				inReq.putPageValue("error", "Wrong answer. Please try again." );
			}
		}
		if( passed )
		{
			inReq.putSessionValue("answer", answer);
			return true;
		}
		else
		{
			String errorpath = inReq.getPageProperty("questionerrorpath");
			if(errorpath != null )
			{
				//forward so the error is still there.
				inReq.forward(errorpath);
			}
			else
			{
				inReq.setCancelActions(true);
			}
			return false;
		}
	}
	public Question loadQuestion(WebPageRequest inReq) throws OpenEditException
	{
		Question q = (Question)inReq.getSessionValue("question");
		if( inReq.getSessionValue("answer") != null || inReq.getUser() != null )
		{
			return q; //already authenticated as a person
		}
		if( q != null)
		{
			return q; //already picked one
		}
		q = getQuestionArchive().getRandomQuestion();
		inReq.putSessionValue("question", q);
		return q;
		
	}
	public QuestionArchive getQuestionArchive()
	{
		return fieldQuestionArchive;
	}

	public void setQuestionArchive(QuestionArchive inQuestionArchive)
	{
		fieldQuestionArchive = inQuestionArchive;
	}
	public void checkForRefererUrl(WebPageRequest inReq) throws Exception
	{
		//Find out where they came from and check the session
		String referal = (String)inReq.getSessionValue("refererurl");
		if( referal == null && inReq.getRequest() != null)
		{
			String referrer = inReq.getRequest().getHeader("REFERER");
			inReq.putSessionValue("refererurl", referrer);
		}
	}
	
	
	public void addEmailToGroup(WebPageRequest inReq)
	{
		String value = inReq.getPageProperty("emailgroupid");
		String email = inReq.getRequestParameter("email");
		email = email.toLowerCase();
		User user = getUserSearcher(inReq).getUserByEmail(email);
		if( user == null)
		{	
			String password = new PasswordGenerator().generate();
			user = getUserManager(inReq).createUser(email,password);
			user.setEmail(email);
			user.setVirtual(false);
		}
		Group group = getGroupSearcher(inReq).getGroup(value);
		if( group == null)
		{
			group = getUserManager(inReq).createGroup(value, value);
		}
		if( !user.isInGroup(group))
		{
			user.addGroup(group);
			getUserSearcher(inReq).saveData(user,inReq.getUser());
		}
		else
		{
			inReq.putPageValue("alreadyadded", Boolean.TRUE);			
		}
		inReq.putPageValue("added", Boolean.TRUE);
	}
	public void removeFromEmailGroup(WebPageRequest inReq)
	{
		String value = inReq.getPageProperty("emailgroupid");
		String email = inReq.getRequestParameter("email");
		email = email.toLowerCase();
		User user = getUserSearcher(inReq).getUserByEmail(email);
		if( user == null)
		{	
			inReq.putPageValue("thanks", "Email is already removed");
		}
		Group group = getGroupSearcher(inReq).getGroup(value);
		if( group == null)
		{
			group = getUserManager(inReq).createGroup(value, value);
		}
		if( !user.isInGroup(group))
		{
			user.removeGroup(group);
			getUserSearcher(inReq).saveData(user,inReq.getUser());
		}
		inReq.putPageValue("removed", Boolean.TRUE);
	}
	
	public void loadPageOfResults(WebPageRequest inReq)
	{
		HitTracker hits = getUserSearcher(inReq).loadPageOfSearch(inReq);
		inReq.putPageValue("hits", hits);
	}
	public void loadPageOfGroupResults(WebPageRequest inReq)
	{
		getGroupSearcher(inReq).loadPageOfSearch(inReq);
	}
	public void loadAllGroupHits(WebPageRequest inReq)
	{
		HitTracker hits = getGroupSearcher(inReq).getAllHits();
		String hitsname = inReq.findValue("hitsname");
		inReq.putPageValue(hitsname, hits);
	}
	
	public void loadHits(WebPageRequest inReq)
	{
		String hitsname = inReq.findValue("hitsname");
		getUserSearcher(inReq).loadHits(inReq, hitsname);
		inReq.putPageValue("searcher", getUserSearcher(inReq));
	}
	public void loadUserSearcher(WebPageRequest inReq)
	{
		inReq.putPageValue("searcher", getUserSearcher(inReq));
	}
	
	public void loadGroupHits(WebPageRequest inReq)
	{
		String hitsname = inReq.findValue("hitsname");
		HitTracker hits = getGroupSearcher(inReq).loadHits(inReq, hitsname);
		//log.info(hits);
		inReq.putPageValue("searcher", getGroupSearcher(inReq));
	}
	public void findAllGroups(WebPageRequest inReq)
	{
		HitTracker all = getGroupSearcher(inReq).getAllHits();
		String hitsname = inReq.findValue("hitsname");
		if (hitsname == null)
		{
			hitsname = "grouplist";
		}
		all.setHitsName(hitsname);
		all.setCatalogId(getGroupSearcher(inReq).getCatalogId());
		inReq.putSessionValue(all.getSessionId(), all);
		inReq.putPageValue(hitsname, all);
		inReq.putPageValue("searcher", getGroupSearcher(inReq));
	}
	public void findGroups(WebPageRequest inReq)
	{
		HitTracker all = getGroupSearcher(inReq).fieldSearch(inReq);
/*		String hitsname = inReq.findValue("hitsname");

		inReq.putSessionValue(hitsname + getGroupSearcher(inReq).getCatalogId(), all);
		inReq.putPageValue("searcher", getGroupSearcher(inReq));*/
	}
	
	public void reindexGroups(WebPageRequest inReq)
	{
		getGroupSearcher(inReq).reIndexAll();
	}
	
	public void reindexUsers(WebPageRequest inReq)
	{
		getUserSearcher(inReq).reIndexAll();
	}
	
	public void setUserProperty(WebPageRequest inReq) 
	{
		String mode = inReq.findValue("userpropertyname");
		String value = inReq.findValue("userpropertyvalue");
		User user = inReq.getUser();
		if( value != null && value.equals( user.get(mode)) )
		{
			return;
		}
		if (mode == null)
		{
			mode = inReq.findValue("userpropertyname");
		}
		user.setValue(mode,value);
		getUserManager(inReq).saveUser(inReq.getUser());
	}
/*
	public void saveLegacyPermissions( WebPageRequest inReq )
			throws UserManagerException, OpenEditException
	{
		checkAdminPermission(inReq);

		String[] savePermissions = inReq.getRequestParameters( "savePermissions" );
		String[] permissions = savePermissions;

		Set permissionsToRetain = new HashSet();
		if (permissions != null)
		{
			for ( int i = 0; i < permissions.length; i++ )
			{
				permissionsToRetain.add( permissions[i] );
			}
		}

		for ( Iterator iter = getUserManager(inReq).getPermissions().iterator(); iter.hasNext(); )
		{
			Group group = getGroup( inReq );
			org.openedit.users.Permission element = (org.openedit.users.Permission) iter.next();
			boolean dirty = false;
			if (!permissionsToRetain.contains( element.getName() )
					&& group.hasPermission( element.getName() ))
			{
				group.removePermission( element.getName() );
				dirty = true;
			}
			else if (permissionsToRetain.contains( element.getName() )
					&& !group.hasPermission( element.getName() ))
			{
				group.addPermission( element.getName() );
				dirty = true;
			}
			if ( dirty)
			{
				getUserManager(inReq).saveGroup(group);
			}
		}
	}
	*/
	public void saveGroupData(WebPageRequest inReq) throws Exception
	{
		//Already save using DataEditModule.saveData
		User user = (User)inReq.getPageValue("data");

		String groups = inReq.getRequestParameter("groups.value");
		if( groups != null)
		{
			Collection groupslist = new ArrayList();
			if( groups != null )
			{
				String[] vals = null;
				if( groups.contains("|") )
				{
					vals = MultiValued.VALUEDELMITER.split(groups);
				}
				else
				{
					vals = groups.split("\\s+"); //legacy
				}
				for (int i = 0; i < vals.length; i++)
				{
					Group group = getGroupSearcher(inReq).getGroup( vals[i] );
					if( group != null )
					{
						groupslist.add( group );
					}
				}
			}
			user.setGroups(groupslist);
			getSearcherManager().getSearcher("system", "user").saveData(user, inReq.getUser());
			
		}
	}
	
	public void loadUserByFolder(WebPageRequest inReq)
	{
		String path = inReq.getPath();
		String username = PathUtilities.extractDirectoryName(path);
		User user = getUserManager(inReq).getUser(username);
		inReq.putPageValue("selecteduser", user);
	}
}
