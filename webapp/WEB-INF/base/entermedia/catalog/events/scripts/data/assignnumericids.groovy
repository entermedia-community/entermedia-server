import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.cluster.IdManager
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker

public void init() {
	
	MediaArchive archive = context.getPageValue("mediaarchive");
	
	Searcher searcher = archive.getSearcher("submission");
	HitTracker submissions = searcher.getAllHits();
	IdManager manager = archive.getModuleManager().getBean(archive.getCatalogId(),"idManager");
	ArrayList everything = new ArrayList();
	submissions.enableBulkOperations();
	submissions.each{
		String id = it.id;
		String worknumber = null;
		if(it.worknumber != null) {
			return;
		}
		
		Data real = searcher.loadData(it);

		if(id.startsWith("A")){
			worknumber = "H" +  manager.nextId("submission");
		} else{
			worknumber= id;
		}
		real.setValue("worknumber", worknumber);
		everything.add(real);
	}
	searcher.saveAllData(everything, null);
	
}


init();