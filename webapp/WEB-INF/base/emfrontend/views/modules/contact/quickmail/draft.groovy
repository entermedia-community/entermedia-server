import org.openedit.Data

import com.openedit.hittracker.ListHitTracker


manager = context.getPageValue("searcherManager");
String sessionid = context.findValue("hitssessionid");
hits =  context.getSessionValue(sessionid);
if(hits != null){
	System.out.println("saving hits in session");
	context.putSessionValue("mailhits", hits);
} else{
	

	System.out.println("NO HITS!!");
	
	String dataid = context.getRequestParameter("id");
	Data data = context.getPageValue("data");
	if(dataid != null){
		ListHitTracker list  = new ListHitTracker();
		list.add(data);
		list.setAllSelected(true);
		context.putSessionValue("mailhits", list);
	}else{
	
	context.putSessionValue("mailhits", null);
	}
}

