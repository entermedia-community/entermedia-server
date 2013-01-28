package org.openedit.entermedia.orders;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.openedit.Data;
import org.openedit.data.BaseData;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.CompositeAsset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.event.WebEvent;
import org.openedit.event.WebEventHandler;
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.users.User;

public class OrderManager 
{
	private static final Log log = LogFactory.getLog(OrderManager.class);
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
		Data order = createNewOrderWithId(frontendappid, inCatlogId,inUser.getUserName());
		
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
		query.addExact("userid", inUser.getId());
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
	public HitTracker findOrderItems(WebPageRequest inReq, String inCatalogid,  Order inOrder) 
	{
		return findOrderItems(inReq, inCatalogid,inOrder.getId() );
	}
	public HitTracker findOrderItems(WebPageRequest inReq, String inCatalogid, String inOrderId) 
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatalogid, "orderitem");
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addExact("orderid", inOrderId);
		query.setHitsName("orderitems");

		HitTracker items =  itemsearcher.cachedSearch(inReq, query);
		return items;
		
	}
	/**
	 * @deprecated Use {@link WebPageRequest}
	 * @param inCatalogid
	 * @param inOrderId
	 * @return
	 */
	public HitTracker findOrderAssets(String inCatalogid, String inOrderId) 
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatalogid, "orderitem");
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addExact("orderid", inOrderId);
		HitTracker items =  itemsearcher.search(query);
		return items;
	}
	public HitTracker findAssets(WebPageRequest inReq, String inCatalogid, Order inOrder) 
	{		
		HitTracker items =  findOrderItems(inReq, inCatalogid, inOrder);
		if( items.size() == 0)
		{
			//log.info("No items");
			return null;
		}
		StringBuffer ids = new StringBuffer();
		for (Iterator iterator = items.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			ids.append(hit.get("assetid"));
			if( iterator.hasNext() )
			{
				ids.append(" ");
			}
		}
		Searcher assetsearcher = getSearcherManager().getSearcher(inCatalogid, "asset");
		SearchQuery query = assetsearcher.createSearchQuery();
		query.setHitsName("orderassets");
		query.addOrsGroup("id", ids.toString());
		inReq.setRequestParameter("hitssessionid", "none");
		HitTracker hits = assetsearcher.cachedSearch(inReq, query);
		return hits;
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

	public Order createOrder(String catalogid, WebPageRequest inReq, boolean saveitems) 
	{
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "order");
		Order order = (Order)searcher.createNewData();
		String[] fields = inReq.getRequestParameters("field");
		searcher.updateData(inReq, fields, order);
		String newstatus = inReq.getRequestParameter("newuserstatus");
		if( newstatus != null)
		{
			OrderHistory history = createNewHistory(catalogid, order, inReq.getUser(), newstatus);
			
			String note = inReq.getRequestParameter("newuserstatusnote");
			history.setProperty("note", note);
			if( saveitems)
			{
				if (fields != null)
				{
					String[] items = inReq.getRequestParameters("itemid");

					Collection itemssaved = saveItems(catalogid,inReq,fields,items);
					List assetids = new ArrayList();
					for (Iterator iterator = itemssaved.iterator(); iterator.hasNext();)
					{
						Data item = (Data) iterator.next();
						assetids.add(item.get("assetid"));
					}
					history.setAssetIds(assetids);
				}
			}
			saveOrderWithHistory(catalogid, inReq.getUser(), order, history);
		}
		else
		{
			saveOrder(catalogid, inReq.getUser(), order);
		}
		return order;
	}
	
	
	public ArrayList saveItems(String catalogid, WebPageRequest inReq, String[] fields, String[] items)
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(catalogid, "orderitem");
		ArrayList toSave = new ArrayList();
		Data order = loadOrder(catalogid,inReq.findValue("orderid"));
		for (String itemid : items)
		{
			Data item = (Data) itemsearcher.searchById(itemid);
			toSave.add(item);

			item.setProperty("userid", order.get("userid"));
			for (String field : fields)
			{
				String value = inReq.getRequestParameter(item.getId() + "." + field + ".value");
				if (value != null)
				{
					item.setProperty(field, value);
				}
			}
		}
		itemsearcher.saveAllData(toSave, inReq.getUser());
		return toSave;
	}

	public Order createNewOrderWithId(String inAppId, String inCatalogId, String inUsername)
	{
		Order order = createNewOrder(inAppId, inCatalogId, inUsername);
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "order");
		if( order.getId() == null)
		{
			String id = searcher.nextId();
			order.setName(id);
			id = id + "_" + UUID.randomUUID().toString().replace('-', '_');
			order.setId(id);
		}
		return order;
	}
	public Order createNewOrder(String inAppId, String inCatalogId, String inUsername)
	{
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "order");
		Order order  = (Order)searcher.createNewData();
		order.setElement(DocumentHelper.createElement(searcher.getSearchType()));
		//order.setId(searcher.nextId());
		order.setProperty("orderstatus", "ordered");
		order.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		order.setSourcePath(inUsername + "/" + order.getId());
		order.setProperty("userid", inUsername);
		order.setProperty("applicationid",inAppId);
		return order;
	}

	public void removeItemFromOrder(String inCatId, Order inOrder, Asset inAsset)
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatId, "orderitem");
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addMatches("orderid", inOrder.getId());
		query.addMatches("assetid", inAsset.getId());
		HitTracker results = itemsearcher.search(query);
		for (Object result : results) {
			itemsearcher.delete((Data)result, null);
		}
	}
	
	public Data addItemToOrder(String inCatId, Order order, Asset inAsset, Map inProps)
	{
		if(inAsset == null){
			return null;
		}
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatId, "orderitem");
		Data item = itemsearcher.createNewData();
		item.setId(itemsearcher.nextId());
		item.setProperty("orderid", order.getId());
		item.setProperty("userid", order.get("userid"));
		
		item.setSourcePath(order.getSourcePath()); //will have orderitem.xml added to it
		item.setProperty("assetsourcepath", inAsset.getSourcePath());
		item.setProperty("assetid", inAsset.getId());
		item.setProperty("status", "preorder");
		if(inProps != null)
		{
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
		Searcher orderearcher = getSearcherManager().getSearcher(inCatalogId, "order");
		orderearcher.saveData(inBasket, inUser );
	}

	public void placeOrder(WebPageRequest inReq, MediaArchive inArchive, Order inOrder, boolean inResetId)
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(inArchive.getCatalogId(), "orderitem");
		Searcher orderseacher = getSearcherManager().getSearcher(inArchive.getCatalogId(), "order");

		HitTracker all = itemsearcher.fieldSearch("orderid", inOrder.getId());
		List tosave = new ArrayList();
		if(inResetId){
			inOrder.setId(orderseacher.nextId());
			inOrder.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		}
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
			if(inResetId){
				row.setProperty("orderid", inOrder.getId());
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
		if( inUser != null)
		{
			inHistory.setProperty("userid",inUser.getId() );
		}
		Searcher historysearcher = getSearcherManager().getSearcher(inCatalogId, "orderhistory");
		inOrder.setRecentOrderHistory(inHistory);
		saveOrder(inCatalogId, inUser, inOrder);
		inHistory.setProperty("orderid",inOrder.getId() );
		historysearcher.saveData(inHistory, inUser);
	}

	public OrderHistory createNewHistory(String inCatId, Order inOrder, User inUser, String inStatus)
	{
		Searcher historysearcher = getSearcherManager().getSearcher(inCatId, "orderhistory");
		OrderHistory history = (OrderHistory)historysearcher.createNewData();
		history.setUserStatus(inStatus);
		if( inUser != null)
		{
			history.setProperty("userid", inUser.getId());
		}
		history.setSourcePath(inOrder.getSourcePath());
		history.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));

		return history;
	}

	public List<String> addConversionAndPublishRequest(Order order, MediaArchive archive, Map<String,String> properties, User inUser)
	{
		HitTracker hits = findOrderAssets(archive.getCatalogId(), order.getId());
		Searcher taskSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "conversiontask");
		Searcher presets = getSearcherManager().getSearcher(archive.getCatalogId(), "convertpreset");

		Searcher publishQueueSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "publishqueue");

		Searcher orderItemSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "orderitem");

		List<String> assetids = new ArrayList<String>();
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data orderitemhit = (Data) iterator.next();
			String assetid = orderitemhit.get("assetid");
			assetids.add(assetid);
			Asset asset = archive.getAsset(assetid);
			//item.getId() + "." + field + ".value"
			String presetid = properties.get(orderitemhit.getId() + ".presetid.value");
			
			if( presetid == null )
			{
				presetid = properties.get("presetid.value");
			}

			if( presetid == null)
			{
				throw new OpenEditException("presetid is required");
			}
			
			Data orderItem = (Data) orderItemSearcher.searchById(orderitemhit.getId());
			orderItem.setProperty("presetid", presetid);
			if( "preview".equals(presetid) )
			{
				orderItemSearcher.saveData(orderItem, inUser);
				continue;
			}

			boolean needstobecreated = true;
			if( orderItem.get("conversiontaskid") != null )
			{
				needstobecreated = false;
			}
			
			Data preset = (Data) presets.searchById(presetid);
			String outputfile = preset.get("outputfile");

			//Make sure preset does not already exists?
			if( needstobecreated && "original".equals( preset.get("type") ) )
			{
				needstobecreated = !archive.getOriginalDocument(asset).exists();
			}	
			else if( needstobecreated && archive.doesAttachmentExist(outputfile, asset) )
			{
				needstobecreated = false;
			}
			if (needstobecreated)
			{
				SearchQuery q = taskSearcher.createSearchQuery().append("assetid", assetid).append("presetid",presetid);
				Data newTask = taskSearcher.searchByQuery(q);
				if( newTask == null )
				{
					newTask = taskSearcher.createNewData();	
				} else{
					newTask = (Data) taskSearcher.searchById(newTask.getId());//load real data
				}
				newTask.setSourcePath(asset.getSourcePath());
				newTask.setProperty("status", "new");
				newTask.setProperty("assetid", assetid);
				newTask.setProperty("presetid", presetid);
				
				//newTask.setProperty("orderid", order.getId());
				//newTask.setProperty("itemid", hit.getId());
				taskSearcher.saveData(newTask, inUser);
				orderItem.setProperty("conversiontaskid",newTask.getId());
			}
			
			//Add a publish task to the publish queue
			String destination = properties.get(orderitemhit.getId() + ".publishdestination.value");
			if( destination == null )
			{
				destination = order.get("publishdestination");
			}
			if( destination != null)
			{
				Data publishqeuerow = publishQueueSearcher.createNewData();
				publishqeuerow.setProperty("assetid", assetid);
				publishqeuerow.setProperty("assetsourcepath", asset.getSourcePath() );
				
				publishqeuerow.setProperty("publishdestination", destination);
				publishqeuerow.setProperty("presetid", presetid);

				String userid = order.get("userid");
				User user = null;
				if( userid != null )
				{
					user = (User)archive.getSearcherManager().getSearcher("system", "user").searchById(userid);
				}
				String exportname = archive.asExportFileName(user, asset, preset);
				publishqeuerow.setProperty("exportname", exportname);
				publishqeuerow.setProperty("status", "new");
				
				Data dest = getSearcherManager().getData(archive.getCatalogId(), "publishdestination", destination);
				if(needstobecreated && Boolean.parseBoolean( dest.get("remotempublish") ) )
				{
					publishqeuerow.setProperty("remotempublishstatus", "new");
				}
				publishqeuerow.setSourcePath(asset.getSourcePath());
				publishqeuerow.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
				publishQueueSearcher.saveData(publishqeuerow, inUser);
				
				orderItem.setProperty("publishqueueid",publishqeuerow.getId());
			}	
			else
			{
				throw new OpenEditException("publishdestination.value is missing");
			}
			orderItemSearcher.saveData(orderItem, inUser);
			if( !needstobecreated )
			{
				//Kick off the publish tasks
				archive.fireMediaEvent("conversions/conversioncomplete", inUser, asset);
			}
		}
		archive.fireSharedMediaEvent("publishing/publishassets");
		return assetids;
	}
	
	public String getPresetForOrderItem(String inCataId, Data inOrderItem)
	{
		String presetid = inOrderItem.get("presetid");
		if( presetid == null )
		{
			return "preview"; //preview? or could be original... I am going to save presetid
		}
		return presetid;
	}
	public String getPublishDestinationForOrderItem(String inCataId, Data inOrderItem)
	{
		String pubqueid = inOrderItem.get("publishqueueid");
		if( pubqueid == null )
		{
			return null;
		}
		Data task = getSearcherManager().getData(inCataId,"publishqueue", pubqueid);
		return task.get("publishdestination");
	}

	public void updateStatus(MediaArchive archive, Order inOrder)
	{
		//look up all the tasks
		//if all done then save order status
		
		Searcher itemsearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "orderitem");
		
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addExact("orderid", inOrder.getId());
		query.setHitsName("orderitems");
		//query.addNot("status", "complete");
		HitTracker hits =  itemsearcher.search(query);
		
		if( hits.size() == 0)
		{
			log.error("No items on order "  + inOrder.getId() + " " + inOrder.getOrderStatus() );
			//error?
			return;
		}
		Searcher taskSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "conversiontask");
		Searcher presets = getSearcherManager().getSearcher(archive.getCatalogId(), "convertpreset");

		Searcher publishQueueSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "publishqueue");

		//Searcher orderItemSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "orderitem");

		int itemscomplted = 0;
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data orderitemhit = (Data) iterator.next();
			boolean convertcomplete = false;
			boolean publishcomplete = false;
			String conversiontaskid = orderitemhit.get("conversiontaskid");
			if( conversiontaskid == null)
			{
				convertcomplete = true;
			}
			else
			{
				Data convert = (Data)taskSearcher.searchById(conversiontaskid);
				if( "complete".equals( convert.get("status") ) )
				{
					convertcomplete = true;
				}
				else if ("error".equals( convert.get("status") ) )
				{
					Data item = (Data)itemsearcher.searchById(orderitemhit.getId());
					item.setProperty("status", "error");
					item.setProperty("errordetails", convert.get("errordetails"));
					itemsearcher.saveData(item, null);
					inOrder.setOrderStatus("error",convert.get("errordetails")); //orders are either open closed or error
					OrderHistory history = createNewHistory(archive.getCatalogId(), inOrder, null, "error");
					saveOrderWithHistory(archive.getCatalogId(), null, inOrder, history);
					return;
				}

			}
			if( convertcomplete)
			{
				String publishqueueid = orderitemhit.get("publishqueueid");
				if( publishqueueid == null)
				{
					publishcomplete = true;
				}
				else
				{
					Data publish = (Data)publishQueueSearcher.searchById(publishqueueid);
					if( publish == null )
					{
						log.error("Publish was null");
						publish = new BaseData();
						 publish.setProperty("status","error");
						 publish.setProperty("errordetails","Publish queue not found in database " + publishqueueid );
					}
					if( "complete".equals( publish.get("status") ) )
					{
						publishcomplete = true;
					}
					else if ("error".equals( publish.get("status") ) )
					{
						Data item = (Data)itemsearcher.searchById(orderitemhit.getId());
						item.setProperty("status", "error");
						item.setProperty("errordetails", publish.get("errordetails"));
						itemsearcher.saveData(item, null);
						inOrder.setOrderStatus("error",publish.get("errordetails")); //orders are either open closed or error
						OrderHistory history = createNewHistory(archive.getCatalogId(), inOrder, null, "error");
						saveOrderWithHistory(archive.getCatalogId(), null, inOrder, history);
						
						return;
					}
				}
				if( publishcomplete)
				{
					Data item = (Data)itemsearcher.searchById(orderitemhit.getId());
					item.setProperty("status", "complete");
					//set date?
					itemsearcher.saveData(item, null);
					itemscomplted++;
					if( itemscomplted == hits.size() )
					{
						WebEvent event = new WebEvent();
						event.setSearchType("order");
						event.setCatalogId(archive.getCatalogId());
						event.setOperation("ordering/finalizeorder");
						event.setUser(null);
						event.setSource(this);
						event.setProperty("sourcepath", inOrder.getSourcePath());
						event.setProperty("orderid", inOrder.getId());
						//archive.getWebEventListener()
						archive.getMediaEventHandler().eventFired(event);

						inOrder.setOrderStatus("complete");
						OrderHistory history = createNewHistory(archive.getCatalogId(), inOrder, null, "complete");
						saveOrderWithHistory(archive.getCatalogId(), null, inOrder, history);

					
					}
				}
			}
			
		}
	}

	public void updatePendingOrders(MediaArchive archive)
	{
		Searcher ordersearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "order");
		SearchQuery query = ordersearcher.createSearchQuery();
		query.addOrsGroup("orderstatus","pending");
		Collection hits = ordersearcher.search(query);
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			Order order = loadOrder(archive.getCatalogId(), hit.getId());
			updateStatus(archive, order);			
		}
	}

	public int addItemsToBasket(WebPageRequest inReq, MediaArchive inArchive, Order inOrder, Collection inSelectedHits, Map inProps)
	{
		HitTracker items =  findOrderItems(inReq, inArchive.getCatalogId(), inOrder);
		Set existing = new HashSet();
		if(items != null){
			for (Iterator iterator = items.iterator(); iterator.hasNext();)
			{
				Data hit = (Data) iterator.next();
				existing.add(hit.get("assetid"));
			}
		}
		int count = 0;
		for (Iterator iterator = inSelectedHits.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			String assetid = hit.getId();
			if( !existing.contains(assetid))
			{
				Asset asset = inArchive.getAsset(assetid);
				if( asset != null)
				{
					addItemToOrder(inArchive.getCatalogId(), inOrder, asset, inProps);
					count++;
				}
			}
		}
		return count;
	}
	
	public boolean isAssetInOrder(String inCatId, Order inOrder, String inAssetId)
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatId, "orderitem");
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addMatches("orderid", inOrder.getId());
		query.addMatches("assetid", inAssetId);
		HitTracker results = itemsearcher.search(query);
		if(results.size() > 0)
		{
			return true;
		}
		return false;
	}

	public void delete(String inCatId, Order inOrder)
	{
		// TODO Auto-generated method stub
		Searcher ordersearcher = getSearcherManager().getSearcher(inCatId, "orderitems");
		//Searcher ordersearcher = getSearcherManager().getSearcher(inCatId, "order");
		
	}

	public void removeItem(String inCatalogid, String inItemid)
	{
		Searcher ordersearcher = getSearcherManager().getSearcher(inCatalogid, "orderitem");
		Data orderitem = (Data) ordersearcher.searchById(inItemid);
		if(orderitem != null){
			ordersearcher.delete(orderitem, null);
		}
	}

	public void removeMissingAssets(WebPageRequest inReq, MediaArchive archive, Order basket, Collection items)
	{
		Collection assets = findAssets(inReq, archive.getCatalogId(), basket);
		if( assets == null )
		{
			assets = Collections.EMPTY_LIST;
		}
		if( assets.size() != items.size() )
		{
			Set assetids = new HashSet();
			for (Iterator iterator = assets.iterator(); iterator.hasNext();)
			{
				Data data = (Data) iterator.next();
				assetids.add(data.getId());
			}
			
			List allitems = new ArrayList(items);
			Searcher itemsearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "orderitem");
			for (Iterator iterator = allitems.iterator(); iterator.hasNext();)
			{
				Data item = (Data) iterator.next();
				if( !assetids.contains( item.get("assetid") ) )
				{
					//asset deleted, remove it
					itemsearcher.delete(item, null);
				}
			}
		}
	}

	public void toggleItemInOrder(MediaArchive inArchive, Order inBasket, Asset inAsset)
	{
		if( inAsset instanceof CompositeAsset )
		{
			CompositeAsset assets = (CompositeAsset)inAsset;
			for (Iterator iterator = assets.iterator(); iterator.hasNext();)
			{
				Asset asset = (Asset) iterator.next();
				boolean inorder = isAssetInOrder(inArchive.getCatalogId(), inBasket, asset.getId());
				if (inorder)
				{
					removeItemFromOrder(inArchive.getCatalogId(), inBasket, asset);
				}
				else
				{
					addItemToOrder(inArchive.getCatalogId(), inBasket, asset, null);
				}
			}
			
		}
		else
		{
			boolean inorder = isAssetInOrder(inArchive.getCatalogId(), inBasket, inAsset.getId());
			if (inorder)
			{
				removeItemFromOrder(inArchive.getCatalogId(), inBasket, inAsset);
			}
			else
			{
				addItemToOrder(inArchive.getCatalogId(), inBasket, inAsset, null);
			}
		}

	}

}
