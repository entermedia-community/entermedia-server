package org.entermediadb.asset.publish.publishers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.orders.Order;
import org.entermediadb.asset.publishing.BasePublisher;
import org.entermediadb.asset.publishing.PublishResult;
import org.entermediadb.asset.publishing.Publisher;
import org.openedit.Data;
import org.openedit.OpenEditException;
import org.openedit.util.DateStorageUtil;

public class emailpublisher extends BasePublisher implements Publisher
{
	private static final Log log = LogFactory.getLog(emailpublisher.class);

	public PublishResult publish(MediaArchive mediaArchive,Order inOrder, Data inOrderItem, Data inDestination, Data inPreset, Asset inAsset)
	{
		PublishResult result = 	checkOnConversion(mediaArchive,inOrderItem,inAsset,inPreset);
		
		if( !result.isReadyToPublish() )
		{
			return result;
		}
		//Make sure all items are ready then send the email
		
		//Send the email and mark as complete
		String emailto = inOrder.get("sharewithemail");
		//String notes = inOrder.get("sharenote");

		if(emailto != null && inOrder.getInt("itemerrorcount") == 0)
		{
			String appid  = inOrder.get("applicationid");
			if( appid == null)
			{
				throw new OpenEditException("applicationid is required");
			}
			String userid = inOrderItem.get("userid");
			if( userid == null)
			{
				throw new OpenEditException("userid is required");
			}
			String template = null;
			
			if( "checkout".equals( inOrder.get("ordertype")) )
			{
				template = "/" + appid + "/theme/emails/checkouttemplate.html";
			}
			else
			{
				template = "/" + appid + "/theme/emails/sharetemplate.html";
			}
			Map params = new HashMap();
			params.put("order",inOrder);
			
			String expireson = inOrder.get("expireson");
			if ((expireson!=null) && (expireson.trim().length()>0))
			{
				Date date = DateStorageUtil.getStorageUtil().parseFromStorage(expireson);
				params.put("expiresondate", date);
				params.put("expiresformat", new SimpleDateFormat("MMM dd, yyyy"));
			}
			try
			{
				mediaArchive.sendEmail(userid,params,emailto,template);
			}
			catch (Exception ex)
			{
				result.setErrorMessage(ex.getMessage());
				return result;
			}
			result.setComplete(true);
		}
		return result;
	}
	
}

