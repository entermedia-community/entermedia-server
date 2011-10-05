package org.openedit.entermedia.orders;

import java.util.Calendar;
import java.util.Date;

import org.openedit.Data;
import org.openedit.util.DateStorageUtil;
import org.openedit.xml.ElementData;

public class Order extends ElementData
{
	protected OrderHistory fieldRecentOrderHistory;

	public OrderHistory getRecentOrderHistory()
	{
		return fieldRecentOrderHistory;
	}

	public void setRecentOrderHistory(OrderHistory inRecentOrderHistory)
	{
		fieldRecentOrderHistory = inRecentOrderHistory;
	}

	public String findValue(Data inChild, String inKey)
	{
		String value = inChild.get(inKey);
		if (value == null)
		{
			value = get(inKey);
		}
		return value;
	}

	public String getOrderStatus()
	{
		return get("orderstatus");  //open/closed/error
	}
	public void setOrderStatus(String inStatus)
	{
		setProperty("orderstatus", inStatus);
	}

	public String toString()
	{
		return getId();
	}
	
	public boolean isExpired(){
		String expStorageDate = getStoredExpirationDate();
		Date expDate = DateStorageUtil.getStorageUtil().parseFromStorage(expStorageDate);
		Date today = Calendar.getInstance().getTime();
		return expDate.before(today);
	}

	protected String getStoredExpirationDate()
	{
		return get("expireson");
	}
	
	@Override
	public String get(String inId)
	{
		if( inId.startsWith("history"))
		{
			String key = inId.substring(7);
			return getRecentOrderHistory().get(key); //may be OrderHistory.EMPTY
		}
		return super.get(inId);
	}
	
}
