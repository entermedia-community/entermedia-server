package model.orders;

import java.text.SimpleDateFormat

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.entermedia.email.PostMail
import org.entermedia.email.TemplateWebEmail
import org.entermedia.locks.Lock
import org.entermedia.locks.LockManager
import org.openedit.Data
import org.openedit.data.Searcher
import org.openedit.data.SearcherManager
import org.openedit.entermedia.Asset
import org.openedit.entermedia.CompositeAsset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.orders.Order
import org.openedit.entermedia.orders.OrderHistory
import org.openedit.entermedia.orders.OrderManager
import org.openedit.event.WebEvent
import org.openedit.event.WebEventHandler
import org.openedit.util.DateStorageUtil

import com.openedit.BaseWebPageRequest
import com.openedit.ModuleManager
import com.openedit.OpenEditException
import com.openedit.WebPageRequest
import com.openedit.hittracker.HitTracker
import com.openedit.hittracker.SearchQuery
import com.openedit.page.manage.PageManager
import com.openedit.users.User
import com.openedit.users.UserManager
import com.openedit.util.RequestUtils

public class BaseOrderManager implements OrderManager {
	private static final Log log = LogFactory.getLog(BaseOrderManager.class);
	protected SearcherManager fieldSearcherManager;
	protected WebEventHandler fieldWebEventHandler;
	protected LockManager fieldLockManager;
	protected ModuleManager fieldModuleManager;
	protected PageManager fieldPageManager;
	
	public WebEventHandler getWebEventHandler() {
		return fieldWebEventHandler;
	}

	public void setWebEventHandler(WebEventHandler inWebEventHandler) {
		fieldWebEventHandler = inWebEventHandler;
	}

	public SearcherManager getSearcherManager() {
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager) {
		fieldSearcherManager = inSearcherManager;
	}

	public Data placeOrder(String frontendappid, String inCatlogId, User inUser, HitTracker inAssets, Map inProperties) {
		Searcher searcher = getSearcherManager().getSearcher(inCatlogId, "order");
		Data order = createNewOrder(frontendappid, inCatlogId,inUser.getUserName());

		for (Iterator iterator = inProperties.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			order.setProperty(key, (String)inProperties.get(key));
		}

		searcher.saveData(order, inUser);

		Searcher itemsearcher = getSearcherManager().getSearcher(inCatlogId, "orderitem");

		List items = new ArrayList();
		for (Iterator iterator = inAssets.iterator(); iterator.hasNext();) {
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

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#findOrdersForUser(java.lang.String, com.openedit.users.User)
	 */

	public HitTracker findOrdersForUser(String inCatlogId, User inUser) {
		Searcher ordersearcher = getSearcherManager().getSearcher(inCatlogId, "order");
		SearchQuery query = ordersearcher.createSearchQuery();
		//query.addOrsGroup("orderstatus","ordered finalizing complete"); //Open ones
		GregorianCalendar cal = new GregorianCalendar();
		cal.add(Calendar.MONTH, -3);
		query.addAfter("date", cal.getTime());
		query.addSortBy("historydateDown");
		query.addExact("userid", inUser.getId());
		return ordersearcher.search(query);
	}
	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#loadOrderHistoryForPage(com.openedit.hittracker.HitTracker)
	 */

	public void loadOrderHistoryForPage(HitTracker inPage) {
		for (Iterator iterator = inPage.getPageOfHits().iterator(); iterator.hasNext();) {
			Order order = (Order) iterator.next();
			loadOrderHistory(inPage.getCatalogId(), order);
		}
	}

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#loadOrderHistory(java.lang.String, org.openedit.entermedia.orders.Order)
	 */

	public OrderHistory loadOrderHistory(String inCataId, Order order) {
		if( order == null) {
			return null;
		}
		if( order.getRecentOrderHistory() == null) {
			OrderHistory history = findRecentOrderHistory(inCataId,order.getId());
			if( history == null) {
				history = OrderHistory.EMPTY;
			}
			order.setRecentOrderHistory(history);
		}
		return order.getRecentOrderHistory();
	}
	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#findOrderItems(com.openedit.WebPageRequest, java.lang.String, org.openedit.entermedia.orders.Order)
	 */

	public HitTracker findOrderItems(WebPageRequest inReq, String inCatalogid,  Order inOrder) {
		return findOrderItems(inReq, inCatalogid,inOrder.getId() );
	}
	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#findOrderItems(com.openedit.WebPageRequest, java.lang.String, java.lang.String)
	 */

	public HitTracker findOrderItems(WebPageRequest inReq, String inCatalogid, String inOrderId) {
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatalogid, "orderitem");
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addExact("orderid", inOrderId);
		query.setHitsName("orderitems");

		HitTracker items =  itemsearcher.cachedSearch(inReq, query);
		return items;
	}
	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#findOrderAssets(java.lang.String, java.lang.String)
	 */

	public HitTracker findOrderAssets(String inCatalogid, String inOrderId) {
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatalogid, "orderitem");
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addExact("orderid", inOrderId);
		query.addSortBy("id");
		HitTracker items =  itemsearcher.search(query);
		return items;
	}
	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#findAssets(com.openedit.WebPageRequest, java.lang.String, org.openedit.entermedia.orders.Order)
	 */

	public HitTracker findAssets(WebPageRequest inReq, String inCatalogid, Order inOrder) {
		HitTracker items =  findOrderItems(inReq, inCatalogid, inOrder);
		if( items.size() == 0) {
			//log.info("No items");
			return null;
		}
		StringBuffer ids = new StringBuffer();
		for (Iterator iterator = items.iterator(); iterator.hasNext();) {
			Data hit = (Data) iterator.next();
			ids.append(hit.get("assetid"));
			if( iterator.hasNext() ) {
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


	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#findOrderHistory(java.lang.String, org.openedit.entermedia.orders.Order)
	 */

	public HitTracker findOrderHistory(String inCatalogid, Order inOrder) {
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatalogid, "orderhistory");
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addExact("orderid", inOrder.getId());
		query.addSortBy("dateDown");
		HitTracker items =  itemsearcher.search(query);

		OrderHistory history = OrderHistory.EMPTY;
		Data hit = (Data)items.first();
		if( hit != null) {
			history = (OrderHistory)itemsearcher.searchById(hit.getId());
		}

		inOrder.setRecentOrderHistory(history);

		return items;
	}
	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#findRecentOrderHistory(java.lang.String, java.lang.String)
	 */

	public OrderHistory findRecentOrderHistory(String inCatalogid, String inOrderId) {
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatalogid, "orderhistory");
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addExact("orderid", inOrderId);
		query.addSortBy("dateDown");
		Data index =  (Data)itemsearcher.uniqueResult(query);
		if( index != null) {
			OrderHistory history = (OrderHistory)itemsearcher.searchById(index.getId());
			return history;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#loadOrder(java.lang.String, java.lang.String)
	 */

	public Order loadOrder(String catalogid, String orderid) {
		Searcher ordersearcher = getSearcherManager().getSearcher(catalogid, "order");
		Order order =  (Order) ordersearcher.searchById(orderid);
		loadOrderHistory(catalogid,order);
		return order;
	}

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#createOrder(java.lang.String, com.openedit.WebPageRequest, boolean)
	 */

	public Order createOrder(String catalogid, WebPageRequest inReq, boolean saveitems) {
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "order");
		Order order = (Order)searcher.createNewData();
		String[] fields = inReq.getRequestParameters("field");
		searcher.updateData(inReq, fields, order);
		String newstatus = inReq.getRequestParameter("newuserstatus");
		if( newstatus != null) {
			OrderHistory history = createNewHistory(catalogid, order, inReq.getUser(), newstatus);

			String note = inReq.getRequestParameter("newuserstatusnote");
			history.setProperty("note", note);
			if( saveitems) {
				if (fields != null) {
					String[] items = inReq.getRequestParameters("itemid");

					Collection itemssaved = saveItems(catalogid,inReq,fields,items);
					List assetids = new ArrayList();
					for (Iterator iterator = itemssaved.iterator(); iterator.hasNext();) {
						Data item = (Data) iterator.next();
						assetids.add(item.get("assetid"));
					}
					history.setAssetIds(assetids);
				}
			}
			saveOrderWithHistory(catalogid, inReq.getUser(), order, history);
		}
		else {
			saveOrder(catalogid, inReq.getUser(), order);
		}
		return order;
	}


	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#saveItems(java.lang.String, com.openedit.WebPageRequest, java.lang.String[], java.lang.String[])
	 */

	public ArrayList saveItems(String catalogid, WebPageRequest inReq, String[] fields, String[] items) {
		Searcher itemsearcher = getSearcherManager().getSearcher(catalogid, "orderitem");
		ArrayList toSave = new ArrayList();
		Data order = loadOrder(catalogid,inReq.findValue("orderid"));
		for (String itemid : items) {
			Data item = (Data) itemsearcher.searchById(itemid);
			toSave.add(item);

			item.setProperty("userid", order.get("userid"));
			for (String field : fields) {
				String value = inReq.getRequestParameter(item.getId() + "." + field + ".value");
				if (value != null) {
					item.setProperty(field, value);
				}
			}
		}
		itemsearcher.saveAllData(toSave, inReq.getUser());
		return toSave;
	}


	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#createNewOrder(java.lang.String, java.lang.String, java.lang.String)
	 */

	public Order createNewOrder(String inAppId, String inCatalogId, String inUsername) {
		Searcher searcher = getSearcherManager().getSearcher(inCatalogId, "order");
		Order order  = (Order)searcher.createNewData();
		//order.setElement(DocumentHelper.createElement(searcher.getSearchType()));
		//order.setId(searcher.nextId());
		order.setProperty("orderstatus", "preorder");
		order.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		
		if( order.getId() == null) {
			//String id = searcher.nextId();
			//order.setName(id);
			String id = UUID.randomUUID().toString().replace('-', '_');
			order.setId(id);
			order.setName(id);
			order.setSourcePath(inUsername + "/" + id.substring(0,2));
		}


		order.setProperty("userid", inUsername);
		order.setProperty("applicationid",inAppId);
		return order;
	}

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#removeItemFromOrder(java.lang.String, org.openedit.entermedia.orders.Order, org.openedit.entermedia.Asset)
	 */

	public void removeItemFromOrder(String inCatId, Order inOrder, Asset inAsset) {
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatId, "orderitem");
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addMatches("orderid", inOrder.getId());
		query.addMatches("assetid", inAsset.getId());
		HitTracker results = itemsearcher.search(query);
		for (Object result : results) {
			itemsearcher.delete((Data)result, null);
		}
	}

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#addItemToOrder(java.lang.String, org.openedit.entermedia.orders.Order, org.openedit.entermedia.Asset, java.util.Map)
	 */

	public Data addItemToOrder(String inCatId, Order order, Asset inAsset, Map inProps) {
		if(inAsset == null){
			return null;
		}
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatId, "orderitem");
		Data item = itemsearcher.createNewData();
		//item.setId(itemsearcher.nextId());
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

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#saveOrder(java.lang.String, com.openedit.users.User, org.openedit.entermedia.orders.Order)
	 */

	public void saveOrder(String inCatalogId, User inUser, Order inBasket)
	{
		Searcher orderearcher = getSearcherManager().getSearcher(inCatalogId, "order");
		orderearcher.saveData(inBasket, inUser );
	}

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#placeOrder(com.openedit.WebPageRequest, org.openedit.entermedia.MediaArchive, org.openedit.entermedia.orders.Order, boolean)
	 */

	public void placeOrder(WebPageRequest inReq, MediaArchive inArchive, Order inOrder, boolean inResetId)
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(inArchive.getCatalogId(), "orderitem");
		Searcher orderseacher = getSearcherManager().getSearcher(inArchive.getCatalogId(), "order");

		HitTracker all = itemsearcher.fieldSearch("orderid", inOrder.getId());
		List tosave = new ArrayList();
		if(inResetId){
			inOrder.setId(orderseacher.nextId());
			inOrder.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			inOrder.setProperty("basket","false");

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
	
	/**
	 * Main entry point for adding conversion and publish requests on item in an order
	 */
	public List<String> addConversionAndPublishRequest(WebPageRequest inReq, Order order, MediaArchive archive, Map<String,String> properties, User inUser)
	{
		//get list of invalid items
		ArrayList<String> omit = new ArrayList<String>();
		String invaliditems = inReq.findValue("invaliditems");
		if (invaliditems != null && !invaliditems.isEmpty() )
		{
			String [] ovals = invaliditems.replace("[","").replace("]", "").split(",");
			for (String oval:ovals)
			{
				omit.add(oval.trim());
			}
		}

		HitTracker hits = findOrderAssets(archive.getCatalogId(), order.getId());
		Searcher taskSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "conversiontask");
		Searcher presets = getSearcherManager().getSearcher(archive.getCatalogId(), "convertpreset");
		Searcher publishQueueSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "publishqueue");
		Searcher orderItemSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "orderitem");

		log.info("Processing " + hits.size() + " order items ");
		List<String> assetids = new ArrayList<String>();
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data orderitemhit = (Data) iterator.next();
			String assetid = orderitemhit.get("assetid");
			assetids.add(assetid);
			Asset asset = archive.getAsset(assetid);
			//if asset is in the exclude list, update the orderitem table with a publisherror status
			if (!omit.isEmpty() && omit.contains(assetid))
			{
				Data data = (Data) orderItemSearcher.searchById(orderitemhit.getId());
				data.setProperty("status","publisherror");
				data.setProperty("errordetails","Publisher is not configured for this preset");
				orderItemSearcher.saveData(data, null);
				omit.remove(assetid);
				continue;
			}

			//item.getId() + "." + field + ".value"
			String presetid = orderitemhit.get("presetid");
			if(presetid == null){
				presetid = properties.get(orderitemhit.getId() + ".presetid.value");

				String rendertype = null;
				if( presetid == null)
				{
					rendertype = archive.getMediaRenderType(asset.getFileFormat());
					presetid = properties.get(rendertype + ".presetid.value");
				}

				if( presetid == null )
				{
					presetid = properties.get("presetid.value");
				}

				if( presetid == null)
				{
					throw new OpenEditException("presetid is required");
				}
			}
			Data orderItem = (Data) orderItemSearcher.searchById(orderitemhit.getId());
			if (orderItem == null)
			{
				log.info("Unknown error: unable to find ${orderitemhit.getId()} in orderitem table, skipping");
				//update what table exactly?
				continue;
			}

			orderItem.setProperty("presetid", presetid);
			if( "preview".equals(presetid) )   //This never touches publishing
			{
				orderItemSearcher.saveData(orderItem, inUser);
				continue;
			}

			String destination = orderitemhit.get("publishdestination");
			if(destination == null){

				//Add a publish task to the publish queue
				destination = properties.get(orderitemhit.getId() + ".publishdestination.value");
				if( destination == null )
				{
					destination = order.get("publishdestination");
				}

				if( destination == null )
				{
					destination = properties.get("publishdestination.value");
				}

				if( destination == null)
				{
					throw new OpenEditException("publishdestination.value is missing");
				}
			}
			Data dest = getSearcherManager().getData(archive.getCatalogId(), "publishdestination", destination);


			String publishstatus = "new";
			Data publishqeuerow = publishQueueSearcher.createNewData();


			String []fields = inReq.getRequestParameters("presetfield");
			if (fields!=null && fields.length!=0)
			{
				for (int i = 0; i < fields.length; i++) {
					String field = fields[i];
					String value = inReq.getRequestParameter(orderitemhit.getId() +"." +  field + ".value");

					publishqeuerow.setProperty(field, value);
				}
			}
			//Add and shared fields across the entire order/publish request
			String [] sharedfields = inReq.getRequestParameters("field");
			if (sharedfields!=null && sharedfields.length!=0)
			{
				for (int i = 0; i < sharedfields.length; i++) {
					String field = sharedfields[i];
					String value = inReq.getRequestParameter( field + ".value");

					publishqeuerow.setProperty(field, value);
				}
			}
			publishqeuerow.setProperty("assetid", assetid);
			publishqeuerow.setProperty("assetsourcepath", asset.getSourcePath() );

			publishqeuerow.setProperty("publishdestination", destination);
			publishqeuerow.setProperty("presetid", presetid);

			String userid = order.get("userid");
			User user = null;
			if( userid != null )
			{
				user = (User)archive.getSearcherManager().getSearcher("system", "user").searchById(userid);
			} else{
				user = inUser;
			}
			Data preset = (Data) presets.searchById(presetid);

			String exportname = archive.asExportFileName(user, asset, preset);
			publishqeuerow.setProperty("exportname", exportname);
			publishqeuerow.setProperty("status", publishstatus);

			publishqeuerow.setSourcePath(asset.getSourcePath());
			publishqeuerow.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			publishQueueSearcher.saveData(publishqeuerow, inUser);

			if( publishqeuerow.getId() == null )
			{
				throw new OpenEditException("Id should not be null");
			}

			orderItem.setProperty("publishqueueid",publishqeuerow.getId());

			orderItemSearcher.saveData(orderItem, inUser);
			//			if( !needstobecreated )
			//			{
			//				//Kick off the publish tasks
			//				archive.fireMediaEvent("conversions/conversioncomplete", inUser, asset);
			//			}
		}
		order.setOrderStatus("processing");
		saveOrder(archive.getCatalogId(), inUser, order);
		archive.fireSharedMediaEvent("publishing/publishassets"); //this might add conversions
		return assetids;
	}

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#getPresetForOrderItem(java.lang.String, org.openedit.Data)
	 */

	public String getPresetForOrderItem(String inCataId, Data inOrderItem)
	{
		String presetid = inOrderItem.get("presetid");
		if( presetid == null )
		{
			return "preview"; //preview? or could be original... I am going to save presetid
		}
		return presetid;
	}
	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#getPublishDestinationForOrderItem(java.lang.String, org.openedit.Data)
	 */

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

	public LockManager getLockManager()
	{
		return fieldLockManager;
	}

	public void setLockManager(LockManager inManager)
	{
		fieldLockManager = inManager;
	}
	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#updateStatus(org.openedit.entermedia.MediaArchive, org.openedit.entermedia.orders.Order)
	 */
	public void updateStatus(MediaArchive archive, Order inOrder)
	{
		//look up all the tasks
		//if all done then save order status
		Lock lock = getLockManager().lock(archive.getCatalogId(), inOrder.getSourcePath(), "BaseOrderManager");

		try
		{
			if( inOrder.getOrderStatus() == "complete" )
			{
				log.debug("Already complete");
				return;
			}

			Searcher itemsearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "orderitem");

			SearchQuery query = itemsearcher.createSearchQuery();
			query.addExact("orderid", inOrder.getId());
			query.setHitsName("orderitems");
			//query.addNot("status", "complete");
			HitTracker hits =  itemsearcher.search(query); //not cached

			if( hits.size() == 0)
			{
				log.error("No items on order "  + inOrder.getId() + " " + inOrder.getOrderStatus() );
				//error?
				return;
			}

			Searcher historysearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "orderhistory");
			OrderHistory temphistory = (OrderHistory)historysearcher.createNewData();

			for (Iterator iterator = hits.iterator(); iterator.hasNext();)
			{
				Data orderitemhit = (Data) iterator.next();
				addItemToHistory(archive, temphistory,  orderitemhit);
			}
			OrderHistory recent = loadOrderHistory(archive.getCatalogId(), inOrder);
			if(temphistory.hasCountChanged(recent))
			{
				saveOrderHistory(archive, temphistory, inOrder)	;
			}
			//If changed then save history and update order

			if((temphistory.getItemErrorCount() + temphistory.getItemSuccessCount()) == hits.size() )
			{
				//Finalize should be only for complete orders.
				inOrder.setOrderStatus('complete');
				try
				{
					sendOrderNotifications(archive, inOrder);
				}
				catch( Exception ex)
				{
					inOrder.setOrderStatus('complete',"could not send notification " + ex.getMessage() );
				}
				saveOrder(archive.getCatalogId(), null, inOrder);
				//complete the order
			}
		}
		finally
		{
			getLockManager().release(archive.getCatalogId(), lock);
		}
	}

	protected boolean addItemToHistory(MediaArchive archive, OrderHistory inHistory, Data orderitemhit)
	{
		//Go find if there are any publishing or conversion errors
		//If both are done then mark as complete

		inHistory.addItemCount();

		//Is the conversion done?
		//Make sure preset does not already exists?


		boolean publishcomplete = false;
		String publishqueueid = orderitemhit.get("publishqueueid");
		if( publishqueueid == null)
		{
			publishcomplete = true;
		}
		else
		{
			Data publish = (Data)archive.getSearcher("publishqueue").searchById(publishqueueid);
			if( publish == null)
			{
				inHistory.addItemErrorCount();
			}
			else
			{
				if( "complete".equals( publish.get("status") ) )
				{
					publishcomplete = true;
				}
				else if ("error".equals( publish.get("status") ) )
				{
					inHistory.addItemErrorCount();
					return;
				}
			}
		}
		if( publishcomplete )
		{
			inHistory.addItemSuccessCount();
		}
		return publishcomplete;
	}
/*
	protected processOrder(MediaArchive archive, Order inOrder)
	{
		inOrder.setOrderStatus("complete");
		OrderHistory history = createNewHistory(archive.getCatalogId(), inOrder, null, "ordercomplete");
		saveOrderWithHistory(archive.getCatalogId(), null, inOrder, history);

		WebEvent event = new WebEvent();
		event.setSearchType("order");
		event.setCatalogId(archive.getCatalogId());
		event.setOperation("ordering/finalizeorder");
		event.setUser(null);
		event.setSource(this);
		event.setProperty("sourcepath", inOrder.getSourcePath());
		event.setProperty("orderid", inOrder.getId());
		//archive.getWebEventListener()
		archive.getMediaEventHandler().eventFired(event); //Marks it as an error if email cant be sent or leaves it as complete
	}
	*/
	
	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#updatePendingOrders(org.openedit.entermedia.MediaArchive)
	 */

	public void updatePendingOrders(MediaArchive archive)
	{
		Searcher ordersearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "order");
		SearchQuery query = ordersearcher.createSearchQuery();
		query.addOrsGroup("orderstatus","processing"); //pending is depreacted
		Collection hits = ordersearcher.search(query);
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			Order order = loadOrder(archive.getCatalogId(), hit.getId());
			updateStatus(archive, order);
		}
	}

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#addItemsToBasket(com.openedit.WebPageRequest, org.openedit.entermedia.MediaArchive, org.openedit.entermedia.orders.Order, java.util.Collection, java.util.Map)
	 */

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

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#isAssetInOrder(java.lang.String, org.openedit.entermedia.orders.Order, java.lang.String)
	 */

	public boolean isAssetInOrder(String inCatId, Order inOrder, String inAssetId)
	{
		if(inAssetId.startsWith("multi")){
			return false;
		}
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatId, "orderitem");
		Data item = itemsearcher.query().match("orderid", inOrder.getId()).match("assetid", inAssetId).searchOne();
		if(item != null)
		{
			return true;
		}
		return false;
	}
	public void delete(String inCatId, Order inOrder)
	{
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

	/* (non-Javadoc)
	 * @see org.openedit.entermedia.orders.OrderManager#toggleItemInOrder(org.openedit.entermedia.MediaArchive, org.openedit.entermedia.orders.Order, org.openedit.entermedia.Asset)
	 */

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
	protected TemplateWebEmail getMail() 
	{
		PostMail mail = (PostMail)getModuleManager().getBean( "postMail");
		return mail.getTemplateWebEmail();
	}

	protected void sendOrderNotifications(MediaArchive inArchive, Order inOrder) 
	{
		Map context = new HashMap();
		context.put("orderid", inOrder.getId());
		context.put("order", inOrder);

		String publishid = inOrder.get("publishdestination");
		String appid = inOrder.get("applicationid");
		if(publishid != null){
			Data dest = inArchive.getSearcherManager().getData(inArchive.getCatalogId(), "publishdestination", publishid);
			String email = dest.get("administrativeemail");
			if(email != null){
				sendEmail(context, email, "/${appid}/views/activity/email/admintemplate.html");
				//TODO: Save the fact that email was sent back to the publishtask?
			}
		}
		String emailto = inOrder.get('sharewithemail');
		String notes = inOrder.get('sharenote');

		if( inOrder.getRecentOrderHistory().getItemErrorCount() == 0)
		{
			if(emailto != null) {
				String expireson=inOrder.get("expireson");
				if ((expireson!=null) && (expireson.trim().length()>0)) {
					Date date = DateStorageUtil.getStorageUtil().parseFromStorage(expireson);
					context.put("expiresondate", date);
					context.put("expiresformat", new SimpleDateFormat("MMM dd, yyyy"));
				}

				sendEmail(context, emailto, "/${appid}/views/activity/email/sharetemplate.html");
			}
		}
		if( "download" != inOrder.get("ordertype") )
		{
			String userid = inOrder.get("userid");
			if(userid != null)
			{
				User muser = inArchive.getUserManager().getUser(userid);
				if(muser != null)
				{
					String owneremail = muser.getEmail();
					if(owneremail != null)
					{
						context.put("sharewithemail", emailto);
						sendEmail(context, owneremail, "/${appid}/views/activity/email/usertemplate.html");
					}
				}
			}
		}
		inOrder.setProperty('emailsent', 'true');
	}


	protected void sendEmail(Map pageValues, String email, String templatePage){
		//send e-mail
		//Page template = getPageManager().getPage(templatePage);
		RequestUtils rutil = getModuleManager().getBean("requestUtils");
		BaseWebPageRequest newcontext = rutil.createVirtualPageRequest(templatePage,null, null); 
		
		newcontext.putPageValues(pageValues);

		TemplateWebEmail mailer = getMail();
		mailer.loadSettings(newcontext);
		mailer.setMailTemplatePath(templatePage);
		mailer.setRecipientsFromCommas(email);
		//mailer.setMessage(inOrder.get("sharenote"));
		//mailer.setWebPageContext(context);
		mailer.send();
		log.info("email sent to ${email}");
	}

	public void saveOrderHistory(MediaArchive inArchive, OrderHistory inHistory,Order inOrder ){
		Searcher orderHistorySearcher = inArchive.getSearcher("orderhistory");
		inOrder.setRecentOrderHistory(inHistory);
		inHistory.setProperty("orderid", inOrder.getId());
		inHistory.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		orderHistorySearcher.saveData(inHistory, null);
	}


	protected ModuleManager getModuleManager()
	{
		return fieldModuleManager;
	}

	public void setModuleManager(ModuleManager inManager)
	{
		fieldModuleManager = inManager;
	}
	protected PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inManager)
	{
		fieldPageManager = inManager;
	}	
}
