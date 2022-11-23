package org.entermediadb.modules.paypal;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.modules.BaseMediaModule;
import org.openedit.Data;
import org.openedit.WebPageRequest;
import org.openedit.data.Searcher;
import org.openedit.users.User;
/**
 
 https://developer.paypal.com/docs/checkout/integration-features/auth-capture/#integrate-authorize-capture
 
sb-sa4oe1476836@personal.example.com
MiI3&3e8
 * 
 * 
 * @author shanti
 *
 */
public class PayPalModule extends BaseMediaModule
{
	private static Log log = LogFactory.getLog(PayPalModule.class);
	
	public void payEventReceived(WebPageRequest inReq)
	{
		//Check keys?
		log.info("Received " + inReq.getJsonRequest());
		//Process event
	}
	
	
	public void processPayment(WebPageRequest inReq)
	{
		String email = inReq.getRequestParameter("paymentemail.value");
		
		if( email == null)
		{
			log.error("No data found");
			return;
		}
		
		String authorizationid = inReq.getRequestParameter("authorizationid.value");
		if( authorizationid == null || authorizationid.length() < 9)
		{
			log.error("Not a valid transaction " + authorizationid);
			return;
		}
		
		
		MediaArchive archive = getMediaArchive(inReq);
		String username =  inReq.getUserName();
		User user = inReq.getUser();
		String collectionid = inReq.findValue("collectionid");
		inReq.putPageValue("collectionid", collectionid);
		Searcher payments = archive.getSearcher("transaction");
		Data payment = payments.createNewData();
		
		payments.updateData(inReq, inReq.getRequestParameters("field"), payment);
			
		payment.setValue("paymentdate",new Date() );
		payment.setValue("paymenttype","paypal" );
		
//		if( user == null && payment.get("userid") != null) {
//			user = getMediaArchive(inReq).getUserManager().getUser(payment.get("userid"));
//		}
//		if( user == null) {
		user = archive.getUserManager().getUserByEmail(email);
		//Crate a guest user
		if( user == null)
		{
			String first = inReq.getRequestParameter("firstName.value");
			String last = inReq.getRequestParameter("lastName.value");
			user = archive.getUserManager().createGuestUser(null, null, "paypal");
			user.setVirtual(false);
			user.setFirstName(first);
			user.setLastName(last);
			user.setEmail(email);
			archive.getUserManager().saveUser(user);
		}
		payment.setValue("userid",user.getId());
		
		
		Boolean isdonation = Boolean.parseBoolean(inReq.getRequestParameter("isdonation"));
		payment.setValue("isdonation", isdonation );
		payment.setValue("receiptstatus", "new");
		
		payments.saveData(payment);
		inReq.putPageValue("payment", payment);
			
		/*
		//TODO: in case different receipt required.
		//Donation Receipt
		if (isdonation) {
			Searcher donationreceipt = archive.getSearcher("donationreceipt");
			Data receipt = donationreceipt.createNewData();
			receipt.setValue("paymentid", payment.getId());
			receipt.setValue("amount", payment.getValue("totalprice"));
			receipt.setValue("donor", user.getName());
			receipt.setValue("donoremail", user.getEmail());
			receipt.setValue("collectionid", collectionid);
			//receipt.setValue("paymentdate", paymentdate);
			receipt.setValue("receiptstatus", "new");
			
			donationreceipt.saveData(receipt);
			
			inReq.putPageValue("receipt", receipt);
		}
		*/
		
		
		/*
		boolean success = getOrderProcessor().process(archive, inReq.getUser(), payment, token);
		if (success)
		{
			payment.setValue("paymentdate", new Date());
			payment.setValue("userid", inReq.getUserName());
			String frequency = inReq.findValue("frequency");
			if (frequency != null && frequency != "")
			{
				Searcher plans = archive.getSearcher("paymentplan");
				Data plan = plans.createNewData();
				plan.setValue("userid", inReq.getUserName());
				plan.setValue("frequency", frequency);
				plan.setValue("amount", payment.getValue("totalprice"));
				plan.setValue("lastprocessed", new Date());
				plan.setValue("planstatus", "active");
				plans.saveData(plan);
				payment.setValue("paymentplan", plan.getId());
			}

			String invoicepayment = inReq.findValue("invoicepayment");
			if ("true".equals(invoicepayment))
			{
				Data invoice = loadCurrentCart(inReq);
				invoice.setValue("paymentstatus", "paid");
				invoice.setValue("paymentdate", new Date());
				invoice.setValue("owner", inReq.getUserName());
				invoice.setValue("transaction", payment.getId());
				String collectionid = inReq.findValue("collectionid");
				invoice.setValue("collectionid", collectionid);
				archive.saveData("collectioninvoice", invoice);
				inReq.removeSessionValue("current-cart");
			}

			payments.saveData(payment);
		}
		*/
	}
}
