import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager

import com.openedit.WebPageRequest
import com.openedit.entermedia.scripts.EnterMediaObject
import com.openedit.hittracker.HitTracker

public class editor extends EnterMediaObject {



	public void batchSave(){
		SearcherManager sm = getModuleManager().getBean("searcherManager");
		String catalogid = context.findValue("catalogid");
		String searchtype = context.findValue("searchtype");
		String sessionid = context.findValue("hitssessionid");
		Searcher searcher = sm.getSearcher(catalogid, searchtype);
		HitTracker targets = context.getSessionValue(sessionid);
		ArrayList toSave = new ArrayList();

		for (Iterator iterator = targets.getSelectedHits().iterator(); iterator.hasNext();) {
			Data hit = (Data) iterator.next();
			Object real = searcher.searchById(hit.getId());
			String[] fields = context.getRequestParameters("field");
			//			public Data updateData(WebPageRequest inReq, String[] fields, Data data);
			if(real != null){
				searcher.updateData(context, fields, real);
				toSave.add(real);
			}

		}
		searcher.saveAllData(toSave, context.getUser());

	}


}