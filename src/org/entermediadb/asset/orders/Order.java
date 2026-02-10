package org.entermediadb.asset.orders;

import java.util.Collection;
import java.util.Date;

import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.DataLoaded;
import org.openedit.data.SaveableData;
import org.openedit.util.DateStorageUtil;

public class Order extends BaseData implements SaveableData, DataLoaded, CatalogEnabled
{
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
