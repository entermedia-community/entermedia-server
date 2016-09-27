package hotfolder;

public void init()
{
	log.info("QueueAsset running");
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	Page templatepage = archive.getPageManager().getPage("/WEB-INF/bin/linux/sync.conf.template");
	
	String content = templatepage.getContent();
	
	StringBuffer output = new StringBuffer();
	
	int keystart = content.indexOf("\"shared_folders\":[");
	int keyend = end + "\"shared_folders\":[".length();
	
	output.append(content.substring(0,keystart);	


	String template =  content.substring(keyend,content.indexOf(keyend,"]"));

	Collection hotfolders = archive.getList("hotfolder");
	for(Data folder in hotfolders)
	{
		String type = folder.get("hotfoldertype");
		if( type.equals("resiliodrive") )
		{
			String enabled = folder.get("enabled");
			if( Boolean.parseBoolean(enabled))
			{		
				String key = folder.get("accesskey");
				String externalpath = folder.get("externalpath");
				String folder = template.replace("%SECRET%",key);
				folder = template.replace("%PATH%",externalpath);
				output.append(folder);
			}
		}	
	}
	output.append(content.substring(keyend + template.length() + 1, content.length());
		
	templatepage = archive.getPageManager().getPage("/WEB-INF/bin/linux/sync.conf");
	archive.getPageManager().saveContent(templatepage,null,output.toString(),null);
	
	List<String> com = Arrays.asList("restart",templatepage.getContentItem().getAbsolutePath());
	ExecResult result = getExec().runExec("setupresiliodrive",com,true);
	if( !result.isRunOk() )
	{
		throw new OpenEditException("Could not setup resiliodrive:" + toplevelfolder + " " +  result.getStandardError());
	}	
	
	
init();