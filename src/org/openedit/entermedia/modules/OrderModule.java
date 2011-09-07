package org.openedit.entermedia.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.albums.Album;
import org.openedit.entermedia.orders.Order;
import org.openedit.entermedia.orders.OrderHistory;
import org.openedit.entermedia.orders.OrderManager;
import org.openedit.util.DateStorageUtil;

import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.ListHitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;

public class OrderModule extends BaseMediaModule
{
	private static final Log log = LogFactory.getLog(OrderModule.class);
	protected OrderManager fieldOrderManager;

	public OrderManager getOrderManager()
	{
		return fieldOrderManager;
	}

	public void setOrderManager(OrderManager inOrderManager)
	{
		fieldOrderManager = inOrderManager;
	}

	public Data placeOrder(WebPageRequest req)
	{
		String catalogid = req.findValue("catalogid");

		Album album = getEnterMedia(req).getAlbumArchive().loadAlbum("4", req.getUserName());
		HitTracker assets = album.getAssets(catalogid, req);
		if (assets.size() > 0)
		{
			Map props = new HashMap();

			String applicationid = req.findValue("applicationid");
			Data order = getOrderManager().placeOrder(applicationid, catalogid, req.getUser(), assets, props);
			req.putPageValue("order", order);
			req.setRequestParameter("orderid", order.getId());
			List realassets = new ArrayList();
			// this is really bizarre. We're loading the assets into memory
			// simply to have them turned into BaseData objects.
			// Need a better way to remove stuff from an album.
			for (Iterator iterator = assets.iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				Asset asset = getMediaArchive(catalogid).getAsset(hit.getId());
				if (asset != null)
				{
					realassets.add(asset);
				}
			}
			album.removeAssets(realassets, req);

			// <property name="subject">Order Placed</property>
			// TODO: Move these to generic fields
			String prefix = req.findValue("subjectprefix");
			if (prefix == null)
			{
				prefix = "Order received:";
			}
			prefix = prefix + " " + order.getId();

			String postfix = req.findValue("subjectpostfix");
			if (postfix != null)
			{
				prefix = prefix + " " + postfix;
			}
			req.putPageValue("subject", prefix);

			return order;
		}
		else
		{
			req.setCancelActions(true);
		}

		return null;
	}
	public Data createNewOrder(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		String applicationid = inReq.findValue("applicationid");

		Order order = (Order) getOrderManager().createNewOrder(applicationid, catalogid, inReq.getUserName());
		inReq.putPageValue("order", order);
		inReq.setRequestParameter("orderid", order.getId());
		
		OrderHistory history = getOrderManager().createNewHistory(catalogid,order, inReq.getUser(),"newrecord");
		
		getOrderManager().saveOrderWithHistory(catalogid, inReq.getUser(), order,history);
		return order;
	}
	
	public Data createOrderFromSelections(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		HitTracker assets = null;
		if (hitssessionid != null)
		{
			assets = (HitTracker) inReq.getSessionValue(hitssessionid);
		}
		else
		{
			assets = new ListHitTracker();
			String[] sourcepaths = inReq.getRequestParameters("sourcepath");
			if (sourcepaths == null)
			{
				log.error("No assets passed in");
				return null;
			}
			for (int i = 0; i < sourcepaths.length; i++)
			{
				Data hit = new BaseData();
				hit.setSourcePath(sourcepaths[i]);
				assets.add(hit);
				assets.addSelection(i);
			}
		}

		Searcher itemsearcher = getSearcherManager().getSearcher(catalogid, "orderitem");
		List orderitems = new ArrayList();

		if (assets.getSelectedHits().size() > 0)
		{
			Map props = new HashMap();

			String applicationid = inReq.findValue("applicationid");
			Order order = (Order) getOrderManager().createNewOrder(applicationid, catalogid, inReq.getUserName());
			inReq.putPageValue("order", order);
			inReq.setRequestParameter("orderid", order.getId());

			for (Iterator iterator = assets.getSelectedHits().iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				Asset asset = getMediaArchive(catalogid).getAssetBySourcePath(hit.getSourcePath());
				getOrderManager().addItemToBasket(catalogid, order, asset, null);
			}
			if (order.get("expireson") == null)
			{
				order.setProperty("expireson", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			}

			getOrderManager().saveOrder(catalogid, inReq.getUser(), order);

			return order;
		}
		else
		{
			inReq.setCancelActions(true);
		}

		return null;
	}

	public Collection saveItems(WebPageRequest inReq) throws Exception
	{
		String[] fields = inReq.getRequestParameters("field");
		if (fields != null)
		{
			String[] items = inReq.getRequestParameters("itemid");
			String catalogid = inReq.findValue("catalogid");
			ArrayList toSave = getOrderManager().saveItems(catalogid, inReq, fields, items);
			return toSave;
		}
		return null;
	}

	
	public Order loadOrder(WebPageRequest inReq)
	{
		String catalogid = inReq.findValue("catalogid");
		String orderid = inReq.findValue("orderid");
		if (orderid == null)
		{
			orderid = inReq.getRequestParameter("id");
		}
		if( orderid == null)
		{
			return null;
		}
		Order order = getOrderManager().loadOrder(catalogid, orderid);
		inReq.putPageValue("order", order);
		return order;
	}
	public Order saveOrder(WebPageRequest inReq) throws Exception
	{
		return saveOrder(inReq,false);
	}
	public Order createOrder(WebPageRequest inReq) throws Exception
	{
		return saveOrder(inReq,true);
	}
	public Order saveOrder(WebPageRequest inReq, boolean saveitems) throws Exception
	{
		Order order = loadOrder(inReq);
		if (order != null)
		{
			String catalogid = inReq.findValue("catalogid");
			order = getOrderManager().createOrder(catalogid, inReq, saveitems);
			inReq.putPageValue("savedok","true");
		}
		return order;
	}


	public HitTracker findOrdersForUser(WebPageRequest req)
	{
		String catalogid = req.findValue("catalogid");
		User owner = (User) req.getPageValue("owner");
		if (owner == null)
		{
			owner = req.getUser();
		}
		HitTracker orders = getOrderManager().findOrdersForUser(catalogid, owner);
		req.putPageValue("orders", orders);
		return orders;
	}

	public HitTracker findOrderAssets(WebPageRequest req)
	{
		Data order = loadOrder(req);
		if (order != null)
		{
			String catalogid = req.findValue("catalogid");
			String orderid = order.getId();
			if (orderid == null)
			{
				orderid = req.getRequestParameter("orderid");
			}
			HitTracker items = getOrderManager().findOrderAssets(catalogid, orderid);
			req.putPageValue("orderassets", items);
			return items;
		}
		return null;
	}

	public HitTracker findOrderHistory(WebPageRequest req)
	{
		Order order = loadOrder(req);
		if (order != null)
		{
			String catalogid = req.findValue("catalogid");
			String orderid = order.getId();
			if (orderid == null)
			{
				orderid = req.getRequestParameter("orderid");
			}
			HitTracker items = getOrderManager().findOrderHistory(catalogid, order);
			req.putPageValue("orderhistory", items);
			return items;
		}
		return null;
	}

	public boolean checkItemApproval(WebPageRequest inReq) throws Exception
	{
		if (inReq.getUser() == null)
		{
			return false;
		}
		MediaArchive archive = getMediaArchive(inReq);

		// Searcher ordersearcher =
		// archive.getSearcherManager().getSearcher(archive.getCatalogId(),
		// "order");
		// SearchQuery search = ordersearcinKeyher.createSearchQuery();
		// search.addExact("userid", inReq.getUser().getId());
		// search.addExact("orderstatus", "processed");
		// search.addSortBy("date");
		// HitTracker hits = ordersearcher.search(search);
		// look for the most recent order for an approved asset
		Asset asset = (Asset) inReq.getPageValue("asset");
		String sourcepath = null;
		if (asset != null)
		{
			sourcepath = asset.getSourcePath();
		}
		else
		{
			sourcepath = archive.getSourcePathForPage(inReq);
		}
		Searcher itemsearcher = archive.getSearcherManager().getSearcher(archive.getCatalogId(), "orderitem");
		SearchQuery search = itemsearcher.createSearchQuery();
		search.addExact("userid", inReq.getUser().getId());
		search.addExact("assetsourcepath", sourcepath);
		search.addMatches("status", "approved");
		HitTracker results = itemsearcher.search(search);
		if (results.size() > 0)
		{
			return true;
		}
		return false;
	}

	public Data addItemToOrderBasket(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Order basket = loadOrderBasket(inReq);
		String[] assetids = inReq.getRequestParameters("assetid");
		String[] fields = inReq.getRequestParameters("field");
		Map props = new HashMap();
		if (fields != null)
		{
			for (int i = 0; i < fields.length; i++)
			{
				String key = fields[i];
				String value = inReq.getRequestParameter(key + ".value");
				props.put(key, value);
			}
		}

		for (int i = 0; i < assetids.length; i++)
		{
			String assetid = assetids[i];
			Asset asset = archive.getAsset(assetid);
			getOrderManager().addItemToBasket(archive.getCatalogId(), basket, asset, props);
		}

		return basket;
	}

	public Data addSelectionsToOrderBasket(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Order basket = loadOrderBasket(inReq);
		String hitssessionid = inReq.getRequestParameter("hitssessionid");
		HitTracker assets = (HitTracker) inReq.getSessionValue(hitssessionid);

		String[] fields = inReq.getRequestParameters("field");
		Map props = new HashMap();
		if (fields != null)
		{
			for (int i = 0; i < fields.length; i++)
			{
				String key = fields[i];
				String value = inReq.getRequestParameter(key + ".value");
				props.put(key, value);
			}
		}

		for (Iterator iterator = assets.getSelectedHits().iterator(); iterator.hasNext();)
		{

			Data hit = (Data) iterator.next();
			Asset asset = getMediaArchive(archive.getCatalogId()).getAsset(hit.getId());
			getOrderManager().addItemToBasket(archive.getCatalogId(), basket, asset, props);

		}

		return basket;
	}

	public Order loadOrderBasket(WebPageRequest inReq)
	{
		MediaArchive archive = getMediaArchive(inReq);
		Order basket = (Order) inReq.getPageValue("orderbasket");

		if (basket == null)
		{
			String appid = inReq.findValue("applicationid");
			Searcher searcher = getSearcherManager().getSearcher(archive.getCatalogId(), "order");
			basket = (Order) searcher.searchByField("orderstatus", "preorder");
			if (basket == null)
			{
				basket = getOrderManager().createNewOrder(appid, archive.getCatalogId(), inReq.getUserName());
				getOrderManager().saveOrder(archive.getCatalogId(), inReq.getUser(), basket);
			}
			inReq.putSessionValue("orderbasket", basket);
		}
		inReq.putPageValue("order", basket);
		HitTracker items = getOrderManager().findOrderAssets(archive.getCatalogId(), basket.getId());
		inReq.putPageValue("orderitems", items);

		return basket;
	}

	public void createConversionAndPublishRequest(WebPageRequest inReq)
	{
		//Order and item should be created from previous step.
		//now we get the items and update the destination information
		Order order = loadOrder(inReq);
		OrderManager manager = getOrderManager();
		if( order == null)
		{
			order = manager.createOrder(inReq.findValue("catalogid"), inReq, true);
		}
		MediaArchive archive = getMediaArchive(inReq);
		Map params = inReq.getParameterMap();
		manager.addConversionAndPublishRequest(order, archive,params, inReq.getUser());
		log.info("Added conversion and publish requests for " + order.getId());
	}



	public Order placeOrderById(WebPageRequest inReq)
	{
		Order order = loadOrder(inReq);
		getOrderManager().placeOrder(inReq, getMediaArchive(inReq), order);

		inReq.removeSessionValue("orderbasket");
		inReq.putPageValue("order", order);
		return order;
		// change the status of all the items and the order and save everything
		// fire event

	}

	public Order placeOrderFromBasket(WebPageRequest inReq)
	{
		Order order = loadOrderBasket(inReq);
		String prefix = inReq.findValue("subjectprefix");
		if (prefix == null)
		{
			prefix = "Order received:";
		}
		prefix = prefix + " " + order.getId();

		String postfix = inReq.findValue("subjectpostfix");
		if (postfix != null)
		{
			prefix = prefix + " " + postfix;
		}
		inReq.putPageValue("subject", prefix);

		getOrderManager().placeOrder(inReq, getMediaArchive(inReq), order);

		inReq.removeSessionValue("orderbasket");
		inReq.putPageValue("order", order);
		return order;
	}

	public Order createOrderFromAssets(WebPageRequest inReq)
	{
		String catalogId = inReq.findValue("catalogid");
		MediaArchive archive = getMediaArchive(catalogId);
		String[] assetids = inReq.getRequestParameters("assetid");
		Order order = getOrderManager().createNewOrder(inReq.findValue("applicationid"), catalogId, inReq.getUserName());

		for (int i = 0; i < assetids.length; i++)
		{
			Asset asset = archive.getAsset(assetids[i]);
			getOrderManager().addItemToBasket(catalogId, order, asset, null);
		}

		getOrderManager().saveOrder(catalogId, inReq.getUser(), order);
		inReq.putPageValue("order", order);

		return order;
	}

	public Order createOrderFromUpload(WebPageRequest inReq)
	{
		String catalogId = inReq.findValue("catalogid");
		MediaArchive archive = getMediaArchive(catalogId);
		Collection assets = (Collection) inReq.getPageValue("uploadedassets");

		Order order = getOrderManager().createNewOrder(inReq.findValue("applicationid"), catalogId, inReq.getUserName());
		//order.setProperty("orderstatus", "newupload");
		List assetids = new ArrayList();
		for (Iterator iter = assets.iterator(); iter.hasNext();)
		{
			Asset asset = (Asset) iter.next();
			assetids.add(asset.getId());
			getOrderManager().addItemToBasket(catalogId, order, asset, null);
		}
		//Order history needs to be updated
		OrderHistory history = getOrderManager().createNewHistory(catalogId, order, inReq.getUser(), "newupload");
		history.setAssetIds(assetids);
		getOrderManager().saveOrderWithHistory(catalogId, inReq.getUser(), order,history);
		
		//getOrderManager().saveOrder(catalogId, inReq.getUser(), order);
		inReq.putPageValue("order", order);

		return order;
	}
	public void loadOrderManager(WebPageRequest inReq)
	{
		inReq.putPageValue("orderManager",getOrderManager());
	}
	
	public Data addUserStatus(WebPageRequest inReq) throws Exception
	{
		Order order = loadOrder(inReq);
		if (order != null)
		{
			String catalogid = inReq.findValue("catalogid");
			String[] fields = inReq.getRequestParameters("field");
			String userstatus = inReq.findValue("userstatus.value");
			OrderHistory history = getOrderManager().createNewHistory(catalogid, order, inReq.getUser(), userstatus);

			Searcher searcher = getSearcherManager().getSearcher(catalogid, "orderhistory");
			searcher.updateData(inReq, fields, history);

			getOrderManager().saveOrderWithHistory(catalogid, inReq.getUser(), order, history);

		}
		return order;
	}
	
	public void updatePendingOrders(WebPageRequest inReq) throws Exception
	{
		MediaArchive archive = getMediaArchive(inReq);
		getOrderManager().updatePendingOrders(archive);
	}
}
