package library;
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive

import com.openedit.hittracker.HitTracker
import com.openedit.page.manage.*
import com.openedit.util.Exec
import com.openedit.util.ExecResult

public void init() {
	String id = context.getRequestParameter("id");

	Data library = context.getPageValue("data");
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	if(library == null){
		if( id == null) {
			id = context.getRequestParameter("id.value");
		}
		if( id == null) {
			return;
		}
		library = mediaArchive.getSearcher("library").searchById(id);
	}


	Searcher libraryusers = mediaArchive.getSearcher("libraryusers");
	if( library != null ) {
		String username = context.getUserName();
		if(username != null){
			HitTracker userlist = libraryusers.fieldSearch("library", library.id);
			if(userlist.size() == 0){
				Data newentry = libraryusers.createNewData();
				newentry.setId(libraryusers.nextId());
				newentry.setProperty("userid", username);
				newentry.setProperty("libraryid", library.getId());
				newentry.setProperty("libraryrole", "owner");//not used yet.
				newentry.setSourcePath(library.getSourcePath());
				libraryusers.saveData(newentry, context.getUser());

			}
			
		}





		String path = library.get("gitpath");
		if( path != null ) {
			Exec exec = (Exec)mediaArchive.getModuleManager().getBean("exec");
			List com = new ArrayList();
			com.add(library.getId());
			com.add(path);

			ExecResult result = exec.runExec("gitaddrepository", com);
			//return result.isRunOk();
		}
	}
}

init();

