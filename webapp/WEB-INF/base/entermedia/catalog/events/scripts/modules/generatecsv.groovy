package modules;

import org.entermediadb.asset.util.CSVWriter
import org.openedit.Data
import org.openedit.data.*
import org.openedit.hittracker.HitTracker


public void runExport(){
	log.info("about to start:");

	HitTracker hits = (HitTracker) context.getPageValue("hits");
	if(hits == null){
		String sessionid = context.getRequestParameter("hitssessionid");
		hits = context.getSessionValue(sessionid);
	}
	hits.enableBulkOperations();
	searcherManager = context.getPageValue("searcherManager");
	searchtype = context.findValue("searchtype");
	catalogid = context.findValue("catalogid");
	searcher = searcherManager.getSearcher(catalogid, searchtype);
	boolean friendly = Boolean.parseBoolean(context.getRequestParameter("friendly"));
	String[] detaillist = context.getRequestParameters("detail");
	Collection details = searcher.getDetailsForView("${searchtype}/resultstable",context.getUserProfile());

	if(details == null){
		details = searcher.getPropertyDetails();
	}

	//StringWriter output  = new StringWriter();
	CSVWriter writer  = new CSVWriter(context.getWriter());
	int count = 0;
	headers = new String[details.size()];
	for (Iterator iterator = details.iterator(); iterator.hasNext();)
	{
		PropertyDetail detail = (PropertyDetail) iterator.next();
		headers[count] = detail.getText(context);
		count++;
	}
	writer.writeNext(headers);
	log.info("about to start: " + hits);
	Iterator i = null;
	HitTracker selectedhits = hits.getSelectedHitracker();
	selectedhits.enableBulkOperations();
	
	for (Iterator iterator = selectedhits.iterator(); iterator.hasNext();)
	{
		hit =  iterator.next();
		tracker = searcher.loadData(hit);

		nextrow = new String[details.size()];//make an extra spot for c
		int fieldcount = 0;
		for (Iterator detailiter = details.iterator(); detailiter.hasNext();)
		{
			PropertyDetail detail = (PropertyDetail) detailiter.next();
			String value = tracker.get(detail.getId());
			//do special logic here
			if(detail.isList() && friendly){
				valuelist = tracker.getValues(detail.getId());
				StringBuffer endval = new StringBuffer();
				valuelist.each {
					Data remote  = searcherManager.getData( detail.getListCatalogId(),detail.getListId(), it);

					if(remote != null){
						
						endval.append(remote.getName());
						endval.append(" | ");

					}
				}
				value = endval.toString()
				
			}
			String render = detail.get("render");
			if(render != null)
			{
				value = searcherManager.getValue(detail.getListCatalogId(), render, tracker.getProperties());
			}

			nextrow[fieldcount] = value;

			fieldcount++;
		}
		writer.writeNext(nextrow);
	}
	writer.close();
}


runExport();