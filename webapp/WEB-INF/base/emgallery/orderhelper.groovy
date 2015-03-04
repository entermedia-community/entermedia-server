import org.openedit.Data
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.modules.OrderModule
import org.openedit.entermedia.orders.Order

import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker


public void init(){
	WebPageRequest req = context;
	MediaArchive archive = req.getPageValue("mediaarchive");
	OrderModule ordermodule = archive.getModuleManager().getBean("OrderModule");
	Order order = ordermodule.loadOrder(req);
	if (order){
		int max = 4;
		List<Data> list = new ArrayList<Data>();
		String catalogid = req.findValue("catalogid");
		String orderid = order.getId();
		HitTracker items = ordermodule.getOrderManager().findAssets(req, catalogid, order);
		items.each{
			if (list.size() == max){
				return;
			}
			list.add(0,(Data)it);//add to tbe beginning
		}
		req.putPageValue("orderassets", list);
		req.putPageValue("number","${items.size()}");
	}
}

init();