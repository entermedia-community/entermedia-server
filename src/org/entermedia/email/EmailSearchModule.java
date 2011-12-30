package org.entermedia.email;

import org.openedit.data.SearcherManager;

import com.openedit.WebPageRequest;
import com.openedit.modules.BaseModule;

public class EmailSearchModule extends BaseModule{

	
	
	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}
	protected SearcherManager fieldSearcherManager;

	public EmailArchive getEmailArchive(WebPageRequest inReq){
		String catalogid = inReq.findValue("catalogid");
		EmailSearcher searcher = (EmailSearcher) getSearcherManager().getSearcher(catalogid, "email");
		return searcher.getEmailArchive();
		
		
		
	}
		


	
	public void reindexAll(WebPageRequest inReq){
		getEmailArchive(inReq).getEmailSearcher().reIndexAll();
		
	}
	public void fieldSearch(WebPageRequest inPageRequest) throws Exception
	{
		EmailArchive arc = getEmailArchive(inPageRequest);
		arc.getEmailSearcher().fieldSearch(inPageRequest);
		inPageRequest.putPageValue("searcher", arc.getEmailSearcher());
	}
	public void loadEmail(WebPageRequest inReq) throws Exception
	{
		String id = inReq.getRequestParameter("id");
		TemplateWebEmail email =  (TemplateWebEmail) getEmailArchive(inReq).getEmailSearcher().searchById(id);
		
		inReq.putPageValue("email", email);
	}
}
