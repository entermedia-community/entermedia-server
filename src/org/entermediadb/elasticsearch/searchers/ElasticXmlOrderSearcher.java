package org.entermediadb.elasticsearch.searchers;

import org.entermediadb.asset.orders.Order;
import org.entermediadb.asset.orders.OrderManager;
import org.openedit.Data;
import org.openedit.data.PropertyDetails;

public class ElasticXmlOrderSearcher extends ElasticXmlFileSearcher
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

//	protected void updateIndex(Data inData, Document doc, PropertyDetails inDetails) 
//	{
//		getOrderManager().loadOrderHistory(getCatalogId(),(Order)inData);
//		super.updateIndex(inData, doc, getPropertyDetails());
//	}
	
	
	@Override
	protected void createContentBuilder(PropertyDetails inDetails, Data inData, String inUser)
	{
		getOrderManager().loadOrderHistory(getCatalogId(),(Order)inData);
		super.createContentBuilder(inDetails, inData, inUser);
	}
	
}
