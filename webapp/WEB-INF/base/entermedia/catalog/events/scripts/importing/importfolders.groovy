package importing

import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.HotFolderManager
import org.openedit.Data
import org.openedit.repository.ContentItem
import org.openedit.util.Exec
import org.openedit.util.ExecResult

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	HotFolderManager manager = (HotFolderManager)moduleManager.getBean("hotFolderManager");
	manager.scanFolders(archive,log);
}
init();
