import org.entermediadb.asset.MediaArchive

public init(){
	log.info("Starting Pre-Deletion Event");
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	
}

init();