package org.entermediadb.elasticsearch.searchers;

import org.entermediadb.asset.orders.Order;
import org.entermediadb.asset.orders.OrderManager;
import org.openedit.Data;
import org.openedit.data.PropertyDetails;
import org.openedit.users.User;

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
	protected void saveToElasticSearch(PropertyDetails inDetails, Data inData, boolean delete, User inUser)
	{
		getOrderManager().loadOrderHistory(getCatalogId(),(Order)inData);
		super.saveToElasticSearch(inDetails, inData, delete, inUser);
	}
	
}
