package org.entermediadb.elasticsearch.searchers;

import org.entermediadb.asset.orders.Order;
import org.entermediadb.asset.orders.OrderManager;
import org.entermediadb.asset.orders.OrderSearcher;
import org.openedit.Data;
import org.openedit.data.PropertyDetails;
import org.openedit.users.User;

public class ElasticOrderSearcher extends BaseElasticSearcher implements OrderSearcher
{
	
	public OrderManager getOrderManager()
	{
		return (OrderManager)getModuleManager().getBean(getCatalogId(),"orderManager");
	}

	public Data createNewData()
	{
		Order order = (Order)super.createNewData();
		order.setCatalogId(getCatalogId());
		order.setOrderManager(getOrderManager());
		return order;
	}
	
//	protected void updateIndex(Data inData, Document doc, PropertyDetails inDetails) 
//	{
//		getOrderManager().loadOrderHistory(getCatalogId(),(Order)inData);
//		super.updateIndex(inData, doc, getPropertyDetails());
//	}
	
	
	
	
}
