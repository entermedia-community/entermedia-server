import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.OpenEditException
import org.openedit.page.Page
import org.openedit.util.Exec
import org.openedit.util.ExecResult


public void init()
{ 
	log.info("Creating sync.conf x");
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	Page templatepage = archive.getPageManager().getPage("/WEB-INF/bin/linux/sync.conf.template");

	String content = templatepage.getContent();
	
	StringBuffer output = new StringBuffer();
	
	int keystart = content.indexOf("\"shared_folders\":[");
	int keyend = keystart + "\"shared_folders\":[".length();
	
	output.append(content.substring(0,keyend));

	String template =  content.substring(keyend,content.indexOf("]",keyend + 1));
	
	log.info("found : ${template}");
	
	Collection hotfolders = archive.getList("hotfolder");
	boolean foundone = false;
	for(Data folder in hotfolders)
	{
		String type = folder.get("hotfoldertype");
		if( type.equals("resiliodrive") )
		{
			if( foundone )
			{
				output.append(",");
			}
			foundone = true;
			log.info("found : ${folder.getName()} ${template}");
			String key = folder.get("secretkey");
			String externalpath = folder.get("externalpath");
			log.info("Key ${key} ${externalpath}");
			String newfolder = template.replace("%SECRET%",key).replace("%PATH%",externalpath);
			output.append(newfolder);
			log.info("ending with : ${newfolder}");
		}
	}
	String footer = content.substring(keyend + template.length(), content.length());
	log.info("ending with : ${footer}");
	output.append(footer);
		
	templatepage = archive.getPageManager().getPage("/WEB-INF/bin/linux/sync.conf");
	archive.getPageManager().saveContent(templatepage,null,output.toString(),null);
	
	List<String> com = Arrays.asList("restart",templatepage.getContentItem().getAbsolutePath());
	
	Exec exec = (Exec)moduleManager.getBean(archive.getCatalogId(),"exec");
	
	ExecResult result = exec.runExec("setupresiliodrive",com,true);
	if( !result.isRunOk() )
	{
		throw new OpenEditException("Could not setup resiliodrive:" +  result.getStandardError());
	}

		
}
	
init();