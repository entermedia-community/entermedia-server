package org.entermediadb.websocket.mediaboat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.websocket.RemoteEndpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;

public class TransactionQueue
{
	protected Object lockObject = new Object();
	protected long transactionid;
	protected Map fieldTransactions = null; 
	protected long fieldMaxWait = 60000; //1m
	
	private static final Log log = LogFactory.getLog(MediaBoatConnection.class);


	public void onMessage(JSONObject map)
	{
		String transactionid = (String)map.get("transactionid");
		if( transactionid != null)
		{
			getTransactions().put(transactionid,map);
			//log.info("Upload saved transaction " + transactionid);
			synchronized (lockObject)
			{
				//log.info("notify.before " + transactionid);
				lockObject.notifyAll();					
				//log.info("notify.done " + transactionid);
			}
		}
	}
	protected Map getTransactions()
	{
		if (fieldTransactions == null)
		{
			fieldTransactions = new HashMap(); //expire this sometimes
		}
		return fieldTransactions;
	}

	public Map sendCommandAndWait(RemoteEndpoint.Basic remoteEndpointBasic, JSONObject inCommand)
	{
		try
		{
			String command = (String)inCommand.get("command");
			transactionid++;
			String thistransaction = System.currentTimeMillis() + "_" + String.valueOf(transactionid);
			inCommand.put("transactionid",thistransaction);
			remoteEndpointBasic.sendText(inCommand.toJSONString());
			//log.info("sent " + command + " to  " + getCurrentConnectionId() );
			
			long waittill= System.currentTimeMillis() + fieldMaxWait; //1 minute max
			Map response = null;
			synchronized (lockObject)
			{
				do
				{
					response = getTransctionsById(thistransaction);   //TODO use notify with timeout
					if( response == null)
					{
						long wait = waittill - System.currentTimeMillis();
						if( wait > 0)
						{
							log.info("wait " + thistransaction);
							lockObject.wait(wait);	
							log.info("continue from wait" + thistransaction);
						}
					}
					log.info( (response != null) + " and " + (System.currentTimeMillis() < waittill) );
				} while (response == null && System.currentTimeMillis() < waittill);
			}	
			if( response == null)
			{
				log.error("Never got back a transaction " + thistransaction);
			}
			else
			{
				getTransactions().remove(thistransaction);
			}
			
			//remove expired transactions
			expireOldTransactions();
			
			return response;
		}
		catch (Exception e)
		{
			log.error(e);
		}
		return null;
	}

	protected void expireOldTransactions()
	{
		if( !getTransactions().isEmpty())
		{
			List toremove = new ArrayList();
			long now = System.currentTimeMillis();
			for (Iterator iterator = getTransactions().keySet().iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				String time = key.substring(0,key.indexOf("_"));
				if( now > Long.parseLong(time))
				{
					toremove.add(key);
				}
			}
			for (Iterator iterator = toremove.iterator(); iterator.hasNext();)
			{
				String key = (String) iterator.next();
				log.error("Expired transation " + key );
				getTransactions().remove(key);
				
			}
		}
		
	}
	protected Map getTransctionsById(String inThistransaction)
	{
		Map res =  (Map)getTransactions().get(inThistransaction);
		return res;
	}

}
