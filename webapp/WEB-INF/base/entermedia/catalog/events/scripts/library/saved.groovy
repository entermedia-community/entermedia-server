package library;
import java.util.*

import org.openedit.data.Searcher
import org.openedit.entermedia.Asset 
import org.openedit.entermedia.MediaArchive

import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.manage.*
import com.openedit.util.ExecResult;

public void init()
{
	String id = context.getRequestParameter("id");
	if( id == null)
	{
		id = context.getRequestParameter("id.value");
    }
	if( id == null)
	{
		return;
	}
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	Data library = mediaArchive.getSearcher("library").searchById(id);
	if( library != null )
	{
		String path = library.get("gitpath");
		if( path != null )
		{
			Exec exec = (Exec)mediaArchive.getModuleManager().getBean("exec");
			List com = new ArrayList();
			com.add(library.getId());
			com.add(path);
			
			ExecResult result = exec.runExec("gitaddrepository", com);
			return result.isRunOk();
		}	
	}
}

init();

