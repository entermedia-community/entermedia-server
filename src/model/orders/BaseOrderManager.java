package model.orders;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.Asset;
import org.entermediadb.asset.CompositeAsset;
import org.entermediadb.asset.MediaArchive;
import org.entermediadb.asset.orders.Order;
import org.entermediadb.asset.orders.OrderDownload;
import org.entermediadb.asset.orders.OrderManager;
import org.entermediadb.email.PostMail;
import org.entermediadb.email.TemplateWebEmail;
import org.entermediadb.email.WebEmail;
import org.openedit.BaseWebPageRequest;
import org.openedit.CatalogEnabled;
import org.openedit.Data;
import org.openedit.ModuleManager;
import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.data.QueryBuilder;
import org.openedit.data.Searcher;
import org.openedit.data.SearcherManager;
import org.openedit.event.EventManager;
import org.openedit.event.WebEvent;
import org.openedit.hittracker.HitTracker;
import org.openedit.hittracker.SearchQuery;
import org.openedit.locks.Lock;
import org.openedit.locks.LockManager;
import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.profile.UserProfile;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.users.UserManager;
import org.openedit.util.DateStorageUtil;
import org.openedit.util.RequestUtils;

public class BaseOrderManager implements OrderManager, CatalogEnabled {
	private static final Log log = LogFactory.getLog(BaseOrderManager.class);
	protected SearcherManager fieldSearcherManager;
	protected EventManager fieldEventManager;
	protected LockManager fieldLockManager;
	protected ModuleManager fieldModuleManager;
	protected PageManager fieldPageManager;
	protected String fieldCatalogId;
	
	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	public void setCatalogId(String inCatalogId)
	{
		fieldCatalogId = inCatalogId;
	}

	protected MediaArchive getMediaArchive()
	{
		MediaArchive archive = (MediaArchive)getModuleManager().getBean(getCatalogId(),"mediaArchive");
		return archive;
	}
	
	public EventManager getEventManager() {
		return fieldEventManager;
	}

	public void setEventManager(EventManager inEventManager) {
		fieldEventManager = inEventManager;
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
		event.setCatalogId(inCatlogId);
		event.setUser(inUser);
		getEventManager().fireEvent(event);

		return order;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.orders.OrderManager#findOrdersForUser(java.lang.String, org.openedit.users.User)
	 */

	public HitTracker findOrdersForUser(String inCatlogId, User inUser) {
		Searcher ordersearcher = getSearcherManager().getSearcher(inCatlogId, "order");
		SearchQuery query = ordersearcher.createSearchQuery();
		//query.addOrsGroup("orderstatus","ordered finalizing complete"); //Open ones
		GregorianCalendar cal = new GregorianCalendar();
		cal.add(Calendar.MONTH, -3);
		query.addAfter("date", cal.getTime());
		query.addSortBy("dateDown");
		query.addExact("userid", inUser.getId());
		return ordersearcher.search(query);
	}
	

	public HitTracker findDownloadOrdersForUser(WebPageRequest inReq, String inCatlogId, User inUser) 
	{
		Searcher ordersearcher = getSearcherManager().getSearcher(inCatlogId, "order");
		SearchQuery query = ordersearcher.createSearchQuery();
		query.setName("downloadorders");
		//query.addOrsGroup("orderstatus","ordered finalizing complete"); //Open ones
		query.addExact("ordertype", "download");
		GregorianCalendar cal = new GregorianCalendar();
		cal.add(Calendar.MONTH, -3);
		query.addAfter("date", cal.getTime());
		query.addSortBy("dateDown");
		query.addExact("userid", inUser.getId());
		HitTracker tracker = ordersearcher.cachedSearch(inReq,query);
		log.info("Searching " + tracker);
		return tracker;
	}
	
	
	//Delete
	public boolean hasPendingDownloadForUser(WebPageRequest inPage, String inCatlogId, User inUser) {
		Searcher ordersearcher = getSearcherManager().getSearcher(inCatlogId, "order");
		QueryBuilder query = ordersearcher.query();
		query.named("hasdownloadorders");
		query.exact("ordertype", "download");
		query.not("orderstatus", "complete"); //Open ones 
		query.not("orderstatus", "preorder"); //Open ones
		
//		GregorianCalendar cal = new GregorianCalendar();
//		cal.add(Calendar.MONTH, -3);
//		query.addAfter("date", cal.getTime());
		query.exact("userid", inUser.getId());
		Data one = query.searchOne(inPage);
		if( one != null)
		{
			return true;
		}
		return false;
	}
	//Delete
	public HitTracker findUnshownDownloadOrdersForUser(WebPageRequest inPage, String inCatlogId, User inUser) {
		Searcher ordersearcher = getSearcherManager().getSearcher(inCatlogId, "order");
		SearchQuery query = ordersearcher.createSearchQuery();
		query.setName("unshowndownloadorders");
		query.addExact("orderstatus", "complete"); //Open ones
		query.addExact("ordertype", "download");
		query.addExact("downloadedstatus", "new"); //Open ones 
		
//		GregorianCalendar cal = new GregorianCalendar();
//		cal.add(Calendar.MONTH, -3);
//		query.addAfter("date", cal.getTime());
		query.addSortBy("dateDown");
		query.addExact("userid", inUser.getId());
		return ordersearcher.search(query);
	}


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.orders.OrderManager#findOrderItems(org.openedit.WebPageRequest, java.lang.String, org.entermediadb.asset.orders.Order)
	 */

	public HitTracker findOrderItems(WebPageRequest inReq, String inCatalogid,  Order inOrder) {
		return findOrderItems(inReq, inCatalogid,inOrder.getId() );
	}
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.orders.OrderManager#findOrderItems(org.openedit.WebPageRequest, java.lang.String, java.lang.String)
	 */

	public HitTracker findOrderItems(WebPageRequest inReq, String inCatalogid, String inOrderId) {
		Searcher itemsearcher = getSearcherManager().getSearcher(inCatalogid, "orderitem");
		SearchQuery query = itemsearcher.createSearchQuery();
		query.addExact("orderid", inOrderId);
		query.setHitsName("orderitems");
		query.setCatalogId(inCatalogid);
		
		HitTracker items =  itemsearcher.cachedSearch(inReq, query);
		return items;
	}
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.orders.OrderManager#findOrderAssets(java.lang.String, java.lang.String)
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
	 * @see org.entermediadb.asset.orders.OrderManager#findAssets(org.openedit.WebPageRequest, java.lang.String, org.entermediadb.asset.orders.Order)
	 */

	public HitTracker findAssets(WebPageRequest inReq, String inCatalogid, Order inOrder) {
		HitTracker items =  findOrderItems(inReq, inCatalogid, inOrder);
		if( items.size() == 0) {
			//log.info("No items");
			return null;
		}
		Collection ids = new ArrayList();
		for (Iterator iterator = items.iterator(); iterator.hasNext();) {
			Data hit = (Data) iterator.next();
			ids.add(hit.get("assetid"));
		}

		Searcher assetsearcher = getSearcherManager().getSearcher(inCatalogid, "asset");
		SearchQuery query = assetsearcher.query().all().getQuery();
		query.setHitsName("orderassets");
		query.setProperty("orderid",inOrder.getId());
		query.setSecurityIds(ids);
		query.setSecurityAttached(true);
		inReq.setRequestParameter("hitssessionid", "none");
		HitTracker hits = assetsearcher.search(query);
		
		String check = inReq.findValue("clearmissing");
		if (Boolean.parseBoolean(check))
		{
			//Make sure these have the same number of assets found
			if( hits.size() != items.size() )
			{
				items.enableBulkOperations();
				
				Set assetids = new HashSet();
				hits.enableBulkOperations();
				for (Iterator iterator = hits.iterator(); iterator.hasNext();)
				{
					Data data = (Data) iterator.next();
					assetids.add(data.getId());
				}
				List allitems = new ArrayList(items);
				List todelete = new ArrayList();
				Searcher itemsearcher = getSearcherManager().getSearcher(inCatalogid, "orderitem");
				for (Iterator iterator = allitems.iterator(); iterator.hasNext();)
				{
					Data item = (Data) iterator.next();
					if( !assetids.contains( item.get("assetid") ) )
					{
						//asset deleted, remove it
						//itemsearcher.delete(item, null);
						todelete.add(item);
					}
				}
				itemsearcher.deleteAll(todelete, null);
				hits = assetsearcher.search(query);
			}
		}
		
		return hits;
	}


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.orders.OrderManager#findOrderHistory(java.lang.String, org.entermediadb.asset.orders.Order)
	 */

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.orders.OrderManager#loadOrder(java.lang.String, java.lang.String)
	 */

	public Order loadOrder(String catalogid, String orderid) {
		Searcher ordersearcher = getSearcherManager().getSearcher(catalogid, "order");
		Order order =  (Order) ordersearcher.searchById(orderid);
		return order;
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.orders.OrderManager#createOrder(java.lang.String, org.openedit.WebPageRequest, boolean)
	 */

	public Order createOrder(String catalogid, WebPageRequest inReq, boolean saveitems) {
		Searcher searcher = getSearcherManager().getSearcher(catalogid, "order");
		Order order = (Order)searcher.createNewData();
		String[] fields = inReq.getRequestParameters("field");
		searcher.updateData(inReq, fields, order);
		/*
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
		*/
		
			
			saveOrder(catalogid, inReq.getUser(), order);
		//}
		return order;
	}


	/* (non-Javadoc)
	 * @see org.entermediadb.asset.orders.OrderManager#saveItems(java.lang.String, org.openedit.WebPageRequest, java.lang.String[], java.lang.String[])
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
	 * @see org.entermediadb.asset.orders.OrderManager#createNewOrder(java.lang.String, java.lang.String, java.lang.String)
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

	@Override
	public Order findOrderFromAssets(String inCatId, User inUser, List inAssetids)
	{
		Searcher ordersearcher = getSearcherManager().getSearcher(inCatId, "order");
		Order order = (Order)ordersearcher.query().exact("userid",inUser.getId()).andgroup("orderassetids", inAssetids).searchOne(); //complete?
		return order;
	}
	
	/* (non-Javadoc)
	 * @see org.entermediadb.asset.orders.OrderManager#removeItemFromOrder(java.lang.String, org.entermediadb.asset.orders.Order, org.entermediadb.asset.Asset)
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
	 * @see org.entermediadb.asset.orders.OrderManager#addItemToOrder(java.lang.String, org.entermediadb.asset.orders.Order, org.entermediadb.asset.Asset, java.util.Map)
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
	 * @see org.entermediadb.asset.orders.OrderManager#saveOrder(java.lang.String, org.openedit.users.User, org.entermediadb.asset.orders.Order)
	 */

	public void saveOrder(String inCatalogId, User inUser, Order inBasket)
	{
		Searcher orderearcher = getSearcherManager().getSearcher(inCatalogId, "order");
		if( inBasket.getInt("itemcount") == 0)
		{
			Searcher itemsearcher = getSearcherManager().getSearcher(inCatalogId, "orderitem");
			HitTracker items = itemsearcher.query().exact("orderid",inBasket.getId()).hitsPerPage(1).search();
			
			inBasket.setValue("itemcount",items.size());
			//setValue("itemsuccesscount",inRecentOrderHistory.getValue("itemsuccesscount"));
		}		
		orderearcher.saveData(inBasket, inUser );
	}

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.orders.OrderManager#placeOrder(org.openedit.WebPageRequest, org.entermediadb.asset.MediaArchive, org.entermediadb.asset.orders.Order, boolean)
	 */

	public void placeOrder(WebPageRequest inReq, MediaArchive inArchive, Order inOrder, boolean inResetId)
	{
		Searcher itemsearcher = getSearcherManager().getSearcher(inArchive.getCatalogId(), "orderitem");
		Searcher orderseacher = getSearcherManager().getSearcher(inArchive.getCatalogId(), "order");

		HitTracker all = itemsearcher.fieldSearch("orderid", inOrder.getId());
		List tosave = new ArrayList();
		if(inResetId){
			inOrder.setId(null);
			inOrder.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
			inOrder.setProperty("basket","false");

		}
		saveOrder(inArchive.getCatalogId(), inReq.getUser(), inOrder);

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

		WebEvent event = new WebEvent();
		event.setSearchType("order");
		event.setCatalogId(inArchive.getCatalogId());
		event.setOperation("orderplaced");
		event.setUser(inReq.getUser());
		event.setSource(this);
		event.setSourcePath(inOrder.getSourcePath());
		event.setProperty("orderid", inOrder.getId());
		inArchive.getEventManager().fireEvent(event);
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
		//Searcher taskSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "conversiontask");
		//Searcher presets = getSearcherManager().getSearcher(archive.getCatalogId(), "convertpreset");
		Searcher publishQueueSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "publishqueue");
		Searcher orderItemSearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "orderitem");

		log.info("Processing " + hits.size() + " order items ");
		String publishstatus = "new";
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

			String changepreview = inReq.getRequestParameter("changepreview");
			if (changepreview != null) {
				if (changepreview.equals("original")) {
					presetid = "0";
				}
				else {
					if("0".equals(presetid)) {
						presetid = null;
					}
				}
			}
			else{
				String newpresetid = properties.get(orderitemhit.getId() + ".presetid.value");
				if (newpresetid != null) {
					presetid = newpresetid;
				}
			}
			if(presetid == ""){
				presetid = null;
			}
			if(presetid == null){
				String rendertype = null;
				if( presetid == null)
				{
					String type = asset.get("assettype");
					if( type ==  null)
					{
						type = "none";
					}
					presetid = properties.get(type + ".presetid.value");
				}
				if( presetid == null)
				{
					rendertype = archive.getMediaRenderType(asset.getFileFormat());
					presetid = properties.get(rendertype + ".presetid.value");
				}

				if( presetid == null )
				{
					presetid = properties.get("presetid.value");
				}
				
				if( presetid == null )
				{
					presetid = inReq.findValue("presetid");
				}
				
				if( presetid == null ) {
					presetid = "thumbimage";
					rendertype = archive.getMediaRenderType(asset.getFileFormat());
					if (rendertype != null) {
						if(rendertype.equals("image")) {
			  	   	    	presetid = "largeimage";
						}
						else if (rendertype.equals("video")) {
							presetid = "videohls";
						}
						else if (rendertype.equals("document")) {
							presetid = "largedocumentpreview";
						}
					}
				}
			}
			
				
			Data orderItem = (Data) orderItemSearcher.searchById(orderitemhit.getId());
			if (orderItem == null)
			{
				log.info("Unknown error: unable to find " + orderitemhit.getId() + " in orderitem table, skipping");
				//update what table exactly?
				continue;
			}

			orderItem.setProperty("presetid", presetid);
			Data preset = archive.getCachedData("convertpreset", presetid);
			if( preset != null)
			{
				String path = archive.asLinkToDownload(asset, preset);
				orderItem.setProperty("itemdownloadurl", path);
			}			

			//orderItem.setProperty("downloaditemstatus", "pending");
			orderItem.setProperty("itemsourcepath", asset.getSourcePath());
			String exportname = archive.asExportFileName(asset,preset);
			orderItem.setProperty("itemexportname", exportname);

			//make sure task exists
			if( "0".equals( presetid) )
			{
				Page orig = archive.getOriginalDocument(asset);
				orderItem.setProperty("itemfilepath", orig.getPath());
				if(orig.exists() )
				{
					orderItem.setValue("downloaditemtotalfilesize", orig.length());
				}				
			}
			else
			{
				Data conversiontask = archive.query("conversiontask").exact("assetid",asset).exact("presetid",presetid).searchOne();
				if( conversiontask == null )
				{
					conversiontask = archive.getSearcher("conversiontask").createNewData();
					conversiontask.setValue("assetid",asset.getId());
					conversiontask.setValue("presetid",presetid);
					conversiontask.setValue("status","new");
					archive.getSearcher("conversiontask").saveData(conversiontask);
				}
				//Check output file for existance
				String generatedfilename = "/WEB-INF/data/" + archive.getCatalogId() + "/generated/" + asset.getSourcePath() + "/" + preset.get("generatedoutputfile");
				orderItem.setProperty("itemfilepath", generatedfilename);
				if( "complete".equals( conversiontask.get("status") ) )
				{
					ContentItem output = archive.getContent(generatedfilename);
					if(output.exists() )
					{
						orderItem.setValue("downloaditemtotalfilesize", output.getLength());
					}
					else
					{
						conversiontask.setValue("status","retry");
						archive.getSearcher("conversiontask").saveData(conversiontask);
					}
				}
			}
			
//			if( "preview".equals(presetid) )   //Browser download never touches publishing
//			{
//				orderItemSearcher.saveData(orderItem, inUser);
//				continue;
//			}

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
			//Data dest = getSearcherManager().getData(archive.getCatalogId(), "publishdestination", destination);

			//Data publishqueuerow = checkPublishing();
			//orderItem.setProperty("publishqueueid",publishqeuerow.getId());
			
			orderItem.setProperty("publishdestination", destination);
			if( orderItem.getValue("downloaditemtotalfilesize") != null )
			{
				publishstatus = "readytopublish";
			}
			orderItem.setProperty("publishstatus", publishstatus);   ////new readytopublish publishing publishingexternal complete error excluded
			orderItem.setValue("publishstartdate", new Date());

			orderItemSearcher.saveData(orderItem, inUser);
			
		}
		order.setOrderStatus("processing");
		saveOrder(archive.getCatalogId(), inUser, order);
		archive.fireSharedMediaEvent("conversions/runconversions");
		archive.fireSharedMediaEvent("publishing/publishassets"); //If conversions are already done
		return assetids;
	}

	/*
	protected Data checkPublishing()
	{

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
		//Data preset = (Data) presets.searchById(presetid);
		if( preset == null)
		{
			throw new OpenEditException("Preset missing " + presetid);
		}
		publishqeuerow.setProperty("exportname", exportname);
		publishqeuerow.setProperty("status", publishstatus);
		publishqeuerow.setValue("user", inUser.getId());
		publishqeuerow.setSourcePath(asset.getSourcePath());
		publishqeuerow.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		publishQueueSearcher.saveData(publishqeuerow, inUser);

		if( publishqeuerow.getId() == null )
		{
			throw new OpenEditException("Id should not be null");
		}
		return null;
	}
	
	public Data createPublishQueue(MediaArchive archive, User inUser, Asset inAsset, String inPresetId, String inPublishDestination) {
		return createPublishQueue(archive, inUser, inAsset, inPresetId, inPublishDestination, true);
	}
	
	public Data createPublishQueue(MediaArchive archive, User inUser, Asset inAsset, String inPresetId, String inPublishDestination, boolean force)
	{

		String publishstatus = "new";
		Searcher publishQueueSearcher = archive.getSearcher("publishqueue");
		Data publishqeuerow = null;
		
		if(force) {
			publishqeuerow = publishQueueSearcher.createNewData();
		} else {
			publishqeuerow = publishQueueSearcher.query().exact("assetid", inAsset.getId()).exact("presetid", inPresetId).exact("publishdestination", inPublishDestination).searchOne();
			if(publishqeuerow != null) {
				if("complete".equals(publishqeuerow.get("status"))){
					return publishqeuerow;
				} else if("error".equals(publishqeuerow.get("status"))) {
					publishqeuerow.setValue("status", "new");
				}
				else {
				
					publishqeuerow =  publishQueueSearcher.createNewData();
				}
			} else {
				publishqeuerow =  publishQueueSearcher.createNewData();

			}
		}

		publishqeuerow.setProperty("assetid", inAsset.getId());
		publishqeuerow.setProperty("assetsourcepath", inAsset.getSourcePath() );

		publishqeuerow.setProperty("publishdestination", inPublishDestination);
		publishqeuerow.setProperty("presetid", inPresetId);

		Data preset = (Data) archive.getData("convertpreset", inPresetId);
		if( preset == null)
		{
			throw new OpenEditException("Preset missing " + inPresetId);
		}
		String exportname = archive.asExportFileName(inUser, inAsset, preset);
		publishqeuerow.setProperty("exportname", exportname);
		
		if( inPresetId.equals("0"))
		{
			ContentItem item = archive.getOriginalContent(inAsset);
			if( item.exists() )
			{
				publishstatus = "new";
			}
			else
			{
				publishstatus = "error";
				publishqeuerow.setProperty("errordetails","Original does not exists");
			}
		}
		publishqeuerow.setProperty("status", publishstatus);

		publishqeuerow.setSourcePath(inAsset.getSourcePath());
		publishqeuerow.setProperty("date", DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		publishQueueSearcher.saveData(publishqeuerow, inUser);

		if( publishqeuerow.getId() == null )
		{
			throw new OpenEditException("Id should not be null");
		}
		return publishqeuerow;
	}
	*/

	/* (non-Javadoc)
	 * @see org.entermediadb.asset.orders.OrderManager#getPresetForOrderItem(java.lang.String, org.openedit.Data)
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
	 * @see org.entermediadb.asset.orders.OrderManager#getPublishDestinationForOrderItem(java.lang.String, org.openedit.Data)
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
	 * @see org.entermediadb.asset.orders.OrderManager#updateStatus(org.entermediadb.asset.MediaArchive, org.entermediadb.asset.orders.Order)
	 */
	public void updateStatus(MediaArchive archive, Order inOrder)
	{
		//Finalize should be only for complete orders.
		if( "checkout".equals( inOrder.get("ordertype")) )
		{
			String status = inOrder.get("checkoutstatus");
			if( status == null || status.equals("pending"))
			{
				log.debug("Order not approved for email yet " + inOrder.getId());
				return; //dont send email yet
			}
		}
		
		//look up all the tasks
		//if all done then save order status
		Lock lock = archive.getLockManager().lockIfPossible("orders" + inOrder.getId(), "BaseOrderManager");
		if( lock == null)
		{
			log.info("Order locked already " + inOrder.getId());
			return;
		}
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
			HitTracker itemhits =  itemsearcher.search(query); //not cached

			int size = itemhits.size();
			if( size == 0)
			{
				log.error("No items on order "  + inOrder.getId() + " " + inOrder.getOrderStatus() );
				//error?
				Date date = inOrder.getDate("date");
				if(date == null){
					date = new Date();

					inOrder.setValue("date", date);
					archive.getSearcher("order").saveData(inOrder);
				}
				inOrder.setValue("itemcount",0);
				Date monthold = new Date(System.currentTimeMillis() - (1000*60*60*24*30));
				if( date.before(monthold))
				{
					inOrder.setValue("orderstatus", "complete");
					inOrder.setValue("orderstatusdetails", "order expired after one month");
					archive.saveData("order",inOrder);
					//saveOrder(archive.getCatalogId(), null, inOrder);
				}
				return;
			}

			int itemsuccesscount = 0;
			int itemerrorcount = 0;

			for (Iterator iterator = itemhits.iterator(); iterator.hasNext();)
			{
				Data orderitemhit = (Data) iterator.next();
				if( "complete".equals( orderitemhit.get("publishstatus") ) )
				{
					itemsuccesscount++;
				}
				else if( "cancelled".equals( orderitemhit.get("publishstatus") ) )
				{
					itemsuccesscount++;
				}
				else if ("error".equals( orderitemhit.get("publishstatus") ) )
				{
					itemerrorcount++;
				}
			}
			//If changed then save history and update order
			inOrder.setValue("itemerrorcount",itemerrorcount);
			inOrder.setValue("itemcount",itemhits.size());
			inOrder.setValue("itemsuccesscount",itemsuccesscount);
			
			if((itemerrorcount + itemsuccesscount) == itemhits.size() )
			{
				inOrder.setOrderStatus("complete");
				saveOrder(archive.getCatalogId(), null, inOrder);
				try
				{
					sendOrderNotifications(archive, inOrder);
				}
				catch( Exception ex)
				{
					log.error("Could not send order notification" , ex);
					inOrder.setOrderStatus("complete",": could not send notification " + ex );
				}
			}
			else
			{
				saveOrder(archive.getCatalogId(), null, inOrder);
			}
		}
		finally
		{
			archive.releaseLock(lock);
		}
	}
	
	public void updateReadyStatus(MediaArchive archive, Order inOrder)
	{
		
		//look up all the tasks
		//if all done then save order status
		Lock lock = archive.getLockManager().lockIfPossible("orders" + inOrder.getId(), "BaseOrderManager");
		if( lock == null)
		{
			return;
		}
		try
		{
			if( inOrder.getOrderStatus() == "complete" );
			{
				
				inOrder.setValue("downloadedstatus", "complete");  //false will not notify again
				saveOrder(archive.getCatalogId(), null, inOrder);
				
			}

		}
		finally
		{
			archive.releaseLock(lock);
		}
	}

	protected Data findPublishQuue(MediaArchive archive, Order inHistory, Data orderitemhit)
	{
		//Go find if there are any publishing or conversion errors
		//If both are done then mark as complete

		//Publishassets.groovy updates the publish status if there is a error in conversions 

		boolean publishcomplete = false;
		String publishqueueid = orderitemhit.get("publishqueueid");
		if( publishqueueid == null)
		{
			return null;
		}
		else
		{
			Data publish = (Data)archive.getSearcher("publishqueue").searchById(publishqueueid);
			return publish;
		}
	}

	public void updatePendingOrders(MediaArchive archive)
	{
		Searcher ordersearcher = getSearcherManager().getSearcher(archive.getCatalogId(), "order");
		SearchQuery query = ordersearcher.createSearchQuery();
		query.addOrsGroup("orderstatus","processing"); //pending is depreacted
		Collection hits = ordersearcher.search(query);
		if( !hits.isEmpty())
		{
			
		}
		for (Iterator iterator = hits.iterator(); iterator.hasNext();)
		{
			Data hit = (Data) iterator.next();
			Order order = loadOrder(archive.getCatalogId(), hit.getId());
			if( order == null)
			{
				log.error("Invalid order: " + archive.getCatalogId() + hit.getId());
				continue;
			}
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
		if(inAssetId == null){
			return false;
		}
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
		//context.put("user", inArchive.getUser)
		String publishid = inOrder.get("publishdestination");
		String appid = inOrder.get("applicationid");
		if(publishid != null){
			Data dest = inArchive.getSearcherManager().getData(inArchive.getCatalogId(), "publishdestination", publishid);
			if(dest != null){
			String email = dest.get("administrativeemail");
			if(email != null){
				sendEmail(inArchive.getCatalogId(),context, email, "/" + appid + "/theme/emails/admintemplate.html");
				//TODO: Save the fact that email was sent back to the publishtask?
			}
			}
		}
		String emailto = inOrder.get("sharewithemail");
		//String notes = inOrder.get("sharenote");
		if(emailto != null) 
		{
			if( inOrder.getInt("itemerrorcount") == 0)
			{
				String expireson=inOrder.get("expireson");
				if ((expireson!=null) && (expireson.trim().length()>0))
				{
					Date date = DateStorageUtil.getStorageUtil().parseFromStorage(expireson);
					context.put("expiresondate", date);
					context.put("expiresformat", new SimpleDateFormat("MMM dd, yyyy"));
				}
				String template = null;
						
				if( "checkout".equals( inOrder.get("ordertype")) )
				{
					template = "/" + appid + "/theme/emails/checkouttemplate.html";
				}
				else
				{
					template = "/" + appid + "/theme/emails/sharetemplate.html";
				}
				
				sendEmail(inArchive.getCatalogId(),context, emailto, template);
			}
		}	
		
//		if( !"download".equals( inOrder.get("ordertype") ) )
//		{
//			String userid = inOrder.get("userid");
//			if(userid != null)
//			{
//				User muser = inArchive.getUserManager().getUser(userid);
//				if(muser != null)
//				{
//					String owneremail = muser.getEmail();
//					if(owneremail != null)
//					{
//						context.put("sharewithemail", emailto); //Why would we need this
//						sendEmail(inArchive.getCatalogId(),context, owneremail, "/" + appid + "/theme/emails/usertemplate.html");
//					}
//				}
//			}
//		}
		inOrder.setValue("emailsent", true);
	}


	protected void sendEmail(String inCatalogId, Map pageValues, String email, String templatePage){
		//send e-mail
		//Page template = getPageManager().getPage(templatePage);
		RequestUtils rutil = (RequestUtils) getModuleManager().getBean("requestUtils");
		User user = (User) getSearcherManager().getData(inCatalogId,"user","admin");
		UserProfile profile = (UserProfile) getSearcherManager().getData(inCatalogId,"userprofile","admin");
		BaseWebPageRequest newcontext = (BaseWebPageRequest) rutil.createVirtualPageRequest(templatePage,user,profile); 
		newcontext.putPageValues(pageValues);

		TemplateWebEmail mailer = getMail();
		mailer.loadSettings(newcontext);
		mailer.setMailTemplatePath(templatePage);
		mailer.setRecipientsFromCommas(email);
		//mailer.setMessage(inOrder.get("sharenote"));
		//mailer.setWebPageContext(context);
		mailer.send();
		log.info("email sent to :" + email);
	}

	public void sendEmailForApproval(String inCatalogId, MediaArchive inArchive, UserManager userManager, String inAppId, Order inOrder)
	{
		String email = inArchive.getCatalogSettingValue("requestapproveremail");
		if (email == null || (email != null && email.isEmpty()))
		{
			throw new OpenEditException("No approver email provided, please contact your administrator");
		}

		User followerUser = (User) userManager.getUserByEmail(email);
		if (followerUser == null)
		{
			throw new OpenEditException("The approver email (" + email  + ") is not linked to any active account, please contact your administrator");
		}
		
		
		RequestUtils rutil = (RequestUtils) getModuleManager().getBean("requestUtils");
		UserProfile profile = (UserProfile) getSearcherManager().getData(inCatalogId,"userprofile","admin");
		String template = "/" + inAppId + "/theme/emails/checkoutrequesttemplate.html";
		WebEmail templatemail = inArchive.createSystemEmail(followerUser, template);

		Map context = new HashMap();
		BaseWebPageRequest newcontext = (BaseWebPageRequest) rutil.createVirtualPageRequest(template,followerUser,profile); 
		newcontext.putPageValues(context);

		templatemail.loadSettings(newcontext);
	    Map objects = new HashMap();
	    
	    objects.put("mediaarchive",inArchive);
	    objects.put("order",inOrder);
	    templatemail.send(objects);
	    log.info("Sent approval request to " + email);
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


	public HitTracker findPendingCheckoutOrders(WebPageRequest inReq, String inCatlogId) 
	{
		Searcher ordersearcher = getSearcherManager().getSearcher(inCatlogId, "order");
		SearchQuery query = ordersearcher.createSearchQuery();
		query.setName("pendingorders");
		//query.addOrsGroup("orderstatus","ordered finalizing complete"); //Open ones
		query.addExact("checkoutstatus", "pending");
		query.addSortBy("dateDown");
		return ordersearcher.cachedSearch(inReq,query);
	}

//downloadedstatus
	public void changeStatus(Order inOrder, String inOrderStatus, String downloadedstatus)
	{
		Lock lock = getMediaArchive().getLockManager().lock("orders" + inOrder.getId(), "BaseOrderManager");
		try
		{
			if(inOrderStatus != null)
			{
				inOrder.setOrderStatus(inOrderStatus);
				inOrder.setValue( "orderstatusdetails","Order " + inOrderStatus );
			}

			if( downloadedstatus != null)
			{
				inOrder.setValue("downloadedstatus",downloadedstatus );
			}
			getMediaArchive().saveData("order", inOrder);

			if( inOrderStatus.equals("canceled"))
			{
				Collection items = inOrder.findOrderAssets();
				for (Iterator iterator = items.iterator(); iterator.hasNext();) {
					Data item = (Data) iterator.next();
					item.setValue("publishstatus","cancelled");
				}
				getMediaArchive().getSearcher("orderitem").saveAllData(items, null);
			}
		}
		finally
		{
			getMediaArchive().getLockManager().release(lock);
		}

	}
	
	public Collection<OrderDownload> findDownloadOrdersForUser(WebPageRequest inReq, User inUser)
	{
		HitTracker orders = findDownloadOrdersForUser(inReq, getCatalogId(), inUser);

		Searcher ordersearcher = getMediaArchive().getSearcher("order");
		
		Collection<OrderDownload> orderdownloads = new ArrayList<OrderDownload>();
		
		for (Iterator iterator = orders.getPageOfHits().iterator(); iterator.hasNext();)
		{
			Data data = (Data) iterator.next();
			Order order = (Order)ordersearcher.loadData(data);
			OrderDownload odownload = new OrderDownload();
			odownload.setOrder(order);
			HitTracker items = getMediaArchive().query("orderitem").exact("orderid",order).search();
			odownload.setItemList(items);
			orderdownloads.add(odownload);
		}
		
		return orderdownloads;
	}

	@Override
	public void saveOrder(MediaArchive inArchive, Order inOrder) {
		inArchive.saveData("orde", inOrder);
		
	}


}
