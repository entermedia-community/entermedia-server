package org.openedit.entermedia.orders;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.DocumentHelper;
import org.openedit.Data;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventHandler;
import org.openedit.util.DateStorageUtil;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;

public class OrderManager 
{
	protected SearcherManager fieldSearcherManager;
	protected WebEventHandler fieldWebEventHandler;
	
	public WebEventHandler getWebEventHandler() {
		return fieldWebEventHandler;
	}

	public void setWebEventHandler(WebEventHandler inWebEventHandler) {
		fieldWebEventHandler = inWebEventHandler;
	}

	public SearcherManager getSearcherManager() 
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) 
	{
		fieldSearcherManager = inSearcherManager;
	}

	public Data placeOrder(String frontendappid, String inCatlogId, User inUser, HitTracker inAssets, Map inProperties)
	{
		Searcher searcher = getSearcherManager().getSearcher(inCatlogId, "order");
		Data order = createNewOrder(frontendappid, inCatlogId,inUser.getUserName());
		
		for (Iterator iterator = inProperties.keySet().iterator(); iterator.hasNext();) 
		{
			String key = (String) iterator.next();
			order.setProperty(key, (String)inProperties.get(key));
			
		}
		
		searcher.saveData(order, inUser);
		
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatlogId, "orderitem");
		
		List items = new ArrayList();
		for (Iterator iterator = inAssets.iterator(); iterator.hasNext();) 
		{
			Data asset = (Data) iterator.next();
			Data item = itemsearcher.createNewData();
			item.setProperty("orderid", order.getId());
			item.setProperty("userid", inUser.getId());
			
			item.setSourcePath(order.getSourcePath()); //will have orderitem.xml added to it
			item.setProperty("assetsourcepath", asset.getSourcePath());
			item.setProperty("assetid", asset.getId());
			item.setProperty("status", "requestreceived");
			//TODO: any other params like notes?
			items.add(item);
		}
		itemsearcher.saveAllData(items, inUser);
		WebEvent event = new WebEvent();
		event.setOperation("orderplaced");
		event.setSourcePath(order.getSourcePath());
		event.setSearchType("order");
		getWebEventHandler().eventFired(event);
		
		return order;
	}

	public HitTracker findOrdersForUser(String inCatlogId, User inUser) 
	{
		Searcher ordersearcher = getSearcherManager().getSearcher(inCatlogId, "order");
		SearchQuery query = ordersearcher.createSearchQuery();
		query.addOrsGroup("orderstatus","ordered complete");
		GregorianCalendar cal = new GregorianCalendar();
		cal.add(Calendar.MONTH, -1);
		query.addAfter("date", cal.getTime());
		query.addSortBy("historydateDown");
		return ordersearcher.search(query);
	}
	public void loadOrderHistoryForPage(HitTracker inPage)
	{
		for (Iterator iterator = inPage.getPageOfHits().iterator(); iterator.hasNext();)
		{
			Order order = (Order) iterator.next();
			loadOrderHistory(inPage.getCatalogId(), order);
		}
	}

	public OrderHistory loadOrderHistory(String inCataId, Order order)
	{
		if( order.getRecentOrderHistory() == null)
		{
			OrderHistory history = findRecentOrderHistory(inCataId,order.getId());
			if( history == null)
			{
				history = OrderHistory.EMPTY;
			}
			order.setRecentOrderHistory(history);
		}
		return order.getRecentOrderHistory();
	}
	public HitTracker findOrderAssets(String inCatalogid, String inOrderId) 
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatalogid, "orderitem");
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addExact("orderid", inOrderId);
		HitTracker items =  itemsearcher.search(query);
		return items;
	}
	public HitTracker findOrderHistory(String inCatalogid, Order inOrder) 
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatalogid, "orderhistory");
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addExact("orderid", inOrder.getId());
		query.addSortBy("dateDown");
		HitTracker items =  itemsearcher.search(query);

		OrderHistory history = OrderHistory.EMPTY;; 
		Data hit = (Data)items.first();
		if( hit != null)
		{
			history = (OrderHistory)itemsearcher.searchById(hit.getId());
		}
		
		inOrder.setRecentOrderHistory(history);
		
		return items;
	}
	public OrderHistory findRecentOrderHistory(String inCatalogid, String inOrderId) 
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatalogid, "orderhistory");
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addExact("orderid", inOrderId);
		query.addSortBy("dateDown");
		Data index =  (Data)itemsearcher.uniqueResult(query);
		if( index != null)
		{
			OrderHistory history = (OrderHistory)itemsearcher.searchById(index.getId());
			return history;
		}
		return null;
	}

	public Order loadOrder(String catalogid, String orderid) 
	{
		Searcher ordersearcher = getSearcherManager().getSearcher(catalogid, "order");
		Order order =  (Order) ordersearcher.searchById(orderid);
		loadOrderHistory(catalogid,order);
		return order;
	}

	public Order createNewOrder(String inAppId, String inCatalogId, String inUsername)
	{
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "order");
		Order order  = (Order)searcher.createNewData();
		order.setElement(DocumentHelper.createElement(searcher.getSearchType()));
		order.setId(searcher.nextId());
		order.setProperty("orderstatus", "ordered");
		order.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		order.setSourcePath(inUsername + "/" + order.getId());
		order.setProperty("userid", inUsername);
		order.setProperty("applicationid",inAppId);
		return order;
	}

	public Data addItemToBasket(String inCatId, Order order, Asset inAsset, Map inProps)
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatId, "orderitem");
		Data item = itemsearcher.createNewData();

		item.setProperty("orderid", order.getId());
		item.setProperty("userid", order.get("userid"));
		
		item.setSourcePath(order.getSourcePath()); //will have orderitem.xml added to it
		item.setProperty("assetsourcepath", inAsset.getSourcePath());
		item.setProperty("assetid", inAsset.getId());
		item.setProperty("status", "preorder");
		if(inProps != null){
		for (Iterator iterator = inProps.keySet().iterator(); iterator.hasNext();)
		{
			String  key = (String ) iterator.next();
			item.setProperty(key, (String)inProps.get(key));
		}
		}
		itemsearcher.saveData(item, null);
		return item;
	}

	public void saveOrder(String inCatalogId, User inUser, Order inBasket)
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatalogId, "order");
		itemsearcher.saveData(inBasket, inUser );
	}

	public void placeOrder(WebPageRequest inReq, MediaArchive inArchive, Order inOrder)
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(inArchive.getCatalogId(), "orderitem");
		HitTracker all = itemsearcher.fieldSearch("orderid", inOrder.getId());
		List tosave = new ArrayList();
		
		//TODO: deal with table of assets
		String[] fields = inReq.getRequestParameters("field");
		for (Iterator iterator = all.iterator(); iterator.hasNext();)
		{
			Data item = (Data) iterator.next();
			Data row = (Data)itemsearcher.searchById(item.getId());
			row.setProperty("status", "requestreceived");
			if( fields != null)
			{
				for (int i = 0; i < fields.length; i++)
				{
					String val = inReq.getRequestParameter(fields[i] + ".value");
					if( val != null)
					{
						row.setProperty(fields[i], val);
					}
				}
			}
			tosave.add(row);
		}
		itemsearcher.saveAllData(tosave,null);
		
		//Change the status and save order
		//TODO: Add history
		saveOrder(inArchive.getCatalogId(), inReq.getUser(), inOrder);

		WebEvent event = new WebEvent();
		event.setSearchType("order");
		event.setCatalogId(inArchive.getCatalogId());
		event.setOperation("orderplaced");
		event.setUser(inReq.getUser());
		event.setSource(this);
		event.setSourcePath(inOrder.getSourcePath());
		event.setProperty("orderid", inOrder.getId());
		inArchive.getMediaEventHandler().eventFired(event);
	}

	public void saveOrderWithHistory(String inCatalogId, User inUser, Order inOrder, OrderHistory inHistory)
	{
		inHistory.setProperty("userid",inUser.getId() );
		inHistory.setProperty("orderid",inOrder.getId() );
		Searcher historysearcher = getSearcherManager().getSearcher(inCatalogId, "orderhistory");
		inOrder.setRecentOrderHistory(inHistory);
		saveOrder(inCatalogId, inUser, inOrder);
		historysearcher.saveData(inHistory, inUser);
	}

	public OrderHistory createNewHistory(String inCatId, Order inOrder, User inUser, String inStatus)
	{
		Searcher historysearcher = getSearcherManager().getSearcher(inCatId, "orderhistory");
		OrderHistory history = (OrderHistory)historysearcher.createNewData();
		history.setUserStatus(inStatus);
		history.setProperty("userid", inUser.getId());
		history.setSourcePath(inOrder.getSourcePath());
		history.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));

		return history;
	}


}
