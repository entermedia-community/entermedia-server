package org.entermediadb.data;

import org.entermediadb.asset.BaseEnterMediaTest;
import org.openedit.data.SearcherManager;

public class LdapSearcherTest extends BaseEnterMediaTest
{
	protected SearcherManager getSearcherManager()
	{
		return (SearcherManager) getBean("searcherManager");
	}
	
	public void testConnection() throws Exception
	{
//		String catalog = "entermedia/catalogs/testcatalog";
//		MediaArchive archive = getMediaArchive(catalog);
//		LdapSearcher searcher  = (LdapSearcher)archive.getSearcher("ldap");
//		Collection found = searcher.query().match("objectClass", "user").search();
//		for (Iterator iterator = found.iterator(); iterator.hasNext();)
//		{
//			Data hit = (Data) iterator.next();
//			System.out.println(hit.getName());
//		}
//		
//		//ldapsearch 
//		
//		Exec exec = (Exec)moduleManager.getBean("exec");
//		List<String> command = new ArrayList();
//		command.add("-x");
//		command.add("-LLL");
//		command.add("-E");
//		command.add("pr=200/noprompt");
//		command.add("-h");
//		command.add("10.169.1.6");
//		command.add("-D");
//		command.add("mediaincbcsrcca\\emshare");
//		command.add("-w");
//		command.add("3m5har3!");
//		command.add("-b");
//		command.add("DC=media, DC=in, DC=cbcsrc, DC=ca");
//		command.add("objectClass=user");
//		command.add("uid");
//		command.add("mail");
//		command.add("givenName");
//		command.add("sn");
//		command.add("memberOf");
//		command.add("+");
//		
//		ExecResult found  = exec.runExec("ldapsearcher", command, true);
//		String output = found.getStandardOut();
//		
//		UserSearcher usersearch = mediaArchive.getSearcher("system","user");
//		GroupSearcher groupsearch = mediaArchive.getSearcher("system","group");
//		
//		Group admingroup = groupsearch.getGroup(PathUtilities.extractId("CBC RC MTL GG EMShare Admin Group"));
//		Group usergroup = groupsearch.getGroup(PathUtilities.extractId("CBC RC MTL GG EMShare Users Group"));
//		Group readonlygroup = groupsearch.getGroup(PathUtilities.extractId("CBC RC MTL GG EMShare Visionnement"));
//		
//		BufferedReader read = new BufferedReader(new StringReader(output));
//		User current = null;
//		 for(String line; (line = read.readLine()) != null; ) 
//		 {
//			 if( line.startsWith("dn: CN=") )
//			 {
//				 String userid = line.substring(6,line.indexOf(","));
//				 if( current != null )
//				 {
//					 //Is this a group we care about?
//					 if( current.isInGroup("CBC RC MTL GG EMShare Admin Group") 
//						|| current.isInGroup("CBC RC MTL GG EMShare Admin Group") 
//						|| current.isInGroup("CBC RC MTL GG EMShare Admin Group") )
//					 {
//						 usersearch.saveData(current,null);
//					 }
//				 }
//				 current = usersearch.getUser(userid);
//				 if( current == null)
//				 {
//					 current = (User)usersearch.createNewData();
//				 }
//				 current.removeGroup(admingroup);
//				 current.removeGroup(usergroup);
//				 current.removeGroup(readonlygroup);
//			 }
//			 if( line.startsWith("memberOf: CN=") && current != null)
//			 {
//				 if( line.contains(admingroup.getName()))
//				 {
//					 current.addGroup(admingroup);
//				 }
//				 else if( line.contains(usergroup.getName()))
//				 {
//					 current.addGroup(usergroup);
//				 }
//				 else if( line.contains(readonlygroup.getName()))
//				 {
//					 current.addGroup(readonlygroup);
//				 }
//			 }
//			 else if( line.startsWith("mail: "))
//			 {
//				 current.setEmail(line.substring(6,line.length()));
//			 }
//			 else if( line.startsWith("sn: "))
//			 {
//				 current.setLastName(line.substring(6,line.length()));
//			 }
//			 else if( line.startsWith("givenName: "))
//			 {
//				 current.setFirstName(line.substring(10,line.length()));
//			 }
//		 }
	}
}
