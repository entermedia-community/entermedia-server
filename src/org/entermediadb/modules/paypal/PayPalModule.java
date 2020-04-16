package org.entermediadb.modules.paypal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.modules.scriptrunner.ScriptModule;
import org.openedit.WebPageRequest;

public class PayPalModule
{
	private static Log log = LogFactory.getLog(PayPalModule.class);
	
	public void payEventReceived(WebPageRequest inReq)
	{
		//Check keys?
		log.info("Received " + inReq.getJsonRequest());
		//Process event
	}
}
