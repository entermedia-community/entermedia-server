package org.openedit.entermedia.orders;

import org.apache.lucene.document.Document;
import org.openedit.Data;
import org.openedit.data.PropertyDetails;
import org.openedit.data.XmlFileSearcher;

public class OrderSearcher extends XmlFileSearcher
{
	
	protected OrderManager fieldOrderManager;
	
	
	public OrderManager getOrderManager()
	{
		return fieldOrderManager;
	}

	public void setOrderManager(OrderManager inOrderManager)
	{
		fieldOrderManager = inOrderManager;
	}

	public Data createNewData()
	{
		Order order = (Order)super.createNewData();
		order.setCatalogId(getCatalogId());
		order.setOrderManager(getOrderManager());
		return order;
	}
	
	
	protected void updateIndex(Data inData, Document doc, PropertyDetails inDetails) 
	{
		//getOrderManager().loadOrderHistory(getCatalogId(),(Order)inData);
		super.updateIndex(inData, doc, getPropertyDetails());
	}
}
