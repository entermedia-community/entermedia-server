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
		MediaArchive archive = getMediaArchive(inReq);
		Searcher payments = archive.getSearcher("transaction");
		Data payment = payments.createNewData();
		payments.updateData(inReq, inReq.getRequestParameters("field"), payment);
		
		payment.setValue("paymentdate",new Date() );
		payment.setValue("paymenttype","paypal" );
		
		//Get the email
		if( payment.get("userid") == null)
		{
			String email = inReq.getRequestParameter("paymentemail.value");
			User user = getMediaArchive(inReq).getUserManager().getUserByEmail(email);
			if( user != null)
			{
				payment.setValue("userid",user.getId());
			}
		}
		
		
		payments.saveData(payment);
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
