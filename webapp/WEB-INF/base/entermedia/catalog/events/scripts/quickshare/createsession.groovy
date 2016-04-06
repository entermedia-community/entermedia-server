package quickshare;

import org.openedit.entermedia.*
import org.openedit.entermedia.creator.*
import org.openedit.entermedia.edit.*
import org.openedit.entermedia.episode.*
import org.openedit.entermedia.modules.*
import org.openedit.entermedia.orders.Order
import org.openedit.entermedia.orders.OrderSearcher
import org.openedit.entermedia.util.*
import org.openedit.xml.*

import com.openedit.hittracker.*
import com.openedit.page.*
import com.openedit.util.*


public void handleUpload() {
	
	MediaArchive archive = (MediaArchive)context.getPageValue("mediaarchive");
	OrderSearcher ordersearcher = archive.getSearcher("order");

	Order order = null
	log.info("entering");
	order = context.getSessionValue("quickshareorder");
		if(order == null){
			 order = ordersearcher.createNewData();
			 context.putSessionValue("quickshareorder", order);
			 order.setProperty("emailsent", "false");
			 
			 ordersearcher.saveData(order, null);
			 context.putPageValue("quickshareorder", order);
			 
		
		}
}
handleUpload();

