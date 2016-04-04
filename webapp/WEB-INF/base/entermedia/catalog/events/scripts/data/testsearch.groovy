package data;

import org.entermediadb.asset.MediaArchive
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker


public void init(){

	MediaArchive mediaarchive = context.getPageValue("mediaarchive");
	Searcher cats = mediaarchive.getSearcher("category");
	HitTracker results = cats.query().all().search();
	log.info("results" + results.hashCode());
	log.info("Page 1");
	results.setPage(1);
	log.info(results.getPageOfHits() );
	log.info("Page 7");
	results.setPage(7);
	log.info(results.getPageOfHits() );
	
	//Thread.sleep(5000);
	HitTracker results2 = cats.query().all().search();
	results2.setPage(7);
	log.info("Page 7 fresh");
	log.info(results2.getPageOfHits() );

}

init();