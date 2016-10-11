import java.util.Arrays;
import java.util.List;

import org.entermediadb.asset.MediaArchive
import org.openedit.Data
import org.openedit.OpenEditException
import org.openedit.page.Page
import org.openedit.repository.ContentItem
import org.openedit.util.Exec
import org.openedit.util.ExecResult


public void init()
{ 
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	Collection hotfolders = archive.getList("hotfolder");
	ContentItem homedir = archive.getContent("/WEB-INF/data");
	Exec exec = (Exec)moduleManager.getBean(archive.getCatalogId(),"exec");
	
	for(Data folder in hotfolders)
	{
		String type = folder.get("hotfoldertype");
		if( type.equals("googledrive") )
		{
			log.info("insync save");
			String externalpath = folder.get("externalpath");

			//HOMEDIR=$2 			THEAUTHCODE=$3					HOTFOLDER=$4
			String key = folder.get("accesskey");
			List<String> com = Arrays.asList(homedir.getAbsolutePath(),key, externalpath);
			
			ExecResult result = exec.runExec("setupinsyncdrive",com,true);
			if( !result.isRunOk() )
			{
				throw new OpenEditException("Could not setup insync:" +  result.getStandardError());
			}

		}
	}
	

		
}
	
init();