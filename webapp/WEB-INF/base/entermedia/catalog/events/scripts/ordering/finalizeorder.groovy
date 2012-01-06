package ordering;

import java.text.SimpleDateFormat;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker
import com.openedit.page.Page;
import com.openedit.users.User;

import org.entermedia.email.PostMail;
import org.entermedia.email.TemplateWebEmail;
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.modules.OrderModule;
import org.openedit.entermedia.orders.Order;
import org.openedit.entermedia.orders.OrderHistory;
import org.openedit.entermedia.orders.OrderManager;
import org.openedit.event.WebEvent;
import org.openedit.entermedia.orders.OrderManager;
import org.openedit.entermedia.orders.OrderHistory;
import org.openedit.util.DateStorageUtil;

MediaArchive mediaarchive = (MediaArchive)context.getPageValue("mediaarchive");

protected Order getOrder(String inOrderId) {
	Searcher ordersearcher = mediaarchive.getSearcherManager().getSearcher(mediaarchive.getCatalogId(), "order");
	Order order = ordersearcher.searchById(inOrderId);
	return order;
}

protected HitTracker getOrderItems(String inOrderId){
	Searcher orderitemssearcher = mediaarchive.getSearcherManager().getSearcher(mediaarchive.getCatalogId(), "orderitem");
	return orderitemssearcher.fieldSearch("orderid", inOrderId)
}


protected TemplateWebEmail getMail() {
	PostMail mail = (PostMail)mediaarchive.getModuleManager().getBean( "postMail");
	return mail.getTemplateWebEmail();
}

protected void sendEmail(Order inOrder) {

	if (inOrder.get('orderstatus') == 'complete') {
		log.info("Order is aleady completed");
		return;
	}

	try {
		context.putPageValue("orderid", inOrder.getId());
		context.putPageValue("order", inOrder);


		String publishid = inOrder.get("publishdestination");
		String appid = inOrder.get("applicationid");


		String adminEmailAddress = null
		boolean isDownload = inOrder.getOrderType()=="download"
		boolean isPreview = inOrder.getOrderType()=="email"
		
		if(publishid != null){
			Data dest = mediaarchive.getSearcherManager().getData(mediaarchive.getCatalogId(), "publishdestination", publishid);
			adminEmailAddress = dest.get("administrativeemail");
			if(adminEmailAddress != null && !isPreview && !isDownload){
				context.setRequestParameter("ordertype", inOrder.getOrderType());
				sendEmail(context, adminEmailAddress, "/${appid}/views/activity/email/admintemplate.html");
			}
		}
		String emailto = inOrder.get('sharewithemail');
		String notes = inOrder.get('sharenote');
		
		

		if(emailto != null && !isDownload) {
			String expireson=inOrder.get("expireson");
			if ((expireson!=null) && (expireson.trim().length()>0)) {
				Date date = DateStorageUtil.getStorageUtil().parseFromStorage(expireson);
				context.putPageValue("expiresondate", date);
				context.putPageValue("expiresformat", new SimpleDateFormat("MMM dd, yyyy"));
			}
			//email to person specified
			sendEmail(context, emailto, "/${appid}/views/activity/email/sharetemplate.html");
			//email to admin
			sendEmail(context, adminEmailAddress, "/${appid}/views/activity/email/sharetemplate.html");
		}
		if (isDownload)
		{
			String userid = inOrder.get("userid");
			if(userid != null){
				User user = userManager.getUser(userid);
	
				if(user != null){
	
					String owneremail =user.getEmail();
					if(owneremail != null){
						context.putPageValue("sharewithemail", emailto);
						//get order contents and data
						HitTracker itemTracker = getOrderItems(inOrder.getId());
						context.putPageValue("orderitems", itemTracker);
						//					OrderModule module = new OrderModule();
						//					context.putPageValue("orderassets", module.findOrderAssets(context));
						//orderitemsearcher
						//field search based on orderid
						if (emailto != null)
							sendEmail(context, emailto, "/${appid}/views/activity/email/usertemplate.html");
						context.putPageValue("user", user)
						sendEmail(context, owneremail, "/${appid}/views/activity/email/usertemplate.html");
						sendEmail(context, adminEmailAddress, "/${appid}/views/activity/email/usertemplate.html");
					}
				}
			}
		}
		inOrder.setProperty('orderstatus', 'complete');
		inOrder.setProperty('emailsent', 'true');
		
	}
	catch (Exception ex) {
		inOrder.setProperty('orderstatus', 'error');
		inOrder.setProperty('orderstatusdetail', "Could not email " + ex);
		ex.printStackTrace();
		log.error("Could not email " + ex);
	}

	OrderManager manager = moduleManager.getBean("orderManager");
	OrderHistory history = manager.createNewHistory( mediaarchive.getCatalogId(), inOrder, context.getUser(), "ordercomplete" );
	manager.saveOrderWithHistory( mediaarchive.getCatalogId(), context.getUser(), inOrder, history );

	log.info("order is complete ${inOrder.getId()}");
}



WebEvent webevent = context.getPageValue("webevent");
String orderid = webevent.get('orderid');

Order order = getOrder(orderid);
sendEmail(order);

protected void sendEmail(WebPageRequest context, String email, String templatePage){
	//send e-mail
	Page template = pageManager.getPage(templatePage);
	WebPageRequest newcontext = context.copy(template);
	TemplateWebEmail mailer = getMail();
	mailer.loadSettings(newcontext);
	mailer.setMailTemplatePath(templatePage);
	mailer.setRecipientsFromCommas(email);
	//mailer.setMessage(inOrder.get("sharenote"));
	//mailer.setWebPageContext(context);
	mailer.send();
	log.info("email sent to ${email}");
}
