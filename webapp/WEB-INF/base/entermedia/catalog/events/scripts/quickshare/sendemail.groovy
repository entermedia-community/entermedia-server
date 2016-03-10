package quickshare;

import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.openedit.data.Searcher
import org.openedit.entermedia.*
import org.openedit.entermedia.creator.*
import org.openedit.entermedia.edit.*
import org.openedit.entermedia.episode.*
import org.openedit.entermedia.modules.*
import org.openedit.entermedia.orders.Order
import org.openedit.entermedia.orders.OrderSearcher
import org.openedit.entermedia.util.*
import org.openedit.xml.*

import com.openedit.WebPageRequest
import com.openedit.hittracker.*
import com.openedit.page.*
import com.openedit.users.User
import com.openedit.util.*


public void handleUpload() {
	MediaArchive archive = (MediaArchive)context.getPageValue("mediaarchive");
	OrderSearcher ordersearcher = archive.getSearcher("order");
	Searcher itemsearcher = archive.getSearcher("orderitem");
	
	User user = context.getUser();
	if(user == null){
		user = archive.getUserManager().createGuestUser("anonymous", "anonymous", "anonymous");
		user.setVirtual(true);
	}
	Order order = context.getSessionValue("quickshareorder");
	if(order ==  null){
		return;
	}	
	
	HitTracker orderitems = itemsearcher.query().match("orderid", order.getId()).search();
//	itemsearcher.saveAllData(orderitems, null);
	
	context.putPageValue("orderitems", orderitems);
	String from = context.getRequestParameter("email.value");
	context.putPageValue("fromemail", from);
	context.putPageValue("order", order);
	String to = context.getRequestParameter("destination.value");
	String sendfrom = context.findValue("quicksharefrom");
	sendEmail(context,  to,sendfrom, "/${context.findValue('applicationid')}/components/quickshare/sharetemplate.html");
	sendEmail(context,  from,sendfrom, "/${context.findValue('applicationid')}/components/quickshare/sharetemplate.html");
	context.putSessionValue("quickshareorder", null);
	

}

protected void sendEmail(WebPageRequest context, String email, String from,  String templatePage){
	Page template = pageManager.getPage(templatePage);
	WebPageRequest newcontext = context.copy(template);
	TemplateWebEmail mailer = getMail();
	mailer.loadSettings(newcontext);
	mailer.setFrom(from);
	mailer.setMailTemplatePath(templatePage);
	mailer.setRecipientsFromCommas(email);
	mailer.send();
	log.info("conversion error email sent to ${email}");
}

protected TemplateWebEmail getMail() {
	PostMail mail = (PostMail)mediaarchive.getModuleManager().getBean( "postMail");
	return mail.getTemplateWebEmail();
}

handleUpload();

