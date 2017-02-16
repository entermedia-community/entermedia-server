package users;

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.data.*
import org.openedit.users.Group
import org.openedit.users.GroupSearcher
import org.openedit.users.User
import org.openedit.users.UserSearcher
import org.openedit.util.Exec
import org.openedit.util.ExecResult
import org.openedit.util.PathUtilities


 
public void init()
{
//ldapsearch -x -LLL -E pr=200/noprompt -h 10.169.1.6 -D "mediaincbcsrcca\emshare" -w '3m5har3!' -b "DC=media, DC=in, DC=cbcsrc, DC=ca" objectClass=user uid mail givenName sn memberOf + | more

			MediaArchive archive = context.getPageValue("mediaarchive");
		Exec exec = (Exec)moduleManager.getBean("exec");
		ArrayList command = new ArrayList();
		
		ExecResult found  = exec.runExec("/media/services/tools/ldapuserexport.sh", command, true);
		
		UserSearcher usersearch = archive.getSearcherManager().getSearcher("system","user");
		GroupSearcher groupsearch = archive.getSearcherManager().getSearcher("system","group");
		
		Group admingroup = createGroup(groupsearch,"CBC RC MTL GG EMShare Admin Group");
		Group usergroup = createGroup(groupsearch,"CBC RC MTL GG EMShare Users Group");
		Group readonlygroup = createGroup(groupsearch,"CBC RC MTL GG EMShare Visionnement");
		Group deletegroup = createGroup(groupsearch,"DTV MTL GG GM EMS FMOD");
		Group uploaddeletegroup = createGroup(groupsearch,"DTV MTL GG GM EMS UPLOAD");

		Searcher profilesearcher =archive.getSearcherManager().getSearcher(archive.getCatalogId(), "userprofile");
		
		BufferedReader read = new BufferedReader(new FileReader("/opt/entermediadb/webapp/WEB-INF/bin/linux/users.txt"));
		try
		{
		 User current = null;
		 for(String line; (line = read.readLine()) != null; )
		 {
			 if( line.startsWith("dn: CN=") )
			 {
				 String userid = line.substring(7,line.indexOf(","));
				// log.info("Found ${userid}");
								 userid = userid.toLowerCase();
							  
				 if( current != null )
				 {
					//TODO: Make a function to save current user and make sure I call it once at the end
					//Is this a group we care about?
					if( current.isInGroup(admingroup)
						|| current.isInGroup(usergroup)
						|| current.isInGroup(readonlygroup)
						|| current.isInGroup(deletegroup)
												|| current.isInGroup(uploaddeletegroup) )
					 {
						 usersearch.saveData(current,null);
												// log.info("Saved ${current.getId()}");
										 Data profile = (Data) profilesearcher.searchById(current.getUserName());
												if( profile == null )
												{
													  profile = profilesearcher.createNewData();
													  profile.setProperty("userid",current.getId());
													   profile.setId(current.getId());
												 }
												 if( current.isInGroup(admingroup) )
												 {
														  profile.setProperty("settingsgroup","administrator");
													   //   log.info("Made admin");
												 }
												 else if( current.isInGroup(usergroup) )
												 {
														  profile.setProperty("settingsgroup","user");
												 }
												 else if( current.isInGroup(readonlygroup) )
												 {
														  profile.setProperty("settingsgroup","AVmPYLWTPbQVdTC3G5OM");
												 }
												 
												 else if( current.isInGroup(deletegroup) )
												 {
														  profile.setProperty("settingsgroup","AVmPVfBNPbQVdTC3G5N4");
												 }
												 else if( current.isInGroup(uploaddeletegroup) )
												 {
														  profile.setProperty("settingsgroup","AVmPXL9pPbQVdTC3G5N8");
												 }
												 log.info("saved profile ${profile.getId()} as ${profile.settingsgroup}");
												profilesearcher.saveData(profile,null);
					 }
				 }
				 current = usersearch.getUser(userid);
				 if( current == null)
				 {
					 current = (User)usersearch.createNewData();
										 current.setId(userid);
				 }
								 //TODO: Save any existing users in old groups
				 current.removeGroup(admingroup);
				 current.removeGroup(usergroup);
				 current.removeGroup(readonlygroup);
				 current.removeGroup(deletegroup);
								 current.removeGroup(uploaddeletegroup);
			 }
			 if( line.startsWith("memberOf: CN=") && current != null)
			 {
				 if( line.contains(admingroup.getName()))
				 {
					 current.addGroup(admingroup);
				 }
				 else if( line.contains(usergroup.getName()))
				 {
					 current.addGroup(usergroup);
				 }
				 else if( line.contains(readonlygroup.getName()))
				 {
					 current.addGroup(readonlygroup);
				 }
				
				 else if( line.contains(deletegroup.getName()))
				 {
					 current.addGroup(deletegroup);
				 }
				 else if( line.contains(uploaddeletegroup.getName()))
				 {
					 current.addGroup(uploaddeletegroup);
				 }
			 }
			 else if( line.startsWith("mail: "))
			 {
				 current.setEmail(line.substring(6,line.length()));
			 }
			 else if( current != null && line.startsWith("sn: "))
			 {
				 current.setLastName(line.substring(3,line.length()));
			 }
			 else if( line.startsWith("givenName: "))
			 {
				 current.setFirstName(line.substring(10,line.length()));
			 }
			}
		}
		finally
		{
			read.close();
		}
}

public Group createGroup(GroupSearcher inGroups, String inName)
{
	String id = PathUtilities.extractId(inName);
	Group found = inGroups.getGroup(id);
	if( found == null )
	{
	 found = inGroups.createNewData();
	 found.setId(id);
	 found.setName(inName);
	 inGroups.saveData(found,null);
   }
   return found;
}
init();