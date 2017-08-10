package org.entermediadb.asset.orders;

import java.util.Collection;
import java.util.Date;

import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.SaveableData;
import org.openedit.util.DateStorageUtil;

public class Order extends BaseData implements SaveableData, CatalogEnabled
{
	protected OrderHistory fieldRecentOrderHistory;
	protected OrderManager fieldOrderManager;
	protected String fieldCatalogId;
	
	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	public OrderManager getOrderManager() 
	{
		return fieldOrderManager;
	}

	public void setOrderManager(OrderManager inOrderManager) {
		this.fieldOrderManager = inOrderManager;
	}

	public OrderHistory getRecentOrderHistory()
	{
		if( fieldRecentOrderHistory == null)
		{
			fieldRecentOrderHistory = getOrderManager().loadOrderHistory(getCatalogId(), this);
			if( fieldRecentOrderHistory == null)
			{
				fieldRecentOrderHistory = new OrderHistory();
			}
		}
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
	
	public void setOrderStatus(String inStatus, String inDetails)
	{
		setProperty("orderstatus", inStatus);
		setProperty("orderstatusdetails", inDetails);
	}

	public void setOrderStatus(String inStatus)
	{
		setProperty("orderstatus", inStatus);
	}

	public String toString()
	{
		return getId();
	}
	
	@Override
	public Object getValue(String inId)
	{
		if( inId.startsWith("history"))
		{
			String key = inId.substring(7);
			if( getRecentOrderHistory() == null )
			{
				return null;
			}
			return getRecentOrderHistory().get(key); //may be OrderHistory.EMPTY
		}
		return super.getValue(inId);
	}

	@Override
	public void setProperty(String inId, String inValue)
	{
		// TODO Auto-generated method stub
		super.setProperty(inId, inValue);
	}
	
	public boolean isExpired()
	{
		String expiration = get("expireson");
		if( expiration != null )
		{
			Date expires = DateStorageUtil.getStorageUtil().parseFromStorage(expiration);
			if( expires.after(new Date() ) )
			{
				return false;
			}
			else
			{
				return true;
			}
		}
		return false;

	}

	public Collection findOrderAssets()
	{
		return getOrderManager().findOrderAssets(getCatalogId(), getId());
	}
	
}
